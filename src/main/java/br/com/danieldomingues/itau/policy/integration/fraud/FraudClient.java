package br.com.danieldomingues.itau.policy.integration.fraud;

import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckRequest;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckResponse;

/**
 * Abstração do cliente da API de Fraudes.
 * Mantém o domínio desacoplado de tecnologia (HTTP/WebClient, etc.).
 */
public interface FraudClient {

  /**
   * Realiza a verificação de fraude de uma solicitação.
   * @param request dados mínimos (customerId, productId)
   * @return classificação retornada pelo serviço de fraudes
   */
  FraudCheckResponse check(FraudCheckRequest request);
}
