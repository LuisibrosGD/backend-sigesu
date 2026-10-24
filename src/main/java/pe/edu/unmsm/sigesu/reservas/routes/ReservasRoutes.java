package pe.edu.unmsm.sigesu.reservas.routes;

import jakarta.validation.Valid;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.identidad.services.Actor;
import pe.edu.unmsm.sigesu.reservas.schemas.ReservasSchemas.*;
import pe.edu.unmsm.sigesu.reservas.services.ReservasReadService;
import pe.edu.unmsm.sigesu.reservas.services.ReservasService;

@RestController
@RequestMapping("/api/reservas")
public class ReservasRoutes {
  private final ReservasReadService reads;
  private final Actor actor;
  private final ReservasService service;
  private final Projections views;

  public ReservasRoutes(ReservasReadService reads, Actor a, ReservasService s, Projections v) {
    this.reads = reads;
    actor = a;
    service = s;
    views = v;
  }

  @GetMapping
  public Object list(@RequestParam Map<String, String> filters) {
    return reads.reservas(actor.require(), filters);
  }

  @GetMapping("/ocupaciones")
  public Object occupancy(@RequestParam Map<String, String> f) {
    return f.isEmpty() ? service.occupancy(actor.require()) : reads.occupants(actor.require(), f);
  }

  @GetMapping("/{id}")
  public Object get(@PathVariable String id) {
    return views.reserva(service.get(actor.require(), id));
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object create(@Valid @RequestBody Nueva p) {
    return views.reserva(service.create(actor.require(), p));
  }

  @PostMapping("/{id}/acciones/{action}")
  public Object action(
      @PathVariable String id, @PathVariable String action, @RequestBody Accion p) {
    return views.reserva(service.action(actor.require(), id, action, p));
  }
}
