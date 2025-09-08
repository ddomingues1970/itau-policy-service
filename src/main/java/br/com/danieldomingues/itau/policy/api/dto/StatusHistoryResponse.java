package br.com.danieldomingues.itau.policy.api.dto;

import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.domain.StatusHistory;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * DTO para representar entradas do histórico de status.
 */
@Value
@Builder
public class StatusHistoryResponse {
  Status status;
  LocalDateTime timestamp;

  public static StatusHistoryResponse fromEntity(StatusHistory entity) {
    return StatusHistoryResponse.builder()
        .status(entity.getStatus())
        .timestamp(entity.getTimestamp().toLocalDateTime())
        .build();
  }
}
