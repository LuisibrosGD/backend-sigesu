package pe.edu.unmsm.sigesu.identidad.repository;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UsuariosReadRepository {
  private final NamedParameterJdbcTemplate jdbc;
  private final UsuarioRepository users;

  public UsuariosReadRepository(NamedParameterJdbcTemplate jdbc, UsuarioRepository users) {
    this.jdbc = jdbc;
    this.users = users;
  }

  public Page<Usuario> page(Pageable pageable) {
    var ids =
        jdbc.queryForList(
            "select id from usuarios order by nombre,id limit :limit offset :offset",
            Map.of("limit", pageable.getPageSize(), "offset", pageable.getOffset()),
            String.class);
    var byId = new HashMap<String, Usuario>();
    if (!ids.isEmpty()) users.findWithAreasByIds(ids).forEach(x -> byId.put(x.id, x));
    return new PageImpl<>(
        ids.stream().map(byId::get).toList(),
        pageable,
        jdbc.queryForObject("select count(*) from usuarios", Map.of(), Long.class));
  }
}
