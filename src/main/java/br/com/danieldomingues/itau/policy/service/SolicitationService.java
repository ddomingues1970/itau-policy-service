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

  /**
   * Consulta por ID inicializando coleções LAZY (sem múltiplos fetches de "bag").
   * Mantém a chamada a findWithHistoryById para compatibilidade com testes existentes.
   */
  @Transactional(readOnly = true)
  public Optional<Solicitation> getWithHistoryById(UUID id) {
    return repository
        .findWithHistoryById(id)
        .map(
            s -> {
              // Inicializa coleções LAZY ainda dentro da sessão/tx:
              if (s.getHistory() != null) s.getHistory().size();
              if (s.getAssistances() != null) s.getAssistances().size();
              if (s.getCoverages() != null) s.getCoverages().size();
              return s;
            });
  }

  /**
   * Lista por customerId inicializando coleções necessárias para o DTO,
   * evitando LazyInitializationException no mapeamento no controller.
   */
  @Transactional(readOnly = true)
  public List<Solicitation> findByCustomerId(UUID customerId) {
    List<Solicitation> list = repository.findByCustomerId(customerId);
    for (Solicitation s : list) {
      if (s.getHistory() != null) s.getHistory().size();
      if (s.getAssistances() != null) s.getAssistances().size();
      if (s.getCoverages() != null) s.getCoverages().size();
    }
    return list;
  }

  /**
   * Cancela a solicitação com regras:
   * - Não permite cancelar APPROVED/REJECTED (terminais) -> IllegalStateException (400).
   * - Idempotente para CANCELLED.
   * - Ao cancelar, define finishedAt e registra histórico.
   * - Se não existir -> IllegalArgumentException (404 pelo ApiExceptionHandler).
   */
  @Transactional
  public void cancel(UUID id) {
    Solicitation s =
        repository
            // Para cancelar, só precisamos do history; evita multiple-bag fetch
            .findWithHistoryById(id)
            .orElseThrow(() -> new IllegalArgumentException("Solicitation not found: " + id));

    Status current = s.getStatus();
    if (current == Status.APPROVED || current == Status.REJECTED) {
      throw new IllegalStateException(
          "Cannot cancel a solicitation with terminal status: " + current);
    }
    if (current == Status.CANCELLED) {
      return; // Idempotente
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    s.setStatus(Status.CANCELLED);
    s.setFinishedAt(now);
    s.addHistory(Status.CANCELLED, now);

    repository.save(s);
  }
}
