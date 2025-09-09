package br.com.danieldomingues.itau.policy.api;

import br.com.danieldomingues.itau.policy.api.dto.SolicitationResponse;
import br.com.danieldomingues.itau.policy.service.FraudValidationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint para acionar a validação de fraude e transitar o estado da solicitação.
 */
@RestController
@RequestMapping("/solicitations")
@RequiredArgsConstructor
public class SolicitationValidationController {

  private final FraudValidationService fraudValidationService;

  @PostMapping(value = "/{id}/validate", produces = "application/json")
  public ResponseEntity<SolicitationResponse> validate(@PathVariable("id") UUID id) {
    var updated = fraudValidationService.validate(id);
    return ResponseEntity.ok(SolicitationResponse.fromEntity(updated));
  }
}
