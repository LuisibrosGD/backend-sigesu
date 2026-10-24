package pe.edu.unmsm.sigesu.catalogos.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "configuracion")
public class Configuracion {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "nombre_institucion")
  public String nombreInstitucion;

  @Column(name = "siglas_institucion")
  public String siglasInstitucion;

  @Column(name = "periodo_academico")
  public String periodoAcademico;

  @Column(name = "correo_soporte_general")
  public String correoSoporteGeneral;

  @Column(name = "telefono_central")
  public String telefonoCentral;

  @Column(name = "permitir_registro_publico")
  public boolean permitirRegistroPublico;

  @Column(name = "tamano_maximo_adjunto_mb")
  public int tamanoMaximoAdjuntoMB;

  @Column(name = "dias_maximos_antelacion_reserva")
  public int diasMaximosAntelacionReserva;

  @Column(name = "mensaje_institucional")
  public String mensajeInstitucional;
}
