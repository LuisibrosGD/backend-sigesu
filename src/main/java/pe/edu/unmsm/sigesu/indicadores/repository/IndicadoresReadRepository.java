package pe.edu.unmsm.sigesu.indicadores.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;

@Repository
public class IndicadoresReadRepository {
  private final NamedParameterJdbcTemplate jdbc;
  private final ObjectMapper json;

  public IndicadoresReadRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Map<String, Object> summary(Usuario u, Map<String, String> f) {
    var params = new HashMap<String, Object>();
    params.put("uid", u.id);
    params.put("now", java.sql.Timestamp.from(Instant.now()));
    params.put("areas", u.areasIds.isEmpty() ? Set.of("") : u.areasIds);
    String permission =
        switch (u.rol) {
          case "Administrador" -> "true";
          case "Estudiante" -> "s.solicitante_id=:uid";
          case "Técnico" -> "s.tecnico_id=:uid";
          case "Supervisor" -> "s.area_id in (:areas)";
          default -> "false";
        };
    if (!u.activo) permission = "false";
    var where = new StringBuilder(permission);
    for (String key : List.of("areaId", "categoriaId", "tecnicoId")) {
      String val = f.get(key);
      if (val == null || val.isBlank() || val.equals("ALL")) continue;
      String col =
          switch (key) {
            case "areaId" -> "s.area_id";
            case "categoriaId" -> "s.categoria_id";
            default -> "s.tecnico_id";
          };
      if (val.equals("UNASSIGNED") && key.equals("tecnicoId"))
        where.append(" and s.tecnico_id is null");
      else {
        where.append(" and " + col + "=:" + key);
        params.put(key, val);
      }
    }
    for (String key : List.of("fechaDesde", "fechaHasta"))
      if (f.containsKey(key) && !f.get(key).isBlank()) {
        Instant date = LocalDate.parse(f.get(key)).atStartOfDay(Rules.LIMA).toInstant();
        if (key.equals("fechaHasta")) date = date.plus(Duration.ofDays(1));
        where.append(
            " and s.fecha_registro " + (key.equals("fechaDesde") ? ">=" : "<") + ":" + key);
        params.put(key, java.sql.Timestamp.from(date));
      }
    String base =
        "with v as (select s.*,c.nombre categoria_nombre,c.tipo_servicio,a.nombre"
            + " area_nombre,u.nombre tecnico_nombre from solicitudes s join categorias c on"
            + " c.id=s.categoria_id join areas a on a.id=s.area_id left join usuarios u on"
            + " u.id=s.tecnico_id where "
            + where
            + ") ";
    String sql =
        base
            + """
             select jsonb_build_object(
               'totalVisibles',(select count(*) from solicitudes s where %s),
               'totalRegistradas',count(*),
               'totalPendientes',count(*) filter(where estado not in ('Resuelta','Cerrada')),
               'totalResueltasYCerradas',count(*) filter(where estado in ('Resuelta','Cerrada')),
               'totalVencidas',count(*) filter(where estado not in ('Resuelta','Cerrada') and fecha_limite<:now),
               'totalFueraPlazo',count(*) filter(where estado in ('Resuelta','Cerrada') and atendida_fuera_de_plazo),
               'porcentajeResueltasNum',coalesce(100.0*(count(*) filter(where estado in ('Resuelta','Cerrada')))/nullif(count(*),0),0),
               'promedioPrimeraAtencionHoras',avg(greatest(extract(epoch from (fecha_inicio_atencion-fecha_registro))/3600,0)) filter(where fecha_inicio_atencion is not null),
               'promedioResolucionHoras',avg(greatest(extract(epoch from (fecha_resolucion-fecha_registro))/3600,0)) filter(where fecha_resolucion is not null),
               'porArea',coalesce((select jsonb_object_agg(area_nombre,n) from (select area_nombre,count(*) n from v group by area_nombre) z),'{}'),
               'distCategoria',coalesce((select jsonb_agg(z) from (select categoria_id id,categoria_nombre nombre,tipo_servicio "tipoServicio",count(*) count from v group by categoria_id,categoria_nombre,tipo_servicio order by count(*) desc,categoria_id) z),'[]'),
               'distPrioridad',coalesce((select jsonb_agg(z order by z.orden desc) from (select p.nivel,p.orden,count(v.id) count from prioridades p left join v on v.prioridad=p.nivel group by p.nivel,p.orden) z),'[]'),
               'distEstado',coalesce((select jsonb_agg(z order by z.orden) from (select e.estado,e.orden,count(v.id) count from (values ('Registrada',1),('En evaluación',2),('Asignada',3),('En atención',4),('Resuelta',5),('Cerrada',6)) e(estado,orden) left join v on v.estado=e.estado group by e.estado,e.orden) z),'[]'),
               'porResponsable',coalesce((select jsonb_agg(z) from (select coalesce(tecnico_id,'SIN_ASIGNAR') id,coalesce(tecnico_nombre,'Pendiente de asignación') nombre,count(*) total,count(*) filter(where estado not in ('Resuelta','Cerrada')) pendientes,count(*) filter(where estado in ('Resuelta','Cerrada')) resueltas from v group by tecnico_id,tecnico_nombre order by count(*) desc,tecnico_id) z),'[]'),
               'evolucionTemporal',coalesce((select jsonb_agg(z order by z.fecha) from (select (fecha_registro at time zone 'America/Lima')::date fecha,to_char(fecha_registro at time zone 'America/Lima','DD/MM/YYYY') label,count(*) registradas,count(*) filter(where estado in ('Resuelta','Cerrada')) resueltas from v group by (fecha_registro at time zone 'America/Lima')::date,to_char(fecha_registro at time zone 'America/Lima','DD/MM/YYYY') order by fecha desc limit 10) z),'[]')
             )::text from v
            """
                .formatted(permission);
    try {
      return json.readValue(jdbc.queryForObject(sql, params, String.class), Map.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Respuesta de indicadores inválida", e);
    }
  }
}
