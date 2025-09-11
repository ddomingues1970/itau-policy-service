package br.com.danieldomingues.itau.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.factory.SolicitationFactory;
import br.com.danieldomingues.itau.policy.integration.fraud.FraudClient;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckRequest;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckResponse;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FraudValidationServiceRulesTest {

  private SolicitationRepository repository;
  private FraudClient fraudClient;
  private FraudValidationService service;
  private SolicitationFactory factory;

  @BeforeEach
  void setUp() {
    repository = mock(SolicitationRepository.class);
    fraudClient = mock(FraudClient.class);
    service = new FraudValidationService(repository, fraudClient);
    factory = new SolicitationFactory();
  }

  private Solicitation newSolicitation(Category category, double insuredAmount) {
    Solicitation s =
        factory.newSolicitation(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            category,
            "MOBILE",
            "CREDIT_CARD",
            BigDecimal.valueOf(100.0),
            BigDecimal.valueOf(insuredAmount),
            Collections.emptyMap(),
            Collections.emptyList());
    // Forçamos status RECEIVED (normalmente @PrePersist cuida)
    s.setId(UUID.randomUUID());
    s.setStatus(Status.RECEIVED);
    return s;
  }

  @Test
  void regularClient_shouldValidate() {
    Solicitation sol = newSolicitation(Category.LIFE, 400_000);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(fraudClient.check(any(FraudCheckRequest.class)))
        .thenReturn(FraudCheckResponse.builder().classification("REGULAR").build());

    service.validate(sol.getId());

    ArgumentCaptor<Solicitation> captor = ArgumentCaptor.forClass(Solicitation.class);
    verify(repository).save(captor.capture());

    assertThat(captor.getValue().getStatus()).isEqualTo(Status.VALIDATED);
  }

  @Test
  void highRiskClient_shouldReject() {
    Solicitation sol = newSolicitation(Category.AUTO, 300_000);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(fraudClient.check(any(FraudCheckRequest.class)))
        .thenReturn(FraudCheckResponse.builder().classification("HIGH_RISK").build());

    service.validate(sol.getId());

    ArgumentCaptor<Solicitation> captor = ArgumentCaptor.forClass(Solicitation.class);
    verify(repository).save(captor.capture());

    assertThat(captor.getValue().getStatus()).isEqualTo(Status.REJECTED);
  }

  @Test
  void preferentialClient_shouldValidate() {
    Solicitation sol = newSolicitation(Category.HOME, 300_000);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(fraudClient.check(any(FraudCheckRequest.class)))
        .thenReturn(FraudCheckResponse.builder().classification("PREFERENTIAL").build());

    service.validate(sol.getId());

    ArgumentCaptor<Solicitation> captor = ArgumentCaptor.forClass(Solicitation.class);
    verify(repository).save(captor.capture());

    assertThat(captor.getValue().getStatus()).isEqualTo(Status.VALIDATED);
  }

  @Test
  void noInfoClient_shouldReject() {
    Solicitation sol = newSolicitation(Category.OTHER, 80_000);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(fraudClient.check(any(FraudCheckRequest.class)))
        .thenReturn(FraudCheckResponse.builder().classification("NO_INFO").build());

    service.validate(sol.getId());

    ArgumentCaptor<Solicitation> captor = ArgumentCaptor.forClass(Solicitation.class);
    verify(repository).save(captor.capture());

    assertThat(captor.getValue().getStatus()).isEqualTo(Status.REJECTED);
  }

  @Test
  void unknownClassification_shouldRejectByDefault() {
    Solicitation sol = newSolicitation(Category.AUTO, 100_000);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));
    when(fraudClient.check(any(FraudCheckRequest.class)))
        .thenReturn(FraudCheckResponse.builder().classification("XYZ").build());

    service.validate(sol.getId());

    ArgumentCaptor<Solicitation> captor = ArgumentCaptor.forClass(Solicitation.class);
    verify(repository).save(captor.capture());

    assertThat(captor.getValue().getStatus()).isEqualTo(Status.REJECTED);
  }

  @Test
  void alreadyValidated_shouldNotChangeStatus() {
    Solicitation sol = newSolicitation(Category.AUTO, 100_000);
    sol.setStatus(Status.VALIDATED);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));

    Solicitation result = service.validate(sol.getId());

    verify(repository, never()).save(any());
    assertThat(result.getStatus()).isEqualTo(Status.VALIDATED);
  }

  @Test
  void invalidState_shouldThrowException() {
    Solicitation sol = newSolicitation(Category.AUTO, 100_000);
    sol.setStatus(Status.PENDING);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));

    assertThatThrownBy(() -> service.validate(sol.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Invalid state");
  }

  @Test
  void nullResponseFromFraudClient_shouldDefaultToNoInfoAndReject() {
    Solicitation sol = newSolicitation(Category.AUTO, 100_000);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));
    // resp == null
    when(fraudClient.check(any(FraudCheckRequest.class))).thenReturn(null);

    service.validate(sol.getId());

    verify(repository).save(any(Solicitation.class));
    assertThat(sol.getStatus()).isEqualTo(Status.REJECTED);
  }

  @Test
  void nullClassification_shouldDefaultToNoInfoAndReject() {
    Solicitation sol = newSolicitation(Category.HOME, 150_000);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));
    // classification == null
    when(fraudClient.check(any(FraudCheckRequest.class)))
        .thenReturn(FraudCheckResponse.builder().classification(null).build());

    service.validate(sol.getId());

    verify(repository).save(any(Solicitation.class));
    assertThat(sol.getStatus()).isEqualTo(Status.REJECTED);
  }

  @Test
  void classificationWithSpacesAndLowercase_shouldBeTrimmedUppercasedAndValidate() {
    Solicitation sol = newSolicitation(Category.LIFE, 250_000);
    when(repository.findById(sol.getId())).thenReturn(Optional.of(sol));
    // " regular " -> REGULAR -> VALIDATED (trim + uppercase)
    when(fraudClient.check(any(FraudCheckRequest.class)))
        .thenReturn(FraudCheckResponse.builder().classification("  regular  ").build());

    service.validate(sol.getId());

    verify(repository).save(any(Solicitation.class));
    assertThat(sol.getStatus()).isEqualTo(Status.VALIDATED);
  }
}
