package pe.edu.unmsm.sigesu.catalogos.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.catalogos.repository.*;
import pe.edu.unmsm.sigesu.catalogos.schemas.CatalogosSchemas.*;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;

@Service
@Transactional
public class AdminCatalogosService {
  private final CatalogosService c;
  private final AreaRepository areas;
  private final FacultadRepository facultades;
  private final EdificioRepository edificios;
  private final CategoriaRepository categorias;
  private final PrioridadRepository prioridades;
  private final AmbienteRepository ambientes;
  private final ConfiguracionRepository configuraciones;
  private final ObjectMapper json;

  public AdminCatalogosService(
      CatalogosService c,
      ObjectMapper j,
      AreaRepository areas,
      FacultadRepository facultades,
      EdificioRepository edificios,
      CategoriaRepository categorias,
      PrioridadRepository prioridades,
      AmbienteRepository ambientes,
      ConfiguracionRepository configuraciones) {
    this.c = c;
    this.areas = areas;
    this.facultades = facultades;
    this.edificios = edificios;
    this.categorias = categorias;
    this.prioridades = prioridades;
    this.ambientes = ambientes;
    this.configuraciones = configuraciones;
    json = j;
  }

  private String id(String id) {
    return id == null || id.isBlank() ? Rules.id() : id;
  }

  public Object area(Usuario u, AreaInput p) {
    Rules.admin(u);
    var a = json.convertValue(p, Area.class);
    a.id = id(p.id());
    a.codigo = a.codigo.trim().toUpperCase();
    return areas.save(a);
  }

  public Object categoria(Usuario u, CategoriaInput p) {
    Rules.admin(u);
    Rules.priority(p.prioridadSugerida());
    Rules.check(!p.activo() || c.area(p.areaId()).activo, "El área responsable está inactiva.");
    var a = json.convertValue(p, Categoria.class);
    a.id = id(p.id());
    return categorias.save(a);
  }

  public Object facultad(Usuario u, FacultadInput p) {
    Rules.admin(u);
    var f = new Facultad();
    f.id = id(p.id());
    f.nombre = p.nombre();
    f.siglas = p.siglas();
    f.decano = p.decano();
    f.activo = p.activo();
    facultades.save(f);
    Set<String> supplied = new HashSet<>();
    for (var e : p.edificios()) {
      var b = json.convertValue(e, Edificio.class);
      b.id = id(e.id());
      if (edificios.existsById(b.id))
        Rules.check(
            edificios.findById(b.id).orElseThrow().facultadId.equals(f.id),
            "El edificio pertenece a otra facultad.");
      b.facultadId = f.id;
      edificios.save(b);
      supplied.add(b.id);
    }
    for (var old : edificios.findByFacultadId(f.id)) {
      if (!supplied.contains(old.id)) {
        old.activo = false;
        edificios.save(old);
      }
    }
    return f;
  }

  public Object ambiente(Usuario u, AmbienteInput p) {
    Rules.admin(u);
    Rules.check(p.horarioFin().isAfter(p.horarioInicio()), "Horario inválido.");
    c.area(p.areaId());
    facultades.findById(p.facultadId()).orElseThrow(ApiException::missing);
    var a =
        p.id() == null
            ? new Ambiente()
            : ambientes.lockById(p.id()).orElseThrow(ApiException::missing);
    try {
      json.updateValue(a, p);
    } catch (Exception e) {
      throw ApiException.bad("Ambiente inválido.");
    }
    a.id = id(p.id());
    return ambientes.save(a);
  }

  public Object prioridades(Usuario u, List<PrioridadInput> p) {
    Rules.admin(u);
    Rules.check(
        p.size() == 4 && p.stream().map(PrioridadInput::nivel).distinct().count() == 4,
        "Se requieren las cuatro prioridades.");
    for (var input : p) {
      Rules.priority(input.nivel());
      var a = json.convertValue(input, Prioridad.class);
      prioridades.save(a);
    }
    return prioridades.findAll();
  }

  public Object config(Usuario u, ConfigInput p) {
    Rules.admin(u);
    var a = json.convertValue(p, Configuracion.class);
    a.id = "general";
    return configuraciones.save(a);
  }
}
