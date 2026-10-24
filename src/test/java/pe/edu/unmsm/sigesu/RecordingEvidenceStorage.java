package pe.edu.unmsm.sigesu;

import java.util.*;
import pe.edu.unmsm.sigesu.archivos.services.EvidenceStorage;

/** Test replacement: no external service or credentials. */
final class RecordingEvidenceStorage implements EvidenceStorage {
  final Map<String, byte[]> objects = new HashMap<>();
  int uploads, deletes;
  boolean failDelete;

  @Override
  public void upload(String path, String mime, byte[] bytes) {
    uploads++;
    objects.put(path, bytes.clone());
  }

  @Override
  public void delete(String path) {
    deletes++;
    if (failDelete) throw new IllegalStateException("Simulated storage failure");
    objects.remove(path);
  }

  @Override
  public String signedUrl(String path) {
    return "https://storage.example.test/" + path;
  }

  @Override
  public int ttl() {
    return 300;
  }
}
