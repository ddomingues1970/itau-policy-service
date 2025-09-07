package br.com.danieldomingues.itau.policy.api;

import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationRequest;
import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationResponse;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.service.SolicitationService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
