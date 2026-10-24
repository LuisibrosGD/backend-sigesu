package pe.edu.unmsm.sigesu.reservas.services;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.catalogos.repository.Ambiente;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesService;
import pe.edu.unmsm.sigesu.reservas.repository.*;
import pe.edu.unmsm.sigesu.reservas.schemas.ReservasSchemas.*;

@Service
public class ReservasService {
  private final ReservaRepository repo;
  private final CatalogosService cats;
  private final UsuarioRepository users;
  private final NotificacionesService events;
  private final Codigos codes;

  public ReservasService(
      ReservaRepository r,
      CatalogosService c,
      UsuarioRepository u,
      NotificacionesService e,
      Codigos co) {
    repo = r;
    cats = c;
    users = u;
    events = e;
    codes = co;
  }

  public boolean canView(Usuario u, Reserva r) {
    var a = cats.findAmbiente(r.ambienteId).orElseThrow();
    return Rules.role(u, "Administrador")
        || Rules.role(u, "Supervisor")
            && (u.areasIds.contains(a.areaId) || u.id.equals(r.solicitanteId))
        || Rules.role(u, "Estudiante") && u.id.equals(r.solicitanteId);
  }

  public List<pe.edu.unmsm.sigesu.reservas.schemas.ReservasSchemas.Ocupacion> occupancy(
      Usuario actor) {
    if (!Rules.creator(actor)) return List.of();
    return repo.findByEstadoIn(List.of("Solicitada", "Aprobada")).stream()
        .map(
            r ->
                new pe.edu.unmsm.sigesu.reservas.schemas.ReservasSchemas.Ocupacion(
                    r.id, r.ambienteId, r.fecha, r.horaInicio, r.horaFin, r.estado))
        .toList();
  }

  public List<Reserva> visible(Usuario u) {
    if (!u.activo) return List.of();
    return repo.findVisible(u.rol, u.id, u.areasIds.isEmpty() ? Set.of("") : u.areasIds);
  }

  public Reserva get(Usuario u, String id) {
    var r = repo.findById(id).orElseThrow(ApiException::missing);
    if (!canView(u, r)) throw ApiException.denied();
    return r;
  }

  public void available(
      Ambiente a, LocalDate date, LocalTime start, LocalTime end, int count, String exclude) {
    Rules.check(a.activo, "Ambiente inactivo.");
    var today = LocalDate.now(Rules.LIMA);
    Rules.check(
        date.isAfter(today)
            && !date.isAfter(today.plusDays(cats.config().diasMaximosAntelacionReserva)),
        "Fecha fuera del rango de reserva permitido.");
    Rules.check(
        end.isAfter(start) && !start.isBefore(a.horarioInicio) && !end.isAfter(a.horarioFin),
        "Horario fuera de disponibilidad.");
    Rules.check(
        count >= 1 && count <= a.capacidad, "La cantidad de participantes supera el aforo.");
    boolean clash =
        repo
            .findByAmbienteIdAndFechaAndEstadoIn(a.id, date, List.of("Solicitada", "Aprobada"))
            .stream()
            .anyMatch(
                r ->
                    !r.id.equals(exclude)
                        && start.isBefore(r.horaFin)
                        && end.isAfter(r.horaInicio));
    if (clash) throw ApiException.conflict("El ambiente ya está reservado en ese horario.");
  }

  @Transactional
  public Reserva create(Usuario u, Nueva p) {
    if (!Rules.creator(u)) throw ApiException.denied();
    var a = cats.lockAmbiente(p.ambienteId()).orElseThrow(ApiException::missing);
    available(a, p.fecha(), p.horaInicio(), p.horaFin(), p.cantidadParticipantes(), null);
    var r = new Reserva();
    r.id = Rules.id();
    r.codigo = codes.next("RES");
    r.ambienteId = a.id;
    r.solicitanteId = u.id;
    r.fecha = p.fecha();
    r.horaInicio = p.horaInicio();
    r.horaFin = p.horaFin();
    r.motivo = p.motivo().trim();
    r.descripcionActividad = p.descripcionActividad().trim();
    r.cantidadParticipantes = p.cantidadParticipantes();
    r.estado = "Solicitada";
    r.fechaRegistro = Instant.now();
    repo.save(r);
    events.event(u, null, r.id, "Registro de reserva", null, r.estado, null);
    notify(r, a);
    return r;
  }

  @Transactional
  public Reserva action(Usuario u, String id, String action, Accion p) {
    var current = get(u, id);
    var a = cats.lockAmbiente(current.ambienteId).orElseThrow();
    var r = repo.lockById(id).orElseThrow();
    String before = r.estado;
    String reason = null;
    if (action.equals("cancelar")) {
      if (!(Rules.role(u, "Administrador") || u.id.equals(r.solicitanteId)))
        throw ApiException.denied();
      Rules.check(
          r.fecha.isAfter(LocalDate.now(Rules.LIMA))
              && Set.of("Solicitada", "Aprobada").contains(r.estado),
          "Solo se pueden cancelar reservas futuras activas.");
      r.estado = "Cancelada";
      reason = p.motivoCancelacion();
    } else {
      if (!(Rules.role(u, "Administrador")
          || Rules.role(u, "Supervisor") && u.areasIds.contains(a.areaId)))
        throw ApiException.denied();
      Rules.check(r.estado.equals("Solicitada"), "La reserva ya fue evaluada.");
      switch (action) {
        case "aprobar" -> {
          available(a, r.fecha, r.horaInicio, r.horaFin, r.cantidadParticipantes, r.id);
          r.estado = "Aprobada";
          reason = p.observacion();
        }
        case "rechazar" -> {
          Rules.check(
              p.motivoRechazo() != null && p.motivoRechazo().trim().length() >= 5,
              "Indique el motivo de rechazo.");
          r.estado = "Rechazada";
          reason = p.motivoRechazo();
        }
        default -> throw ApiException.bad("Acción desconocida.");
      }
      r.aprobadoPorId = u.id;
    }
    r.motivoRechazoOCancelacion = reason;
    repo.save(r);
    events.event(u, null, id, action, before, r.estado, reason);
    notify(r, a);
    return r;
  }

  private void notify(Reserva r, Ambiente a) {
    Set<String> ids = new HashSet<>();
    ids.add(r.solicitanteId);
    users.findByRolAndActivoTrue("Supervisor").stream()
        .filter(u -> u.areasIds.contains(a.areaId))
        .forEach(u -> ids.add(u.id));
    ids.forEach(
        uid ->
            events.send(
                uid, "RESERVA_ESTADO", "RESERVA", r.id, r.codigo, "Reserva " + r.estado, null));
  }
}
