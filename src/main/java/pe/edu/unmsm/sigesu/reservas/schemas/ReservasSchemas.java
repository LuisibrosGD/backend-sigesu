package pe.edu.unmsm.sigesu.reservas.schemas;

import jakarta.validation.constraints.*;
import java.time.*;

public final class ReservasSchemas {
  public record Ocupacion(
      String id,
      String ambienteId,
      LocalDate fecha,
      LocalTime horaInicio,
      LocalTime horaFin,
      String estado) {}

  public record Nueva(
      @NotBlank String ambienteId,
      @NotNull LocalDate fecha,
      @NotNull LocalTime horaInicio,
      @NotNull LocalTime horaFin,
      @NotBlank String motivo,
      @NotBlank String descripcionActividad,
      @Min(1) int cantidadParticipantes) {}

  public record Accion(String observacion, String motivoRechazo, String motivoCancelacion) {}
}
