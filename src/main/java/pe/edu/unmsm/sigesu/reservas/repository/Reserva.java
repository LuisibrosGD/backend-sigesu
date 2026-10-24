package pe.edu.unmsm.sigesu.reservas.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "reservas")
public class Reserva {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "codigo")
  public String codigo;

  @Column(name = "ambiente_id")
  public String ambienteId;

  @Column(name = "solicitante_id")
  public String solicitanteId;

  @Column(name = "fecha")
  public LocalDate fecha;

  @Column(name = "hora_inicio")
  public LocalTime horaInicio;

  @Column(name = "hora_fin")
  public LocalTime horaFin;

  @Column(name = "motivo")
  public String motivo;

  @Column(name = "descripcion_actividad")
  public String descripcionActividad;

  @Column(name = "cantidad_participantes")
  public int cantidadParticipantes;

  @Column(name = "estado")
  public String estado;

  @Column(name = "motivo_rechazo_o_cancelacion")
  public String motivoRechazoOCancelacion;

  @Column(name = "aprobado_por_id")
  public String aprobadoPorId;

  @Column(name = "fecha_registro")
  public Instant fechaRegistro;

  @Version public long version;
}
