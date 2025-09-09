package br.com.danieldomingues.itau.policy.integration.fraud;

import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckRequest;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Implementação HTTP do FraudClient.
 * Usa timeouts e baseUrl configuráveis via FraudApiProperties.
 */
@Slf4j
@Component
public class HttpFraudClient implements FraudClient {

  private final RestTemplate restTemplate;
  private final String baseUrl;

  public HttpFraudClient(RestTemplateBuilder builder, FraudApiProperties props) {
    this.restTemplate =
        builder
            .setConnectTimeout(props.getConnectTimeout())
            .setReadTimeout(props.getReadTimeout())
            .build();
    this.baseUrl = props.getBaseUrl();
  }

  @Override
  public FraudCheckResponse check(FraudCheckRequest request) {
    String url = baseUrl + "/fraud/check"; // ajuste aqui se seu WireMock expõe outro caminho
    try {
      ResponseEntity<FraudCheckResponse> resp =
          restTemplate.postForEntity(url, request, FraudCheckResponse.class);

      if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
        return resp.getBody();
      }
      log.warn("Fraud API returned status={} body={}", resp.getStatusCode(), resp.getBody());
      throw new IllegalStateException(
          "Unexpected response from Fraud API: " + resp.getStatusCode());
    } catch (RestClientException ex) {
      log.error("Error calling Fraud API at {}: {}", url, ex.getMessage(), ex);
      throw new IllegalStateException("Failed to call Fraud API", ex);
    }
  }
}
