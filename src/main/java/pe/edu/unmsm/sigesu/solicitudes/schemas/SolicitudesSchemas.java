package pe.edu.unmsm.sigesu.solicitudes.schemas;

import jakarta.validation.constraints.*;

public final class SolicitudesSchemas {
  public record Nueva(
      @NotBlank String categoriaId,
      @NotBlank @Size(min = 5, max = 200) String titulo,
      @NotBlank @Size(min = 10, max = 10000) String descripcion,
      @NotBlank String facultadId,
      @NotBlank String edificioId,
      @NotBlank String ubicacionEspecifica,
      @NotBlank String prioridad) {}

  public record Editar(
      @NotBlank String categoriaId,
      @NotBlank @Size(min = 5, max = 200) String titulo,
      @NotBlank @Size(min = 10, max = 10000) String descripcion,
      @NotBlank String facultadId,
      @NotBlank String edificioId,
      @NotBlank String ubicacionEspecifica) {}

  public record Accion(
      String categoriaId,
      String prioridad,
      boolean pasarAEnEvaluacion,
      String motivoCambio,
      String tecnicoId,
      String motivo,
      String notaInicial,
      String solucionDescripcion,
      String comentarioCierre,
      String motivoDevolucion) {}

  public record ComentarioInput(@NotBlank @Size(max = 10000) String texto, boolean esInterno) {}
}
