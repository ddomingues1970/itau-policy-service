package br.com.danieldomingues.itau.policy.service;

import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.integration.fraud.FraudClient;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckRequest;
import br.com.danieldomingues.itau.policy.integration.fraud.dto.FraudCheckResponse;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a validação de fraudes e realiza a transição de estado:
 * RECEBIDO -> VALIDADO/REJEITADO, registrando histórico.
 *
 * Regras (Mock/WireMock):
 *  - REGULAR/PREFERENTIAL  -> VALIDADO
 *  - HIGH_RISK/NO_INFO     -> REJEITADO  (finaliza solicitação)
 *
 * Idempotente: se a solicitação já estiver VALIDADO/REJEITADO, não altera.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudValidationService {

  private final SolicitationRepository repository;
  private final FraudClient fraudClient;

  @Transactional
  public Solicitation validate(UUID solicitationId) {
    Solicitation s =
        repository
            .findById(solicitationId)
            .orElseThrow(
                () -> new IllegalArgumentException("Solicitation not found: " + solicitationId));

    // Idempotência: já processada
    if (s.getStatus() == Status.VALIDADO || s.getStatus() == Status.REJEITADO) {
      log.info("Solicitation {} already processed with status {}", s.getId(), s.getStatus());
      return s;
    }

    if (s.getStatus() != Status.RECEBIDO) {
      throw new IllegalStateException(
          "Invalid state to validate: " + s.getStatus() + " (expected RECEBIDO)");
    }

    FraudCheckRequest req =
        FraudCheckRequest.builder()
            .customerId(s.getCustomerId())
            .productId(s.getProductId())
            .build();

    FraudCheckResponse resp = fraudClient.check(req);
    String classification =
        resp != null && resp.getClassification() != null
            ? resp.getClassification().trim().toUpperCase(Locale.ROOT)
            : "NO_INFO";

    OffsetDateTime now = OffsetDateTime.now();
    switch (classification) {
      case "REGULAR", "PREFERENTIAL" -> {
        s.setStatus(Status.VALIDADO);
        s.addHistory(Status.VALIDADO, now);
      }
      case "HIGH_RISK", "NO_INFO" -> {
        s.setStatus(Status.REJEITADO);
        s.addHistory(Status.REJEITADO, now);
        s.setFinishedAt(now);
      }
      default -> {
        log.warn("Unknown fraud classification '{}', defaulting to REJEITADO", classification);
        s.setStatus(Status.REJEITADO);
        s.addHistory(Status.REJEITADO, now);
        s.setFinishedAt(now);
      }
    }

    return repository.save(s);
  }
}
