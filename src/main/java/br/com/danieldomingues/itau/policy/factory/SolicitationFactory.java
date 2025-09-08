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
    Solicitation s =
        Solicitation.builder()
            .id(UUID.randomUUID())
            .customerId(request.getCustomerId())
            .productId(request.getProductId())
            .category(request.getCategory())
            .salesChannel(request.getSalesChannel())
            .paymentMethod(request.getPaymentMethod())
            .insuredAmount(request.getInsuredAmount())
            .totalMonthlyPremiumAmount(request.getTotalMonthlyPremiumAmount())
            .build();

    // Coleções defensivas
    if (request.getCoverages() != null) {
      s.getCoverages().putAll(request.getCoverages());
    }
    if (request.getAssistances() != null) {
      s.getAssistances().addAll(request.getAssistances());
    }

    // Defaults de criação
    OffsetDateTime now = OffsetDateTime.now();
    s.setCreatedAt(now);
    s.setStatus(Status.RECEIVED);
    s.addHistory(Status.RECEIVED, now);

    return s;
  }

  /**
   * Cria uma nova solicitação já com os valores iniciais padronizados.
   */
  public Solicitation newSolicitation(
      UUID customerId,
      String productId,
      Category category,
      String salesChannel,
      String paymentMethod,
      java.math.BigDecimal totalMonthlyPremiumAmount,
      java.math.BigDecimal insuredAmount,
      Map<String, java.math.BigDecimal> coverages,
      java.util.List<String> assistances) {

    Solicitation s =
        Solicitation.builder()
            .customerId(customerId)
            .productId(productId)
            .category(category)
            .salesChannel(salesChannel)
            .paymentMethod(paymentMethod)
            .totalMonthlyPremiumAmount(totalMonthlyPremiumAmount)
            .insuredAmount(insuredAmount)
            .build();

    if (coverages != null) {
      s.getCoverages().putAll(coverages);
    }
    if (assistances != null) {
      s.getAssistances().addAll(assistances);
    }

    OffsetDateTime now = OffsetDateTime.now();
    s.setStatus(Status.RECEIVED);
    s.setCreatedAt(now);
    s.addHistory(Status.RECEIVED, now);

    return s;
  }
}
