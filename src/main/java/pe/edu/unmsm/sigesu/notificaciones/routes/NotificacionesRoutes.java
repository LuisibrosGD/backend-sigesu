package pe.edu.unmsm.sigesu.notificaciones.routes;

import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.identidad.services.Actor;
import pe.edu.unmsm.sigesu.notificaciones.schemas.NotificacionesSchemas.*;
import pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesService;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionesRoutes {
  private final pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesReadService reads;
  private final Actor actor;
  private final NotificacionesService service;

  public NotificacionesRoutes(
      Actor a,
      NotificacionesService s,
      pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesReadService reads) {
    this.reads = reads;
    actor = a;
    service = s;
  }

  @GetMapping
  public Object list() {
    return service.list(actor.require());
  }

  @GetMapping("/pagina")
  public Object page(@RequestParam java.util.Map<String, String> filters) {
    return reads.notifications(actor.require(), filters);
  }

  @GetMapping("/resumen")
  public Object summary() {
    return reads.notificationSummary(actor.require());
  }

  @PatchMapping("/lectura")
  public Object read(@RequestBody Lectura p) {
    return service.read(actor.require(), p.ids(), p.leida());
  }
}
