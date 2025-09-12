package br.com.danieldomingues.itau.policy.repo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ExtendWith(SpringExtension.class)
class SolicitationRepositoryTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("policydb")
          .withUsername("policy")
          .withPassword("policy");

  @Autowired private SolicitationRepository repo;

  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
  }

  @Test
  void shouldPersistAndFind() {
    Solicitation solicitation =
        Solicitation.builder()
            .customerId(UUID.randomUUID())
            .productId("test-product")
            .category(Category.AUTO)
            .salesChannel("WEB")
            .paymentMethod("CREDIT_CARD")
            .totalMonthlyPremiumAmount(BigDecimal.valueOf(100.0))
            .insuredAmount(BigDecimal.valueOf(50000.0))
            .status(Status.RECEBIDO)
            .createdAt(OffsetDateTime.now())
            .build();

    Solicitation saved = repo.save(solicitation);

    Optional<Solicitation> found = repo.findById(saved.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getProductId()).isEqualTo("test-product");
  }
}
