package pe.edu.unmsm.sigesu.solicitudes.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SolicitudRepository
    extends JpaRepository<Solicitud, String>, JpaSpecificationExecutor<Solicitud> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Solicitud e where e.id = :id")
  Optional<Solicitud> lockById(@Param("id") String id);

  @Query(
      "select s from Solicitud s where :role = 'Administrador' or (:role = 'Estudiante' and"
          + " s.solicitanteId = :uid) or (:role = 'Técnico' and s.tecnicoId = :uid) or (:role ="
          + " 'Supervisor' and s.areaId in :areas)")
  List<Solicitud> findVisible(
      @Param("role") String role,
      @Param("uid") String uid,
      @Param("areas") Collection<String> areas);
}
