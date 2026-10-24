package pe.edu.unmsm.sigesu.reservas.services;

import static pe.edu.unmsm.sigesu.common.ReadPages.*;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.application.services.BatchProjections;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.reservas.repository.*;

@Service
@Transactional(readOnly = true)
public class ReservasReadService {
  private final ReservaRepository reservations;
  private final BatchProjections batch;

  public ReservasReadService(ReservaRepository reservations, BatchProjections batch) {
    this.reservations = reservations;
    this.batch = batch;
  }

  public Map<String, Object> reservas(Usuario u, Map<String, String> f) {
    var p = reservations.findAll(ReservaSpecifications.filter(u, f), page(f));
    var snapshot = batch.load(List.of(), p.getContent(), List.of(u), false);
    var result =
        new LinkedHashMap<String, Object>(
            envelope(
                p,
                p.stream()
                    .map(
                        r -> {
                          var m = snapshot.reserva(r);
                          m.remove("historial");
                          return m;
                        })
                    .toList()));
    result.put(
        "pendientes",
        reservations.count(
            ReservaSpecifications.filter(u, f)
                .and((r, q, cb) -> cb.equal(r.get("estado"), "Solicitada"))));
    return result;
  }

  public Object occupants(Usuario u, Map<String, String> f) {
    if (!Rules.creator(u)) return List.of();
    LocalDate from =
        has(f, "fechaDesde") ? LocalDate.parse(f.get("fechaDesde")) : LocalDate.now(Rules.LIMA);
    LocalDate to = has(f, "fechaHasta") ? LocalDate.parse(f.get("fechaHasta")) : from.plusDays(31);
    Rules.check(
        !to.isBefore(from) && to.isBefore(from.plusDays(63)),
        "Rango de disponibilidad inválido (máximo 62 días).");
    return reservations
        .findAll(
            (r, q, cb) ->
                cb.and(
                    r.get("estado").in("Solicitada", "Aprobada"),
                    cb.between(r.get("fecha"), from, to),
                    has(f, "ambienteId")
                        ? cb.equal(r.get("ambienteId"), f.get("ambienteId"))
                        : cb.conjunction()))
        .stream()
        .map(
            r ->
                Map.of(
                    "id",
                    r.id,
                    "ambienteId",
                    r.ambienteId,
                    "fecha",
                    r.fecha,
                    "horaInicio",
                    r.horaInicio,
                    "horaFin",
                    r.horaFin,
                    "estado",
                    r.estado))
        .toList();
  }

  private static boolean has(Map<String, String> f, String k) {
    return f.containsKey(k) && !f.get(k).isBlank() && !f.get(k).equals("ALL");
  }
}
