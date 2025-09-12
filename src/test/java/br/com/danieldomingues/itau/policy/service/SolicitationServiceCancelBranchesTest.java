package br.com.danieldomingues.itau.policy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SolicitationServiceCancelBranchesTest {

  @Test
  void cancel_shouldThrow_whenApproved() {
    SolicitationRepository repo = mock(SolicitationRepository.class);
    SolicitationService service = new SolicitationService(repo);
    UUID id = UUID.randomUUID();
    Solicitation s = Solicitation.builder().id(id).status(Status.APROVADO).build();

    when(repo.findWithHistoryById(id)).thenReturn(Optional.of(s));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.cancel(id));
    assertTrue(ex.getMessage().contains("terminal"));
  }

  @Test
  void cancel_shouldThrow_whenRejected() {
    SolicitationRepository repo = mock(SolicitationRepository.class);
    SolicitationService service = new SolicitationService(repo);
    UUID id = UUID.randomUUID();
    Solicitation s = Solicitation.builder().id(id).status(Status.REJEITADO).build();

    when(repo.findWithHistoryById(id)).thenReturn(Optional.of(s));

    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.cancel(id));
    assertTrue(ex.getMessage().contains("terminal"));
  }
}
