package pe.edu.unmsm.sigesu.solicitudes.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "solicitudes")
public class Solicitud {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "codigo")
  public String codigo;

  @Column(name = "solicitante_id")
  public String solicitanteId;

  @Column(name = "categoria_id")
  public String categoriaId;

  @Column(name = "titulo")
  public String titulo;

  @Column(name = "descripcion")
  public String descripcion;

  @Column(name = "facultad_id")
  public String facultadId;

  @Column(name = "edificio_id")
  public String edificioId;

  @Column(name = "ubicacion_especifica")
  public String ubicacionEspecifica;

  @Column(name = "prioridad")
  public String prioridad;

  @Column(name = "fecha_registro")
  public Instant fechaRegistro;

  @Column(name = "fecha_limite")
  public Instant fechaLimite;

  @Column(name = "estado")
  public String estado;

  @Column(name = "area_id")
  public String areaId;

  @Column(name = "tecnico_id")
  public String tecnicoId;

  @Column(name = "fecha_evaluacion")
  public Instant fechaEvaluacion;

  @Column(name = "fecha_asignacion")
  public Instant fechaAsignacion;

  @Column(name = "fecha_inicio_atencion")
  public Instant fechaInicioAtencion;

  @Column(name = "fecha_resolucion")
  public Instant fechaResolucion;

  @Column(name = "fecha_cierre")
  public Instant fechaCierre;

  @Column(name = "solucion_descripcion")
  public String solucionDescripcion;

  @Column(name = "atendida_fuera_de_plazo")
  public boolean atendidaFueraDePlazo;

  // Read-only fallback for records whose workflow timestamps are still empty.
  @com.fasterxml.jackson.annotation.JsonIgnore
  @org.hibernate.annotations.Formula(
      "(select max(h.fecha) from historial h where h.solicitud_id = id)")
  public Instant ultimaFechaHistorial;

  @Version public long version;
}
