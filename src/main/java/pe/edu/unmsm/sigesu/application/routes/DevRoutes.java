package pe.edu.unmsm.sigesu.application.routes;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.application.services.DevResetService;
import pe.edu.unmsm.sigesu.identidad.services.Actor;

@RestController
@Profile("dev")
public class DevRoutes {
  private final Actor actor;
  private final DevResetService service;

  public DevRoutes(Actor a, DevResetService s) {
    actor = a;
    service = s;
  }

  @PostMapping("/api/admin/reset-demo")
  public void reset() throws Exception {
    service.reset(actor.require());
  }
}
