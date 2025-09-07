package br.com.danieldomingues.itau.policy.service;

import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationRequest;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.factory.SolicitationFactory;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    // Momento de criação em UTC (consistente para logs/testes)
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    // Constrói a entidade a partir do DTO
    Solicitation entity = SolicitationFactory.from(req);

    // Garante os metadados iniciais
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(now);
    }
    entity.setStatus(Status.RECEIVED);
    entity.addHistory(Status.RECEIVED, now);

    // Persiste e retorna a entidade gerenciada
    return repository.save(entity);
  }
}
