package pe.edu.unmsm.sigesu.indicadores.services;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;
import pe.edu.unmsm.sigesu.indicadores.repository.IndicadoresReadRepository;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesReadService;

@Service
@Transactional(readOnly = true)
public class DashboardReadService {
  private final IndicadoresReadRepository repository;
  private final SolicitudesReadService reads;

  public DashboardReadService(IndicadoresReadRepository repository, SolicitudesReadService reads) {
    this.repository = repository;
    this.reads = reads;
  }

  public Object summary(Usuario u, Map<String, String> f) {
    var result = repository.summary(u, f);
    long total = ((Number) result.get("totalRegistradas")).longValue();
    for (String key : List.of("distCategoria", "distPrioridad", "distEstado"))
      for (var row : (List<Map<String, Object>>) result.get(key)) {
        double pct = total == 0 ? 0 : 100.0 * ((Number) row.get("count")).doubleValue() / total;
        row.put("pctFormatted", String.format(Locale.ROOT, "%.1f", pct).replace('.', ',') + " %");
        row.put("pctWidth", Math.round(pct));
      }
    var top = new HashMap<>(f);
    top.put("quickTab", "PENDIENTES");
    top.put("sort", "DASHBOARD");
    top.put("size", "10");
    top.put("page", "0");
    result.put(
        "pendientesPrioritarios",
        "true".equals(f.get("countsOnly")) ? List.of() : reads.solicitudes(u, top).get("content"));
    result.put("total", result.get("totalRegistradas"));
    result.put("vencidas", result.get("totalVencidas"));
    Map<String, Long> estados = new LinkedHashMap<>(), prioridades = new LinkedHashMap<>();
    for (var row : (List<Map<String, Object>>) result.get("distEstado"))
      estados.put((String) row.get("estado"), ((Number) row.get("count")).longValue());
    for (var row : (List<Map<String, Object>>) result.get("distPrioridad"))
      prioridades.put((String) row.get("nivel"), ((Number) row.get("count")).longValue());
    result.put("porEstado", estados);
    result.put("porPrioridad", prioridades);
    return result;
  }
}
