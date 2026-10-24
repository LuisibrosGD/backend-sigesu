package pe.edu.unmsm.sigesu.notificaciones.services;

import java.time.*;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.notificaciones.repository.*;
import pe.edu.unmsm.sigesu.solicitudes.repository.*;

@Service
public class NotificacionesService {
  private final NotificacionRepository repo;
  private final UsuarioRepository users;
  private final SolicitudRepository solicitudes;
  private final EventoRepository history;

  public NotificacionesService(
      NotificacionRepository r, UsuarioRepository u, SolicitudRepository s, EventoRepository h) {
    repo = r;
    users = u;
    solicitudes = s;
    history = h;
  }

  public void send(
      String uid,
      String tipo,
      String refTipo,
      String refId,
      String code,
      String message,
      String unique) {
    var n = new Notificacion();
    n.id = Rules.id();
    n.usuarioId = uid;
    n.tipo = tipo;
    n.titulo = code;
    n.mensaje = message;
    n.fecha = Instant.now();
    n.referenciaTipo = refTipo;
    n.referenciaId = refId;
    n.referenciaCodigo = code;
    n.claveUnicaVencimiento = unique;
    repo.save(n);
  }

  public Set<String> recipients(Solicitud s, boolean internal) {
    Set<String> ids = new HashSet<>();
    if (!internal) ids.add(s.solicitanteId);
    if (s.tecnicoId != null) ids.add(s.tecnicoId);
    users.findByRolAndActivoTrue("Supervisor").stream()
        .filter(u -> u.areasIds.contains(s.areaId))
        .forEach(u -> ids.add(u.id));
    return ids;
  }

  public void solicitud(Solicitud s, String tipo, String message, boolean internal) {
    recipients(s, internal)
        .forEach(id -> send(id, tipo, "SOLICITUD", s.id, s.codigo, message, null));
  }

  public void event(
      Usuario actor,
      String solicitud,
      String reserva,
      String accion,
      String before,
      String after,
      String motivo) {
    var e = new Evento();
    e.id = Rules.id();
    e.actorId = actor.id;
    e.actorNombre = actor.nombre;
    e.actorRol = actor.rol;
    e.solicitudId = solicitud;
    e.reservaId = reserva;
    e.accion = accion;
    e.estadoAnterior = before;
    e.estadoNuevo = after;
    e.motivo = motivo;
    e.fecha = Instant.now();
    history.save(e);
  }

  public List<Notificacion> list(Usuario u) {
    return repo.findByUsuarioIdOrderByFechaDesc(u.id);
  }

  @Transactional
  public List<String> read(Usuario actor, Set<String> ids, boolean leida) {
    var selected =
        repo.findByUsuarioIdOrderByFechaDesc(actor.id).stream()
            .filter(n -> ids == null || ids.contains(n.id))
            .filter(n -> n.leida != leida)
            .toList();
    selected.forEach(n -> n.leida = leida);
    repo.saveAll(selected);
    return selected.stream().map(n -> n.id).toList();
  }

  @Scheduled(fixedDelayString = "${app.overdue-interval-ms:3600000}")
  @Transactional
  public void overdue() {
    var now = Instant.now();
    for (var candidate : solicitudes.findAll()) {
      if (Set.of("Resuelta", "Cerrada").contains(candidate.estado)
          || !candidate.fechaLimite.isBefore(now)) continue;
      var s = solicitudes.lockById(candidate.id).orElseThrow();
      for (var uid : recipients(s, false)) {
        String key = "venc-" + s.id + "-" + uid;
        if (!repo.existsByClaveUnicaVencimiento(key))
          send(
              uid,
              "VENCIMIENTO_SOLICITUD",
              "SOLICITUD",
              s.id,
              s.codigo,
              "La solicitud superó su fecha límite de atención.",
              key);
      }
    }
  }
}
