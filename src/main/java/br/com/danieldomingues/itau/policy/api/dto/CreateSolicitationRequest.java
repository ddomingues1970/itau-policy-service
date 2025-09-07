package br.com.danieldomingues.itau.policy.api.dto;

import br.com.danieldomingues.itau.policy.domain.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSolicitationRequest {

  @NotNull private UUID customerId;

  @NotBlank private String productId;

  @NotNull private Category category;

  @NotBlank private String salesChannel;

  @NotBlank private String paymentMethod;

  @NotNull
  @DecimalMin("0.00")
  private BigDecimal totalMonthlyPremiumAmount;

  @NotNull
  @DecimalMin("0.00")
  private BigDecimal insuredAmount;

  @NotNull
  @Size(min = 1, message = "coverages não pode ser vazio")
  private Map<@NotBlank String, @NotNull @DecimalMin("0.00") BigDecimal> coverages;

  @NotNull private List<@NotBlank String> assistances;
}
