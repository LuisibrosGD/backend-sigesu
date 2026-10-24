package pe.edu.unmsm.sigesu.notificaciones.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "notificaciones")
public class Notificacion {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "usuario_id")
  public String usuarioId;

  @Column(name = "tipo")
  public String tipo;

  @Column(name = "titulo")
  public String titulo;

  @Column(name = "mensaje")
  public String mensaje;

  @Column(name = "fecha")
  public Instant fecha;

  @Column(name = "leida")
  public boolean leida;

  @Column(name = "referencia_tipo")
  public String referenciaTipo;

  @Column(name = "referencia_id")
  public String referenciaId;

  @Column(name = "referencia_codigo")
  public String referenciaCodigo;

  @Column(name = "clave_unica_vencimiento")
  public String claveUnicaVencimiento;
}
