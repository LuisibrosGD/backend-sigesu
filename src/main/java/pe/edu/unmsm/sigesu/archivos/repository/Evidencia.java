package pe.edu.unmsm.sigesu.archivos.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "evidencias")
public class Evidencia {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "solicitud_id")
  public String solicitudId;

  @Column(name = "nombre_archivo")
  public String nombreArchivo;

  @Column(name = "tipo_mime")
  public String tipoMime;

  @Column(name = "tamano_bytes")
  public long tamanoBytes;

  @Column(name = "storage_path")
  public String storagePath;

  @Column(name = "subido_por_id")
  public String subidoPorId;

  @Column(name = "fecha_subida")
  public Instant fechaSubida;
}
