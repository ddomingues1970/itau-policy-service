package br.com.danieldomingues.itau.policy.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PolicyEventsConfig {

  // Exchange principal para o ciclo de vida
  public static final String EXCHANGE_POLICY_LIFECYCLE = "policy.lifecycle";

  // Filas base para eventos RECEIVED -> VALIDATED/REJECTED
  public static final String QUEUE_VALIDATED = "policy.lifecycle.validated.q";
  public static final String QUEUE_REJECTED = "policy.lifecycle.rejected.q";

  // Routing keys
  public static final String RK_VALIDATED = "policy.lifecycle.validated";
  public static final String RK_REJECTED = "policy.lifecycle.rejected";

  @Bean
  TopicExchange policyLifecycleExchange() {
    return ExchangeBuilder.topicExchange(EXCHANGE_POLICY_LIFECYCLE).durable(true).build();
  }

  @Bean(name = "validatedQueue")
  Queue validatedQueue() {
    return QueueBuilder.durable(QUEUE_VALIDATED).build();
  }

  @Bean(name = "rejectedQueue")
  Queue rejectedQueue() {
    return QueueBuilder.durable(QUEUE_REJECTED).build();
  }

  @Bean
  Binding bindValidated(
      @Qualifier("validatedQueue") Queue validatedQueue, TopicExchange policyLifecycleExchange) {
    return BindingBuilder.bind(validatedQueue).to(policyLifecycleExchange).with(RK_VALIDATED);
  }

  @Bean
  Binding bindRejected(
      @Qualifier("rejectedQueue") Queue rejectedQueue, TopicExchange policyLifecycleExchange) {
    return BindingBuilder.bind(rejectedQueue).to(policyLifecycleExchange).with(RK_REJECTED);
  }

  @Bean
  Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
