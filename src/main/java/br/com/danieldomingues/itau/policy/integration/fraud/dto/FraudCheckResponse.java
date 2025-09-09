package br.com.danieldomingues.itau.policy.integration.fraud.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Resposta da API de Fraudes.
 * Mantemos simples por enquanto: apenas a classificação retornada pelo mock.
 * Em um próximo passo, podemos trocar 'classification' por um enum forte.
 */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class FraudCheckResponse {
  String classification; // "REGULAR", "PREFERENTIAL", "HIGH_RISK", "NO_INFO"
}
