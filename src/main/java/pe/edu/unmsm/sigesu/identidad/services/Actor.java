package pe.edu.unmsm.sigesu.identidad.services;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;

@Service
public class Actor {
  private final UsuarioRepository users;

  public Actor(UsuarioRepository users) {
    this.users = users;
  }

  public Usuario require() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) throw ApiException.denied();
    return users.findById(auth.getName()).filter(u -> u.activo).orElseThrow(ApiException::denied);
  }
}
