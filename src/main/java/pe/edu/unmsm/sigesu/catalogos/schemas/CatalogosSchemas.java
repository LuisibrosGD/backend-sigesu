package pe.edu.unmsm.sigesu.catalogos.schemas;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class CatalogosSchemas {
  public record AreaInput(
      String id,
      @NotBlank String nombre,
      @NotBlank String codigo,
      @NotNull String descripcion,
      @Email String correoContacto,
      @NotNull String ubicacionOficina,
      boolean activo) {}

  public record CategoriaInput(
      String id,
      @NotBlank String nombre,
      @NotBlank
          @Pattern(regexp = "Mantenimiento|Infraestructura|Soporte informático|Administrativa")
          String tipoServicio,
      @NotBlank String areaId,
      @NotBlank String prioridadSugerida,
      @NotNull String descripcion,
      boolean activo) {}

  public record EdificioInput(
      String id, @NotBlank String nombre, @Min(1) int pisos, String referencia, boolean activo) {}

  public record FacultadInput(
      String id,
      @NotBlank String nombre,
      @NotBlank String siglas,
      String decano,
      boolean activo,
      @NotNull List<@jakarta.validation.Valid EdificioInput> edificios) {}

  public record AmbienteInput(
      String id,
      @NotBlank String codigo,
      @NotBlank String nombre,
      @NotBlank String tipo,
      @NotBlank String facultadId,
      @NotBlank String edificioNombre,
      @NotBlank String ubicacionPiso,
      @Min(1) int capacidad,
      @NotBlank String areaId,
      @NotNull LocalTime horarioInicio,
      @NotNull LocalTime horarioFin,
      @NotNull Set<String> equipamiento,
      boolean activo) {}

  public record PrioridadInput(
      @NotBlank String nivel,
      @Min(1) @Max(720) int plazoHoras,
      @NotNull String descripcion,
      @Min(1) @Max(4) int orden) {}

  public record ConfigInput(
      @NotBlank String nombreInstitucion,
      @NotBlank String siglasInstitucion,
      @NotBlank String periodoAcademico,
      @NotBlank @Email String correoSoporteGeneral,
      @NotBlank String telefonoCentral,
      boolean permitirRegistroPublico,
      @Min(1) @Max(5) int tamanoMaximoAdjuntoMB,
      @Min(1) @Max(365) int diasMaximosAntelacionReserva,
      @NotNull String mensajeInstitucional) {}
}
