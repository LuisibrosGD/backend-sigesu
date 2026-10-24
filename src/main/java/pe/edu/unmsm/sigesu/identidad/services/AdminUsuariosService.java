package pe.edu.unmsm.sigesu.identidad.services;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.identidad.schemas.IdentidadSchemas.UsuarioInput;
import pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesService;
import pe.edu.unmsm.sigesu.solicitudes.repository.*;

@Service
public class AdminUsuariosService {
  private final UsuarioRepository users;
  private final SolicitudRepository requests;
  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwords;
  private final CatalogosService cats;
  private final NotificacionesService events;

  public AdminUsuariosService(
      UsuarioRepository u,
      SolicitudRepository s,
      JdbcTemplate j,
      PasswordEncoder p,
      CatalogosService c,
      NotificacionesService e) {
    users = u;
    requests = s;
    jdbc = j;
    passwords = p;
    cats = c;
    events = e;
  }

  @Transactional
  public Usuario save(Usuario actor, UsuarioInput p) {
    Rules.admin(actor);
    jdbc.queryForObject("select pg_advisory_xact_lock(713220)", Object.class);
    var u =
        p.id() == null ? new Usuario() : users.lockById(p.id()).orElseThrow(ApiException::missing);
    boolean fresh = u.id == null;
    if (!fresh
        && u.activo
        && u.rol.equals("Administrador")
        && (!p.activo() || !p.rol().equals("Administrador")))
      Rules.check(
          users.findByRolAndActivoTrue("Administrador").stream().anyMatch(a -> !a.id.equals(u.id)),
          "No se puede desactivar al último administrador.");
    for (var area : p.areasIds()) cats.area(area);
    if (p.facultadId() != null && !p.facultadId().isBlank())
      cats.findFacultad(p.facultadId()).orElseThrow(ApiException::missing);
    if (!fresh) {
      for (var candidate :
          requests.findAll().stream()
              .filter(
                  s ->
                      u.id.equals(s.tecnicoId)
                          && Set.of("Asignada", "En atención").contains(s.estado))
              .toList()) {
        var s = requests.lockById(candidate.id).orElseThrow();
        if (!p.activo() || !p.rol().equals("Técnico") || !p.areasIds().contains(s.areaId)) {
          var replacement =
              users
                  .findById(Objects.requireNonNullElse(p.reasignarTecnicoId(), ""))
                  .orElseThrow(
                      () ->
                          ApiException.bad(
                              "Seleccione un técnico de reemplazo para las solicitudes"
                                  + " pendientes."));
          Rules.check(
              !replacement.id.equals(u.id)
                  && Rules.role(replacement, "Técnico")
                  && replacement.areasIds.contains(s.areaId),
              "El reemplazo debe estar activo y pertenecer al área.");
          s.tecnicoId = replacement.id;
          requests.save(s);
          events.event(
              actor, s.id, null, "Reasignación por cambio de usuario", s.estado, s.estado, null);
          events.solicitud(s, "ASIGNACION_SOLICITUD", "Técnico responsable actualizado.", false);
        }
      }
    }
    if (fresh) {
      Rules.check(
          p.password() != null && !p.password().isBlank(), "Indique la contraseña inicial.");
      u.id = Rules.id();
      u.fechaRegistro = Instant.now();
    }
    if (p.password() != null && !p.password().isBlank())
      u.passwordHash = passwords.encode(p.password());
    u.codigoUniversitario = p.codigoUniversitario().trim();
    u.email = p.email().trim().toLowerCase(Locale.ROOT);
    u.nombre = p.nombre().trim();
    u.rol = p.rol();
    u.activo = p.activo();
    u.facultadId = p.facultadId() == null || p.facultadId().isBlank() ? null : p.facultadId();
    u.areasIds = new HashSet<>(p.areasIds());
    u.telefono = p.telefono();
    u.cargo = p.cargo();
    return users.saveAndFlush(u);
  }
}
