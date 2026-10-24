package pe.edu.unmsm.sigesu.identidad.services;

import java.time.*;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.identidad.schemas.IdentidadSchemas.*;

@Service
public class IdentidadService {
  private final UsuarioRepository users;
  private final PasswordEncoder passwords;
  private final CatalogosService catalogs;

  public IdentidadService(
      UsuarioRepository users, PasswordEncoder passwords, CatalogosService catalogs) {
    this.users = users;
    this.passwords = passwords;
    this.catalogs = catalogs;
  }

  @Transactional
  public Usuario register(Registro p) {
    Rules.check(
        catalogs.config().permitirRegistroPublico, "El registro público está deshabilitado.");
    var u = new Usuario();
    u.id = Rules.id();
    u.nombre = p.nombre().trim();
    u.email = p.email().trim().toLowerCase(Locale.ROOT);
    u.codigoUniversitario = p.codigoUniversitario().trim();
    u.facultadId = p.facultadId();
    Rules.check(
        catalogs.findFacultad(u.facultadId).filter(f -> f.activo).isPresent(),
        "Facultad inválida.");
    u.telefono = p.telefono();
    u.cargo = "Estudiante Universitario";
    u.passwordHash = passwords.encode(p.password());
    u.rol = "Estudiante";
    u.activo = true;
    u.fechaRegistro = Instant.now();
    return users.saveAndFlush(u);
  }

  @Transactional
  public Usuario profile(Usuario actor, Perfil p) {
    var u = users.lockById(actor.id).orElseThrow(ApiException::missing);
    u.nombre = p.nombre().trim();
    u.telefono = p.telefono().trim();
    u.cargo = p.cargo().trim();
    return users.save(u);
  }

  public boolean verify(Usuario u, String p) {
    return passwords.matches(p, u.passwordHash);
  }

  @Transactional
  public void password(Usuario actor, Clave p) {
    var u = users.lockById(actor.id).orElseThrow(ApiException::missing);
    Rules.check(verify(u, p.currentPassword()), "La contraseña actual no es correcta.");
    u.passwordHash = passwords.encode(p.newPassword());
    users.save(u);
  }
}
