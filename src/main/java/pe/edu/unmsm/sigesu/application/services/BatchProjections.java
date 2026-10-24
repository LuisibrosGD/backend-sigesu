package pe.edu.unmsm.sigesu.application.services;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.archivos.repository.*;
import pe.edu.unmsm.sigesu.catalogos.repository.*;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.Rules;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.reservas.repository.Reserva;
import pe.edu.unmsm.sigesu.solicitudes.repository.*;

/** Loads relations once per request; rendering a snapshot never queries the database. */
@Service
public class BatchProjections {
  private final CatalogosService cats;
  private final UsuarioRepository users;
  private final ComentarioRepository comments;
  private final EventoRepository events;
  private final EvidenciaRepository files;
  private final Projections views;

  public BatchProjections(
      CatalogosService c,
      UsuarioRepository u,
      ComentarioRepository cm,
      EventoRepository e,
      EvidenciaRepository f,
      Projections v) {
    cats = c;
    users = u;
    comments = cm;
    events = e;
    files = f;
    views = v;
  }

  private static <T> List<T> batches(
      Collection<String> ids, Function<Collection<String>, List<T>> query) {
    var list = new ArrayList<>(ids);
    var result = new ArrayList<T>();
    for (int i = 0; i < list.size(); i += 500)
      result.addAll(query.apply(list.subList(i, Math.min(i + 500, list.size()))));
    return result;
  }

  private static <T> Map<String, T> index(List<T> list, Function<T, String> key) {
    return list.stream()
        .collect(Collectors.toMap(key, Function.identity(), (a, b) -> a, LinkedHashMap::new));
  }

  private static <T> Map<String, List<T>> group(List<T> list, Function<T, String> key) {
    return list.stream().collect(Collectors.groupingBy(key));
  }

  @Transactional(readOnly = true)
  public Snapshot load(
      List<Solicitud> solicitudes, List<Reserva> reservas, Collection<Usuario> extra) {
    return load(solicitudes, reservas, extra, true);
  }

  @Transactional(readOnly = true)
  public Snapshot load(
      List<Solicitud> solicitudes,
      List<Reserva> reservas,
      Collection<Usuario> extra,
      boolean details) {
    var sids = details ? solicitudes.stream().map(s -> s.id).toList() : List.<String>of();
    var rids = details ? reservas.stream().map(r -> r.id).toList() : List.<String>of();
    var cm = batches(sids, comments::findBySolicitudIdInOrderByFechaAsc);
    var timeline = new ArrayList<Evento>();
    for (int i = 0; i < Math.max(sids.size(), rids.size()); i += 500) {
      Collection<String> ss =
          i < sids.size() ? sids.subList(i, Math.min(i + 500, sids.size())) : List.of("");
      Collection<String> rr =
          i < rids.size() ? rids.subList(i, Math.min(i + 500, rids.size())) : List.of("");
      timeline.addAll(events.findForReferences(ss, rr));
    }
    var sh = timeline.stream().filter(e -> e.solicitudId != null).toList();
    var rh = timeline.stream().filter(e -> e.reservaId != null).toList();
    var ev = batches(sids, files::findBySolicitudIdIn);
    Set<String> uids = new HashSet<>();
    solicitudes.forEach(
        s -> {
          uids.add(s.solicitanteId);
          if (s.tecnicoId != null) uids.add(s.tecnicoId);
        });
    reservas.forEach(
        r -> {
          uids.add(r.solicitanteId);
          if (r.aprobadoPorId != null) uids.add(r.aprobadoPorId);
        });
    cm.forEach(c -> uids.add(c.autorId));
    ev.forEach(
        e -> {
          if (e.subidoPorId != null) uids.add(e.subidoPorId);
        });
    var um = index(new ArrayList<>(extra), u -> u.id);
    uids.removeAll(um.keySet());
    um.putAll(index(batches(uids, users::findWithAreasByIds), u -> u.id));
    return new Snapshot(
        um,
        index(cats.listAreas(), a -> a.id),
        index(
            !details && solicitudes.isEmpty() ? List.<Categoria>of() : cats.listCategorias(),
            c -> c.id),
        index(cats.listFacultades(), f -> f.id),
        index(
            !details && solicitudes.isEmpty() ? List.<Edificio>of() : cats.listEdificios(),
            e -> e.id),
        index(
            !details && reservas.isEmpty()
                ? List.<Ambiente>of()
                : cats.listAmbientesWithEquipment(),
            a -> a.id),
        details ? cats.listPrioridades() : List.of(),
        details ? cats.config() : null,
        group(cm, c -> c.solicitudId),
        group(sh, h -> h.solicitudId),
        group(rh, h -> h.reservaId),
        group(ev, e -> e.solicitudId));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> solicitudes(List<Solicitud> list, Usuario actor) {
    if (list.isEmpty()) return List.of();
    var snapshot = load(list, List.of(), List.of(actor));
    return list.stream().map(s -> snapshot.solicitud(s, actor)).toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> reservas(List<Reserva> list) {
    if (list.isEmpty()) return List.of();
    var snapshot = load(List.of(), list, List.of());
    return list.stream().map(snapshot::reserva).toList();
  }

  public final class Snapshot {
    private final Map<String, Usuario> usuarios;
    private final Map<String, Area> areas;
    private final Map<String, Categoria> categorias;
    private final Map<String, Facultad> facultades;
    private final Map<String, Edificio> edificios;
    private final Map<String, Ambiente> ambientes;
    private final List<Prioridad> prioridades;
    private final Configuracion config;
    private final Map<String, List<Comentario>> comentarios;
    private final Map<String, List<Evento>> historialSolicitudes, historialReservas;
    private final Map<String, List<Evidencia>> evidencias;

    Snapshot(
        Map<String, Usuario> u,
        Map<String, Area> a,
        Map<String, Categoria> c,
        Map<String, Facultad> f,
        Map<String, Edificio> ed,
        Map<String, Ambiente> am,
        List<Prioridad> p,
        Configuracion co,
        Map<String, List<Comentario>> cm,
        Map<String, List<Evento>> sh,
        Map<String, List<Evento>> rh,
        Map<String, List<Evidencia>> ev) {
      usuarios = u;
      areas = a;
      categorias = c;
      facultades = f;
      edificios = ed;
      ambientes = am;
      prioridades = p;
      config = co;
      comentarios = cm;
      historialSolicitudes = sh;
      historialReservas = rh;
      evidencias = ev;
    }

    private String nombre(String id) {
      return id == null ? null : usuarios.containsKey(id) ? usuarios.get(id).nombre : "Usuario";
    }

    public Map<String, Object> facultad(Facultad f) {
      var m = views.plain(f);
      m.put(
          "edificios",
          edificios.values().stream()
              .filter(e -> f.id.equals(e.facultadId))
              .map(views::plain)
              .toList());
      return m;
    }

    private Map<String, Object> ambiente(Ambiente a) {
      var m = views.plain(a);
      m.put("facultadNombre", facultades.get(a.facultadId).nombre);
      m.put("areaNombre", areas.get(a.areaId).nombre);
      return m;
    }

    public Map<String, Object> catalogs() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("areas", areas.values().stream().map(views::plain).toList());
      m.put("categorias", categorias.values().stream().map(views::plain).toList());
      m.put("prioridades", prioridades.stream().map(views::plain).toList());
      m.put("facultades", facultades.values().stream().map(this::facultad).toList());
      m.put("ambientes", ambientes.values().stream().map(this::ambiente).toList());
      m.put("configuracion", views.plain(config));
      return m;
    }

    private Map<String, Object> comentario(Comentario c) {
      var m = views.plain(c);
      var u = usuarios.get(c.autorId);
      m.put("autorNombre", u.nombre);
      m.put("autorRol", u.rol);
      return m;
    }

    private Map<String, Object> evidencia(Evidencia e) {
      var m = views.plain(e);
      m.remove("storagePath");
      m.put("subidoPorNombre", nombre(e.subidoPorId));
      m.put("dataUrl", "/api/evidencias/" + e.id + "/contenido");
      return m;
    }

    public Map<String, Object> solicitud(Solicitud s, Usuario actor) {
      var m = views.plain(s);
      var owner = usuarios.get(s.solicitanteId);
      var cat = categorias.get(s.categoriaId);
      m.put("solicitanteNombre", owner.nombre);
      m.put("solicitanteEmail", owner.email);
      m.put("categoriaNombre", cat.nombre);
      m.put("tipoServicio", cat.tipoServicio);
      m.put("areaNombre", areas.get(s.areaId).nombre);
      m.put("facultadNombre", facultades.get(s.facultadId).nombre);
      m.put("edificioNombre", edificios.get(s.edificioId).nombre);
      m.put("tecnicoNombre", nombre(s.tecnicoId));
      m.put(
          "comentarios",
          comentarios.getOrDefault(s.id, List.of()).stream()
              .filter(c -> !c.esInterno || !Rules.role(actor, "Estudiante"))
              .map(this::comentario)
              .toList());
      m.put(
          "historial",
          historialSolicitudes.getOrDefault(s.id, List.of()).stream().map(views::plain).toList());
      m.put(
          "evidencias",
          evidencias.getOrDefault(s.id, List.of()).stream().map(this::evidencia).toList());
      return m;
    }

    public Map<String, Object> reserva(Reserva r) {
      var m = views.plain(r);
      var a = ambientes.get(r.ambienteId);
      var owner = usuarios.get(r.solicitanteId);
      m.put("ambienteNombre", a.nombre);
      m.put("ambienteCodigo", a.codigo);
      m.put("facultadId", a.facultadId);
      m.put("facultadNombre", facultades.get(a.facultadId).nombre);
      m.put("areaId", a.areaId);
      m.put("areaNombre", areas.get(a.areaId).nombre);
      m.put("solicitanteNombre", owner.nombre);
      m.put("solicitanteRol", owner.rol);
      m.put("aprobadoPorNombre", nombre(r.aprobadoPorId));
      m.put(
          "historial",
          historialReservas.getOrDefault(r.id, List.of()).stream().map(views::plain).toList());
      return m;
    }
  }
}
