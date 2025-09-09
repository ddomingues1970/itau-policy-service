package br.com.danieldomingues.itau.policy.api;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.service.SolicitationService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

/**
 * Integration (web slice) tests for SolicitationController using MockMvc.
 */
@WebMvcTest(controllers = SolicitationController.class)
@AutoConfigureMockMvc
class SolicitationControllerIT {

  @Autowired private MockMvc mockMvc;

  @MockBean private SolicitationService service;

  private static final UUID CUSTOMER_ID = UUID.fromString("adc56d77-348c-4bf0-908f-22d402ee715c");
  private static final String PRODUCT_ID = "1b2da7cc-b367-4196-8a78-9cfeec21f587";

  @Test
  @DisplayName("POST /solicitations -> 201 Created + Location + body {id, createdAt}")
  void create_shouldReturn201AndBody() throws Exception {
    UUID generatedId = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.now();

    Solicitation saved = minimalEntity(generatedId, createdAt);
    when(service.create(ArgumentMatchers.any())).thenReturn(saved);

    String requestJson =
        """
        {
          "customerId": "%s",
          "productId": "%s",
          "category": "AUTO",
          "salesChannel": "MOBILE",
          "paymentMethod": "CREDIT_CARD",
          "totalMonthlyPremiumAmount": 75.25,
          "insuredAmount": 275000.50,
          "coverages": {
            "Roubo": 100000.25,
            "Perda Total": 100000.25,
            "Colisao com Terceiros": 75000.00
          },
          "assistances": [
            "Guincho até 250km",
            "Troca de Óleo",
            "Chaveiro 24h"
          ]
        }
        """
            .formatted(CUSTOMER_ID, PRODUCT_ID);

    mockMvc
        .perform(
            post("/solicitations").contentType(MediaType.APPLICATION_JSON).content(requestJson))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/solicitations/" + generatedId)))
        .andExpect(jsonPath("$.id", is(generatedId.toString())))
        .andExpect(jsonPath("$.createdAt", notNullValue()));
  }

  @Test
  @DisplayName("GET /solicitations/{id} -> 200 + DTO completo + history size = 1")
  void getById_shouldReturn200AndDto() throws Exception {
    UUID id = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.now();
    Solicitation s = fullEntity(id, createdAt);

    when(service.getWithHistoryById(id)).thenReturn(Optional.of(s));

    mockMvc
        .perform(get("/solicitations/{id}", id))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(id.toString())))
        .andExpect(jsonPath("$.customerId", is(CUSTOMER_ID.toString())))
        .andExpect(jsonPath("$.productId", is(PRODUCT_ID)))
        .andExpect(jsonPath("$.category", is("AUTO")))
        .andExpect(jsonPath("$.salesChannel", is("MOBILE")))
        .andExpect(jsonPath("$.paymentMethod", is("CREDIT_CARD")))
        .andExpect(jsonPath("$.totalMonthlyPremiumAmount", is(75.25)))
        .andExpect(jsonPath("$.insuredAmount", is(275000.50)))
        .andExpect(jsonPath("$.coverages.Roubo", is(100000.25)))
        .andExpect(jsonPath("$.assistances", hasSize(3)))
        .andExpect(jsonPath("$.status", is("RECEIVED")))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.finishedAt", nullValue()))
        .andExpect(jsonPath("$.history", hasSize(1)))
        .andExpect(jsonPath("$.history[0].status", is("RECEIVED")));
  }

  @Test
  @DisplayName("GET /solicitations/{id} -> 404 quando não encontrado")
  void getById_shouldReturn404_whenNotFound() throws Exception {
    UUID randomId = UUID.randomUUID();
    when(service.getWithHistoryById(randomId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/solicitations/{id}", randomId)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /solicitations?customerId=... -> 200 + lista com a solicitação")
  void listByCustomer_shouldReturn200AndList() throws Exception {
    OffsetDateTime createdAt = OffsetDateTime.now();
    Solicitation s = fullEntity(UUID.randomUUID(), createdAt);
    when(service.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(s));

    mockMvc
        .perform(get("/solicitations").param("customerId", CUSTOMER_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].customerId", is(CUSTOMER_ID.toString())))
        .andExpect(jsonPath("$[0].history", hasSize(1)));
  }

  @Test
  @DisplayName("GET /solicitations sem customerId -> 400")
  void listByCustomer_shouldReturn400_whenMissingParam() throws Exception {
    mockMvc.perform(get("/solicitations")).andExpect(status().isBadRequest());
  }

  // -------- helpers --------

  private Solicitation minimalEntity(UUID id, OffsetDateTime createdAt) {
    Solicitation s =
        Solicitation.builder()
            .id(id)
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
    s.setStatus(Status.RECEIVED);
    s.setCreatedAt(createdAt);
    s.addHistory(Status.RECEIVED, createdAt);
    return s;
  }

  private Solicitation fullEntity(UUID id, OffsetDateTime createdAt) {
    return minimalEntity(id, createdAt);
  }
}
