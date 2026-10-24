package pe.edu.unmsm.sigesu.notificaciones.services;

import static pe.edu.unmsm.sigesu.common.ReadPages.*;

import jakarta.persistence.criteria.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.notificaciones.repository.*;

@Service
@Transactional(readOnly = true)
public class NotificacionesReadService {
  private final NotificacionRepository notices;
  private final Projections views;

  public NotificacionesReadService(NotificacionRepository notices, Projections views) {
    this.notices = notices;
    this.views = views;
  }

  public Object notificationSummary(Usuario u) {
    return Map.of(
        "total",
        notices.count((r, q, cb) -> cb.equal(r.get("usuarioId"), u.id)),
        "noLeidas",
        notices.count(
            (r, q, cb) -> cb.and(cb.equal(r.get("usuarioId"), u.id), cb.isFalse(r.get("leida")))));
  }

  public Object notifications(Usuario u, Map<String, String> f) {
    Specification<Notificacion> spec =
        (r, q, cb) -> {
          var p = new ArrayList<Predicate>();
          p.add(cb.equal(r.get("usuarioId"), u.id));
          if ("UNREAD".equals(f.get("filterRead"))) p.add(cb.isFalse(r.get("leida")));
          String type = f.getOrDefault("filterType", "ALL");
          if (type.equals("SOLICITUDES"))
            p.add(
                r.get("tipo")
                    .in("REGISTRO_SOLICITUD", "ASIGNACION_SOLICITUD", "CAMBIO_ESTADO_SOLICITUD"));
          else if (type.equals("RESERVAS")) p.add(cb.equal(r.get("referenciaTipo"), "RESERVA"));
          else if (type.equals("COMENTARIOS")) p.add(cb.like(r.get("tipo"), "%COMENTARIO%"));
          else if (type.equals("VENCIMIENTOS")) p.add(cb.like(r.get("tipo"), "%VENCIMIENTO%"));
          q.orderBy(cb.desc(r.get("fecha")), cb.asc(r.get("id")));
          return cb.and(p.toArray(Predicate[]::new));
        };
    var p = notices.findAll(spec, page(f));
    return envelope(p, p.stream().map(views::plain).toList());
  }
}
