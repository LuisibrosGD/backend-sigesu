package pe.edu.unmsm.sigesu.notificaciones.schemas;

import java.util.*;

public final class NotificacionesSchemas {
  public record Lectura(Set<String> ids, boolean leida) {}
}
