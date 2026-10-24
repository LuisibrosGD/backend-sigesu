package pe.edu.unmsm.sigesu.solicitudes.repository;

import jakarta.persistence.criteria.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;

public class SolicitudSpecifications {
  private SolicitudSpecifications() {}

  private static Instant day(String text) {
    try {
      return LocalDate.parse(text).atStartOfDay(Rules.LIMA).toInstant();
    } catch (Exception e) {
      throw ApiException.bad("Fecha inválida.");
    }
  }

  private static Predicate pending(Root<Solicitud> s, CriteriaBuilder cb) {
    return cb.not(s.get("estado").in("Resuelta", "Cerrada"));
  }

  private static Predicate overdue(Root<Solicitud> s, CriteriaBuilder cb) {
    return cb.and(pending(s, cb), cb.lessThan(s.get("fechaLimite"), Instant.now()));
  }

  public static Specification<Solicitud> filter(Usuario u, Map<String, String> f) {
    return (s, q, cb) -> {
      var p = new ArrayList<Predicate>();
      if (!u.activo) p.add(cb.disjunction());
      switch (u.rol) {
        case "Administrador" -> {}
        case "Estudiante" -> p.add(cb.equal(s.get("solicitanteId"), u.id));
        case "Técnico" -> p.add(cb.equal(s.get("tecnicoId"), u.id));
        case "Supervisor" ->
            p.add(u.areasIds.isEmpty() ? cb.disjunction() : s.get("areaId").in(u.areasIds));
        default -> p.add(cb.disjunction());
      }
      for (String k : List.of("estado", "prioridad", "categoriaId", "areaId", "solicitanteId"))
        if (has(f, k)) p.add(cb.equal(s.get(k), f.get(k)));
      if (has(f, "tecnicoId"))
        p.add(
            f.get("tecnicoId").equals("UNASSIGNED")
                ? cb.isNull(s.get("tecnicoId"))
                : cb.equal(s.get("tecnicoId"), f.get("tecnicoId")));
      if (has(f, "tipoServicio")) {
        var cq = q.subquery(String.class);
        var c = cq.from(pe.edu.unmsm.sigesu.catalogos.repository.Categoria.class);
        cq.select(c.get("id")).where(cb.equal(c.get("tipoServicio"), f.get("tipoServicio")));
        p.add(s.get("categoriaId").in(cq));
      }
      if (has(f, "fechaDesde"))
        p.add(cb.greaterThanOrEqualTo(s.get("fechaRegistro"), day(f.get("fechaDesde"))));
      if (has(f, "fechaHasta"))
        p.add(
            cb.lessThan(s.get("fechaRegistro"), day(f.get("fechaHasta")).plus(Duration.ofDays(1))));
      if (has(f, "search")) {
        String term =
            "%"
                + f.get("search")
                    .toLowerCase(Locale.ROOT)
                    .replace("!", "!!")
                    .replace("%", "!%")
                    .replace("_", "!_")
                + "%";
        var matches = new ArrayList<Predicate>();
        for (String k : List.of("titulo", "codigo", "ubicacionEspecifica"))
          matches.add(cb.like(cb.lower(s.get(k)), term, '!'));
        Map<String, Class<?>> refs = new LinkedHashMap<>();
        refs.put("facultadId", pe.edu.unmsm.sigesu.catalogos.repository.Facultad.class);
        refs.put("edificioId", pe.edu.unmsm.sigesu.catalogos.repository.Edificio.class);
        refs.put("tecnicoId", Usuario.class);
        if (!u.rol.equals("Estudiante")) refs.put("solicitanteId", Usuario.class);
        refs.forEach(
            (field, type) -> {
              var lookup = q.subquery(String.class);
              var related = lookup.from(type);
              lookup
                  .select(related.get("id"))
                  .where(cb.like(cb.lower(related.get("nombre")), term, '!'));
              matches.add(s.get(field).in(lookup));
            });
        p.add(cb.or(matches.toArray(Predicate[]::new)));
      }
      if ("true".equals(f.get("pendienteTecnico")))
        p.add(s.get("estado").in("Asignada", "En atención"));
      String tab = f.getOrDefault("quickTab", "TODAS");
      if (tab.equals("PENDIENTES"))
        p.add(
            u.rol.equals("Técnico")
                ? s.get("estado").in("Asignada", "En atención")
                : pending(s, cb));
      if (tab.equals("RESUELTAS_CERRADAS")) p.add(s.get("estado").in("Resuelta", "Cerrada"));
      if ("true".equals(f.get("soloVencidas")) || "VENCIDAS".equals(f.get("condicionPlazo")))
        p.add(overdue(s, cb));
      if ("POR_VENCER".equals(f.get("condicionPlazo")))
        p.add(
            cb.and(
                pending(s, cb),
                cb.between(
                    s.get("fechaLimite"),
                    Instant.now(),
                    Instant.now().plus(Duration.ofHours(24)))));
      if ("EN_PLAZO".equals(f.get("condicionPlazo")))
        p.add(
            cb.or(
                cb.and(
                    pending(s, cb), cb.greaterThanOrEqualTo(s.get("fechaLimite"), Instant.now())),
                cb.and(
                    s.get("estado").in("Resuelta", "Cerrada"),
                    cb.isFalse(s.get("atendidaFueraDePlazo")))));
      if ("FUERA_PLAZO".equals(f.get("condicionPlazo")))
        p.add(cb.isTrue(s.get("atendidaFueraDePlazo")));
      if (q.getResultType() != Long.class && q.getResultType() != long.class) {
        var orders = new ArrayList<Order>();
        String sort = f.getOrDefault("sort", "RECIENTES");
        Expression<Integer> weight =
            cb.<String, Integer>selectCase(s.get("prioridad"))
                .when("Crítica", 4)
                .when("Alta", 3)
                .when("Media", 2)
                .otherwise(1);
        if (sort.equals("DASHBOARD")) {
          orders.add(cb.asc(cb.<Integer>selectCase().when(overdue(s, cb), 0).otherwise(1)));
          orders.add(
              cb.asc(
                  cb.<Integer>selectCase()
                      .when(
                          cb.between(
                              s.get("fechaLimite"),
                              Instant.now(),
                              Instant.now().plus(Duration.ofHours(6))),
                          0)
                      .otherwise(1)));
          orders.add(cb.desc(weight));
          orders.add(cb.asc(s.get("fechaLimite")));
        } else if (sort.equals("prioridad"))
          orders.add("asc".equals(f.get("order")) ? cb.asc(weight) : cb.desc(weight));
        else if (sort.equals("URGENCIA") || sort.equals("VENCIMIENTO")) {
          orders.add(cb.asc(cb.<Integer>selectCase().when(pending(s, cb), 0).otherwise(1)));
          orders.add(cb.asc(cb.<Integer>selectCase().when(overdue(s, cb), 0).otherwise(1)));
          orders.add(cb.asc(s.get("fechaLimite")));
          orders.add(cb.desc(weight));
        } else if (sort.equals("PRIORIDAD")) orders.add(cb.desc(weight));
        else if (sort.equals("ACTUALIZACION"))
          orders.add(
              cb.desc(
                  cb.<Instant>coalesce()
                      .value(s.get("fechaCierre"))
                      .value(s.get("fechaResolucion"))
                      .value(s.get("fechaInicioAtencion"))
                      .value(s.get("fechaAsignacion"))
                      .value(s.get("fechaEvaluacion"))
                      .value(s.get("ultimaFechaHistorial"))
                      .value(s.get("fechaRegistro"))));
        else if (Set.of("codigo", "titulo", "prioridad", "estado", "fechaRegistro", "fechaLimite")
            .contains(sort))
          orders.add("asc".equals(f.get("order")) ? cb.asc(s.get(sort)) : cb.desc(s.get(sort)));
        orders.add(cb.desc(s.get("fechaRegistro")));
        orders.add(cb.asc(s.get("id")));
        q.orderBy(orders);
      }
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  private static boolean has(Map<String, String> f, String k) {
    return f.containsKey(k) && !f.get(k).isBlank() && !f.get(k).equals("ALL");
  }
}
