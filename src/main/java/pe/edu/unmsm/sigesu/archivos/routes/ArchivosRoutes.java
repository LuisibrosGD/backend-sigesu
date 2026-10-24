package pe.edu.unmsm.sigesu.archivos.routes;

import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.unmsm.sigesu.application.services.Projections;
import pe.edu.unmsm.sigesu.archivos.services.ArchivosService;
import pe.edu.unmsm.sigesu.identidad.services.Actor;

@RestController
public class ArchivosRoutes {
  private final Actor actor;
  private final ArchivosService service;
  private final Projections views;

  public ArchivosRoutes(Actor a, ArchivosService s, Projections v) {
    actor = a;
    service = s;
    views = v;
  }

  @PostMapping(
      value = "/api/solicitudes/{id}/evidencias",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Object upload(@PathVariable String id, @RequestPart("file") MultipartFile file) {
    return views.evidencia(service.upload(actor.require(), id, file));
  }

  @GetMapping("/api/evidencias/{id}/url")
  public ResponseEntity<?> url(@PathVariable String id) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.link(actor.require(), id));
  }

  @GetMapping("/api/evidencias/{id}/contenido")
  public ResponseEntity<Void> download(@PathVariable String id) {
    return ResponseEntity.status(302)
        .cacheControl(CacheControl.noStore())
        .location(URI.create(service.link(actor.require(), id).url()))
        .build();
  }
}
