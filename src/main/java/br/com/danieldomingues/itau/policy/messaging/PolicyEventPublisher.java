package br.com.danieldomingues.itau.policy.messaging;

import java.time.OffsetDateTime;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de domínio (Approved/Rejected) no RabbitMQ.
 * Os nomes de exchange e routing keys vêm do application.yml.
 */
@Component
public class PolicyEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final TopicExchange policyExchange;

  @Value("${amqp.routing.approved}")
  private String approvedRoutingKey;

  @Value("${amqp.routing.rejected}")
  private String rejectedRoutingKey;

  public PolicyEventPublisher(RabbitTemplate rabbitTemplate, TopicExchange policyExchange) {
    this.rabbitTemplate = rabbitTemplate;
    this.policyExchange = policyExchange;
  }

  /** Publica evento de aprovação. */
  public void publishSolicitationApproved(
      String solicitationId, String customerId, String details) {
    ApprovedEvent payload = new ApprovedEvent();
    payload.setSolicitationId(solicitationId);
    payload.setCustomerId(customerId);
    payload.setDetails(details);
    payload.setOccurredAt(OffsetDateTime.now());

    rabbitTemplate.convertAndSend(policyExchange.getName(), approvedRoutingKey, payload);
  }

  /** Publica evento de rejeição. */
  public void publishSolicitationRejected(String solicitationId, String customerId, String reason) {
    RejectedEvent payload = new RejectedEvent();
    payload.setSolicitationId(solicitationId);
    payload.setCustomerId(customerId);
    payload.setReason(reason);
    payload.setOccurredAt(OffsetDateTime.now());

    rabbitTemplate.convertAndSend(policyExchange.getName(), rejectedRoutingKey, payload);
  }

  // ===== Payloads simples (POJO) =====

  public static class ApprovedEvent {
    private String solicitationId;
    private String customerId;
    private String details;
    private OffsetDateTime occurredAt;

    public String getSolicitationId() {
      return solicitationId;
    }

    public void setSolicitationId(String solicitationId) {
      this.solicitationId = solicitationId;
    }

    public String getCustomerId() {
      return customerId;
    }

    public void setCustomerId(String customerId) {
      this.customerId = customerId;
    }

    public String getDetails() {
      return details;
    }

    public void setDetails(String details) {
      this.details = details;
    }

    public OffsetDateTime getOccurredAt() {
      return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
      this.occurredAt = occurredAt;
    }
  }

  public static class RejectedEvent {
    private String solicitationId;
    private String customerId;
    private String reason;
    private OffsetDateTime occurredAt;

    public String getSolicitationId() {
      return solicitationId;
    }

    public void setSolicitationId(String solicitationId) {
      this.solicitationId = solicitationId;
    }

    public String getCustomerId() {
      return customerId;
    }

    public void setCustomerId(String customerId) {
      this.customerId = customerId;
    }

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }

    public OffsetDateTime getOccurredAt() {
      return occurredAt;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
      this.occurredAt = occurredAt;
    }
  }
}
