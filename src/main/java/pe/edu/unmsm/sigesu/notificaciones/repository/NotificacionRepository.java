package pe.edu.unmsm.sigesu.notificaciones.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface NotificacionRepository
    extends JpaRepository<Notificacion, String>, JpaSpecificationExecutor<Notificacion> {
  List<Notificacion> findByUsuarioIdOrderByFechaDesc(String id);

  boolean existsByClaveUnicaVencimiento(String clave);
}
