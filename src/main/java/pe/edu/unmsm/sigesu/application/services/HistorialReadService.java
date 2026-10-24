package pe.edu.unmsm.sigesu.application.services;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.application.repository.HistorialReadRepository;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;

@Service
@Transactional(readOnly = true)
public class HistorialReadService {
  private final HistorialReadRepository repository;

  public HistorialReadService(HistorialReadRepository repository) {
    this.repository = repository;
  }

  public Object history(Usuario actor, Map<String, String> filters) {
    Rules.admin(actor);
    return repository.history(filters);
  }
}
