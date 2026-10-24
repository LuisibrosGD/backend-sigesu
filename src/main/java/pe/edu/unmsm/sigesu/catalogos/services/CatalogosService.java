package pe.edu.unmsm.sigesu.catalogos.services;

import java.util.*;
import org.springframework.stereotype.Service;
import pe.edu.unmsm.sigesu.catalogos.repository.*;
import pe.edu.unmsm.sigesu.common.*;

@Service
public class CatalogosService {
  private final AreaRepository areas;
  private final FacultadRepository facultades;
  private final EdificioRepository edificios;
  private final CategoriaRepository categorias;
  private final PrioridadRepository prioridades;
  private final AmbienteRepository ambientes;
  private final ConfiguracionRepository configuraciones;

  public CatalogosService(
      AreaRepository a,
      FacultadRepository f,
      EdificioRepository e,
      CategoriaRepository c,
      PrioridadRepository p,
      AmbienteRepository am,
      ConfiguracionRepository co) {
    areas = a;
    facultades = f;
    edificios = e;
    categorias = c;
    prioridades = p;
    ambientes = am;
    configuraciones = co;
  }

  public Configuracion config() {
    return configuraciones.findById("general").orElseThrow(ApiException::missing);
  }

  public Categoria categoria(String id) {
    var c = categorias.findById(id).orElseThrow(ApiException::missing);
    Rules.check(c.activo && area(c.areaId).activo, "Categoría o área inactiva.");
    return c;
  }

  public Area area(String id) {
    return areas.findById(id).orElseThrow(ApiException::missing);
  }

  public void ubicacion(String facultad, String edificio) {
    var f = facultades.findById(facultad).orElseThrow(ApiException::missing);
    var e = edificios.findById(edificio).orElseThrow(ApiException::missing);
    Rules.check(
        f.activo && e.activo && facultad.equals(e.facultadId), "Facultad o edificio inválido.");
  }

  public int plazo(String nivel) {
    Rules.priority(nivel);
    return prioridades.findById(nivel).orElseThrow(ApiException::missing).plazoHoras;
  }

  public List<Area> listAreas() {
    return areas.findAll();
  }

  public List<Facultad> listFacultades() {
    return facultades.findAll();
  }

  public List<Edificio> listEdificios() {
    return edificios.findAll();
  }

  public List<Categoria> listCategorias() {
    return categorias.findAll();
  }

  public List<Prioridad> listPrioridades() {
    return prioridades.findAll();
  }

  public List<Ambiente> listAmbientes() {
    return ambientes.findAll();
  }

  public List<Ambiente> listAmbientesWithEquipment() {
    return ambientes.findAllWithEquipment();
  }

  public Optional<Facultad> findFacultad(String id) {
    return facultades.findById(id);
  }

  public Optional<Edificio> findEdificio(String id) {
    return edificios.findById(id);
  }

  public Optional<Categoria> findCategoria(String id) {
    return categorias.findById(id);
  }

  public Optional<Ambiente> findAmbiente(String id) {
    return ambientes.findById(id);
  }

  public List<Edificio> edificiosDeFacultad(String id) {
    return edificios.findByFacultadId(id);
  }

  /** Joins the caller's transaction; no new transaction may release the row lock early. */
  public Optional<Ambiente> lockAmbiente(String id) {
    return ambientes.lockById(id);
  }
}
