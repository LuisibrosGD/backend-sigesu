package pe.edu.unmsm.sigesu.solicitudes.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EventoRepository extends JpaRepository<Evento, String> {
  List<Evento> findBySolicitudIdOrderByFechaAsc(String id);

  List<Evento> findByReservaIdOrderByFechaAsc(String id);

  List<Evento> findBySolicitudIdInOrderByFechaAsc(Collection<String> ids);

  List<Evento> findByReservaIdInOrderByFechaAsc(Collection<String> ids);

  @Query(
      "select e from Evento e where e.solicitudId in :solicitudes or e.reservaId in :reservas order"
          + " by e.fecha asc")
  List<Evento> findForReferences(
      @org.springframework.data.repository.query.Param("solicitudes")
          Collection<String> solicitudes,
      @org.springframework.data.repository.query.Param("reservas") Collection<String> reservas);
}
