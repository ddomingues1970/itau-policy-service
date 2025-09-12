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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(value = "/solicitations")
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

  @GetMapping(value = "/{id}", produces = "application/json")
  public ResponseEntity<SolicitationResponse> getById(@PathVariable("id") UUID id) {
    return service
        .getWithHistoryById(id)
        .map(SolicitationResponse::fromEntity)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping(produces = "application/json")
  public ResponseEntity<List<SolicitationResponse>> listByCustomer(
      @RequestParam(value = "customerId") UUID customerId) {

    List<SolicitationResponse> list =
        service.findByCustomerId(customerId).stream()
            .map(SolicitationResponse::fromEntity)
            .toList();

    return ResponseEntity.ok(list);
  }

  @DeleteMapping(value = "/{id}")
  public ResponseEntity<Void> cancel(@PathVariable("id") UUID id) {
    service.cancel(id);
    return ResponseEntity.noContent().build();
  }
}
