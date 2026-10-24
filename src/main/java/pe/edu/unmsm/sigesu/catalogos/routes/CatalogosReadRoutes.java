package pe.edu.unmsm.sigesu.catalogos.routes;

import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.application.services.BootstrapService;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosReadService;
import pe.edu.unmsm.sigesu.identidad.services.Actor;

@RestController
public class CatalogosReadRoutes {
  private final Actor actor;
  private final CatalogosReadService reads;
  private final BootstrapService bootstrap;

  public CatalogosReadRoutes(Actor actor, CatalogosReadService reads, BootstrapService bootstrap) {
    this.actor = actor;
    this.reads = reads;
    this.bootstrap = bootstrap;
  }

  @GetMapping("/api/catalogos")
  public Object catalogs(@RequestParam(required = false) String include) {
    var user = actor.require();
    return include == null ? bootstrap.catalogs() : reads.catalogs(user, include);
  }
}
