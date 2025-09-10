package br.com.danieldomingues.itau.policy.messaging;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.factory.SolicitationFactory;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentEventListenerTest {

  private SolicitationRepository repository;
  private PolicyEventPublisher publisher;
  private PaymentEventListener listener;
  private SolicitationFactory factory;

  @BeforeEach
  void setUp() {
    repository = mock(SolicitationRepository.class);
    publisher = mock(PolicyEventPublisher.class);
    listener = new PaymentEventListener(repository, publisher);
    factory = new SolicitationFactory();
  }

  private Solicitation pendingSolicitation() {
    Solicitation s =
        factory.newSolicitation(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            Category.AUTO,
            "MOBILE",
            "CREDIT_CARD",
            BigDecimal.valueOf(100.0),
            BigDecimal.valueOf(10000.0),
            Collections.emptyMap(),
            Collections.emptyList());
    s.setId(UUID.randomUUID());
    s.setStatus(Status.PENDING);
    return s;
  }

  @Test
  void onPaymentApproved_shouldSetApproved_andPublishApproved() {
    Solicitation s = pendingSolicitation();
    when(repository.findById(s.getId())).thenReturn(Optional.of(s));

    Map<String, Object> payload = Map.of("solicitationId", s.getId().toString());
    listener.onPaymentApproved(payload);

    verify(repository, times(1)).save(s);
    // terminou em estado terminal
    assert s.getStatus() == Status.APPROVED;
    verify(publisher, times(1))
        .publishSolicitationApproved(
            eq(s.getId().toString()), eq(s.getCustomerId().toString()), anyString());
    verify(publisher, never()).publishSolicitationRejected(any(), any(), any());
  }

  @Test
  void onPaymentRejected_shouldSetRejected_andPublishRejected() {
    Solicitation s = pendingSolicitation();
    when(repository.findById(s.getId())).thenReturn(Optional.of(s));

    Map<String, Object> payload = Map.of("solicitationId", s.getId().toString());
    listener.onPaymentRejected(payload);

    verify(repository, times(1)).save(s);
    assert s.getStatus() == Status.REJECTED;
    verify(publisher, times(1))
        .publishSolicitationRejected(
            eq(s.getId().toString()), eq(s.getCustomerId().toString()), anyString());
    verify(publisher, never()).publishSolicitationApproved(any(), any(), any());
  }

  @Test
  void shouldIgnoreWhenSolicitationNotFound() {
    UUID missing = UUID.randomUUID();
    when(repository.findById(missing)).thenReturn(Optional.empty());
    listener.onPaymentApproved(Map.of("solicitationId", missing.toString()));
    verify(repository, never()).save(any());
    verifyNoInteractions(publisher);
  }

  @Test
  void shouldIgnoreWhenNotPending() {
    Solicitation s = pendingSolicitation();
    s.setStatus(Status.VALIDATED); // não é PENDING
    when(repository.findById(s.getId())).thenReturn(Optional.of(s));
    listener.onPaymentApproved(Map.of("solicitationId", s.getId().toString()));
    verify(repository, never()).save(any());
    verifyNoInteractions(publisher);
  }
}
