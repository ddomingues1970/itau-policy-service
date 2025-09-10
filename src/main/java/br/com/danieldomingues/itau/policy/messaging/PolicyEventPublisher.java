package br.com.danieldomingues.itau.policy.messaging;

import br.com.danieldomingues.itau.policy.config.PolicyEventsConfig;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PolicyEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  public PolicyEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
    // Garantir uso do Jackson2JsonMessageConverter se registrado no contexto
    // (Spring Boot autoconfig faz isso quando o bean existe)
  }

  public void publishSolicitationValidated(
      String solicitationId, String customerId, String classification) {
    Objects.requireNonNull(solicitationId, "solicitationId is required");
    Map<String, Object> payload =
        Map.of(
            "type", "SolicitationValidatedEvent",
            "solicitationId", solicitationId,
            "customerId", customerId,
            "classification", classification,
            "occurredAt", OffsetDateTime.now().toString());
    rabbitTemplate.convertAndSend(
        PolicyEventsConfig.EXCHANGE_POLICY_LIFECYCLE, PolicyEventsConfig.RK_VALIDATED, payload);
  }

  public void publishSolicitationRejected(String solicitationId, String customerId, String reason) {
    Objects.requireNonNull(solicitationId, "solicitationId is required");
    Map<String, Object> payload =
        Map.of(
            "type", "SolicitationRejectedEvent",
            "solicitationId", solicitationId,
            "customerId", customerId,
            "reason", reason,
            "occurredAt", OffsetDateTime.now().toString());
    rabbitTemplate.convertAndSend(
        PolicyEventsConfig.EXCHANGE_POLICY_LIFECYCLE, PolicyEventsConfig.RK_REJECTED, payload);
  }
}
