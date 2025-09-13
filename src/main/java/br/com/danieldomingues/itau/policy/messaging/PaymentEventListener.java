package br.com.danieldomingues.itau.policy.messaging;

import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

  private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

  private final SolicitationRepository repository;
  private final PolicyEventPublisher publisher;

  public PaymentEventListener(SolicitationRepository repository, PolicyEventPublisher publisher) {
    this.repository = repository;
    this.publisher = publisher;
  }

  /**
   * Consome mensagens na fila de pagamento.
   * Espera payload com campos:
   *  - solicitationId: String (UUID)
   *  - approved: boolean
   */
  @RabbitListener(queues = "${amqp.queues.payment}")
  public void onPayment(@Payload Map<String, Object> payload) {
    String idStr = (String) payload.get("solicitationId");
    Object approvedObj = payload.get("approved");
    if (idStr == null || approvedObj == null) {
      log.warn("Pagamento inválido: payload incompleto {}", payload);
      return;
    }

    boolean approved =
        (approvedObj instanceof Boolean)
            ? (Boolean) approvedObj
            : Boolean.parseBoolean(approvedObj.toString());

    UUID id;
    try {
      id = UUID.fromString(idStr);
    } catch (IllegalArgumentException e) {
      log.warn("Pagamento inválido: solicitationId inválido {}", idStr);
      return;
    }

    Optional<Solicitation> opt = repository.findById(id);
    if (opt.isEmpty()) {
      log.warn("Pagamento recebido para solicitacao inexistente: {}", id);
      return;
    }

    Solicitation s = opt.get();

    // Regra: processa somente quando PENDENTE (idempotência)
    if (s.getStatus() != Status.PENDENTE) {
      log.info("Ignorando pagamento para {} pois status atual é {}", s.getId(), s.getStatus());
      return;
    }

    OffsetDateTime now = OffsetDateTime.now();

    if (approved) {
      s.setStatus(Status.APROVADO);
      s.addHistory(Status.APROVADO, now);
      s.setFinishedAt(now);
      repository.save(s);
      try {
        publisher.publishSolicitationApproved(
            s.getId().toString(), s.getCustomerId().toString(), "payment-approved");
      } catch (Exception e) {
        log.error("Falha ao publicar ApprovedEvent para {}: {}", s.getId(), e.getMessage(), e);
      }
    } else {
      s.setStatus(Status.REJEITADO);
      s.addHistory(Status.REJEITADO, now);
      s.setFinishedAt(now);
      repository.save(s);
      try {
        publisher.publishSolicitationRejected(
            s.getId().toString(), s.getCustomerId().toString(), "payment-rejected");
      } catch (Exception e) {
        log.error("Falha ao publicar RejectedEvent para {}: {}", s.getId(), e.getMessage(), e);
      }
    }

    log.info("Pagamento processado para {} -> {}", s.getId(), s.getStatus());
  }
}
