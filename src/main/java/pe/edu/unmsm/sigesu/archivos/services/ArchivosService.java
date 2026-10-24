package pe.edu.unmsm.sigesu.archivos.services;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.unmsm.sigesu.archivos.repository.*;
import pe.edu.unmsm.sigesu.archivos.schemas.ArchivoSchemas.*;
import pe.edu.unmsm.sigesu.catalogos.services.CatalogosService;
import pe.edu.unmsm.sigesu.common.*;
import pe.edu.unmsm.sigesu.identidad.repository.Usuario;
import pe.edu.unmsm.sigesu.notificaciones.services.NotificacionesService;
import pe.edu.unmsm.sigesu.solicitudes.repository.SolicitudRepository;
import pe.edu.unmsm.sigesu.solicitudes.services.SolicitudesService;

@Service
public class ArchivosService {
  private final EvidenciaRepository repo;
  private final SolicitudesService requests;
  private final SolicitudRepository solicitudRepo;
  private final CatalogosService cats;
  private final EvidenceStorage storage;
  private final TransactionTemplate tx;
  private final NotificacionesService events;
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(ArchivosService.class);

  public ArchivosService(
      EvidenciaRepository r,
      SolicitudesService s,
      SolicitudRepository sr,
      CatalogosService c,
      EvidenceStorage st,
      PlatformTransactionManager tm,
      NotificacionesService n) {
    repo = r;
    requests = s;
    solicitudRepo = sr;
    cats = c;
    storage = st;
    tx = new TransactionTemplate(tm);
    events = n;
  }

  public static String detect(byte[] b) {
    if (b.length >= 8
        && Arrays.equals(Arrays.copyOf(b, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
      return "image/png";
    if (b.length >= 3 && (b[0] & 255) == 255 && (b[1] & 255) == 216 && (b[2] & 255) == 255)
      return "image/jpeg";
    if (b.length >= 5
        && new String(b, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-"))
      return "application/pdf";
    throw ApiException.bad("Solo se permiten archivos PDF, JPG y PNG válidos.");
  }

  public Evidencia upload(Usuario u, String id, MultipartFile file) {
    var s = requests.get(u, id);
    Rules.check(!s.estado.equals("Cerrada"), "La solicitud está cerrada.");
    Rules.check(
        !file.isEmpty()
            && file.getSize() <= (long) cats.config().tamanoMaximoAdjuntoMB * 1024 * 1024,
        "El archivo excede el tamaño permitido.");
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (java.io.IOException ex) {
      throw ApiException.bad("No se pudo leer el archivo.");
    }
    String mime = detect(bytes);
    Rules.check(
        mime.equals(file.getContentType()), "El contenido y el tipo del archivo no coinciden.");
    String ext = mime.equals("image/png") ? "png" : mime.equals("image/jpeg") ? "jpg" : "pdf";
    String original =
        Objects.requireNonNullElse(file.getOriginalFilename(), "archivo." + ext)
            .replaceAll("[\\\\/\\r\\n]", "_");
    Rules.check(original.length() <= 200, "Nombre de archivo demasiado largo.");
    String lower = original.toLowerCase(Locale.ROOT);
    Rules.check(
        lower.endsWith("." + ext) || ext.equals("jpg") && lower.endsWith(".jpeg"),
        "Extensión de archivo inválida.");
    var e = new Evidencia();
    e.id = Rules.id();
    e.solicitudId = id;
    e.nombreArchivo = original;
    e.tipoMime = mime;
    e.tamanoBytes = bytes.length;
    e.subidoPorId = u.id;
    e.fechaSubida = Instant.now();
    e.storagePath = "solicitudes/" + id + "/" + e.id + "." + ext;
    storage.upload(e.storagePath, mime, bytes);
    try {
      return tx.execute(
          status -> {
            var locked = solicitudRepo.lockById(id).orElseThrow(ApiException::missing);
            if (!Rules.view(u, locked)) throw ApiException.denied();
            Rules.check(!locked.estado.equals("Cerrada"), "La solicitud está cerrada.");
            repo.saveAndFlush(e);
            events.event(u, id, null, "Archivo adjuntado", locked.estado, locked.estado, null);
            events.solicitud(locked, "COMENTARIO_SOLICITUD", "Nueva evidencia adjuntada.", false);
            return e;
          });
    } catch (RuntimeException ex) {
      try {
        storage.delete(e.storagePath);
      } catch (RuntimeException cleanup) {
        log.error("No se pudo compensar carga de evidencia {}", e.id);
      }
      throw ex;
    }
  }

  public Enlace link(Usuario u, String id) {
    var e = repo.findById(id).orElseThrow(ApiException::missing);
    requests.get(u, e.solicitudId);
    return new Enlace(storage.signedUrl(e.storagePath), storage.ttl());
  }
}
