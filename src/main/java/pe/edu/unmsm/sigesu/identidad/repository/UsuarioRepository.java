package pe.edu.unmsm.sigesu.identidad.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository
    extends JpaRepository<Usuario, String>, JpaSpecificationExecutor<Usuario> {
  @Override
  @EntityGraph(attributePaths = "areasIds")
  Optional<Usuario> findById(String id);

  @EntityGraph(attributePaths = "areasIds")
  Optional<Usuario> findByEmailIgnoreCase(String email);

  boolean existsByCodigoUniversitarioIgnoreCase(String codigo);

  List<Usuario> findByRolAndActivoTrue(String rol);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Usuario e where e.id = :id")
  Optional<Usuario> lockById(@Param("id") String id);

  @EntityGraph(attributePaths = "areasIds")
  @Query("select distinct u from Usuario u")
  List<Usuario> findAllWithAreas();

  @EntityGraph(attributePaths = "areasIds")
  @Query("select distinct u from Usuario u where u.id in :ids")
  List<Usuario> findWithAreasByIds(@Param("ids") Collection<String> ids);

  @EntityGraph(attributePaths = "areasIds")
  @Query(
      "select distinct u from Usuario u left join u.areasIds a where u.activo=true and u.rol in"
          + " ('Técnico','Supervisor') and (:admin=true or a in :areas)")
  List<Usuario> findStaff(@Param("admin") boolean admin, @Param("areas") Collection<String> areas);
}
