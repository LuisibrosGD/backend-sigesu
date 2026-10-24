package pe.edu.unmsm.sigesu.common;

import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class Errors {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<?> domain(ApiException e) {
    return ResponseEntity.status(e.status).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    Map<String, String> errors = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> errors.put(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest()
        .body(Map.of("message", "Revise los campos ingresados.", "errors", errors));
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<?> authentication() {
    return ResponseEntity.status(401).body(Map.of("message", "Credenciales inválidas."));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "message",
                "La operación entra en conflicto con los datos actuales. Actualice e intente"
                    + " nuevamente."));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    jakarta.validation.ConstraintViolationException.class,
    org.springframework.web.method.annotation.HandlerMethodValidationException.class
  })
  ResponseEntity<?> malformed() {
    return ResponseEntity.badRequest().body(Map.of("message", "Formato de datos inválido."));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<?> size() {
    return ResponseEntity.status(413)
        .body(Map.of("message", "El archivo excede el límite permitido."));
  }
}
