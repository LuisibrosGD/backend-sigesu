package pe.edu.unmsm.sigesu.application.routes;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.application.services.HistorialReadService;
import pe.edu.unmsm.sigesu.identidad.services.Actor;

@RestController
public class HistorialRoutes {
  private final Actor actor;
  private final HistorialReadService reads;

  public HistorialRoutes(Actor actor, HistorialReadService reads) {
    this.actor = actor;
    this.reads = reads;
  }

  @GetMapping("/api/admin/historial")
  public Object history(@RequestParam Map<String, String> f) {
    return reads.history(actor.require(), f);
  }
}
