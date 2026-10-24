package pe.edu.unmsm.sigesu.solicitudes.services;

import static pe.edu.unmsm.sigesu.common.ReadPages.*;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.application.services.BatchProjections;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.solicitudes.repository.*;

@Service
@Transactional(readOnly = true)
public class SolicitudesReadService {
  private final SolicitudRepository requests;
  private final BatchProjections batch;

  public SolicitudesReadService(SolicitudRepository requests, BatchProjections batch) {
    this.requests = requests;
    this.batch = batch;
  }

  private Map<String, Object> requestRow(
      Solicitud s, BatchProjections.Snapshot snapshot, Usuario u) {
    var m = snapshot.solicitud(s, u);
    m.remove("comentarios");
    m.remove("historial");
    m.remove("evidencias");
    m.put(
        "fechaUltimaActualizacion",
        java.util.stream.Stream.of(
                s.fechaCierre,
                s.fechaResolucion,
                s.fechaInicioAtencion,
                s.fechaAsignacion,
                s.fechaEvaluacion,
                s.ultimaFechaHistorial,
                s.fechaRegistro)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(s.fechaRegistro));
    return m;
  }

  public Map<String, Object> solicitudes(Usuario u, Map<String, String> f) {
    var p = requests.findAll(SolicitudSpecifications.filter(u, f), page(f));
    var snapshot = batch.load(p.getContent(), List.of(), List.of(u), false);
    return envelope(p, p.stream().map(s -> requestRow(s, snapshot, u)).toList());
  }

  public List<Map<String, Object>> exportRequests(Usuario u, Map<String, String> f) {
    var result = new ArrayList<Map<String, Object>>();
    var query = new HashMap<>(f);
    query.put("size", "100");
    int n = 0;
    while (true) {
      query.put("page", "" + n++);
      var p = solicitudes(u, query);
      result.addAll((List<Map<String, Object>>) p.get("content"));
      if (result.size() >= ((Number) p.get("totalElements")).longValue()) return result;
    }
  }
}
