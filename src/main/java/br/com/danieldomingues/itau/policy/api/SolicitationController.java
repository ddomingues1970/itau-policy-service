package br.com.danieldomingues.itau.policy.api;

import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationRequest;
import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationResponse;
import br.com.danieldomingues.itau.policy.api.dto.SolicitationResponse;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.service.SolicitationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/solicitacoes")
@RequiredArgsConstructor
public class SolicitationController {

  private final SolicitationService service;

  @PostMapping(consumes = "application/json", produces = "application/json")
  public ResponseEntity<CreateSolicitationResponse> create(
      @Valid @RequestBody CreateSolicitationRequest req) {
    Solicitation saved = service.create(req);

    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.getId())
            .toUri();

    return ResponseEntity.created(location)
        .body(new CreateSolicitationResponse(saved.getId(), saved.getCreatedAt()));
  }

  @GetMapping("/solicitations/{id}")
  public ResponseEntity<SolicitationResponse> getById(@PathVariable UUID id) {
    return service
        .getWithHistoryById(id)
        .map(SolicitationResponse::fromEntity) // conversão para DTO
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/solicitations")
  public ResponseEntity<List<SolicitationResponse>> listByCustomer(@RequestParam UUID customerId) {
    List<SolicitationResponse> list =
        service.findByCustomerId(customerId).stream()
            .map(SolicitationResponse::fromEntity)
            .toList();
    return ResponseEntity.ok(list);
  }
}
