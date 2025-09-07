package br.com.danieldomingues.itau.policy.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.danieldomingues.itau.policy.api.dto.CreateSolicitationRequest;
import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
class SolicitationServiceTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("policydb")
          .withUsername("itau")
          .withPassword("itau");

  CreateSolicitationRequest req;
  @Resource private SolicitationService service;

  @Resource private SolicitationRepository repo;

  @DynamicPropertySource
  static void datasourceProps(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    r.add("spring.datasource.username", POSTGRES::getUsername);
    r.add("spring.datasource.password", POSTGRES::getPassword);
    r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
  }

  @BeforeEach
  void setUp() {
    req = new CreateSolicitationRequest();
    req.setCustomerId(UUID.fromString("adc56d77-348c-4bf0-908f-22d402ee715c"));
    req.setProductId("1b2da7cc-b367-4196-8a78-9cfeec21f587");
    req.setCategory(Category.AUTO);
    req.setSalesChannel("MOBILE");
    req.setPaymentMethod("CREDIT_CARD");
    req.setTotalMonthlyPremiumAmount(new BigDecimal("75.25"));
    req.setInsuredAmount(new BigDecimal("275000.50"));
    req.setCoverages(Map.of("Roubo", new BigDecimal("100000.25")));
    req.setAssistances(List.of("Guincho 250km"));
  }

  @Test
  @DisplayName("Deve criar e persistir uma solicitação com status RECEIVED e histórico inicial")
  void shouldCreateSolicitation() {
    Solicitation saved = service.create(req);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getStatus().name()).isEqualTo("RECEIVED");

    // Recarrega do repositório para garantir cascade/history
    var fromDb = repo.findById(saved.getId()).orElseThrow();
    assertThat(fromDb.getHistory()).hasSize(1);
    assertThat(fromDb.getHistory().get(0).getStatus().name()).isEqualTo("RECEIVED");
  }
}
