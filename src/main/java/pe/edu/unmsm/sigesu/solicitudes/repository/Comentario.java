package pe.edu.unmsm.sigesu.solicitudes.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "comentarios")
public class Comentario {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "solicitud_id")
  public String solicitudId;

  @Column(name = "autor_id")
  public String autorId;

  @Column(name = "texto")
  public String texto;

  @Column(name = "es_interno")
  public boolean esInterno;

  @Column(name = "fecha")
  public Instant fecha;
}
