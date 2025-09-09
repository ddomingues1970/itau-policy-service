package br.com.danieldomingues.itau.policy.integration.fraud.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * Request enviado à API de Fraudes para classificar o risco do cliente
 * no contexto de uma solicitação específica.
 * Mantemos apenas o mínimo necessário: customerId e productId.
 */
@Value
@Builder
public class FraudCheckRequest {
  UUID customerId;
  String productId;
}
