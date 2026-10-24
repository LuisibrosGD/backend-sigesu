package pe.edu.unmsm.sigesu.identidad.routes;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.identidad.schemas.IdentidadSchemas.*;
import pe.edu.unmsm.sigesu.identidad.services.*;

@RestController
@RequestMapping("/api/auth")
public class IdentidadRoutes {
  private final AuthenticationManager manager;
  private final Actor actor;
  private final IdentidadService service;
  private final Projections views;

  public IdentidadRoutes(AuthenticationManager m, Actor a, IdentidadService s, Projections v) {
    manager = m;
    actor = a;
    service = s;
    views = v;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @PostMapping("/login")
  public Object login(
      @Valid @RequestBody Login p, HttpServletRequest req, HttpServletResponse res) {
    var auth =
        manager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(p.email(), p.password()));
    if (req.getSession(false) != null) req.getSession(false).invalidate();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    new HttpSessionSecurityContextRepository().saveContext(context, req, res);
    return views.usuario(actor.require());
  }

  @PostMapping("/register")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object register(@Valid @RequestBody Registro p) {
    return views.usuario(service.register(p));
  }

  @GetMapping("/me")
  public Object me() {
    return views.usuario(actor.require());
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest req) {
    if (req.getSession(false) != null) req.getSession(false).invalidate();
    SecurityContextHolder.clearContext();
  }

  @PutMapping("/profile")
  public Object profile(@Valid @RequestBody Perfil p) {
    return views.usuario(service.profile(actor.require(), p));
  }

  @PostMapping("/verify-password")
  public Object verify(@Valid @RequestBody Verificar p) {
    return Map.of("valid", service.verify(actor.require(), p.currentPassword()));
  }

  @PutMapping("/password")
  public void password(@Valid @RequestBody Clave p) {
    service.password(actor.require(), p);
  }
}
