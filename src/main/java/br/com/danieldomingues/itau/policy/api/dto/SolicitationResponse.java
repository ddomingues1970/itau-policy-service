package br.com.danieldomingues.itau.policy.api.dto;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import org.hibernate.Hibernate;

@Value
@Builder
public class SolicitationResponse {

  UUID id;
  UUID customerId;
  String productId;
  Category category;
  String salesChannel; // <-- novo
  String paymentMethod; // <-- opcional, mas útil para consistência
  BigDecimal totalMonthlyPremiumAmount; // <-- opcional, mantém precisão
  BigDecimal insuredAmount; // <-- opcional, mantém precisão
  Status status;
  OffsetDateTime createdAt;
  OffsetDateTime finishedAt;

  Map<String, BigDecimal> coverages;
  List<String> assistances;
  List<StatusHistoryResponse> history;

  public static SolicitationResponse fromEntity(Solicitation e) {
    return SolicitationResponse.builder()
        .id(e.getId())
        .customerId(e.getCustomerId())
        .productId(e.getProductId())
        .category(e.getCategory())
        .salesChannel(e.getSalesChannel()) // <-- mapeado
        .paymentMethod(e.getPaymentMethod()) // <-- mapeado
        .totalMonthlyPremiumAmount(e.getTotalMonthlyPremiumAmount()) // <-- mapeado
        .insuredAmount(e.getInsuredAmount()) // <-- mapeado
        .status(e.getStatus())
        .createdAt(e.getCreatedAt())
        .finishedAt(e.getFinishedAt())
        // Defensive: só copia se a coleção estiver inicializada; senão, responde vazio
        .coverages(
            e.getCoverages() == null || !Hibernate.isInitialized(e.getCoverages())
                ? Map.<String, BigDecimal>of()
                : Map.copyOf(e.getCoverages()))
        .assistances(
            e.getAssistances() == null || !Hibernate.isInitialized(e.getAssistances())
                ? List.<String>of()
                : List.copyOf(e.getAssistances()))
        .history(
            e.getHistory() == null || !Hibernate.isInitialized(e.getHistory())
                ? List.<StatusHistoryResponse>of()
                : e.getHistory().stream().map(StatusHistoryResponse::fromEntity).toList())
        .build();
  }
}
