package br.com.danieldomingues.itau.policy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Exercita todos os ramos de cancelamento:
 * - not found -> IllegalArgumentException
 * - status APPROVED/REJECTED -> IllegalStateException
 * - status CANCELLED -> idempotente
 * - status PENDING -> cancela, define finishedAt e adiciona histórico
 */
class SolicitationServiceCancelTest {

  private SolicitationRepository repository;
  private SolicitationService service;

  @BeforeEach
  void setup() {
    repository = mock(SolicitationRepository.class);
    service = new SolicitationService(repository);
  }

  private static Solicitation newSolicitation(Status status) {
    return Solicitation.builder()
        .id(UUID.randomUUID())
        .customerId(UUID.randomUUID())
        .productId(UUID.randomUUID().toString())
        .category(Category.AUTO)
        .salesChannel("MOBILE")
        .paymentMethod("CREDIT_CARD")
        .totalMonthlyPremiumAmount(new BigDecimal("100.00"))
        .insuredAmount(new BigDecimal("10000.00"))
        .status(status)
        .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5))
        .build();
  }

  @Test
  @DisplayName("Deve lançar IllegalArgumentException quando a solicitação não existe")
  void cancel_notFound() {
    UUID id = UUID.randomUUID();
    when(repository.findWithHistoryById(id)).thenReturn(Optional.empty());

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> service.cancel(id));

    assertTrue(ex.getMessage().contains("Solicitation not found"));
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("Não deve cancelar quando status é APPROVED (regra terminal)")
  void cancel_approved_forbidden() {
    UUID id = UUID.randomUUID();
    Solicitation s = newSolicitation(Status.APPROVED);
    when(repository.findWithHistoryById(id)).thenReturn(Optional.of(s));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.cancel(id));

    assertTrue(ex.getMessage().contains("terminal status"));
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("Não deve cancelar quando status é REJECTED (regra terminal)")
  void cancel_rejected_forbidden() {
    UUID id = UUID.randomUUID();
    Solicitation s = newSolicitation(Status.REJECTED);
    when(repository.findWithHistoryById(id)).thenReturn(Optional.of(s));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.cancel(id));

    assertTrue(ex.getMessage().contains("terminal status"));
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("Idempotente: se já estiver CANCELLED, não altera nem salva")
  void cancel_alreadyCancelled_isIdempotent() {
    UUID id = UUID.randomUUID();
    Solicitation s = newSolicitation(Status.CANCELLED);
    when(repository.findWithHistoryById(id)).thenReturn(Optional.of(s));

    assertDoesNotThrow(() -> service.cancel(id));
    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName(
      "Cancela com sucesso quando status é PENDING: define finishedAt e adiciona histórico")
  void cancel_fromPending_success() {
    UUID id = UUID.randomUUID();
    Solicitation s = newSolicitation(Status.PENDING);
    int historyBefore = s.getHistory() == null ? 0 : s.getHistory().size();
    when(repository.findWithHistoryById(id)).thenReturn(Optional.of(s));
    when(repository.save(any(Solicitation.class))).thenAnswer(inv -> inv.getArgument(0));

    service.cancel(id);

    // Verifica alterações
    assertEquals(Status.CANCELLED, s.getStatus(), "Status deveria ser CANCELLED");
    assertNotNull(s.getFinishedAt(), "finishedAt deve ser definido no cancelamento");
    assertNotNull(s.getHistory(), "histórico não pode ser nulo");

    // Deve ter adicionado exatamente 1 item de histórico novo (CANCELLED)
    assertTrue(s.getHistory().size() >= historyBefore + 1, "Histórico deveria ter aumentado");

    // Garante que salvou a entidade alterada
    ArgumentCaptor<Solicitation> captor = ArgumentCaptor.forClass(Solicitation.class);
    verify(repository).save(captor.capture());
    assertEquals(Status.CANCELLED, captor.getValue().getStatus());
  }
}
