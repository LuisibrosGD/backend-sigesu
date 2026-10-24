package pe.edu.unmsm.sigesu.identidad.schemas;

import jakarta.validation.constraints.*;
import java.util.*;

public final class IdentidadSchemas {
  public record Login(@NotBlank @Email String email, @NotBlank String password) {}

  public record Registro(
      @NotBlank @Size(min = 5, max = 200) String nombre,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 6, max = 80) String codigoUniversitario,
      @NotBlank String facultadId,
      @NotNull String telefono,
      @NotBlank @Size(min = 6, max = 72) String password) {}

  public record Perfil(
      @NotBlank @Size(min = 5, max = 200) String nombre,
      @NotNull String telefono,
      @NotNull String cargo) {}

  public record Clave(
      @NotBlank String currentPassword, @NotBlank @Size(min = 6, max = 72) String newPassword) {}

  public record Verificar(@NotBlank String currentPassword) {}

  public record UsuarioInput(
      String id,
      @NotBlank String codigoUniversitario,
      @NotBlank String nombre,
      @NotBlank @Email String email,
      @Size(min = 6, max = 72) String password,
      @NotBlank @Pattern(regexp = "Estudiante|Técnico|Supervisor|Administrador") String rol,
      String facultadId,
      @NotNull Set<String> areasIds,
      String telefono,
      String cargo,
      boolean activo,
      String reasignarTecnicoId) {}
}
