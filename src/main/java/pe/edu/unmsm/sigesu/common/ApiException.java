package pe.edu.unmsm.sigesu.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  public final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public static ApiException bad(String s) {
    return new ApiException(HttpStatus.BAD_REQUEST, s);
  }

  public static ApiException denied() {
    return new ApiException(HttpStatus.FORBIDDEN, "No tiene permisos para esta operación.");
  }

  public static ApiException missing() {
    return new ApiException(HttpStatus.NOT_FOUND, "Registro no encontrado.");
  }

  public static ApiException conflict(String s) {
    return new ApiException(HttpStatus.CONFLICT, s);
  }
}
