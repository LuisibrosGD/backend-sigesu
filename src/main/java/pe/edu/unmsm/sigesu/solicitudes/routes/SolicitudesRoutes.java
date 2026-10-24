package pe.edu.unmsm.sigesu.solicitudes.routes;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.identidad.services.Actor;
import pe.edu.unmsm.sigesu.solicitudes.schemas.SolicitudesSchemas.*;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesReadService;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesService;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudesRoutes {
  private final SolicitudesReadService reads;
  private final Actor actor;
  private final SolicitudesService service;
  private final Projections views;

  public SolicitudesRoutes(
      SolicitudesReadService reads, Actor a, SolicitudesService s, Projections v) {
    this.reads = reads;
    actor = a;
    service = s;
    views = v;
  }

  @GetMapping
  public Object list(@RequestParam Map<String, String> filters) {
    return reads.solicitudes(actor.require(), filters);
  }

  @GetMapping("/exportar")
  public Object export(@RequestParam Map<String, String> f) {
    return reads.exportRequests(actor.require(), f);
  }

  @GetMapping("/{id}")
  public Object get(@PathVariable String id) {
    var u = actor.require();
    return views.solicitud(service.get(u, id), u);
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object create(@Valid @RequestBody Nueva p) {
    var u = actor.require();
    return views.solicitud(service.create(u, p), u);
  }

  @PutMapping("/{id}")
  public Object edit(@PathVariable String id, @Valid @RequestBody Editar p) {
    var u = actor.require();
    return views.solicitud(service.edit(u, id, p), u);
  }

  @PostMapping("/{id}/acciones/{action}")
  public Object action(
      @PathVariable String id, @PathVariable String action, @RequestBody Accion p) {
    var u = actor.require();
    return views.solicitud(service.action(u, id, action, p), u);
  }

  @PostMapping("/{id}/comentarios")
  public Object comment(@PathVariable String id, @Valid @RequestBody ComentarioInput p) {
    return views.comentario(service.comment(actor.require(), id, p));
  }
}
