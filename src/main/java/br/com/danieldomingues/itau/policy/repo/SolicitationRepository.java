package br.com.danieldomingues.itau.policy.repo;

import br.com.danieldomingues.itau.policy.domain.Solicitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitationRepository extends JpaRepository<Solicitation, UUID> {

  // Carrega a coleção 'history' junto com a Solicitation (evita LazyInitializationException nos
  // testes/serviço)
  @EntityGraph(attributePaths = "history")
  Optional<Solicitation> findWithHistoryById(UUID id);

  // Útil para endpoint de busca por cliente
  List<Solicitation> findByCustomerId(UUID customerId);
}
