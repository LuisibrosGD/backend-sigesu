package pe.edu.unmsm.sigesu.indicadores.routes;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.identidad.services.Actor;
import pe.edu.unmsm.sigesu.indicadores.services.DashboardReadService;

@RestController
@RequestMapping("/api/dashboard")
public class IndicadoresRoutes {
  private final Actor actor;
  private final DashboardReadService service;

  public IndicadoresRoutes(Actor a, DashboardReadService s) {
    actor = a;
    service = s;
  }

  @GetMapping({"/resumen", "/indicadores"})
  public Object summary(@RequestParam Map<String, String> filters) {
    return service.summary(actor.require(), filters);
  }
}
