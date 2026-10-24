package pe.edu.unmsm.sigesu;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import pe.edu.unmsm.sigesu.archivos.repository.*;
import pe.edu.unmsm.sigesu.archivos.services.*;
import pe.edu.unmsm.sigesu.catalogos.repository.Configuracion;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.ApiException;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;
import pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesService;
import pe.edu.unmsm.sigesu.solicitudes.repository.*;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesService;

class ArchivosServiceTest {
  private static class Fixture {
    final EvidenciaRepository repo = mock(EvidenciaRepository.class);
    final SolicitudesService requests = mock(SolicitudesService.class);
    final SolicitudRepository requestRepo = mock(SolicitudRepository.class);
    final CatalogosService cats = mock(CatalogosService.class);
    final RecordingEvidenceStorage storage = new RecordingEvidenceStorage();
    final PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
    final SimpleTransactionStatus status = new SimpleTransactionStatus();
    final NotificacionesService events = mock(NotificacionesService.class);
    final Usuario actor = new Usuario();
    final ArchivosService service;

    Fixture() {
      actor.id = "actor";
      actor.rol = "Administrador";
      actor.activo = true;
      var request = new Solicitud();
      request.id = "request";
      request.estado = "Registrada";
      when(requests.get(actor, "request")).thenReturn(request);
      when(requestRepo.lockById("request")).thenReturn(Optional.of(request));
      var config = new Configuracion();
      config.tamanoMaximoAdjuntoMB = 2;
      when(cats.config()).thenReturn(config);
      when(tx.getTransaction(any())).thenReturn(status);
      when(repo.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
      service = new ArchivosService(repo, requests, requestRepo, cats, storage, tx, events);
    }

    MockMultipartFile file() {
      return new MockMultipartFile("file", "prueba.pdf", "application/pdf", "%PDF-1.7".getBytes());
    }
  }

  @Test
  void storageReplacementSupportsUploadLinkAndCommit() {
    var f = new Fixture();
    var evidence = f.service.upload(f.actor, "request", f.file());
    assertTrue(f.storage.objects.containsKey(evidence.storagePath));
    verify(f.tx).commit(f.status);
    verify(f.tx, never()).rollback(any());
    when(f.repo.findById(evidence.id)).thenReturn(Optional.of(evidence));
    var link = f.service.link(f.actor, evidence.id);
    assertEquals(f.storage.signedUrl(evidence.storagePath), link.url());
    assertEquals(300, link.expiresIn());
  }

  @Test
  void metadataFailureRollsBackAndDeletesUploadedObject() {
    var f = new Fixture();
    var failure = ApiException.conflict("Metadata failure");
    doThrow(failure).when(f.repo).saveAndFlush(any());
    assertSame(
        failure,
        assertThrows(ApiException.class, () -> f.service.upload(f.actor, "request", f.file())));
    verify(f.tx).rollback(f.status);
    verify(f.tx, never()).commit(any());
    assertEquals(1, f.storage.deletes);
    assertTrue(f.storage.objects.isEmpty());
  }

  @Test
  void failedCompensationDoesNotHideOriginalFailure() {
    var f = new Fixture();
    f.storage.failDelete = true;
    var failure = ApiException.conflict("Metadata failure");
    doThrow(failure).when(f.repo).saveAndFlush(any());
    assertSame(
        failure,
        assertThrows(ApiException.class, () -> f.service.upload(f.actor, "request", f.file())));
    verify(f.tx).rollback(f.status);
    assertEquals(1, f.storage.deletes);
  }

  @Test
  void invalidFileDoesNotReachStorageOrTransaction() {
    var f = new Fixture();
    var invalid = new MockMultipartFile("file", "bad.pdf", "application/pdf", "invalid".getBytes());
    assertThrows(ApiException.class, () -> f.service.upload(f.actor, "request", invalid));
    assertEquals(0, f.storage.uploads);
    verifyNoInteractions(f.tx);
  }

  @Test
  void developmentResetAlsoUsesStorageContract() throws Exception {
    var f = new Fixture();
    var initial = mock(pe.edu.unmsm.sigesu.application.services.InitialData.class);
    var evidence = new Evidencia();
    evidence.id = "file";
    evidence.storagePath = "request/file.pdf";
    when(f.repo.findAll()).thenReturn(List.of(evidence));
    var reset =
        new pe.edu.unmsm.sigesu.application.services.DevResetService(initial, f.repo, f.storage);
    reset.reset(f.actor);
    assertEquals(1, f.storage.deletes);
    verify(f.repo).deleteById("file");
    verify(initial).resetDemo();
  }
}
