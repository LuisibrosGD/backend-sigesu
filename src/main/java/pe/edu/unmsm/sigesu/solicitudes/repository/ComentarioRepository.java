package pe.edu.unmsm.sigesu.solicitudes.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ComentarioRepository extends JpaRepository<Comentario, String> {
  List<Comentario> findBySolicitudIdOrderByFechaAsc(String id);

  List<Comentario> findBySolicitudIdInOrderByFechaAsc(Collection<String> ids);
}
