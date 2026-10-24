package pe.edu.unmsm.sigesu.catalogos.routes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.catalogos.schemas.CatalogosSchemas.*;
import pe.edu.unmsm.sigesu.catalogos.services.AdminCatalogosService;
import pe.edu.unmsm.sigesu.identidad.schemas.IdentidadSchemas.UsuarioInput;
import pe.edu.unmsm.sigesu.identidad.services.*;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminRoutes {
  private final Actor actor;
  private final AdminCatalogosService c;
  private final AdminUsuariosService users;
  private final Projections views;

  public AdminRoutes(Actor a, AdminCatalogosService c, AdminUsuariosService u, Projections v) {
    actor = a;
    this.c = c;
    users = u;
    views = v;
  }

  @PostMapping("/usuarios")
  public Object user(@Valid @RequestBody UsuarioInput p) {
    return views.usuario(users.save(actor.require(), p));
  }

  @PostMapping("/areas")
  public Object area(@Valid @RequestBody AreaInput p) {
    return views.plain(c.area(actor.require(), p));
  }

  @PostMapping("/categorias")
  public Object category(@Valid @RequestBody CategoriaInput p) {
    return views.plain(c.categoria(actor.require(), p));
  }

  @PostMapping("/facultades")
  public Object faculty(@Valid @RequestBody FacultadInput p) {
    return views.plain(c.facultad(actor.require(), p));
  }

  @PostMapping("/ambientes")
  public Object room(@Valid @RequestBody AmbienteInput p) {
    return views.plain(c.ambiente(actor.require(), p));
  }

  @PutMapping("/prioridades")
  public Object priorities(@RequestBody @NotEmpty List<@Valid PrioridadInput> p) {
    return c.prioridades(actor.require(), p);
  }

  @PutMapping("/configuracion")
  public Object config(@Valid @RequestBody ConfigInput p) {
    return views.plain(c.config(actor.require(), p));
  }
}
