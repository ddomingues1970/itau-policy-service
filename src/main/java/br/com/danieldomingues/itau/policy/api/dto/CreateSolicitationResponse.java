package br.com.danieldomingues.itau.policy.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateSolicitationResponse {
  private UUID id;

  @JsonFormat(shape = JsonFormat.Shape.STRING)
  private OffsetDateTime createdAt;
}
