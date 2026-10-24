package pe.edu.unmsm.sigesu.catalogos.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "edificios")
public class Edificio {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "facultad_id")
  public String facultadId;

  @Column(name = "nombre")
  public String nombre;

  @Column(name = "pisos")
  public int pisos;

  @Column(name = "referencia")
  public String referencia;

  @Column(name = "activo")
  public boolean activo;
}
