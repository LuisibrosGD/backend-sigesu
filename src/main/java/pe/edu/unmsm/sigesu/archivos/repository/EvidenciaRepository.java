package pe.edu.unmsm.sigesu.archivos.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EvidenciaRepository extends JpaRepository<Evidencia, String> {
  List<Evidencia> findBySolicitudId(String id);

  List<Evidencia> findBySolicitudIdIn(Collection<String> ids);
}
