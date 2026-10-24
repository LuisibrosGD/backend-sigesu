package pe.edu.unmsm.sigesu.application.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import pe.edu.unmsm.sigesu.archivos.repository.*;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.Rules;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.reservas.repository.Reserva;
import pe.edu.unmsm.sigesu.solicitudes.repository.*;

@Service
public class Projections {
  private final ObjectMapper json;
  private final UsuarioRepository users;
  private final CatalogosService cats;
  private final ComentarioRepository comments;
  private final EventoRepository events;
  private final EvidenciaRepository files;

  public Projections(
      ObjectMapper j,
      UsuarioRepository u,
      CatalogosService c,
      ComentarioRepository cm,
      EventoRepository e,
      EvidenciaRepository f) {
    json = j;
    users = u;
    cats = c;
    comments = cm;
    events = e;
    files = f;
  }

  public Map<String, Object> plain(Object o) {
    Map<String, Object> m =
        json.convertValue(o, new TypeReference<LinkedHashMap<String, Object>>() {});
    m.remove("version");
    m.remove("passwordHash");
    return m;
  }

  public Map<String, Object> usuario(Usuario u) {
    return plain(u);
  }

  private String nombre(String id) {
    return id == null ? null : users.findById(id).map(u -> u.nombre).orElse("Usuario");
  }

  public Map<String, Object> facultad(pe.edu.unmsm.sigesu.catalogos.repository.Facultad f) {
    var m = plain(f);
    m.put("edificios", cats.edificiosDeFacultad(f.id).stream().map(this::plain).toList());
    return m;
  }

  public Map<String, Object> ambiente(pe.edu.unmsm.sigesu.catalogos.repository.Ambiente a) {
    var m = plain(a);
    m.put("facultadNombre", cats.findFacultad(a.facultadId).orElseThrow().nombre);
    m.put("areaNombre", cats.area(a.areaId).nombre);
    return m;
  }

  public Map<String, Object> comentario(Comentario c) {
    var m = plain(c);
    var u = users.findById(c.autorId).orElseThrow();
    m.put("autorNombre", u.nombre);
    m.put("autorRol", u.rol);
    return m;
  }

  public Map<String, Object> evidencia(Evidencia e) {
    var m = plain(e);
    m.remove("storagePath");
    m.put("subidoPorNombre", nombre(e.subidoPorId));
    m.put("dataUrl", "/api/evidencias/" + e.id + "/contenido");
    return m;
  }

  public Map<String, Object> solicitud(Solicitud s, Usuario actor) {
    var m = plain(s);
    var owner = users.findById(s.solicitanteId).orElseThrow();
    m.put("solicitanteNombre", owner.nombre);
    m.put("solicitanteEmail", owner.email);
    var cat = cats.findCategoria(s.categoriaId).orElseThrow();
    m.put("categoriaNombre", cat.nombre);
    m.put("tipoServicio", cat.tipoServicio);
    m.put("areaNombre", cats.area(s.areaId).nombre);
    m.put("facultadNombre", cats.findFacultad(s.facultadId).orElseThrow().nombre);
    m.put("edificioNombre", cats.findEdificio(s.edificioId).orElseThrow().nombre);
    m.put("tecnicoNombre", nombre(s.tecnicoId));
    m.put(
        "comentarios",
        comments.findBySolicitudIdOrderByFechaAsc(s.id).stream()
            .filter(c -> !c.esInterno || !Rules.role(actor, "Estudiante"))
            .map(this::comentario)
            .toList());
    m.put(
        "historial",
        events.findBySolicitudIdOrderByFechaAsc(s.id).stream().map(this::plain).toList());
    m.put("evidencias", files.findBySolicitudId(s.id).stream().map(this::evidencia).toList());
    return m;
  }

  public Map<String, Object> reserva(Reserva r) {
    var m = plain(r);
    var a = cats.findAmbiente(r.ambienteId).orElseThrow();
    var owner = users.findById(r.solicitanteId).orElseThrow();
    m.put("ambienteNombre", a.nombre);
    m.put("ambienteCodigo", a.codigo);
    m.put("facultadId", a.facultadId);
    m.put("facultadNombre", cats.findFacultad(a.facultadId).orElseThrow().nombre);
    m.put("areaId", a.areaId);
    m.put("areaNombre", cats.area(a.areaId).nombre);
    m.put("solicitanteNombre", owner.nombre);
    m.put("solicitanteRol", owner.rol);
    m.put("aprobadoPorNombre", nombre(r.aprobadoPorId));
    m.put(
        "historial",
        events.findByReservaIdOrderByFechaAsc(r.id).stream().map(this::plain).toList());
    return m;
  }
}
