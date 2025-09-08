package br.com.danieldomingues.itau.policy.api.dto;

import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.domain.StatusHistory;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/**
 * DTO de resposta para Solicitation, usado nas consultas da API.
 * Responsabilidade: expor dados necessários ao cliente de forma segura.
 */
@Value
@Builder
public class SolicitationResponse {

  UUID id;
  UUID customerId;
  Long productId;
  String category;
  String salesChannel;
  String paymentMethod;
  BigDecimal totalMonthlyPremiumAmount;
  BigDecimal insuredAmount;
  Map<String, BigDecimal> coverages;
  List<String> assistances;
  Status status;
  LocalDateTime createdAt;
  LocalDateTime finishedAt;
  List<StatusHistoryResponse> history;

  /**
   * Constrói um DTO a partir da entidade de domínio.
   */
  public static SolicitationResponse fromEntity(Solicitation e) {
    return SolicitationResponse.builder()
        .id(e.getId())
        .customerId(e.getCustomerId())
        .productId(toLong(e.getProductId()))
        .category(toCategoryString(e.getCategory()))
        .salesChannel(e.getSalesChannel())
        .paymentMethod(e.getPaymentMethod())
        .totalMonthlyPremiumAmount(e.getTotalMonthlyPremiumAmount())
        .insuredAmount(e.getInsuredAmount())
        .coverages(e.getCoverages() == null ? Map.of() : Map.copyOf(e.getCoverages()))
        .assistances(e.getAssistances() == null ? List.of() : List.copyOf(e.getAssistances()))
        .status(e.getStatus())
        .createdAt(toLocalDateTime(e.getCreatedAt()))
        .finishedAt(toLocalDateTime(e.getFinishedAt()))
        .history(
            e.getHistory() == null
                ? List.of()
                : e.getHistory().stream().map(StatusHistoryResponse::fromEntity).toList())
        .build();
  }

  // --- helpers privados ---

  private static String toCategoryString(Object category) {
    if (category == null) return null;
    if (category instanceof Enum<?> en) return en.name();
    return category.toString();
  }

  private static Long toLong(Object v) {
    if (v == null) return null;
    if (v instanceof Number n) return n.longValue();
    if (v instanceof String s && !s.isBlank()) return Long.parseLong(s);
    throw new IllegalArgumentException("productId inválido: " + v);
  }

  private static LocalDateTime toLocalDateTime(Object t) {
    if (t == null) return null;
    if (t instanceof LocalDateTime ldt) return ldt;
    if (t instanceof OffsetDateTime odt) return odt.toLocalDateTime();
    if (t instanceof Instant i) return LocalDateTime.ofInstant(i, ZoneId.systemDefault());
    if (t instanceof Timestamp ts) return ts.toLocalDateTime();
    throw new IllegalArgumentException("Tipo de data não suportado: " + t.getClass());
  }

  /**
   * DTO interno para histórico de status.
   */
  @Value
  @Builder
  public static class StatusHistoryResponse {
    Status status;
    LocalDateTime timestamp;

    public static StatusHistoryResponse fromEntity(StatusHistory entity) {
      return StatusHistoryResponse.builder()
          .status(entity.getStatus())
          .timestamp(toLocalDateTime(entity.getTimestamp()))
          .build();
    }
  }
}
