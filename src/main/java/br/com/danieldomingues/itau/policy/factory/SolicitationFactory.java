package br.com.danieldomingues.itau.policy.factory;

import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationRequest;
import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SolicitationFactory {

  public static Solicitation from(CreateSolicitationRequest request) {
    return Solicitation.builder()
        .id(UUID.randomUUID())
        .customerId(request.getCustomerId())
        .productId(request.getProductId())
        .insuredAmount(request.getInsuredAmount())
        .totalMonthlyPremiumAmount(request.getTotalMonthlyPremiumAmount())
        .createdAt(OffsetDateTime.now())
        .status(Status.RECEIVED) // estado inicial conforme planejamento
        .build();
  }

  /**
   * Cria uma nova solicitação já com os valores iniciais padronizados:
   * - status: RECEIVED
   * - createdAt: agora
   * - histórico com entrada RECEIVED
   */
  public Solicitation newSolicitation(
      java.util.UUID customerId,
      String productId,
      Category category,
      String salesChannel,
      String paymentMethod,
      java.math.BigDecimal totalMonthlyPremiumAmount,
      java.math.BigDecimal insuredAmount,
      Map<String, java.math.BigDecimal> coverages,
      java.util.List<String> assistances) {
    var s =
        Solicitation.builder()
            .customerId(customerId)
            .productId(productId)
            .category(category)
            .salesChannel(salesChannel)
            .paymentMethod(paymentMethod)
            .totalMonthlyPremiumAmount(totalMonthlyPremiumAmount)
            .insuredAmount(insuredAmount)
            .build();

    // Coleções defensivas (evita NPE e preserva ordem)
    if (coverages != null) {
      s.getCoverages().putAll(coverages);
    }
    if (assistances != null) {
      s.getAssistances().addAll(assistances);
    }

    // Defaults de criação
    s.setStatus(Status.RECEIVED);
    OffsetDateTime now = OffsetDateTime.now();
    s.setCreatedAt(now);
    s.addHistory(Status.RECEIVED, now);

    return s;
  }
}
