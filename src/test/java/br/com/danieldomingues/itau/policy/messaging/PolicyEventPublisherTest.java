package br.com.danieldomingues.itau.policy.messaging;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class PolicyEventPublisherTest {

  @Test
  void shouldPublishWithCorrectRoutingKeys() {
    RabbitTemplate rabbit = mock(RabbitTemplate.class);
    TopicExchange exchange = mock(TopicExchange.class);
    when(exchange.getName()).thenReturn("policy.lifecycle");

    // construtor real: (RabbitTemplate, TopicExchange)
    PolicyEventPublisher publisher = new PolicyEventPublisher(rabbit, exchange);

    publisher.publishSolicitationValidated("s1", "c1", "REGULAR");
    publisher.publishSolicitationRejected("s2", "c2", "NO_INFO");
    publisher.publishSolicitationApproved("s3", "c3", "PAYMENT_APPROVED");

    // usar anyMap() evita a ambiguidade de overloads do convertAndSend
    verify(rabbit, times(1))
        .convertAndSend(eq("policy.lifecycle"), eq("solicitation.validated"), anyMap());
    verify(rabbit, times(1))
        .convertAndSend(eq("policy.lifecycle"), eq("solicitation.rejected"), anyMap());
    verify(rabbit, times(1))
        .convertAndSend(eq("policy.lifecycle"), eq("solicitation.approved"), anyMap());
  }
}
