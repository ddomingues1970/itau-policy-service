package br.com.danieldomingues.itau.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.integration.fraud.FraudClient;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckRequest;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckResponse;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudValidationServiceTest {

  @Mock SolicitationRepository repository;
  @Mock FraudClient fraudClient;

  @InjectMocks FraudValidationService service;

  private static final UUID SOLICITATION_ID = UUID.randomUUID();
  private static final UUID CUSTOMER_ID = UUID.fromString("adc56d77-348c-4bf0-908f-22d402ee715c");
  private static final String PRODUCT_ID = "1b2da7cc-b367-4196-8a78-9cfeec21f587";

  // ---- helpers

  private Solicitation newReceivedSolicitation() {
    Solicitation s =
        Solicitation.builder()
            .id(SOLICITATION_ID)
            .customerId(CUSTOMER_ID)
            .productId(PRODUCT_ID)
            .category(Category.AUTO)
            .salesChannel("MOBILE")
            .paymentMethod("CREDIT_CARD")
            .totalMonthlyPremiumAmount(new BigDecimal("75.25"))
            .insuredAmount(new BigDecimal("275000.50"))
            .build();
    // simulamos estado RECEBIDO (como após @PrePersist)
    s.setStatus(Status.RECEBIDO);
    s.setCreatedAt(OffsetDateTime.now());
    s.addHistory(Status.RECEBIDO, s.getCreatedAt());
    return s;
  }

  private void mockFound(Solicitation s) {
    // apenas findById aqui (sem stubbar save)
    given(repository.findById(SOLICITATION_ID)).willReturn(Optional.of(s));
  }

  private void mockSavePassthrough() {
    given(repository.save(ArgumentMatchers.any(Solicitation.class)))
        .willAnswer(inv -> inv.getArgument(0, Solicitation.class));
  }

  // ---- tests

  @Test
  @DisplayName("REGULAR -> VALIDADO (mantém finishedAt nulo e adiciona histórico)")
  void validate_regular() {
    Solicitation s = newReceivedSolicitation();
    mockFound(s);
    mockSavePassthrough();
    given(
            fraudClient.check(
                FraudCheckRequest.builder().customerId(CUSTOMER_ID).productId(PRODUCT_ID).build()))
        .willReturn(FraudCheckResponse.builder().classification("REGULAR").build());

    Solicitation result = service.validate(SOLICITATION_ID);

    assertThat(result.getStatus()).isEqualTo(Status.VALIDADO);
    assertThat(result.getFinishedAt()).isNull();
    assertThat(result.getHistory())
        .extracting(h -> h.getStatus())
        .containsExactly(Status.RECEBIDO, Status.VALIDADO);
  }

  @Test
  @DisplayName("PREFERENTIAL -> VALIDADO")
  void validate_preferential() {
    Solicitation s = newReceivedSolicitation();
    mockFound(s);
    mockSavePassthrough();
    given(
            fraudClient.check(
                FraudCheckRequest.builder().customerId(CUSTOMER_ID).productId(PRODUCT_ID).build()))
        .willReturn(FraudCheckResponse.builder().classification("PREFERENTIAL").build());

    Solicitation result = service.validate(SOLICITATION_ID);

    assertThat(result.getStatus()).isEqualTo(Status.VALIDADO);
    assertThat(result.getFinishedAt()).isNull();
  }

  @Test
  @DisplayName("HIGH_RISK -> REJEITADO (finaliza e adiciona histórico)")
  void validate_highRisk() {
    Solicitation s = newReceivedSolicitation();
    mockFound(s);
    mockSavePassthrough();
    given(
            fraudClient.check(
                FraudCheckRequest.builder().customerId(CUSTOMER_ID).productId(PRODUCT_ID).build()))
        .willReturn(FraudCheckResponse.builder().classification("HIGH_RISK").build());

    Solicitation result = service.validate(SOLICITATION_ID);

    assertThat(result.getStatus()).isEqualTo(Status.REJEITADO);
    assertThat(result.getFinishedAt()).isNotNull();
    assertThat(result.getHistory())
        .extracting(h -> h.getStatus())
        .containsExactly(Status.RECEBIDO, Status.REJEITADO);
  }

  @Test
  @DisplayName("NO_INFO (ou classificação nula) -> REJEITADO")
  void validate_noInfo() {
    Solicitation s = newReceivedSolicitation();
    mockFound(s);
    mockSavePassthrough();
    given(
            fraudClient.check(
                FraudCheckRequest.builder().customerId(CUSTOMER_ID).productId(PRODUCT_ID).build()))
        .willReturn(FraudCheckResponse.builder().classification(null).build());

    Solicitation result = service.validate(SOLICITATION_ID);

    assertThat(result.getStatus()).isEqualTo(Status.REJEITADO);
    assertThat(result.getFinishedAt()).isNotNull();
  }

  @Test
  @DisplayName("Classificação desconhecida -> REJEITADO (fallback)")
  void validate_unknownClassification() {
    Solicitation s = newReceivedSolicitation();
    mockFound(s);
    mockSavePassthrough();
    given(
            fraudClient.check(
                FraudCheckRequest.builder().customerId(CUSTOMER_ID).productId(PRODUCT_ID).build()))
        .willReturn(FraudCheckResponse.builder().classification("???").build());

    Solicitation result = service.validate(SOLICITATION_ID);

    assertThat(result.getStatus()).isEqualTo(Status.REJEITADO);
    assertThat(result.getFinishedAt()).isNotNull();
  }

  @Test
  @DisplayName("Idempotência: já VALIDADO -> não chama FraudClient")
  void validate_alreadyValidated() {
    Solicitation s = newReceivedSolicitation();
    s.setStatus(Status.VALIDADO);
    mockFound(s);
    // não stubbar save: método retorna antes

    Solicitation result = service.validate(SOLICITATION_ID);

    assertThat(result.getStatus()).isEqualTo(Status.VALIDADO);
    verify(fraudClient, never()).check(ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Estado inválido (!= RECEBIDO) e não terminal -> erro")
  void validate_invalidState() {
    Solicitation s = newReceivedSolicitation();
    s.setStatus(Status.PENDENTE); // estado inválido para validação
    mockFound(s);
    // não stubbar save: exceção antes

    assertThatThrownBy(() -> service.validate(SOLICITATION_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid state to validate");
  }
}
