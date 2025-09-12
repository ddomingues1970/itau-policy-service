package br.com.danieldomingues.itau.policy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolicitationDomainTest {

  private Solicitation newBare() {
    return Solicitation.builder()
        .customerId(UUID.randomUUID())
        .productId("prod-1")
        .category(Category.AUTO)
        .salesChannel("MOBILE")
        .paymentMethod("CREDIT_CARD")
        .totalMonthlyPremiumAmount(new BigDecimal("10.00"))
        .insuredAmount(new BigDecimal("1000.00"))
        .build();
  }

  @Test
  @DisplayName("@PrePersist deve setar defaults e registrar RECEBIDO uma única vez")
  void prePersist_shouldInitDefaultsAndAddReceivedOnce() {
    Solicitation s = newBare();

    // antes do prePersist
    assertThat(s.getStatus()).isNull();
    assertThat(s.getCreatedAt()).isNull();
    assertThat(s.getHistory()).isEmpty();

    // chama método package-private diretamente (mesmo pacote)
    s.prePersist();

    assertThat(s.getStatus()).isEqualTo(Status.RECEBIDO);
    assertThat(s.getCreatedAt()).isNotNull();
    assertThat(s.getHistory()).hasSize(1);
    assertThat(s.getHistory().get(0).getStatus()).isEqualTo(Status.RECEBIDO);
    assertThat(s.getHistory().get(0).getTimestamp()).isEqualTo(s.getCreatedAt());

    // chamando novamente não deve duplicar RECEBIDO
    s.prePersist();
    assertThat(s.getHistory()).hasSize(1);
  }

  @Test
  @DisplayName("@PrePersist não duplica RECEBIDO quando histórico já contém RECEBIDO")
  void prePersist_shouldNotDuplicateReceivedIfAlreadyPresent() {
    Solicitation s = newBare();
    OffsetDateTime created = OffsetDateTime.now().minusMinutes(5);
    s.setCreatedAt(created);
    s.setStatus(Status.RECEBIDO);
    s.addHistory(Status.RECEBIDO, created);

    s.prePersist();

    assertThat(s.getCreatedAt()).isEqualTo(created);
    assertThat(s.getStatus()).isEqualTo(Status.RECEBIDO);
    assertThat(s.getHistory()).hasSize(1);
    assertThat(s.getHistory().get(0).getStatus()).isEqualTo(Status.RECEBIDO);
  }

  @Test
  @DisplayName("addHistory mantém ordenação cronológica (OrderBy ASC) e não altera finishedAt")
  void addHistory_shouldAppendInOrderAndKeepFinishedAt() {
    Solicitation s = newBare();
    s.prePersist(); // adiciona RECEBIDO no createdAt

    OffsetDateTime after = s.getCreatedAt().plusMinutes(1);
    s.addHistory(Status.VALIDADO, after);

    assertThat(s.getFinishedAt()).isNull();
    assertThat(s.getHistory()).hasSize(2);
    assertThat(s.getHistory().get(0).getTimestamp()).isBefore(s.getHistory().get(1).getTimestamp());
    assertThat(s.getHistory().get(0).getStatus()).isEqualTo(Status.RECEBIDO);
    assertThat(s.getHistory().get(1).getStatus()).isEqualTo(Status.VALIDADO);
  }
}
