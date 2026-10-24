package pe.edu.unmsm.sigesu.indicadores.services;

import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.stereotype.Service;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.common.Paging;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesService;

@Service
public class IndicadoresService {
  private final SolicitudesService service;
  private final Projections views;

  public IndicadoresService(SolicitudesService s, Projections v) {
    service = s;
    views = v;
  }

  public Object summary(Usuario u, Map<String, String> filters) {
    var items =
        Paging.filter(
            service.visible(u).stream().map(s -> views.solicitud(s, u)).toList(), filters);
    long vencidas =
        items.stream()
            .filter(
                s ->
                    !Set.of("Resuelta", "Cerrada").contains(s.get("estado"))
                        && Instant.parse(s.get("fechaLimite").toString()).isBefore(Instant.now()))
            .count();
    return Map.of(
        "total",
        items.size(),
        "vencidas",
        vencidas,
        "porEstado",
        items.stream().collect(Collectors.groupingBy(s -> s.get("estado"), Collectors.counting())),
        "porPrioridad",
        items.stream()
            .collect(Collectors.groupingBy(s -> s.get("prioridad"), Collectors.counting())),
        "porArea",
        items.stream()
            .collect(Collectors.groupingBy(s -> s.get("areaNombre"), Collectors.counting())));
  }
}
