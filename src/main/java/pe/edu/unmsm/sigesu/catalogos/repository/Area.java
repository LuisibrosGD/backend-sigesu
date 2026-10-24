package pe.edu.unmsm.sigesu.catalogos.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "areas")
public class Area {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "nombre")
  public String nombre;

  @Column(name = "codigo")
  public String codigo;

  @Column(name = "descripcion")
  public String descripcion;

  @Column(name = "correo_contacto")
  public String correoContacto;

  @Column(name = "ubicacion_oficina")
  public String ubicacionOficina;

  @Column(name = "activo")
  public boolean activo;
}
