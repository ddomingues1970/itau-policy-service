package br.com.danieldomingues.itau.policy.api;

import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    List<Map<String, String>> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                fe -> {
                  Map<String, String> e = new HashMap<>();
                  e.put("field", fe.getField());
                  e.put("message", fe.getDefaultMessage());
                  return e;
                })
            .toList();

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", OffsetDateTime.now().toString());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Validation failed");
    body.put("errors", errors);

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex) {
    List<Map<String, String>> errors =
        ex.getConstraintViolations().stream()
            .map(
                cv -> {
                  Map<String, String> e = new HashMap<>();
                  e.put("property", cv.getPropertyPath().toString());
                  e.put("message", cv.getMessage());
                  return e;
                })
            .toList();

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", OffsetDateTime.now().toString());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Constraint violation");
    body.put("errors", errors);

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", OffsetDateTime.now().toString());
    body.put("status", HttpStatus.NOT_FOUND.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", OffsetDateTime.now().toString());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", ex.getMessage());
    return ResponseEntity.badRequest().body(body);
  }
}
