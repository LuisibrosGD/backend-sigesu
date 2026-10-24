package pe.edu.unmsm.sigesu.application.services;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unmsm.sigesu.catalogos.repository.*;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosInitializationService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.notificaciones.repository.*;
import pe.edu.unmsm.sigesu.reservas.repository.*;
import pe.edu.unmsm.sigesu.solicitudes.repository.*;

@Service
public class InitialData implements CommandLineRunner {
  private final CatalogosInitializationService c;
  private final UsuarioRepository users;
  private final SolicitudRepository requests;
  private final ReservaRepository reservations;
  private final ComentarioRepository comments;
  private final EventoRepository history;
  private final NotificacionRepository notifications;
  private final ObjectMapper json;
  private final PasswordEncoder passwords;
  private final Environment env;
  private final JdbcTemplate jdbc;
  private final String email, password;

  public InitialData(
      CatalogosInitializationService c,
      UsuarioRepository u,
      SolicitudRepository s,
      ReservaRepository r,
      ComentarioRepository cm,
      EventoRepository h,
      NotificacionRepository n,
      ObjectMapper j,
      PasswordEncoder p,
      Environment e,
      JdbcTemplate jd,
      @Value("${app.bootstrap.email}") String email,
      @Value("${app.bootstrap.password}") String password) {
    this.c = c;
    users = u;
    requests = s;
    reservations = r;
    comments = cm;
    history = h;
    notifications = n;
    json = j.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    passwords = p;
    env = e;
    jdbc = jd;
    this.email = email;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    jdbc.queryForObject("select pg_advisory_xact_lock(713221)", Object.class);
    c.ensureDefaults();
    if (Arrays.asList(env.getActiveProfiles()).contains("dev") && users.count() == 0) demo();
    if (users.findByRolAndActivoTrue("Administrador").isEmpty() && !email.isBlank()) {
      Rules.check(
          password.length() >= 12,
          "La contraseña del administrador inicial debe tener al menos 12 caracteres.");
      var u = new Usuario();
      u.id = Rules.id();
      u.codigoUniversitario = "ADM-INICIAL";
      u.nombre = "Administrador inicial";
      u.email = email.trim().toLowerCase(Locale.ROOT);
      u.passwordHash = passwords.encode(password);
      u.rol = "Administrador";
      u.activo = true;
      u.fechaRegistro = Instant.now();
      users.save(u);
    }
  }

  @Transactional
  public void resetDemo() throws Exception {
    Rules.check(
        Arrays.asList(env.getActiveProfiles()).contains("dev"),
        "Operación exclusiva de desarrollo.");
    jdbc.execute(
        "truncate"
            + " evidencias,notificaciones,historial,comentarios,reservas,solicitudes,usuario_areas,usuarios,ambiente_equipamiento,ambientes,categorias,edificios,facultades,areas,prioridades,configuracion,contadores");
    demo();
  }

  private <T> T from(JsonNode n, Class<T> type) {
    return json.convertValue(n, type);
  }

  private Evento event(JsonNode node, String solicitud, String reserva) {
    var h = from(node, Evento.class);
    h.id = Rules.id();
    h.solicitudId = solicitud;
    h.reservaId = reserva;
    return h;
  }

  private void demo() throws Exception {
    var in = getClass().getResourceAsStream("/demo.json");
    if (in == null) throw new IllegalStateException("Falta demo.json");
    var root = json.readTree(in);
    c.seedDefinitions(root);
    for (var n : root.path("usuarios")) {
      var u = from(n, Usuario.class);
      u.passwordHash = passwords.encode(n.path("password").asText());
      users.save(u);
    }
    c.seedAmbientes(root);
    for (var n : root.path("solicitudes")) {
      var s = from(n, Solicitud.class);
      requests.save(s);
      for (var cn : n.path("comentarios")) {
        var cm = from(cn, Comentario.class);
        cm.solicitudId = s.id;
        comments.save(cm);
      }
      for (var hn : n.path("historial")) history.save(event(hn, s.id, null));
    }
    for (var n : root.path("reservas")) {
      var r = from(n, Reserva.class);
      reservations.save(r);
      for (var hn : n.path("historial")) history.save(event(hn, null, r.id));
    }
    for (var n : root.path("notificaciones")) notifications.save(from(n, Notificacion.class));
    c.seedConfiguration(root);
    users.flush();
    requests.flush();
    reservations.flush();
    jdbc.update(
        "insert into contadores(tipo,anio,valor) select"
            + " 'SOL',split_part(codigo,'-',2)::int,max(split_part(codigo,'-',3)::bigint) from"
            + " solicitudes group by split_part(codigo,'-',2) on conflict(tipo,anio) do update set"
            + " valor=excluded.valor");
    jdbc.update(
        "insert into contadores(tipo,anio,valor) select"
            + " 'RES',split_part(codigo,'-',2)::int,max(split_part(codigo,'-',3)::bigint) from"
            + " reservas group by split_part(codigo,'-',2) on conflict(tipo,anio) do update set"
            + " valor=excluded.valor");
  }
}
