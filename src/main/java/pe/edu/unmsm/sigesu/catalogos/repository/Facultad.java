package pe.edu.unmsm.sigesu.catalogos.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "facultades")
public class Facultad {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "nombre")
  public String nombre;

  @Column(name = "siglas")
  public String siglas;

  @Column(name = "decano")
  public String decano;

  @Column(name = "activo")
  public boolean activo;
}
