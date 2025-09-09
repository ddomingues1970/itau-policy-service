package br.com.danieldomingues.itau.policy.api;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.service.FraudValidationService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SolicitationValidationController.class)
@AutoConfigureMockMvc
class SolicitationValidationControllerIT {

  @Autowired private MockMvc mockMvc;

  @MockBean private FraudValidationService fraudValidationService;

  private static final UUID ID = UUID.randomUUID();
  private static final UUID CUSTOMER_ID = UUID.fromString("adc56d77-348c-4bf0-908f-22d402ee715c");
  private static final String PRODUCT_ID = "1b2da7cc-b367-4196-8a78-9cfeec21f587";

  // -------- helpers --------

  private Solicitation baseEntity() {
    Solicitation s =
        Solicitation.builder()
            .id(ID)
            .customerId(CUSTOMER_ID)
            .productId(PRODUCT_ID)
            .category(Category.AUTO)
            .salesChannel("MOBILE")
            .paymentMethod("CREDIT_CARD")
            .totalMonthlyPremiumAmount(new BigDecimal("75.25"))
            .insuredAmount(new BigDecimal("275000.50"))
            .build();
    s.getCoverages()
        .putAll(
            Map.of(
                "Roubo", new BigDecimal("100000.25"),
                "Perda Total", new BigDecimal("100000.25"),
                "Colisao com Terceiros", new BigDecimal("75000.00")));
    s.getAssistances().addAll(List.of("Guincho até 250km", "Troca de Óleo", "Chaveiro 24h"));
    OffsetDateTime created = OffsetDateTime.now();
    s.setCreatedAt(created);
    s.setStatus(Status.RECEIVED);
    s.addHistory(Status.RECEIVED, created);
    return s;
  }

  // -------- tests --------

  @Test
  @DisplayName("POST /solicitations/{id}/validate -> 200 VALIDATED com histórico")
  void validate_shouldReturn200Validated() throws Exception {
    Solicitation validated = baseEntity();
    validated.setStatus(Status.VALIDATED);
    validated.addHistory(Status.VALIDATED, OffsetDateTime.now());

    when(fraudValidationService.validate(ArgumentMatchers.eq(ID))).thenReturn(validated);

    mockMvc
        .perform(post("/solicitations/{id}/validate", ID).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(ID.toString())))
        .andExpect(jsonPath("$.customerId", is(CUSTOMER_ID.toString())))
        .andExpect(jsonPath("$.productId", is(PRODUCT_ID)))
        .andExpect(jsonPath("$.status", is("VALIDATED")))
        .andExpect(jsonPath("$.finishedAt").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.history[0].status", is("RECEIVED")))
        .andExpect(jsonPath("$.history[1].status", is("VALIDATED")));
  }

  @Test
  @DisplayName("POST /solicitations/{id}/validate -> 200 REJECTED com finishedAt")
  void validate_shouldReturn200Rejected() throws Exception {
    Solicitation rejected = baseEntity();
    rejected.setStatus(Status.REJECTED);
    OffsetDateTime now = OffsetDateTime.now();
    rejected.addHistory(Status.REJECTED, now);
    rejected.setFinishedAt(now);

    when(fraudValidationService.validate(ArgumentMatchers.eq(ID))).thenReturn(rejected);

    mockMvc
        .perform(post("/solicitations/{id}/validate", ID).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(ID.toString())))
        .andExpect(jsonPath("$.status", is("REJECTED")))
        .andExpect(jsonPath("$.finishedAt", notNullValue()))
        .andExpect(jsonPath("$.history[0].status", is("RECEIVED")))
        .andExpect(jsonPath("$.history[1].status", is("REJECTED")));
  }
}
