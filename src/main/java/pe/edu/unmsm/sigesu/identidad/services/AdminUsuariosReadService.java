package pe.edu.unmsm.sigesu.identidad.services;

import static pe.edu.unmsm.sigesu.common.ReadPages.*;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesReadService;

@Service
@Transactional(readOnly = true)
public class AdminUsuariosReadService {
  private final UsuariosReadRepository repository;
  private final Projections views;
  private final SolicitudesReadService requests;

  public AdminUsuariosReadService(
      UsuariosReadRepository repository, Projections views, SolicitudesReadService requests) {
    this.repository = repository;
    this.views = views;
    this.requests = requests;
  }

  public Object adminUsers(Usuario u, Map<String, String> f) {
    Rules.admin(u);
    var p = repository.page(page(f));
    return envelope(p, p.stream().map(views::usuario).toList());
  }

  public Object pendingWork(Usuario u, String id) {
    Rules.admin(u);
    return requests.solicitudes(
        u, Map.of("tecnicoId", id, "pendienteTecnico", "true", "size", "100"));
  }
}
