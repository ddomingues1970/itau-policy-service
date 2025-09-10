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

class SubscriptionEventListenerTest {

  private SolicitationRepository repository;
  private PolicyEventPublisher publisher;
  private SubscriptionEventListener listener;
  private SolicitationFactory factory;

  @BeforeEach
  void setUp() {
    repository = mock(SolicitationRepository.class);
    publisher = mock(PolicyEventPublisher.class);
    listener = new SubscriptionEventListener(repository, publisher);
    factory = new SolicitationFactory();
  }

  private Solicitation pendingSolicitation() {
    Solicitation s =
        factory.newSolicitation(
            UUID.randomUUID(),
            UUID.randomUUID().toString(),
            Category.LIFE,
            "WEB",
            "PIX",
            BigDecimal.valueOf(50.0),
            BigDecimal.valueOf(5000.0),
            Collections.emptyMap(),
            Collections.emptyList());
    s.setId(UUID.randomUUID());
    s.setStatus(Status.PENDING);
    return s;
  }

  @Test
  void onSubscriptionActive_shouldApprove_andPublishApproved() {
    Solicitation s = pendingSolicitation();
    when(repository.findById(s.getId())).thenReturn(Optional.of(s));

    listener.onSubscriptionActive(Map.of("solicitationId", s.getId().toString()));

    verify(repository, times(1)).save(s);
    assert s.getStatus() == Status.APPROVED;
    verify(publisher, times(1))
        .publishSolicitationApproved(
            eq(s.getId().toString()), eq(s.getCustomerId().toString()), anyString());
    verify(publisher, never()).publishSolicitationRejected(any(), any(), any());
  }

  @Test
  void onSubscriptionRejected_shouldReject_andPublishRejected() {
    Solicitation s = pendingSolicitation();
    when(repository.findById(s.getId())).thenReturn(Optional.of(s));

    listener.onSubscriptionRejected(Map.of("solicitationId", s.getId().toString()));

    verify(repository, times(1)).save(s);
    assert s.getStatus() == Status.REJECTED;
    verify(publisher, times(1))
        .publishSolicitationRejected(
            eq(s.getId().toString()), eq(s.getCustomerId().toString()), anyString());
    verify(publisher, never()).publishSolicitationApproved(any(), any(), any());
  }

  @Test
  void shouldIgnoreWhenMissingId() {
    listener.onSubscriptionActive(Map.of()); // sem solicitationId
    verifyNoInteractions(repository, publisher);
  }
}
