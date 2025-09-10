package br.com.danieldomingues.itau.policy.messaging;

import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventListener {

  private final SolicitationRepository repository;
  private final PolicyEventPublisher eventPublisher;

  @Transactional
  @RabbitListener(queues = "policy.subscription.active.q")
  public void onSubscriptionActive(Map<String, Object> payload) {
    processSubscription(payload, true);
  }

  @Transactional
  @RabbitListener(queues = "policy.subscription.rejected.q")
  public void onSubscriptionRejected(Map<String, Object> payload) {
    processSubscription(payload, false);
  }

  private void processSubscription(Map<String, Object> payload, boolean active) {
    UUID solId = extractSolicitationId(payload);
    if (solId == null) {
      log.warn("SubscriptionEvent without valid solicitationId: {}", payload);
      return; // ack & ignore
    }

    Optional<Solicitation> opt = repository.findById(solId);
    if (opt.isEmpty()) {
      log.warn("Solicitation not found for subscription event: {}", solId);
      return; // ack & ignore
    }

    Solicitation s = opt.get();
    if (s.getStatus() != Status.PENDING) {
      log.info("Ignoring subscription event for {} because status is {}", s.getId(), s.getStatus());
      return; // idempotência
    }

    OffsetDateTime now = OffsetDateTime.now();
    if (active) {
      s.setStatus(Status.APPROVED);
      s.addHistory(Status.APPROVED, now);
      s.setFinishedAt(now);
      repository.save(s);
      try {
        eventPublisher.publishSolicitationApproved(
            s.getId().toString(), s.getCustomerId().toString(), "SUBSCRIPTION_ACTIVE");
      } catch (Exception e) {
        log.warn(
            "Failed to publish SolicitationApprovedEvent for {}: {}", s.getId(), e.getMessage(), e);
      }
    } else {
      s.setStatus(Status.REJECTED);
      s.addHistory(Status.REJECTED, now);
      s.setFinishedAt(now);
      repository.save(s);
      try {
        eventPublisher.publishSolicitationRejected(
            s.getId().toString(), s.getCustomerId().toString(), "SUBSCRIPTION_REJECTED");
      } catch (Exception e) {
        log.warn(
            "Failed to publish SolicitationRejectedEvent for {}: {}", s.getId(), e.getMessage(), e);
      }
    }
  }

  private UUID extractSolicitationId(Map<String, Object> payload) {
    Object idObj = payload.get("solicitationId");
    if (!(idObj instanceof String s) || s.isBlank() || "null".equalsIgnoreCase(s)) {
      return null;
    }
    try {
      return UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
