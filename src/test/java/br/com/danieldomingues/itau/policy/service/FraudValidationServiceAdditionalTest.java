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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FraudValidationServiceAdditionalTest {

  private SolicitationRepository repository;
  private FraudClient fraudClient;
  private FraudValidationService service;
  private SolicitationFactory factory;

  @BeforeEach
  void setup() {
    repository = mock(SolicitationRepository.class);
    fraudClient = mock(FraudClient.class);
    service = new FraudValidationService(repository, fraudClient);
    factory = new SolicitationFactory();
  }

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
    // simulamos o estado inicial esperado pelo domínio (@PrePersist faria isso no runtime)
    s.setId(UUID.randomUUID());
    s.setStatus(Status.RECEIVED);
    s.addHistory(Status.RECEIVED, OffsetDateTime.now());
    return s;
  }

  @Test
  void notFound_shouldThrowIllegalArgumentException() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.validate(id))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Solicitation not found");

    verify(repository, never()).save(any());
    verify(fraudClient, never()).check(any());
  }

  @Test
  void alreadyRejected_shouldBeIdempotent_andNotCallFraudOrSave() {
    Solicitation s = newSolicitation(Category.AUTO, 100_000);
    s.setStatus(Status.REJECTED);
    s.setFinishedAt(OffsetDateTime.now());

    when(repository.findById(s.getId())).thenReturn(Optional.of(s));

    Solicitation out = service.validate(s.getId());

    // não chama cliente nem persiste
    verify(fraudClient, never()).check(any());
    verify(repository, never()).save(any());

    assertThat(out.getStatus()).isEqualTo(Status.REJECTED);
    assertThat(out.getFinishedAt()).isNotNull();
  }

  @Test
  void highRisk_shouldReject_setFinishedAt_andAppendHistory() {
    Solicitation s = newSolicitation(Category.RESIDENTIAL, 200_000);

    when(repository.findById(s.getId())).thenReturn(Optional.of(s));
    when(fraudClient.check(any(FraudCheckRequest.class)))
        .thenReturn(FraudCheckResponse.builder().classification("HIGH_RISK").build());

    Solicitation saved = newSolicitation(Category.RESIDENTIAL, 200_000);
    saved.setId(s.getId()); // simula retorno do save com o mesmo id
    saved.setStatus(Status.REJECTED);
    saved.setFinishedAt(OffsetDateTime.now());
    // histórico já continha RECEIVED; adiciona REJECTED
    saved.addHistory(Status.REJECTED, saved.getFinishedAt());

    when(repository.save(any(Solicitation.class))).thenReturn(saved);

    Solicitation result = service.validate(s.getId());

    assertThat(result.getStatus()).isEqualTo(Status.REJECTED);
    assertThat(result.getFinishedAt()).isNotNull();
    // deve ter pelo menos RECEIVED + REJECTED
    assertThat(result.getHistory().size()).isGreaterThanOrEqualTo(2);
    assertThat(result.getHistory().get(0).getStatus()).isEqualTo(Status.RECEIVED);
    assertThat(result.getHistory().get(result.getHistory().size() - 1).getStatus())
        .isEqualTo(Status.REJECTED);
  }

  @Test
  void shouldBuildFraudCheckRequest_withCustomerAndProduct_fromEntity() {
    Solicitation s = newSolicitation(Category.LIFE, 300_000);

    when(repository.findById(s.getId())).thenReturn(Optional.of(s));

    // capturar o request enviado ao client
    ArgumentCaptor<FraudCheckRequest> reqCap = ArgumentCaptor.forClass(FraudCheckRequest.class);
    when(fraudClient.check(reqCap.capture()))
        .thenReturn(FraudCheckResponse.builder().classification("REGULAR").build());

    // salvar devolve a própria entidade alterada
    when(repository.save(any(Solicitation.class))).thenAnswer(inv -> inv.getArgument(0));

    service.validate(s.getId());

    FraudCheckRequest sent = reqCap.getValue();
    assertThat(sent).isNotNull();
    assertThat(sent.getCustomerId()).isEqualTo(s.getCustomerId());
    assertThat(sent.getProductId()).isEqualTo(s.getProductId());
  }
}
