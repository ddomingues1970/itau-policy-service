package br.com.danieldomingues.itau.policy.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.danieldomingues.itau.policy.service.SolicitationService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SolicitationController.class)
@Import(ApiExceptionHandler.class)
class SolicitationCancelControllerIT {

  @Autowired private MockMvc mockMvc;

  @MockBean private SolicitationService service;

  @Test
  @DisplayName("DELETE /solicitations/{id} -> 204 quando cancelamento ocorre com sucesso")
  void cancel_ok_returns204() throws Exception {
    doNothing().when(service).cancel(any(UUID.class));

    mockMvc
        .perform(
            delete("/solicitations/{id}", UUID.randomUUID()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("DELETE /solicitations/{id} -> 404 quando solicitação não existe")
  void cancel_notFound_returns404() throws Exception {
    doThrow(new IllegalArgumentException("Solicitation not found"))
        .when(service)
        .cancel(any(UUID.class));

    mockMvc
        .perform(
            delete("/solicitations/{id}", UUID.randomUUID()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("DELETE /solicitations/{id} -> 400 quando status terminal (regra de negócio)")
  void cancel_ruleViolation_returns400() throws Exception {
    doThrow(new IllegalStateException("Cannot cancel a solicitation with terminal status"))
        .when(service)
        .cancel(any(UUID.class));

    mockMvc
        .perform(
            delete("/solicitations/{id}", UUID.randomUUID()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }
}
