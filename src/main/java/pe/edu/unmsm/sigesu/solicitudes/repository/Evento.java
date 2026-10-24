package pe.edu.unmsm.sigesu.solicitudes.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "historial")
public class Evento {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "solicitud_id")
  public String solicitudId;

  @Column(name = "reserva_id")
  public String reservaId;

  @Column(name = "actor_id")
  public String actorId;

  @Column(name = "actor_nombre")
  public String actorNombre;

  @Column(name = "actor_rol")
  public String actorRol;

  @Column(name = "accion")
  public String accion;

  @Column(name = "estado_anterior")
  public String estadoAnterior;

  @Column(name = "estado_nuevo")
  public String estadoNuevo;

  @Column(name = "motivo")
  public String motivo;

  @Column(name = "detalles")
  public String detalles;

  @Column(name = "fecha")
  public Instant fecha;
}
