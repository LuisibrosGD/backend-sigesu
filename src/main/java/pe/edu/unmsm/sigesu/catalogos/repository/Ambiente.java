package pe.edu.unmsm.sigesu.catalogos.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "ambientes")
public class Ambiente {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "codigo")
  public String codigo;

  @Column(name = "nombre")
  public String nombre;

  @Column(name = "tipo")
  public String tipo;

  @Column(name = "facultad_id")
  public String facultadId;

  @Column(name = "edificio_nombre")
  public String edificioNombre;

  @Column(name = "ubicacion_piso")
  public String ubicacionPiso;

  @Column(name = "capacidad")
  public int capacidad;

  @Column(name = "area_id")
  public String areaId;

  @Column(name = "horario_inicio")
  public LocalTime horarioInicio;

  @Column(name = "horario_fin")
  public LocalTime horarioFin;

  @Column(name = "activo")
  public boolean activo;

  @Version public long version;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "ambiente_equipamiento", joinColumns = @JoinColumn(name = "ambiente_id"))
  @Column(name = "equipo")
  public Set<String> equipamiento = new HashSet<>();
}
