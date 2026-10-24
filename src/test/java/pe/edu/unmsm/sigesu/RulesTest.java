package pe.edu.unmsm.sigesu;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pe.edu.unmsm.sigesu.archivos.services.ArchivosService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;
import pe.edu.unmsm.sigesu.solicitudes.repository.Solicitud;

class RulesTest {
  @Test
  void roleAndOwnershipCannotBeBypassed() {
    var s = new Solicitud();
    s.solicitanteId = "owner";
    s.areaId = "ti";
    s.tecnicoId = "tech";
    var u = new Usuario();
    u.id = "stranger";
    u.activo = true;
    u.rol = "Estudiante";
    assertFalse(Rules.view(u, s));
    u.id = "owner";
    assertTrue(Rules.view(u, s));
    u.activo = false;
    assertFalse(Rules.view(u, s));
    u.activo = true;
    u.rol = "Supervisor";
    assertFalse(Rules.view(u, s));
    u.areasIds.add("ti");
    assertTrue(Rules.view(u, s));
    u.rol = "Técnico";
    assertFalse(Rules.view(u, s));
    u.id = "tech";
    assertTrue(Rules.view(u, s));
  }

  @Test
  void rejectsSpoofedAndUnsupportedFiles() {
    assertEquals("application/pdf", ArchivosService.detect("%PDF-1.7".getBytes()));
    assertEquals(
        "image/png", ArchivosService.detect(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}));
    assertEquals(
        "image/jpeg", ArchivosService.detect(new byte[] {(byte) 255, (byte) 216, (byte) 255}));
    assertThrows(ApiException.class, () -> ArchivosService.detect("<svg/>".getBytes()));
    assertThrows(ApiException.class, () -> ArchivosService.detect(new byte[0]));
  }
}
