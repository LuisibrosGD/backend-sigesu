package pe.edu.unmsm.sigesu.reservas.repository;

import jakarta.persistence.criteria.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;

public class ReservaSpecifications {
  private ReservaSpecifications() {}

  public static Specification<Reserva> filter(Usuario u, Map<String, String> f) {
    return (r, q, cb) -> {
      var p = new ArrayList<Predicate>();
      var area = q.subquery(String.class);
      var a = area.from(pe.edu.unmsm.sigesu.catalogos.repository.Ambiente.class);
      area.select(a.get("id"))
          .where(u.areasIds.isEmpty() ? cb.disjunction() : a.get("areaId").in(u.areasIds));
      if (!u.activo) p.add(cb.disjunction());
      switch (u.rol) {
        case "Administrador" -> {}
        case "Estudiante" -> p.add(cb.equal(r.get("solicitanteId"), u.id));
        case "Supervisor" ->
            p.add(cb.or(cb.equal(r.get("solicitanteId"), u.id), r.get("ambienteId").in(area)));
        default -> p.add(cb.disjunction());
      }
      for (String k : List.of("estado", "ambienteId", "solicitanteId"))
        if (has(f, k)) p.add(cb.equal(r.get(k), f.get(k)));
      if ("true".equals(f.get("supervision")) && Rules.role(u, "Supervisor"))
        p.add(r.get("ambienteId").in(area));
      if ("true".equals(f.get("propias"))) p.add(cb.equal(r.get("solicitanteId"), u.id));
      if (has(f, "fechaDesde"))
        p.add(cb.greaterThanOrEqualTo(r.get("fecha"), LocalDate.parse(f.get("fechaDesde"))));
      if (has(f, "fechaHasta"))
        p.add(cb.lessThanOrEqualTo(r.get("fecha"), LocalDate.parse(f.get("fechaHasta"))));
      var today = LocalDate.now(Rules.LIMA);
      var historic =
          cb.or(
              cb.lessThan(r.get("fecha"), today),
              r.get("estado").in("Cancelada", "Rechazada", "Finalizada"));
      if ("HISTORIAL".equals(f.get("subtab"))) p.add(historic);
      if ("PROXIMAS".equals(f.get("subtab"))) p.add(cb.not(historic));
      q.orderBy(cb.desc(r.get("fecha")), cb.asc(r.get("horaInicio")), cb.asc(r.get("id")));
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private static boolean has(Map<String, String> f, String k) {
    return f.containsKey(k) && !f.get(k).isBlank() && !f.get(k).equals("ALL");
  }
}
