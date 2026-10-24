package pe.edu.unmsm.sigesu.application.services;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pe.edu.unmsm.sigesu.archivos.repository.EvidenciaRepository;
import pe.edu.unmsm.sigesu.archivos.services.EvidenceStorage;
import pe.edu.unmsm.sigesu.common.Rules;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;

@Service
@Profile("dev")
public class DevResetService {
  private final InitialData seed;
  private final EvidenciaRepository files;
  private final EvidenceStorage storage;

  public DevResetService(InitialData i, EvidenciaRepository f, EvidenceStorage s) {
    seed = i;
    files = f;
    storage = s;
  }

  public void reset(Usuario u) throws Exception {
    Rules.admin(u);
    for (var file : files.findAll()) {
      storage.delete(file.storagePath);
      files.deleteById(file.id);
    }
    seed.resetDemo();
  }
}
