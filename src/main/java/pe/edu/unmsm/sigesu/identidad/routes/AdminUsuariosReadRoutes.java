package pe.edu.unmsm.sigesu.identidad.routes;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.identidad.services.*;

@RestController
public class AdminUsuariosReadRoutes {
  private final Actor actor;
  private final AdminUsuariosReadService reads;

  public AdminUsuariosReadRoutes(Actor actor, AdminUsuariosReadService reads) {
    this.actor = actor;
    this.reads = reads;
  }

  @GetMapping("/api/admin/usuarios")
  public Object users(@RequestParam Map<String, String> f) {
    return reads.adminUsers(actor.require(), f);
  }

  @GetMapping("/api/admin/usuarios/{id}/pendientes")
  public Object pending(@PathVariable String id) {
    return reads.pendingWork(actor.require(), id);
  }
}
