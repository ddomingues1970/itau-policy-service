package br.com.danieldomingues.itau.policy.factory;

import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationRequest;
import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SolicitationFactory {

  public static Solicitation from(CreateSolicitationRequest request) {
    // Delega status/createdAt/history para @PrePersist em Solicitation
    Solicitation s =
        Solicitation.builder()
            // NÃO definir id aqui: JPA gera
            .customerId(request.getCustomerId())
            .productId(request.getProductId())
            .category(request.getCategory())
            .salesChannel(request.getSalesChannel())
            .paymentMethod(request.getPaymentMethod())
            .insuredAmount(request.getInsuredAmount())
            .totalMonthlyPremiumAmount(request.getTotalMonthlyPremiumAmount())
            .build();

    // Copia defensiva das coleções
    if (request.getCoverages() != null) {
      s.getCoverages().putAll(request.getCoverages());
    }
    if (request.getAssistances() != null) {
      s.getAssistances().addAll(request.getAssistances());
    }

    // Não setar createdAt/status/history aqui — entidade cuida no @PrePersist
    return s;
  }

  /**
   * Fábrica alternativa (mesma regra: @PrePersist define estado inicial).
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

    if (coverages != null) s.getCoverages().putAll(coverages);
    if (assistances != null) s.getAssistances().addAll(assistances);

    // Sem createdAt/status/history — @PrePersist cuida disso
    return s;
  }
}
