package br.com.danieldomingues.itau.policy.service;

import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationRequest;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.factory.SolicitationFactory;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SolicitationService {

  private final SolicitationRepository repository;

  public SolicitationService(SolicitationRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public Solicitation create(CreateSolicitationRequest req) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    Solicitation entity = SolicitationFactory.from(req);

    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(now);
    }
    entity.setStatus(Status.RECEIVED);
    entity.addHistory(Status.RECEIVED, now);

    return repository.save(entity);
  }

  @Transactional(readOnly = true)
  public Optional<Solicitation> getWithHistoryById(UUID id) {
    return repository.findWithHistoryById(id);
  }

  @Transactional(readOnly = true)
  public List<Solicitation> findByCustomerId(UUID customerId) {
    return repository.findByCustomerId(customerId);
  }

  /**
   * Cancela a solicitação, respeitando as regras:
   * - Não permite cancelar se status for APPROVED ou REJECTED (terminais).
   * - Idempotente se já estiver CANCELLED.
   * - Ao cancelar, define finishedAt e registra histórico.
   *
   * Exceções:
   * - IllegalArgumentException -> 404 (não encontrada)
   * - IllegalStateException -> 400 (violação de regra)
   */
  @Transactional
  public void cancel(UUID id) {
    Solicitation s =
        repository
            .findWithHistoryById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitation not found: " + id));

    Status current = s.getStatus();
    if (current == Status.APPROVED || current == Status.REJECTED) {
      throw new IllegalStateException(
          "Cannot cancel a solicitation with terminal status: " + current);
    }
    if (current == Status.CANCELLED) {
      return; // idempotente
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    s.setStatus(Status.CANCELLED);
    s.setFinishedAt(now);
    s.addHistory(Status.CANCELLED, now);

    repository.save(s);
  }
}
