package pe.edu.unmsm.sigesu;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.*;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.context.*;
import org.springframework.web.bind.annotation.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@Import(CsrfConcurrencyIntegrationTest.RaceConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class CsrfConcurrencyIntegrationTest {
  private static final String ATTRIBUTE = HttpSessionCsrfTokenRepository.class.getName() + ".CSRF_TOKEN";

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    r.add("spring.flyway.url", postgres::getJdbcUrl);
    r.add("spring.flyway.user", postgres::getUsername);
    r.add("spring.flyway.password", postgres::getPassword);
    r.add("app.bootstrap.email", () -> "");
    r.add("app.bootstrap.password", () -> "");
  }

  SessionRepository<Session> sessions;

  @Autowired
  @SuppressWarnings("unchecked")
  void sessionRepository(JdbcIndexedSessionRepository repository) {
    // JdbcSession is package-private; exercise it through the public Session API.
    sessions = (SessionRepository<Session>) (SessionRepository<?>) repository;
  }
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired RaceState race;
  @LocalServerPort int port;

  @Test
  void concurrentSessionSnapshotsPreserveOneAttributeWithoutDuplicateKey() throws Exception {
    var initial = sessions.createSession();
    sessions.save(initial);
    try {
      var first = sessions.findById(initial.getId());
      var second = sessions.findById(initial.getId());
      assertNull(first.getAttribute(ATTRIBUTE));
      assertNull(second.getAttribute(ATTRIBUTE));
      first.setAttribute(ATTRIBUTE, new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "first"));
      second.setAttribute(ATTRIBUTE, new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "second"));
      var barrier = new CyclicBarrier(2);
      try (var executor = Executors.newFixedThreadPool(2)) {
        var a = executor.submit(() -> { await(barrier); sessions.save(first); });
        var b = executor.submit(() -> { await(barrier); sessions.save(second); });
        a.get(20, TimeUnit.SECONDS);
        b.get(20, TimeUnit.SECONDS);
      }
      CsrfToken stored = sessions.findById(initial.getId()).getAttribute(ATTRIBUTE);
      assertTrue(Set.of("first", "second").contains(stored.getToken()));
      assertEquals(1, jdbc.queryForObject(
          "SELECT count(*) FROM SPRING_SESSION_ATTRIBUTES a JOIN SPRING_SESSION s"
              + " ON s.PRIMARY_ID = a.SESSION_PRIMARY_ID WHERE s.SESSION_ID = ? AND a.ATTRIBUTE_NAME = ?",
          Integer.class, initial.getId(), ATTRIBUTE));
    } finally {
      sessions.deleteById(initial.getId());
    }
  }

  @Test
  void realCookiesRecoverLosingTokenBeforeBusinessOperationAndLogout() throws Exception {
    race.operations.set(0);
    var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    try (var client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10)).build()) {
      var missing = request(client, "POST", "/api/auth/login", null, "{}");
      assertEquals(403, missing.statusCode());
      assertEquals("CSRF_INVALID", json.readTree(missing.body()).path("code").asText());
      String beforeLogin = token(request(client, "GET", "/api/auth/csrf", null, null));
      var login = request(client, "POST", "/api/auth/login", beforeLogin,
          "{\"email\":\"estudiante@demo.edu.pe\",\"password\":\"Demo2026!\"}");
      assertEquals(200, login.statusCode(), login.body());
      assertFalse(json.readTree(login.body()).has("passwordHash"));
      assertFalse(json.readTree(login.body()).has("password"));
      assertTrue(cookies.getCookieStore().getCookies().stream()
          .anyMatch(c -> c.getName().equals("SIGESU_SESSION") && c.isHttpOnly()));
      assertEquals(200, request(client, "GET", "/api/auth/me", null, null).statusCode());

      // Both servlet requests load the session without CSRF before either generates its token.
      race.gate.set(new CyclicBarrier(2));
      List<String> tokens;
      try (var executor = Executors.newFixedThreadPool(2)) {
        var a = executor.submit(() -> token(request(client, "GET", "/api/auth/csrf?race=true", null, null)));
        var b = executor.submit(() -> token(request(client, "GET", "/api/auth/csrf?race=true", null, null)));
        tokens = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
      } finally {
        race.gate.set(null);
      }
      var first = request(client, "POST", "/api/test/csrf-probe", tokens.get(0), "{}");
      var second = request(client, "POST", "/api/test/csrf-probe", tokens.get(1), "{}");
      assertEquals(Set.of(200, 403), Set.of(first.statusCode(), second.statusCode()));
      var rejected = first.statusCode() == 403 ? first : second;
      assertEquals("CSRF_INVALID", json.readTree(rejected.body()).path("code").asText());
      assertEquals(1, race.operations.get(), "CSRF rejection must precede the controller");

      String fresh = token(request(client, "GET", "/api/auth/csrf", null, null));
      assertEquals(200, request(client, "POST", "/api/test/csrf-probe", fresh, "{}").statusCode());
      assertEquals(2, race.operations.get());
      var denied = request(client, "GET", "/api/test/csrf-denied", null, null);
      assertEquals(403, denied.statusCode());
      assertFalse(json.readTree(denied.body()).has("code"));
      assertEquals(200, request(client, "POST", "/api/auth/logout", fresh, null).statusCode());
      assertEquals(401, request(client, "GET", "/api/auth/me", null, null).statusCode());
    }
  }

  private HttpResponse<String> request(HttpClient client, String method, String path, String token, String body)
      throws Exception {
    var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
        .timeout(Duration.ofSeconds(15));
    if (token != null) builder.header("X-CSRF-TOKEN", token);
    if (body != null) builder.header("Content-Type", "application/json");
    return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
  }

  private String token(HttpResponse<String> response) throws Exception {
    assertEquals(200, response.statusCode(), response.body());
    assertEquals("X-CSRF-TOKEN", json.readTree(response.body()).path("headerName").asText());
    return json.readTree(response.body()).path("token").asText();
  }

  private static void await(CyclicBarrier barrier) {
    try {
      barrier.await(10, TimeUnit.SECONDS);
    } catch (Exception ex) {
      throw new IllegalStateException("Concurrent CSRF test did not reach its barrier", ex);
    }
  }

  static class RaceState {
    final AtomicReference<CyclicBarrier> gate = new AtomicReference<>();
    final AtomicInteger operations = new AtomicInteger();
  }

  @TestConfiguration
  static class RaceConfiguration {
    @Bean RaceState raceState() { return new RaceState(); }
    @Bean ProbeController probeController(RaceState state) { return new ProbeController(state); }

    @Bean
    FilterRegistrationBean<Filter> csrfRaceBarrier(RaceState state) {
      Filter filter = (request, response, chain) -> {
        var req = (jakarta.servlet.http.HttpServletRequest) request;
        var barrier = state.gate.get();
        if (barrier != null && req.getRequestURI().equals("/api/auth/csrf")
            && "true".equals(req.getParameter("race"))) {
          assertNotNull(req.getSession(false));
          assertNull(req.getSession(false).getAttribute(ATTRIBUTE));
          await(barrier);
        }
        chain.doFilter(request, response);
      };
      var registration = new FilterRegistrationBean<>(filter);
      registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER + 1);
      return registration;
    }
  }

  @RestController
  @org.springframework.boot.test.context.TestComponent
  static class ProbeController {
    private final RaceState state;
    ProbeController(RaceState state) { this.state = state; }
    @PostMapping("/api/test/csrf-probe")
    Map<String, Integer> probe() { return Map.of("operations", state.operations.incrementAndGet()); }
    @GetMapping("/api/test/csrf-denied")
    void denied() { throw new AccessDeniedException("Denied for test"); }
  }
}
