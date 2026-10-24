package pe.edu.unmsm.sigesu.application.services;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesService;
import pe.edu.unmsm.sigesu.reservas.services.ReservasService;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesService;

@Service
public class BootstrapService {
  private final pe.edu.unmsm.sigesu.identidad.services.Actor actor;
  private final BatchProjections batch;
  private final Projections v;
  private final CatalogosService c;
  private final UsuarioRepository users;
  private final SolicitudesService requests;
  private final ReservasService reservations;
  private final NotificacionesService notices;

  public BootstrapService(
      pe.edu.unmsm.sigesu.identidad.services.Actor actor,
      BatchProjections batch,
      Projections v,
      CatalogosService c,
      UsuarioRepository u,
      SolicitudesService s,
      ReservasService r,
      NotificacionesService n) {
    this.actor = actor;
    this.batch = batch;
    this.v = v;
    this.c = c;
    users = u;
    requests = s;
    reservations = r;
    notices = n;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> catalogs() {
    return batch.load(List.of(), List.of(), List.of()).catalogs();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> bootstrap() {
    return bootstrap(actor.require());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> bootstrap(Usuario actor) {
    var visibleUsers = users.findAllWithAreas();
    var solicitudes = requests.visible(actor);
    var reservas = reservations.visible(actor);
    var snapshot = batch.load(solicitudes, reservas, visibleUsers);
    var m = snapshot.catalogs();
    m.put("version", 1);
    m.put("ocupaciones", reservations.occupancy(actor));
    m.put(
        "usuarios",
        visibleUsers.stream()
            .filter(
                u ->
                    u.id.equals(actor.id)
                        || Rules.role(actor, "Administrador")
                        || Rules.role(actor, "Supervisor")
                            && Rules.role(u, "Técnico", "Supervisor")
                            && !Collections.disjoint(u.areasIds, actor.areasIds))
            .map(v::usuario)
            .toList());
    m.put("solicitudes", solicitudes.stream().map(s -> snapshot.solicitud(s, actor)).toList());
    m.put("reservas", reservas.stream().map(snapshot::reserva).toList());
    m.put("notificaciones", notices.list(actor).stream().map(v::plain).toList());
    m.put("currentUser", v.usuario(actor));
    return m;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> publicData() {
    var edificios = c.listEdificios();
    var facultades =
        c.listFacultades().stream()
            .map(
                f -> {
                  var m = v.plain(f);
                  m.put(
                      "edificios",
                      edificios.stream()
                          .filter(e -> f.id.equals(e.facultadId))
                          .map(v::plain)
                          .toList());
                  return m;
                })
            .toList();
    return Map.of("facultades", facultades, "configuracion", v.plain(c.config()));
  }
}
