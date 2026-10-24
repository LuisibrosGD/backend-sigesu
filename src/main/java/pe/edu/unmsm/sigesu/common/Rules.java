package pe.edu.unmsm.sigesu.common;

import java.time.*;
import java.util.*;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;
import pe.edu.unmsm.sigesu.solicitudes.repository.Solicitud;

public final class Rules {
  public static final ZoneId LIMA = ZoneId.of("America/Lima");

  public static String id() {
    return UUID.randomUUID().toString();
  }

  public static void check(boolean ok, String message) {
    if (!ok) throw ApiException.bad(message);
  }

  public static boolean role(Usuario u, String... roles) {
    return u.activo && Arrays.asList(roles).contains(u.rol);
  }

  public static void admin(Usuario u) {
    if (!role(u, "Administrador")) throw ApiException.denied();
  }

  public static boolean view(Usuario u, Solicitud s) {
    return role(u, "Administrador")
        || role(u, "Estudiante") && u.id.equals(s.solicitanteId)
        || role(u, "Técnico") && u.id.equals(s.tecnicoId)
        || role(u, "Supervisor") && u.areasIds.contains(s.areaId);
  }

  public static boolean supervise(Usuario u, Solicitud s) {
    return role(u, "Administrador") || role(u, "Supervisor") && u.areasIds.contains(s.areaId);
  }

  public static boolean creator(Usuario u) {
    return role(u, "Estudiante", "Supervisor", "Administrador");
  }

  public static String priority(String p) {
    check(Set.of("Baja", "Media", "Alta", "Crítica").contains(p), "Prioridad inválida.");
    return p;
  }

  private Rules() {}
}
