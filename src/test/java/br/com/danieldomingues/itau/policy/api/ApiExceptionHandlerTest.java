package br.com.danieldomingues.itau.policy.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  // método dummy apenas para criar MethodParameter válido
  void dummy(String p) {}

  @Test
  @DisplayName("handleValidation -> 400 + corpo com errors[field,message]")
  void handleValidation_shouldReturn400() throws NoSuchMethodException {
    BindingResult br = new BeanPropertyBindingResult(new Object(), "request");
    br.addError(new FieldError("request", "customerId", "must not be null"));

    MethodParameter mp =
        new MethodParameter(this.getClass().getDeclaredMethod("dummy", String.class), 0);

    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, br);

    ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex);

    assertThat(resp.getStatusCode().value()).isEqualTo(400);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().get("error")).isEqualTo("Validation failed");
    @SuppressWarnings("unchecked")
    List<Map<String, String>> errors = (List<Map<String, String>>) resp.getBody().get("errors");
    assertThat(errors).isNotEmpty();
    assertThat(errors.get(0).get("field")).isEqualTo("customerId");
  }

  @Test
  @DisplayName("handleConstraint -> 400 + corpo com errors[property,message]")
  void handleConstraint_shouldReturn400() {
    @SuppressWarnings("unchecked")
    ConstraintViolation<Object> cv = (ConstraintViolation<Object>) mock(ConstraintViolation.class);
    Path path = mock(Path.class);
    when(path.toString()).thenReturn("request.customerId");
    when(cv.getPropertyPath()).thenReturn(path);
    when(cv.getMessage()).thenReturn("must not be null");

    ConstraintViolationException ex = new ConstraintViolationException(Set.of(cv));

    ResponseEntity<Map<String, Object>> resp = handler.handleConstraint(ex);

    assertThat(resp.getStatusCode().value()).isEqualTo(400);
    assertThat(resp.getBody()).isNotNull();
    @SuppressWarnings("unchecked")
    List<Map<String, String>> errors = (List<Map<String, String>>) resp.getBody().get("errors");
    assertThat(errors.get(0).get("property")).contains("customerId");
    assertThat(errors.get(0).get("message")).isEqualTo("must not be null");
  }

  @Test
  @DisplayName("handleIllegalArgument -> 404 + mensagem do erro")
  void handleIllegalArgument_shouldReturn404() {
    ResponseEntity<Map<String, Object>> resp =
        handler.handleIllegalArgument(new IllegalArgumentException("Solicitation not found"));
    assertThat(resp.getStatusCode().value()).isEqualTo(404);
    assertThat(resp.getBody().get("error")).isEqualTo("Solicitation not found");
  }

  @Test
  @DisplayName("handleIllegalState -> 400 + mensagem do erro")
  void handleIllegalState_shouldReturn400() {
    ResponseEntity<Map<String, Object>> resp =
        handler.handleIllegalState(new IllegalStateException("Invalid state"));
    assertThat(resp.getStatusCode().value()).isEqualTo(400);
    assertThat(resp.getBody().get("error")).isEqualTo("Invalid state");
  }
}
