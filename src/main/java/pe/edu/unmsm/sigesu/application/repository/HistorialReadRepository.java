package pe.edu.unmsm.sigesu.application.repository;

import static pe.edu.unmsm.sigesu.common.ReadPages.*;

import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class HistorialReadRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public HistorialReadRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Object history(Map<String, String> f) {
    int size = number(f, "size", 10, 100), page = number(f, "page", 0, 1000000);
    var data =
        jdbc.queryForList(
            "select h.*,coalesce(s.codigo,r.codigo) as codigo,case when h.solicitud_id is not null"
                + " then 'Solicitud' else 'Reserva' end as tipo from historial h left join"
                + " solicitudes s on s.id=h.solicitud_id left join reservas r on r.id=h.reserva_id"
                + " order by h.fecha desc,h.id limit :limit offset :offset",
            Map.of("limit", size, "offset", (long) page * size));
    var content =
        data.stream()
            .map(
                row -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  row.forEach(
                      (k, v) ->
                          m.put(
                              camel(k),
                              v instanceof java.sql.Timestamp t ? t.toInstant().toString() : v));
                  return m;
                })
            .toList();
    return Map.of(
        "content",
        content,
        "page",
        page,
        "size",
        size,
        "totalElements",
        jdbc.queryForObject("select count(*) from historial", Map.of(), Long.class));
  }

  private String camel(String s) {
    var b = new StringBuilder();
    boolean upper = false;
    for (char c : s.toCharArray())
      if (c == '_') upper = true;
      else {
        b.append(upper ? Character.toUpperCase(c) : c);
        upper = false;
      }
    return b.toString();
  }
}
