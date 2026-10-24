package pe.edu.unmsm.sigesu;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import pe.edu.unmsm.sigesu.archivos.services.SupabaseStorage;

class StorageTest {
  @Test
  void privateStorageUsesServerCredentialAndShortExpiry() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicInteger uploads = new AtomicInteger();
    AtomicReference<String> expires = new AtomicReference<>();
    server.createContext(
        "/storage/v1/",
        e -> {
          assertEquals("Bearer server-test-key", e.getRequestHeaders().getFirst("Authorization"));
          String reply = "{}";
          if (e.getRequestURI().getPath().contains("/object/sign/")) {
            expires.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            reply = "{\"signedURL\":\"/object/sign/private/file.pdf?token=temporary\"}";
          } else uploads.incrementAndGet();
          byte[] b = reply.getBytes();
          e.sendResponseHeaders(200, b.length);
          e.getResponseBody().write(b);
          e.close();
        });
    server.start();
    try {
      String base = "http://127.0.0.1:" + server.getAddress().getPort();
      var storage =
          new SupabaseStorage(base, "server-test-key", "private", 300, new ObjectMapper());
      storage.upload("file.pdf", "application/pdf", "%PDF-1".getBytes());
      String link = storage.signedUrl("file.pdf");
      assertTrue(link.startsWith(base + "/storage/v1/object/sign/private/"));
      assertEquals(300, new ObjectMapper().readTree(expires.get()).get("expiresIn").asInt());
      assertEquals(1, uploads.get());
    } finally {
      server.stop(0);
    }
  }
}
