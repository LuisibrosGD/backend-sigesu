package pe.edu.unmsm.sigesu.catalogos.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AmbienteRepository extends JpaRepository<Ambiente, String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Ambiente e where e.id = :id")
  Optional<Ambiente> lockById(@Param("id") String id);

  @EntityGraph(attributePaths = "equipamiento")
  @Query("select distinct a from Ambiente a")
  List<Ambiente> findAllWithEquipment();
}
