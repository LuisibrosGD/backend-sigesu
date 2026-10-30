package pe.edu.unmsm.sigesu.identidad.services;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.*;

class SecurityConfigTest {
  @Test
  void onlyCsrfFailuresExposeTheRecoverableCode() throws Exception {
    var expected = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "expected");
    var failures = new AccessDeniedException[] {
      new MissingCsrfTokenException(null),
      new InvalidCsrfTokenException(expected, "stale"),
      new AccessDeniedException("Sin permiso")
    };
    var handler = new SecurityConfig().csrfAwareAccessDeniedHandler();
    var json = new ObjectMapper();
    for (var failure : failures) {
      var response = new MockHttpServletResponse();
      handler.handle(new MockHttpServletRequest(), response, failure);
      assertEquals(403, response.getStatus());
      assertEquals("application/json;charset=UTF-8", response.getContentType());
      var body = json.readTree(response.getContentAsString());
      if (failure instanceof CsrfException) {
        assertEquals("CSRF_INVALID", body.path("code").asText());
        assertEquals("Token CSRF inválido o vencido.", body.path("message").asText());
      } else {
        assertFalse(body.has("code"));
        assertEquals("Permiso o token CSRF inválido.", body.path("message").asText());
      }
    }
  }
}
