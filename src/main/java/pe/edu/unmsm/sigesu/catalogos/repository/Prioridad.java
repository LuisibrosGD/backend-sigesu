package pe.edu.unmsm.sigesu.catalogos.repository;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "prioridades")
public class Prioridad {
  @Id
  @Column(length = 80)
  public String nivel;

  @Column(name = "plazo_horas")
  public int plazoHoras;

  @Column(name = "descripcion")
  public String descripcion;

  @Column(name = "orden")
  public int orden;
}
