package pe.edu.unmsm.sigesu.catalogos.services;

import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.catalogos.repository.*;

@Service
@Transactional
public class CatalogosInitializationService {
  private final AreaRepository areas;
  private final FacultadRepository facultades;
  private final EdificioRepository edificios;
  private final CategoriaRepository categorias;
  private final PrioridadRepository prioridades;
  private final AmbienteRepository ambientes;
  private final ConfiguracionRepository configuraciones;
  private final ObjectMapper json;

  public CatalogosInitializationService(
      ObjectMapper json,
      AreaRepository areas,
      FacultadRepository facultades,
      EdificioRepository edificios,
      CategoriaRepository categorias,
      PrioridadRepository prioridades,
      AmbienteRepository ambientes,
      ConfiguracionRepository configuraciones) {
    this.json = json.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    this.areas = areas;
    this.facultades = facultades;
    this.edificios = edificios;
    this.categorias = categorias;
    this.prioridades = prioridades;
    this.ambientes = ambientes;
    this.configuraciones = configuraciones;
  }

  private <T> T from(JsonNode n, Class<T> type) {
    return json.convertValue(n, type);
  }

  public void ensureDefaults() {
    if (configuraciones.count() == 0) {
      var conf = new Configuracion();
      conf.id = "general";
      conf.nombreInstitucion = "UNMSM";
      conf.siglasInstitucion = "UNMSM";
      conf.periodoAcademico = "2026-II";
      conf.correoSoporteGeneral = "soporte@unmsm.edu.pe";
      conf.telefonoCentral = "(01) 619-7000";
      conf.tamanoMaximoAdjuntoMB = 2;
      conf.diasMaximosAntelacionReserva = 30;
      conf.mensajeInstitucional = "Servicios universitarios UNMSM";
      configuraciones.save(conf);
    }
    String[] levels = {"Baja", "Media", "Alta", "Crítica"};
    int[] hours = {120, 48, 24, 8};
    for (int i = 0; i < 4; i++)
      if (!prioridades.existsById(levels[i])) {
        var p = new Prioridad();
        p.nivel = levels[i];
        p.plazoHoras = hours[i];
        p.orden = i + 1;
        p.descripcion = levels[i];
        prioridades.save(p);
      }
  }

  public void seedDefinitions(JsonNode root) {
    for (var n : root.path("areas")) areas.save(from(n, Area.class));
    for (var n : root.path("facultades")) {
      var f = from(n, Facultad.class);
      facultades.save(f);
      for (var b : n.path("edificios")) {
        var e = from(b, Edificio.class);
        e.facultadId = f.id;
        edificios.save(e);
      }
    }
    for (var n : root.path("categorias")) categorias.save(from(n, Categoria.class));
    for (var n : root.path("prioridades")) prioridades.save(from(n, Prioridad.class));
  }

  public void seedAmbientes(JsonNode root) {
    for (var n : root.path("ambientes")) ambientes.save(from(n, Ambiente.class));
  }

  public void seedConfiguration(JsonNode root) {
    var config = from(root.path("configuracion"), Configuracion.class);
    config.id = "general";
    configuraciones.save(config);
  }
}
