package br.com.danieldomingues.itau.policy.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final TopicExchange policyExchange;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public void publishSolicitationValidated(
      String solicitationId, String customerId, String reason) {
    publishEvent("solicitation.validated", solicitationId, customerId, reason);
  }

  public void publishSolicitationRejected(String solicitationId, String customerId, String reason) {
    publishEvent("solicitation.rejected", solicitationId, customerId, reason);
  }

  public void publishSolicitationApproved(String solicitationId, String customerId, String reason) {
    publishEvent("solicitation.approved", solicitationId, customerId, reason);
  }

  public void publishSolicitationCancelled(
      String solicitationId, String customerId, String reason) {
    publishEvent("solicitation.cancelled", solicitationId, customerId, reason);
  }

  private void publishEvent(
      String routingKey, String solicitationId, String customerId, String reason) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("solicitationId", solicitationId);
      payload.put("customerId", customerId);
      payload.put("reason", reason);

      rabbitTemplate.convertAndSend(policyExchange.getName(), routingKey, payload);
      log.info("Published event {} for solicitation {}", routingKey, solicitationId);
    } catch (Exception e) {
      log.error("Failed to publish event {} for solicitation {}", routingKey, solicitationId, e);
    }
  }
}
