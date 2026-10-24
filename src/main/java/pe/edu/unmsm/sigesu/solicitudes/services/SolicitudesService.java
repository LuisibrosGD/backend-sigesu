package pe.edu.unmsm.sigesu.solicitudes.services;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesService;
import pe.edu.unmsm.sigesu.solicitudes.repository.*;
import pe.edu.unmsm.sigesu.solicitudes.schemas.SolicitudesSchemas.*;

@Service
public class SolicitudesService {
  private final SolicitudRepository repo;
  private final ComentarioRepository comments;
  private final UsuarioRepository users;
  private final CatalogosService cats;
  private final NotificacionesService events;
  private final Codigos codes;

  public SolicitudesService(
      SolicitudRepository r,
      ComentarioRepository c,
      UsuarioRepository u,
      CatalogosService ca,
      NotificacionesService e,
      Codigos co) {
    repo = r;
    comments = c;
    users = u;
    cats = ca;
    events = e;
    codes = co;
  }

  public List<Solicitud> visible(Usuario u) {
    if (!u.activo) return List.of();
    return repo.findVisible(u.rol, u.id, u.areasIds.isEmpty() ? Set.of("") : u.areasIds);
  }

  public Solicitud get(Usuario u, String id) {
    var s = repo.findById(id).orElseThrow(ApiException::missing);
    if (!Rules.view(u, s)) throw ApiException.denied();
    return s;
  }

  @Transactional
  public Solicitud create(Usuario actor, Nueva p) {
    if (!Rules.creator(actor)) throw ApiException.denied();
    cats.ubicacion(p.facultadId(), p.edificioId());
    var c = cats.categoria(p.categoriaId());
    var s = new Solicitud();
    s.id = Rules.id();
    s.codigo = codes.next("SOL");
    s.solicitanteId = actor.id;
    s.categoriaId = c.id;
    s.areaId = c.areaId;
    s.titulo = p.titulo().trim();
    s.descripcion = p.descripcion().trim();
    s.facultadId = p.facultadId();
    s.edificioId = p.edificioId();
    s.ubicacionEspecifica = p.ubicacionEspecifica().trim();
    s.prioridad = Rules.priority(p.prioridad());
    s.fechaRegistro = Instant.now();
    s.fechaLimite = s.fechaRegistro.plus(Duration.ofHours(cats.plazo(s.prioridad)));
    s.estado = "Registrada";
    repo.save(s);
    events.event(actor, s.id, null, "Registro inicial de solicitud", null, s.estado, null);
    events.solicitud(s, "REGISTRO_SOLICITUD", "Solicitud registrada.", false);
    return s;
  }

  @Transactional
  public Solicitud edit(Usuario u, String id, Editar p) {
    var s = repo.lockById(id).orElseThrow(ApiException::missing);
    if (!(Rules.role(u, "Administrador")
        || Rules.role(u, "Estudiante") && u.id.equals(s.solicitanteId)))
      throw ApiException.denied();
    Rules.check(
        Set.of("Registrada", "En evaluación").contains(s.estado),
        "Esta solicitud ya no puede editarse.");
    cats.ubicacion(p.facultadId(), p.edificioId());
    var c = cats.categoria(p.categoriaId());
    s.categoriaId = c.id;
    s.areaId = c.areaId;
    s.titulo = p.titulo().trim();
    s.descripcion = p.descripcion().trim();
    s.facultadId = p.facultadId();
    s.edificioId = p.edificioId();
    s.ubicacionEspecifica = p.ubicacionEspecifica().trim();
    events.event(u, id, null, "Edición de contenido", s.estado, s.estado, null);
    return repo.save(s);
  }

  @Transactional
  public Solicitud action(Usuario u, String id, String action, Accion p) {
    var s = repo.lockById(id).orElseThrow(ApiException::missing);
    if (!Rules.view(u, s)) throw ApiException.denied();
    String before = s.estado;
    Instant now = Instant.now();
    String reason = p.motivo();
    switch (action) {
      case "evaluar" -> {
        if (!Rules.supervise(u, s)) throw ApiException.denied();
        Rules.check(!s.estado.equals("Cerrada"), "Solicitud cerrada.");
        var c = cats.categoria(p.categoriaId());
        if (!s.areaId.equals(c.areaId) && s.tecnicoId != null) {
          var t = users.findById(s.tecnicoId).orElseThrow();
          Rules.check(
              t.areasIds.contains(c.areaId),
              "Reasigne un técnico del área destino antes de recategorizar.");
        }
        s.categoriaId = c.id;
        s.areaId = c.areaId;
        s.prioridad = Rules.priority(p.prioridad());
        s.fechaLimite = s.fechaRegistro.plus(Duration.ofHours(cats.plazo(s.prioridad)));
        if (s.estado.equals("Registrada") && p.pasarAEnEvaluacion()) s.estado = "En evaluación";
        s.fechaEvaluacion = now;
        reason = p.motivoCambio();
      }
      case "asignar" -> {
        if (!Rules.supervise(u, s)) throw ApiException.denied();
        Rules.check(
            !Set.of("Cerrada", "Resuelta").contains(s.estado), "Estado inválido para asignar.");
        var t =
            users
                .findById(Objects.requireNonNullElse(p.tecnicoId(), ""))
                .orElseThrow(ApiException::missing);
        Rules.check(
            Rules.role(t, "Técnico") && t.areasIds.contains(s.areaId),
            "Seleccione un técnico activo del área responsable.");
        s.tecnicoId = t.id;
        s.fechaAsignacion = now;
        if (Set.of("Registrada", "En evaluación").contains(s.estado)) s.estado = "Asignada";
      }
      case "iniciar" -> {
        if (!(Rules.role(u, "Administrador")
            || Rules.role(u, "Técnico") && u.id.equals(s.tecnicoId))) throw ApiException.denied();
        Rules.check(
            s.estado.equals("Asignada") && s.tecnicoId != null,
            "La solicitud debe estar asignada.");
        s.estado = "En atención";
        s.fechaInicioAtencion = now;
        reason = p.notaInicial();
      }
      case "resolver" -> {
        if (!(Rules.role(u, "Administrador")
            || Rules.role(u, "Técnico") && u.id.equals(s.tecnicoId))) throw ApiException.denied();
        Rules.check(s.estado.equals("En atención"), "La solicitud debe estar en atención.");
        Rules.check(
            p.solucionDescripcion() != null && p.solucionDescripcion().trim().length() >= 10,
            "Describa la solución (mínimo 10 caracteres).");
        s.solucionDescripcion = p.solucionDescripcion().trim();
        s.estado = "Resuelta";
        s.fechaResolucion = now;
        s.atendidaFueraDePlazo = now.isAfter(s.fechaLimite);
        reason = s.solucionDescripcion;
      }
      case "cerrar", "devolver" -> {
        if (!Rules.supervise(u, s)) throw ApiException.denied();
        Rules.check(s.estado.equals("Resuelta"), "La solicitud debe estar resuelta.");
        if (action.equals("cerrar")) {
          s.estado = "Cerrada";
          s.fechaCierre = now;
          reason = p.comentarioCierre();
        } else {
          Rules.check(
              p.motivoDevolucion() != null && p.motivoDevolucion().trim().length() >= 5,
              "Indique el motivo de devolución.");
          s.estado = "En atención";
          s.fechaResolucion = null;
          reason = p.motivoDevolucion();
        }
      }
      default -> throw ApiException.bad("Acción desconocida.");
    }
    repo.save(s);
    events.event(u, id, null, action, before, s.estado, reason);
    events.solicitud(
        s,
        action.equals("asignar") ? "ASIGNACION_SOLICITUD" : "CAMBIO_ESTADO_SOLICITUD",
        "Actualización: " + s.estado,
        false);
    return s;
  }

  @Transactional
  public Comentario comment(Usuario u, String id, ComentarioInput p) {
    var s = repo.lockById(id).orElseThrow(ApiException::missing);
    if (!Rules.view(u, s) || p.esInterno() && Rules.role(u, "Estudiante"))
      throw ApiException.denied();
    Rules.check(!s.estado.equals("Cerrada"), "La solicitud está cerrada.");
    var c = new Comentario();
    c.id = Rules.id();
    c.solicitudId = id;
    c.autorId = u.id;
    c.texto = p.texto().trim();
    c.esInterno = p.esInterno();
    c.fecha = Instant.now();
    comments.save(c);
    events.solicitud(s, "COMENTARIO_SOLICITUD", "Nuevo comentario en la solicitud.", p.esInterno());
    return c;
  }
}
