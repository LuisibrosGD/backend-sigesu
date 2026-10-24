package pe.edu.unmsm.sigesu.identidad.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
  @Id
  @Column(length = 80)
  public String id;

  @Column(name = "codigo_universitario")
  public String codigoUniversitario;

  @Column(name = "nombre")
  public String nombre;

  @Column(name = "email")
  public String email;

  @Column(name = "password_hash")
  public String passwordHash;

  @Column(name = "rol")
  public String rol;

  @Column(name = "facultad_id")
  public String facultadId;

  @Column(name = "telefono")
  public String telefono;

  @Column(name = "cargo")
  public String cargo;

  @Column(name = "activo")
  public boolean activo;

  @Column(name = "fecha_registro")
  public Instant fechaRegistro;

  @Version public long version;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "usuario_areas", joinColumns = @JoinColumn(name = "usuario_id"))
  @Column(name = "area_id")
  public Set<String> areasIds = new HashSet<>();
}
