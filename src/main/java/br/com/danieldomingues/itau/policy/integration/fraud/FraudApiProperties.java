package br.com.danieldomingues.itau.policy.integration.fraud;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propriedades da API de Fraudes.
 * Configure via application.yml:
 *
 * fraud:
 *   api:
 *     base-url: http://localhost:8082
 *     connect-timeout: 2s
 *     read-timeout: 5s
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fraud.api")
public class FraudApiProperties {

  /** Base URL do serviço de fraudes (ex.: http://localhost:8082 para o WireMock). */
  private String baseUrl = "http://localhost:8082";

  /** Timeout de conexão HTTP. */
  private Duration connectTimeout = Duration.ofSeconds(2);

  /** Timeout de leitura HTTP. */
  private Duration readTimeout = Duration.ofSeconds(5);
}
