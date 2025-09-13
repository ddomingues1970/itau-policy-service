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
public class SubscriptionEventListener {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionEventListener.class);

  private final SolicitationRepository repository;
  private final PolicyEventPublisher publisher;

  public SubscriptionEventListener(
      SolicitationRepository repository, PolicyEventPublisher publisher) {
    this.repository = repository;
    this.publisher = publisher;
  }

  /**
   * Consome mensagens da fila de subscrição.
   * Payload esperado:
   *  - solicitationId: String (UUID)
   *  - active: boolean   (true = assinatura ativa/autorizada)
   */
  @RabbitListener(queues = "${amqp.queues.subscription}")
  public void onSubscription(@Payload Map<String, Object> payload) {
    String idStr = (String) payload.get("solicitationId");
    Object activeObj = payload.get("active");
    if (idStr == null || activeObj == null) {
      log.warn("Subscricao invalida: payload incompleto {}", payload);
      return;
    }

    boolean active =
        (activeObj instanceof Boolean)
            ? (Boolean) activeObj
            : Boolean.parseBoolean(activeObj.toString());

    UUID id;
    try {
      id = UUID.fromString(idStr);
    } catch (IllegalArgumentException e) {
      log.warn("Subscricao invalida: solicitationId invalido {}", idStr);
      return;
    }

    Optional<Solicitation> opt = repository.findById(id);
    if (opt.isEmpty()) {
      log.warn("Subscricao recebida para solicitacao inexistente: {}", id);
      return;
    }

    Solicitation s = opt.get();

    // Regra: processa somente quando PENDENTE (idempotencia)
    if (s.getStatus() != Status.PENDENTE) {
      log.info("Ignorando subscricao para {} pois status atual e {}", s.getId(), s.getStatus());
      return;
    }

    OffsetDateTime now = OffsetDateTime.now();

    if (active) {
      s.setStatus(Status.APROVADO);
      s.addHistory(Status.APROVADO, now);
      s.setFinishedAt(now);
      repository.save(s);
      try {
        publisher.publishSolicitationApproved(
            s.getId().toString(), s.getCustomerId().toString(), "subscription-active");
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
            s.getId().toString(), s.getCustomerId().toString(), "subscription-rejected");
      } catch (Exception e) {
        log.error("Falha ao publicar RejectedEvent para {}: {}", s.getId(), e.getMessage(), e);
      }
    }

    log.info("Subscricao processada para {} -> {}", s.getId(), s.getStatus());
  }
}
