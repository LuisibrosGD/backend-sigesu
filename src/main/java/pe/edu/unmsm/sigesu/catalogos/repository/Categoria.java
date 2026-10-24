package pe.edu.unmsm.sigesu.catalogos.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "categorias")
public class Categoria {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "nombre")
  public String nombre;

  @Column(name = "tipo_servicio")
  public String tipoServicio;

  @Column(name = "area_id")
  public String areaId;

  @Column(name = "prioridad_sugerida")
  public String prioridadSugerida;

  @Column(name = "descripcion")
  public String descripcion;

  @Column(name = "activo")
  public boolean activo;
}
