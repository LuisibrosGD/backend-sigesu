package pe.edu.unmsm.sigesu;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.*;
import pe.edu.unmsm.sigesu.reservas.schemas.ReservasSchemas;
import pe.edu.unmsm.sigesu.reservas.services.ReservasService;
import pe.edu.unmsm.sigesu.solicitudes.schemas.SolicitudesSchemas.*;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
class WorkflowIntegrationTest {
  @DynamicPropertySource
  static void db(DynamicPropertyRegistry r) {
    r.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    r.add(
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener",
        () -> "OFF");
    r.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
    r.add(
        "spring.datasource.username",
        () -> System.getenv().getOrDefault("TEST_DATABASE_USERNAME", "sigesu"));
    r.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", ""));
    r.add("spring.flyway.url", () -> System.getenv("TEST_DATABASE_URL"));
    r.add(
        "spring.flyway.user",
        () -> System.getenv().getOrDefault("TEST_DATABASE_USERNAME", "sigesu"));
    r.add(
        "spring.flyway.password", () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", ""));
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UsuarioRepository users;
  @Autowired CatalogosService cats;
  @Autowired SolicitudesService service;
  @Autowired ReservasService reservations;
  @Autowired Projections views;
  @Autowired PlatformTransactionManager tm;
  @Autowired pe.edu.unmsm.sigesu.identidad.services.AdminUsuariosService admin;
  @Autowired pe.edu.unmsm.sigesu.identidad.services.IdentidadService identity;
  @Autowired JdbcTemplate jdbc;

  Usuario user(String id) {
    return users.findById(id).orElseThrow();
  }

  Nueva request() {
    var cat =
        cats.listCategorias().stream()
            .filter(c -> c.activo && c.areaId.equals("area-ti"))
            .findFirst()
            .orElseThrow();
    var e = cats.edificiosDeFacultad("fac-ing").getFirst();
    return new Nueva(
        cat.id,
        "Prueba de soporte",
        "Descripción suficientemente detallada para prueba",
        "fac-ing",
        e.id,
        "Laboratorio",
        "Media");
  }

  Accion action(String tech, String solution) {
    return new Accion(null, null, false, null, tech, null, null, solution, null, null);
  }

  @Test
  void authenticationNeedsCsrfAndNeverExposesPassword() throws Exception {
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"estudiante@demo.edu.pe\",\"password\":\"Demo2026!\"}"))
        .andExpect(status().isForbidden());
    var response = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
    String token = json.readTree(response.getResponse().getContentAsString()).get("token").asText();
    var session = (MockHttpSession) response.getRequest().getSession();
    var login =
        mvc.perform(
                post("/api/auth/login")
                    .cookie(response.getResponse().getCookie("SIGESU_SESSION"))
                    .header("X-CSRF-TOKEN", token)
                    .contentType("application/json")
                    .content("{\"email\":\"estudiante@demo.edu.pe\",\"password\":\"Demo2026!\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rol").value("Estudiante"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn();
    assertNotNull(login.getResponse().getCookie("SIGESU_SESSION"));
    mvc.perform(get("/api/auth/me").cookie(login.getResponse().getCookie("SIGESU_SESSION")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rol").value("Estudiante"));
  }

  @Test
  @Transactional
  void lifecycleAndInternalCommentsRespectRoles() {
    var student = user("usr-est-1");
    var tech = user("usr-tec-1");
    var supervisor = user("usr-sup-1");
    var s = service.create(student, request());
    assertThrows(ApiException.class, () -> service.get(user("usr-est-2"), s.id));
    assertThrows(
        ApiException.class, () -> service.action(student, s.id, "asignar", action(tech.id, null)));
    service.comment(supervisor, s.id, new ComentarioInput("Comentario interno", true));
    assertEquals(0, ((List<?>) views.solicitud(s, student).get("comentarios")).size());
    assertEquals(1, ((List<?>) views.solicitud(s, supervisor).get("comentarios")).size());
    service.action(supervisor, s.id, "asignar", action(tech.id, null));
    service.action(tech, s.id, "iniciar", action(null, null));
    service.action(tech, s.id, "resolver", action(null, "Se corrigió el problema reportado"));
    service.action(supervisor, s.id, "cerrar", action(null, null));
    assertEquals("Cerrada", service.get(student, s.id).estado);
    assertThrows(
        ApiException.class,
        () -> service.comment(student, s.id, new ComentarioInput("No permitido", false)));
  }

  @Test
  @Transactional
  void hashesAreStoredAndNotExposed() {
    assertTrue(user("usr-est-1").passwordHash.startsWith("$2"));
    mvcNoop();
  }

  private void mvcNoop() {
    assertFalse(views.usuario(user("usr-est-1")).containsKey("passwordHash"));
  }

  @Test
  void concurrentReservationsCannotOverlap() throws Exception {
    var u = user("usr-est-1");
    var room = cats.listAmbientes().stream().filter(a -> a.activo).findFirst().orElseThrow();
    var date = LocalDate.now(Rules.LIMA).plusDays(28);
    var p =
        new ReservasSchemas.Nueva(
            room.id,
            date,
            room.horarioInicio,
            room.horarioInicio.plusHours(1),
            "Prueba concurrente",
            "Actividad de prueba",
            1);
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    List<String> created = new CopyOnWriteArrayList<>();
    Callable<Boolean> task =
        () -> {
          start.await();
          try {
            created.add(reservations.create(u, p).id);
            return true;
          } catch (ApiException ex) {
            assertEquals(409, ex.status.value());
            return false;
          }
        };
    try {
      var a = executor.submit(task);
      var b = executor.submit(task);
      start.countDown();
      assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
      assertEquals(1, created.size());
    } finally {
      executor.shutdownNow();
      new TransactionTemplate(tm)
          .execute(
              status -> {
                for (var id : created) {
                  jdbc.update("delete from notificaciones where referencia_id=?", id);
                  jdbc.update("delete from historial where reserva_id=?", id);
                  jdbc.update("delete from reservas where id=?", id);
                }
                return null;
              });
    }
  }

  @Test
  @Transactional
  void lastAdminCannotBeDisabled() {
    var a = user("usr-adm-1");
    for (var other : users.findByRolAndActivoTrue("Administrador")) {
      if (!other.id.equals(a.id)) {
        other.activo = false;
        users.save(other);
      }
    }
    var p =
        new pe.edu.unmsm.sigesu.identidad.schemas.IdentidadSchemas.UsuarioInput(
            a.id,
            a.codigoUniversitario,
            a.nombre,
            a.email,
            null,
            a.rol,
            a.facultadId,
            a.areasIds,
            a.telefono,
            a.cargo,
            false,
            null);
    assertThrows(ApiException.class, () -> admin.save(a, p));
  }

  @Test
  @Transactional
  void technicianWithPendingWorkNeedsReplacement() {
    var a = user("usr-adm-1");
    for (var other : users.findByRolAndActivoTrue("Administrador")) {
      if (!other.id.equals(a.id)) {
        other.activo = false;
        users.save(other);
      }
    }
    var t = user("usr-tec-1");
    var p =
        new pe.edu.unmsm.sigesu.identidad.schemas.IdentidadSchemas.UsuarioInput(
            t.id,
            t.codigoUniversitario,
            t.nombre,
            t.email,
            null,
            t.rol,
            t.facultadId,
            t.areasIds,
            t.telefono,
            t.cargo,
            false,
            null);
    assertThrows(ApiException.class, () -> admin.save(a, p));
  }

  @Test
  @Transactional
  void publicRegistrationCannotEscalateRole() {
    var p =
        new pe.edu.unmsm.sigesu.identidad.schemas.IdentidadSchemas.Registro(
            "Usuario Integración",
            "integration-test@example.org",
            "TEST-REG-01",
            "fac-ing",
            "",
            "Secret123!");
    var u = identity.register(p);
    assertEquals("Estudiante", u.rol);
    assertTrue(u.areasIds.isEmpty());
    assertTrue(u.passwordHash.startsWith("$2"));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class, () -> identity.register(p));
  }

  @Test
  void occupancyDoesNotDiscloseOtherUsersDetails() {
    var slots = reservations.occupancy(user("usr-est-1"));
    assertFalse(slots.isEmpty());
    var fields = json.convertValue(slots.getFirst(), java.util.Map.class);
    assertEquals(
        java.util.Set.of("id", "ambienteId", "fecha", "horaInicio", "horaFin", "estado"),
        fields.keySet());
    assertTrue(reservations.occupancy(user("usr-tec-1")).isEmpty());
  }

  @Test
  void invalidPayloadGets400() throws Exception {
    mvc.perform(
            post("/api/solicitudes")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user("usr-est-1"))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  private Map<String, Object> stableTimeline(Map<String, Object> value) {
    var result = new LinkedHashMap<>(value);
    for (String key : List.of("historial", "comentarios")) {
      if (value.get(key) instanceof List<?> entries) {
        var sorted = new ArrayList<>((List<Map<String, Object>>) entries);
        sorted.sort(
            Comparator.comparing((Map<String, Object> item) -> String.valueOf(item.get("fecha")))
                .thenComparing(item -> String.valueOf(item.get("id"))));
        result.put(key, sorted);
      }
    }
    return result;
  }

  @Autowired pe.edu.unmsm.sigesu.application.services.BootstrapService bootstrap;
  @Autowired pe.edu.unmsm.sigesu.application.services.BatchProjections batch;
  @Autowired jakarta.persistence.EntityManagerFactory emf;
  @Autowired jakarta.persistence.EntityManager em;
  @Autowired pe.edu.unmsm.sigesu.solicitudes.repository.SolicitudRepository requestRepo;

  @Test
  @Transactional
  void batchPreservesPayloadAndPermissionsForEveryRole() {
    for (String id : List.of("usr-est-1", "usr-tec-1", "usr-sup-1", "usr-adm-1")) {
      var actor = user(id);
      var all = requestRepo.findAll();
      var visible = service.visible(actor);
      assertEquals(
          all.stream()
              .filter(s -> Rules.view(actor, s))
              .map(s -> s.id)
              .collect(java.util.stream.Collectors.toSet()),
          visible.stream().map(s -> s.id).collect(java.util.stream.Collectors.toSet()));
      var reservas = reservations.visible(actor);
      var snapshot = batch.load(visible, reservas, List.of());
      for (var request : visible)
        assertEquals(
            stableTimeline(views.solicitud(request, actor)),
            stableTimeline(snapshot.solicitud(request, actor)));
      for (var reserva : reservas)
        assertEquals(
            stableTimeline(views.reserva(reserva)), stableTimeline(snapshot.reserva(reserva)));
      var data = bootstrap.bootstrap(actor);
      assertFalse(json.valueToTree(data).toString().contains("passwordHash"));
      if (actor.rol.equals("Estudiante")) {
        for (var request : visible) {
          var comments =
              (List<Map<String, Object>>) snapshot.solicitud(request, actor).get("comentarios");
          assertTrue(comments.stream().noneMatch(c -> Boolean.TRUE.equals(c.get("esInterno"))));
        }
      }
      if (actor.rol.equals("Técnico")) assertTrue(reservas.isEmpty());
    }
  }

  @Test
  @Transactional
  void renderingBatchSnapshotExecutesNoQueries() {
    var actor = user("usr-adm-1");
    var requests = service.visible(actor);
    var rs = reservations.visible(actor);
    var snapshot = batch.load(requests, rs, List.of());
    var stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    stats.clear();
    snapshot.catalogs();
    requests.forEach(s -> snapshot.solicitud(s, actor));
    rs.forEach(snapshot::reserva);
    assertEquals(0, stats.getPrepareStatementCount());
  }

  @Test
  @Transactional
  void bootstrapQueryCountDoesNotGrowPerRequest() {
    var actor = user("usr-adm-1");
    em.flush();
    em.clear();
    var stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    stats.clear();
    bootstrap.bootstrap(actor);
    long before = stats.getPrepareStatementCount();
    var template = requestRepo.findAll().getFirst();
    for (int i = 0; i < 100; i++) {
      var copy =
          json.convertValue(
              views.plain(template), pe.edu.unmsm.sigesu.solicitudes.repository.Solicitud.class);
      copy.id = Rules.id();
      copy.codigo = "SOL-2099-" + (800000 + i);
      requestRepo.save(copy);
    }
    em.flush();
    em.clear();
    stats.clear();
    bootstrap.bootstrap(actor);
    long after = stats.getPrepareStatementCount();
    assertEquals(15, before);
    assertEquals(15, after);
    assertTrue(after <= before + 2, "Queries grew from " + before + " to " + after);
    assertTrue(after <= 30, "Expected at most 30 batch queries, got " + after);
    System.out.println("BOOTSTRAP_BATCH_QUERIES before=" + before + " after100=" + after);
  }

  @Autowired pe.edu.unmsm.sigesu.archivos.repository.EvidenciaRepository evidenceRepo;

  @Test
  @Transactional
  void batchEvidencePreservesMetadataWithoutExposingStoragePath() {
    var actor = user("usr-est-1");
    var request = service.visible(actor).getFirst();
    var evidence = new pe.edu.unmsm.sigesu.archivos.repository.Evidencia();
    evidence.id = Rules.id();
    evidence.solicitudId = request.id;
    evidence.nombreArchivo = "prueba.pdf";
    evidence.tipoMime = "application/pdf";
    evidence.tamanoBytes = 123;
    evidence.storagePath = "private/test.pdf";
    evidence.subidoPorId = actor.id;
    evidence.fechaSubida = Instant.now();
    evidenceRepo.saveAndFlush(evidence);
    var snapshot = batch.load(List.of(request), List.of(), List.of(actor));
    var actual = (List<Map<String, Object>>) snapshot.solicitud(request, actor).get("evidencias");
    assertEquals(List.of(views.evidencia(evidence)), actual);
    assertFalse(actual.getFirst().containsKey("storagePath"));
    assertEquals("/api/evidencias/" + evidence.id + "/contenido", actual.getFirst().get("dataUrl"));
  }

  @Autowired pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesReadService requestReads;
  @Autowired pe.edu.unmsm.sigesu.reservas.services.ReservasReadService reservationReads;
  @Autowired pe.edu.unmsm.sigesu.catalogos.services.CatalogosReadService catalogReads;

  @Autowired
  pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesReadService notificationReads;

  @Autowired pe.edu.unmsm.sigesu.identidad.services.AdminUsuariosReadService userReads;
  @Autowired pe.edu.unmsm.sigesu.application.services.HistorialReadService historyReads;
  @Autowired pe.edu.unmsm.sigesu.indicadores.services.DashboardReadService dashboard;

  @Test
  @Transactional
  void progressivePagesAreAuthorizedAndDoNotContainDetails() {
    for (String id : List.of("usr-est-1", "usr-tec-1", "usr-sup-1", "usr-adm-1")) {
      var actor = user(id);
      var allowed =
          service.visible(actor).stream()
              .map(r -> r.id)
              .collect(java.util.stream.Collectors.toSet());
      var page = requestReads.solicitudes(actor, Map.of("size", "100", "sort", "RECIENTES"));
      var rows = (List<Map<String, Object>>) page.get("content");
      assertEquals(
          allowed,
          rows.stream()
              .map(r -> (String) r.get("id"))
              .collect(java.util.stream.Collectors.toSet()));
      for (var row : rows)
        for (String field : List.of("historial", "comentarios", "evidencias"))
          assertFalse(row.containsKey(field));
      var res = (Map<String, Object>) reservationReads.reservas(actor, Map.of("size", "100"));
      assertEquals(
          reservations.visible(actor).size(), ((Number) res.get("totalElements")).intValue());
    }
    assertThrows(ApiException.class, () -> userReads.adminUsers(user("usr-est-1"), Map.of()));
    assertThrows(ApiException.class, () -> historyReads.history(user("usr-est-1"), Map.of()));
  }

  @Test
  @Transactional
  void thousandRequestsArePagedInSqlAndExportIncludesAllFilteredRows() {
    jdbc.execute(
        "insert into solicitudes select"
            + " (jsonb_populate_record(null::solicitudes,to_jsonb(s)||jsonb_build_object('id','load-test-'||g,'codigo','SOL-2098-'||g))).*"
            + " from solicitudes s cross join generate_series(1,1000) g where s.id=(select id from"
            + " solicitudes order by id limit 1)");
    em.flush();
    em.clear();
    var actor = user("usr-adm-1");
    em.clear();
    var stats = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
    stats.clear();
    var first = requestReads.solicitudes(actor, Map.of("size", "10", "page", "0"));
    long loaded = stats.getEntityLoadCount();
    long queries = stats.getPrepareStatementCount();
    assertEquals(10, ((List<?>) first.get("content")).size());
    assertTrue(((Number) first.get("totalElements")).longValue() >= 1000);
    assertEquals(40, loaded, "The same fixture must preserve bounded entity loading");
    assertEquals(7, queries, "The same page must preserve seven SQL statements");
    var second = requestReads.solicitudes(actor, Map.of("size", "10", "page", "1"));
    Set<Object> ids = new HashSet<>();
    for (var row : (List<Map<String, Object>>) first.get("content")) ids.add(row.get("id"));
    for (var row : (List<Map<String, Object>>) second.get("content"))
      assertFalse(ids.contains(row.get("id")));
    var exported = requestReads.exportRequests(actor, Map.of("search", "SOL-2098-"));
    assertEquals(1000, exported.size());
    System.out.println(
        "PROGRESSIVE_SQL_PAGE rows=10 total="
            + first.get("totalElements")
            + " entities="
            + loaded
            + " queries="
            + queries);
  }

  @Test
  @Transactional
  void sqlDashboardMatchesExistingRoleCountsAndDateFilters() {
    for (String id : List.of("usr-est-1", "usr-tec-1", "usr-sup-1", "usr-adm-1")) {
      var actor = user(id);
      var visible = service.visible(actor);
      var summary = (Map<String, Object>) dashboard.summary(actor, Map.of());
      assertEquals(visible.size(), ((Number) summary.get("totalRegistradas")).intValue());
      assertEquals(
          visible.stream().filter(r -> !Set.of("Resuelta", "Cerrada").contains(r.estado)).count(),
          ((Number) summary.get("totalPendientes")).longValue());
      assertTrue(((List<?>) summary.get("pendientesPrioritarios")).size() <= 10);
      var empty =
          (Map<String, Object>)
              dashboard.summary(
                  actor, Map.of("fechaDesde", "2099-01-01", "fechaHasta", "2099-01-01"));
      assertEquals(0, ((Number) empty.get("totalRegistradas")).intValue());
      assertEquals(visible.size(), ((Number) empty.get("totalVisibles")).intValue());
    }
  }

  @Test
  @Transactional
  void notificationsAndCalendarRemainBoundedAndPrivate() {
    var actor = user("usr-est-1");
    var page = (Map<String, Object>) notificationReads.notifications(actor, Map.of("size", "2"));
    assertTrue(((List<?>) page.get("content")).size() <= 2);
    for (var row : (List<Map<String, Object>>) page.get("content"))
      assertEquals(actor.id, row.get("usuarioId"));
    assertThrows(
        ApiException.class,
        () ->
            reservationReads.occupants(
                actor, Map.of("fechaDesde", "2026-01-01", "fechaHasta", "2027-01-01")));
    var slots =
        (List<Map<String, Object>>)
            reservationReads.occupants(
                actor, Map.of("fechaDesde", "2026-10-01", "fechaHasta", "2026-10-31"));
    for (var row : slots)
      assertEquals(
          Set.of("id", "ambienteId", "fecha", "horaInicio", "horaFin", "estado"), row.keySet());
    var catalog = catalogReads.catalogs(actor, "areas");
    assertEquals(Set.of("areas"), catalog.keySet());
  }

  @Test
  @Transactional
  void progressiveFiltersPreserveDeadlineSearchAndLimaDateBoundaries() {
    var actor = user("usr-adm-1");
    var visible = service.visible(actor);
    for (String state :
        List.of("Registrada", "En evaluación", "Asignada", "En atención", "Resuelta", "Cerrada")) {
      var result = requestReads.solicitudes(actor, Map.of("estado", state, "size", "100"));
      assertEquals(
          visible.stream().filter(s -> s.estado.equals(state)).count(),
          ((Number) result.get("totalElements")).longValue());
    }
    var inTime =
        requestReads.solicitudes(actor, Map.of("condicionPlazo", "EN_PLAZO", "size", "100"));
    assertEquals(
        visible.stream()
            .filter(
                s ->
                    Set.of("Resuelta", "Cerrada").contains(s.estado)
                        ? !s.atendidaFueraDePlazo
                        : !s.fechaLimite.isBefore(Instant.now()))
            .count(),
        ((Number) inTime.get("totalElements")).longValue());
    var template = requestRepo.findAll().getFirst();
    for (int n = 0; n < 2; n++) {
      var copy =
          json.convertValue(
              views.plain(template), pe.edu.unmsm.sigesu.solicitudes.repository.Solicitud.class);
      copy.id = Rules.id();
      copy.codigo = "SOL-2097-" + (100 + n);
      copy.titulo = "Lima boundary fixture";
      copy.fechaRegistro = Instant.parse(n == 0 ? "2026-10-23T04:59:59Z" : "2026-10-23T05:00:00Z");
      requestRepo.save(copy);
    }
    em.flush();
    var filtered =
        requestReads.solicitudes(
            actor,
            Map.of(
                "search",
                "Lima boundary fixture",
                "fechaDesde",
                "2026-10-23",
                "fechaHasta",
                "2026-10-23"));
    assertEquals(1, ((Number) filtered.get("totalElements")).intValue());
    var summary = (Map<String, Object>) dashboard.summary(actor, Map.of());
    var rows = service.visible(actor).stream().filter(s -> s.fechaInicioAtencion != null).toList();
    if (!rows.isEmpty()) {
      double average =
          rows.stream()
              .mapToDouble(
                  s ->
                      Math.max(
                          0,
                          Duration.between(s.fechaRegistro, s.fechaInicioAtencion).toMillis()
                              / 3600000.0))
              .average()
              .orElseThrow();
      assertEquals(
          average, ((Number) summary.get("promedioPrimeraAtencionHoras")).doubleValue(), 0.0001);
    }
    var catalog = catalogReads.catalogs(actor, "ambientes");
    for (var row : (List<Map<String, Object>>) catalog.get("ambientes")) {
      assertNotNull(row.get("facultadNombre"));
      assertNotNull(row.get("areaNombre"));
    }
  }

  @Test
  @Transactional
  void updateSortUsesHistoryWithoutDownloadingIt() {
    var created = service.create(user("usr-est-1"), request());
    em.flush();
    var changed = Instant.parse("2097-01-01T00:00:00Z");
    jdbc.update(
        "update historial set fecha=? where solicitud_id=?",
        java.sql.Timestamp.from(changed),
        created.id);
    em.clear();
    var page =
        requestReads.solicitudes(user("usr-adm-1"), Map.of("sort", "ACTUALIZACION", "size", "10"));
    var row = ((List<Map<String, Object>>) page.get("content")).getFirst();
    assertEquals(created.id, row.get("id"));
    assertEquals(changed, row.get("fechaUltimaActualizacion"));
    assertFalse(row.containsKey("historial"));
  }

  private void assertReadRoute(String actorId, String url, Object expected) throws Exception {
    var result =
        mvc.perform(
                get(url)
                    .with(
                        org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.user(actorId)))
            .andExpect(status().isOk())
            .andReturn();
    assertEquals(
        json.readTree(json.writeValueAsBytes(expected)),
        json.readTree(result.getResponse().getContentAsByteArray()));
  }

  @Test
  @Transactional
  void movedReadRoutesKeepTheirContractsForAllRoles() throws Exception {
    for (String id : List.of("usr-est-1", "usr-tec-1", "usr-sup-1", "usr-adm-1")) {
      var actor = user(id);
      var filters = Map.of("size", "2");
      assertReadRoute(id, "/api/solicitudes?size=2", requestReads.solicitudes(actor, filters));
      assertReadRoute(id, "/api/reservas?size=2", reservationReads.reservas(actor, filters));
      assertReadRoute(
          id, "/api/notificaciones/pagina?size=2", notificationReads.notifications(actor, filters));
      assertReadRoute(
          id, "/api/notificaciones/resumen", notificationReads.notificationSummary(actor));
      assertReadRoute(id, "/api/catalogos?include=areas", catalogReads.catalogs(actor, "areas"));
      assertReadRoute(id, "/api/catalogos", bootstrap.catalogs());
      assertReadRoute(
          id,
          "/api/dashboard/indicadores?countsOnly=true",
          dashboard.summary(actor, Map.of("countsOnly", "true")));
      if (actor.rol.equals("Administrador")) {
        assertReadRoute(id, "/api/admin/usuarios?size=2", userReads.adminUsers(actor, filters));
        assertReadRoute(id, "/api/admin/historial?size=2", historyReads.history(actor, filters));
        assertReadRoute(
            id,
            "/api/admin/usuarios/usr-tec-1/pendientes",
            userReads.pendingWork(actor, "usr-tec-1"));
      } else {
        for (String path :
            List.of(
                "/api/admin/usuarios",
                "/api/admin/historial",
                "/api/admin/usuarios/usr-tec-1/pendientes"))
          mvc.perform(
                  get(path)
                      .with(
                          org.springframework.security.test.web.servlet.request
                              .SecurityMockMvcRequestPostProcessors.user(id)))
              .andExpect(status().isForbidden());
      }
    }
  }

  @Test
  @Transactional
  void selectiveCatalogsPreserveLegacyValuesAndStaffVisibility() {
    String include = "areas,categorias,prioridades,facultades,ambientes,configuracion";
    for (String id : List.of("usr-est-1", "usr-tec-1", "usr-sup-1", "usr-adm-1")) {
      var actor = user(id);
      assertEquals(
          json.valueToTree(bootstrap.catalogs()),
          json.valueToTree(catalogReads.catalogs(actor, include)));
      var staff =
          (List<Map<String, Object>>) catalogReads.catalogs(actor, "tecnicos").get("usuarios");
      if (!Set.of("Administrador", "Supervisor").contains(actor.rol)) assertTrue(staff.isEmpty());
      for (var row : staff) {
        assertEquals(true, row.get("activo"));
        assertTrue(Set.of("Técnico", "Supervisor").contains(row.get("rol")));
        assertFalse(row.containsKey("passwordHash"));
        if (actor.rol.equals("Supervisor"))
          assertFalse(Collections.disjoint(actor.areasIds, (Collection<?>) row.get("areasIds")));
      }
    }
    assertThrows(ApiException.class, () -> catalogReads.catalogs(user("usr-adm-1"), "desconocido"));
  }

  @Test
  void evidenceMetadataIsReallyRolledBackBeforeCompensatingStorage() {
    var actor = user("usr-est-1");
    var request =
        service.visible(actor).stream()
            .filter(s -> !s.estado.equals("Cerrada"))
            .findFirst()
            .orElseThrow();
    var storage = new RecordingEvidenceStorage();
    var failingEvents =
        org.mockito.Mockito.mock(
            pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesService.class);
    var expected = ApiException.conflict("Failure after evidence flush");
    org.mockito.Mockito.doThrow(expected)
        .when(failingEvents)
        .event(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.isNull());
    var files =
        new pe.edu.unmsm.sigesu.archivos.services.ArchivosService(
            evidenceRepo, service, requestRepo, cats, storage, tm, failingEvents);
    long before = evidenceRepo.count();
    var file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "fixture.pdf", "application/pdf", "%PDF-1.7".getBytes());
    assertSame(
        expected, assertThrows(ApiException.class, () -> files.upload(actor, request.id, file)));
    assertEquals(
        before, evidenceRepo.count(), "The real transaction must roll back the flushed row");
    assertEquals(1, storage.uploads);
    assertEquals(1, storage.deletes);
    assertTrue(storage.objects.isEmpty());
  }
}
