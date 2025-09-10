package br.com.danieldomingues.itau.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.factory.SolicitationFactory;
import br.com.danieldomingues.itau.policy.integration.fraud.FraudClient;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckRequest;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckResponse;
import br.com.danieldomingues.itau.policy.messaging.PolicyEventPublisher;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FraudValidationServiceAdditionalTest {

  private SolicitationRepository repository;
  private FraudClient fraudClient;
  private PolicyEventPublisher eventPublisher; // novo
  private FraudValidationService service;

  private final SolicitationFactory factory = new SolicitationFactory();

  @BeforeEach
  void setup() {
    repository = mock(SolicitationRepository.class);
    fraudClient = mock(FraudClient.class);
    eventPublisher = mock(PolicyEventPublisher.class); // novo
    service = new FraudValidationService(repository, fraudClient, eventPublisher); // 3 args
  }

  @Test
  void shouldValidateWhenRegular() {
    Solicitation s = newSolicitation(Category.AUTO, 10000.00);
    when(repository.findById(s.getId())).thenReturn(java.util.Optional.of(s));
    ArgumentCaptor<FraudCheckRequest> reqCap = ArgumentCaptor.forClass(FraudCheckRequest.class);
    when(fraudClient.check(reqCap.capture()))
        .thenReturn(FraudCheckResponse.builder().classification("REGULAR").build());
    when(repository.save(any(Solicitation.class))).thenAnswer(inv -> inv.getArgument(0));

    Solicitation result = service.validate(s.getId());

    assertThat(result.getStatus()).isEqualTo(Status.VALIDATED);
    FraudCheckRequest sent = reqCap.getValue();
    assertThat(sent).isNotNull();
    assertThat(sent.getCustomerId()).isEqualTo(s.getCustomerId());
    assertThat(sent.getProductId()).isEqualTo(s.getProductId());

    verify(eventPublisher, times(1))
        .publishSolicitationValidated(
            eq(s.getId().toString()), eq(s.getCustomerId().toString()), any());
    verify(eventPublisher, never()).publishSolicitationRejected(any(), any(), any());
  }

  @Test
  void shouldRejectWhenHighRisk() {
    Solicitation s = newSolicitation(Category.LIFE, 50000.00);
    when(repository.findById(s.getId())).thenReturn(java.util.Optional.of(s));
    when(fraudClient.check(any()))
        .thenReturn(FraudCheckResponse.builder().classification("HIGH_RISK").build());
    when(repository.save(any(Solicitation.class))).thenAnswer(inv -> inv.getArgument(0));

    Solicitation result = service.validate(s.getId());

    assertThat(result.getStatus()).isEqualTo(Status.REJECTED);
    verify(eventPublisher, times(1))
        .publishSolicitationRejected(
            eq(s.getId().toString()), eq(s.getCustomerId().toString()), any());
    verify(eventPublisher, never()).publishSolicitationValidated(any(), any(), any());
  }

  @Test
  void shouldBeIdempotent() {
    Solicitation s = newSolicitation(Category.OTHER, 1000.00);
    s.setStatus(Status.VALIDATED);
    when(repository.findById(s.getId())).thenReturn(java.util.Optional.of(s));

    Solicitation result = service.validate(s.getId());

    assertThat(result.getStatus()).isEqualTo(Status.VALIDATED);
    verifyNoInteractions(eventPublisher);
    verify(repository, never()).save(any());
  }

  @Test
  void shouldThrowWhenNotReceived() {
    Solicitation s = newSolicitation(Category.RESIDENTIAL, 2000.00);
    s.setStatus(Status.PENDING); // estado inválido para validar
    when(repository.findById(s.getId())).thenReturn(java.util.Optional.of(s));

    assertThatThrownBy(() -> service.validate(s.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid state to validate");

    verifyNoInteractions(eventPublisher);
    verify(repository, never()).save(any());
  }

  // ---- Helper

  private Solicitation newSolicitation(Category category, double insuredAmount) {
    Solicitation s =
        factory.newSolicitation(
            UUID.randomUUID(), // customerId
            UUID.randomUUID().toString(), // productId
            category,
            "MOBILE",
            "CREDIT_CARD",
            BigDecimal.valueOf(123.45),
            BigDecimal.valueOf(insuredAmount),
            Collections.emptyMap(),
            Collections.emptyList());
    // Garante identidade/estado inicial esperado pelos testes:
    s.setId(UUID.randomUUID());
    s.setStatus(Status.RECEIVED);
    return s;
  }
}
