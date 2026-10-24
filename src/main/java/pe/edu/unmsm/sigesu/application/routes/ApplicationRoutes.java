package pe.edu.unmsm.sigesu.application.routes;

import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.application.services.*;

@RestController
public class ApplicationRoutes {
  private final BootstrapService app;

  public ApplicationRoutes(BootstrapService b) {
    app = b;
  }

  @GetMapping("/api/bootstrap")
  public Object bootstrap() {
    return app.bootstrap();
  }

  @GetMapping("/api/public/configuracion")
  public Object publicData() {
    return app.publicData();
  }
}
