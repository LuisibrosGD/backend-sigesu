package pe.edu.unmsm.sigesu.reservas.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository
    extends JpaRepository<Reserva, String>, JpaSpecificationExecutor<Reserva> {
  List<Reserva> findByEstadoIn(Collection<String> estados);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Reserva e where e.id = :id")
  Optional<Reserva> lockById(@Param("id") String id);

  List<Reserva> findByAmbienteIdAndFechaAndEstadoIn(
      String ambiente, LocalDate fecha, Collection<String> estados);

  @Query(
      "select r from Reserva r, Ambiente a where a.id = r.ambienteId and (:role = 'Administrador'"
          + " or (:role = 'Estudiante' and r.solicitanteId = :uid) or (:role = 'Supervisor' and"
          + " (r.solicitanteId = :uid or a.areaId in :areas)))")
  List<Reserva> findVisible(
      @Param("role") String role,
      @Param("uid") String uid,
      @Param("areas") Collection<String> areas);
}
