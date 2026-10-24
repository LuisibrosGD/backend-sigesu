package pe.edu.unmsm.sigesu.archivos.services;

/** Private evidence objects; implementations must preserve failure and expiry semantics. */
public interface EvidenceStorage {
  void upload(String path, String mime, byte[] bytes);

  void delete(String path);

  String signedUrl(String path);

  int ttl();
}
