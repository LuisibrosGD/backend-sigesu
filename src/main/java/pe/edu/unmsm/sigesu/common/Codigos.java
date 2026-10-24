package pe.edu.unmsm.sigesu.common;

import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class Codigos {
  private final JdbcTemplate jdbc;

  public Codigos(JdbcTemplate j) {
    jdbc = j;
  }

  public String next(String tipo) {
    int year = LocalDate.now(Rules.LIMA).getYear();
    Long n =
        jdbc.queryForObject(
            "insert into contadores(tipo,anio,valor) values(?,?,1) on conflict(tipo,anio) do update"
                + " set valor=contadores.valor+1 returning valor",
            Long.class,
            tipo,
            year);
    return "%s-%d-%04d".formatted(tipo, year, n);
  }
}
