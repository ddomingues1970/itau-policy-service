package br.com.danieldomingues.itau.policy.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de Exchange/Queues/Bindings e conversor JSON para AMQP.
 * Usa nomes vindos de application.yml.
 */
@EnableRabbit
@Configuration
public class PolicyEventsConfig {

  // Exchange
  @Value("${amqp.exchanges.policy}")
  private String policyExchangeName;

  // Queues de entrada
  @Value("${amqp.queues.payment}")
  private String paymentQueueName;

  @Value("${amqp.queues.subscription}")
  private String subscriptionQueueName;

  // Queues de saída (eventos)
  @Value("${amqp.queues.events.approved}")
  private String approvedQueueName;

  @Value("${amqp.queues.events.rejected}")
  private String rejectedQueueName;

  // Routing keys
  @Value("${amqp.routing.payment}")
  private String paymentRoutingKey;

  @Value("${amqp.routing.subscription}")
  private String subscriptionRoutingKey;

  @Value("${amqp.routing.approved}")
  private String approvedRoutingKey;

  @Value("${amqp.routing.rejected}")
  private String rejectedRoutingKey;

  // ===== Exchange =====
  @Bean
  public TopicExchange policyExchange() {
    return new TopicExchange(policyExchangeName, true, false);
  }

  // ===== Queues =====
  @Bean
  public Queue paymentQueue() {
    return new Queue(paymentQueueName, true);
  }

  @Bean
  public Queue subscriptionQueue() {
    return new Queue(subscriptionQueueName, true);
  }

  @Bean
  public Queue approvedQueue() {
    return new Queue(approvedQueueName, true);
  }

  @Bean
  public Queue rejectedQueue() {
    return new Queue(rejectedQueueName, true);
  }

  // ===== Bindings =====
  @Bean
  public Binding bindPaymentQueue(
      @Qualifier("paymentQueue") Queue paymentQueue, TopicExchange policyExchange) {
    return BindingBuilder.bind(paymentQueue).to(policyExchange).with(paymentRoutingKey);
  }

  @Bean
  public Binding bindSubscriptionQueue(
      @Qualifier("subscriptionQueue") Queue subscriptionQueue, TopicExchange policyExchange) {
    return BindingBuilder.bind(subscriptionQueue).to(policyExchange).with(subscriptionRoutingKey);
  }

  @Bean
  public Binding bindApprovedQueue(
      @Qualifier("approvedQueue") Queue approvedQueue, TopicExchange policyExchange) {
    return BindingBuilder.bind(approvedQueue).to(policyExchange).with(approvedRoutingKey);
  }

  @Bean
  public Binding bindRejectedQueue(
      @Qualifier("rejectedQueue") Queue rejectedQueue, TopicExchange policyExchange) {
    return BindingBuilder.bind(rejectedQueue).to(policyExchange).with(rejectedRoutingKey);
  }

  // ===== Conversor JSON + RabbitTemplate =====
  @Bean
  public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, Jackson2JsonMessageConverter messageConverter) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(messageConverter);
    // Opcional: usar exchange/rk padrão se for publicar sem especificar toda hora
    template.setExchange(policyExchangeName);
    return template;
  }
}
