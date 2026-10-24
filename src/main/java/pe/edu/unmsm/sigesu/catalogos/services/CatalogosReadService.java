package pe.edu.unmsm.sigesu.catalogos.services;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;

@Service
@Transactional(readOnly = true)
public class CatalogosReadService {
  private final CatalogosService cats;
  private final Projections views;
  private final UsuarioRepository users;

  public CatalogosReadService(CatalogosService cats, Projections views, UsuarioRepository users) {
    this.cats = cats;
    this.views = views;
    this.users = users;
  }

  public Map<String, Object> catalogs(Usuario u, String include) {
    var result = new LinkedHashMap<String, Object>();
    for (String key : include.split(","))
      switch (key) {
        case "areas" -> result.put(key, cats.listAreas().stream().map(views::plain).toList());
        case "categorias" ->
            result.put(key, cats.listCategorias().stream().map(views::plain).toList());
        case "prioridades" ->
            result.put(key, cats.listPrioridades().stream().map(views::plain).toList());
        case "facultades" -> {
          var ed = cats.listEdificios();
          result.put(
              key,
              cats.listFacultades().stream()
                  .map(
                      f -> {
                        var m = views.plain(f);
                        m.put(
                            "edificios",
                            ed.stream()
                                .filter(e -> f.id.equals(e.facultadId))
                                .map(views::plain)
                                .toList());
                        return m;
                      })
                  .toList());
        }
        case "ambientes" -> {
          var faculties = new HashMap<String, String>();
          cats.listFacultades().forEach(x -> faculties.put(x.id, x.nombre));
          var areas = new HashMap<String, String>();
          cats.listAreas().forEach(x -> areas.put(x.id, x.nombre));
          result.put(
              key,
              cats.listAmbientesWithEquipment().stream()
                  .map(
                      x -> {
                        var m = views.plain(x);
                        m.put("facultadNombre", faculties.get(x.facultadId));
                        m.put("areaNombre", areas.get(x.areaId));
                        return m;
                      })
                  .toList());
        }
        case "configuracion" -> result.put(key, views.plain(cats.config()));
        case "tecnicos" ->
            result.put(
                "usuarios",
                (Rules.role(u, "Administrador", "Supervisor")
                        ? users.findStaff(
                            Rules.role(u, "Administrador"),
                            u.areasIds.isEmpty() ? Set.of("") : u.areasIds)
                        : List.<Usuario>of())
                    .stream().map(views::usuario).toList());
        default -> throw ApiException.bad("Catálogo no válido.");
      }
    return result;
  }
}
