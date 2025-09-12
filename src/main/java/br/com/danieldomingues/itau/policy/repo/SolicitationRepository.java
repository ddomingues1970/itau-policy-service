package br.com.danieldomingues.itau.policy.repo;

import br.com.danieldomingues.itau.policy.domain.Solicitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitationRepository extends JpaRepository<Solicitation, UUID> {

  // Mantemos apenas o fetch do history para cenários que realmente precisem (ex.: cancelamento).
  @EntityGraph(attributePaths = "history")
  Optional<Solicitation> findWithHistoryById(UUID id);

  // Para listagem por cliente (sem forçar fetch de bags; service poderá inicializar o que precisar)
  List<Solicitation> findByCustomerId(UUID customerId);
}
