package br.com.danieldomingues.itau.policy.api.dto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.danieldomingues.itau.policy.domain.Category;
import br.com.danieldomingues.itau.policy.domain.Solicitation;
import br.com.danieldomingues.itau.policy.domain.Status;
import br.com.danieldomingues.itau.policy.domain.StatusHistory;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import br.com.danieldomingues.itau.policy.service.SolicitationService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SolicitationServiceCoverageTest {

  private SolicitationRepository repository;
  private SolicitationService service;

  @BeforeEach
  void setup() {
    repository = mock(SolicitationRepository.class);
    service = new SolicitationService(repository);
  }

  private static Solicitation newEntityBase() {
    return Solicitation.builder()
        .id(UUID.randomUUID())
        .customerId(UUID.randomUUID())
        .productId("P-123")
        .category(Category.AUTO)
        .salesChannel("MOBILE")
        .paymentMethod("CREDIT_CARD")
        .totalMonthlyPremiumAmount(new BigDecimal("10.00"))
        .insuredAmount(new BigDecimal("1000.00"))
        .status(Status.RECEBIDO)
        .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5))
        .build();
  }

  @Test
  @DisplayName(
      "getWithHistoryById: deve retornar Optional.empty quando não encontrado (ramo empty)")
  void getWithHistoryById_empty() {
    UUID id = UUID.randomUUID();
    when(repository.findWithHistoryById(id)).thenReturn(Optional.empty());

    Optional<Solicitation> out = service.getWithHistoryById(id);

    assertTrue(out.isEmpty());
    verify(repository).findWithHistoryById(id);
  }

  @Test
  @DisplayName("getWithHistoryById: inicializa coleções quando estão nulas (ramos false)")
  void getWithHistoryById_present_nullCollections() {
    UUID id = UUID.randomUUID();
    Solicitation s = newEntityBase();
    // coleções ficam NULL de propósito para exercitar os ramos 'if (...) == null'
    when(repository.findWithHistoryById(id)).thenReturn(Optional.of(s));

    Optional<Solicitation> out = service.getWithHistoryById(id);

    assertTrue(out.isPresent());
    // Apenas garante que não estourou LazyInitializationException
    assertEquals(Status.RECEBIDO, out.get().getStatus());
    verify(repository).findWithHistoryById(id);
  }

  @Test
  @DisplayName("getWithHistoryById: inicializa coleções quando presentes (ramos true)")
  void getWithHistoryById_present_initializedCollections() {
    UUID id = UUID.randomUUID();
    Solicitation s = newEntityBase();

    // coverages (Map), assistances (List) e history (List)
    Map<String, BigDecimal> coverages = new HashMap<>();
    coverages.put("Roubo", new BigDecimal("500.00"));
    s.setCoverages(coverages);

    List<String> assistances = new ArrayList<>();
    assistances.add("Guincho 100km");
    s.setAssistances(assistances);

    List<StatusHistory> history = new ArrayList<>();
    history.add(new StatusHistory(null, Status.RECEBIDO, s.getCreatedAt(), s));
    s.setHistory(history);

    when(repository.findWithHistoryById(id)).thenReturn(Optional.of(s));

    Optional<Solicitation> out = service.getWithHistoryById(id);

    assertTrue(out.isPresent());
    assertEquals("Guincho 100km", out.get().getAssistances().get(0));
    assertEquals(new BigDecimal("500.00"), out.get().getCoverages().get("Roubo"));
    assertFalse(out.get().getHistory().isEmpty());
    verify(repository).findWithHistoryById(id);
  }

  @Test
  @DisplayName("findByCustomerId: materializa coleções em itens da lista (mix nulo vs preenchido)")
  void findByCustomerId_materializeCollections() {
    UUID customer = UUID.randomUUID();

    // item A com coleções nulas (ramo false)
    Solicitation a = newEntityBase();
    a.setCustomerId(customer);

    // item B com coleções preenchidas (ramo true)
    Solicitation b = newEntityBase();
    b.setCustomerId(customer);

    Map<String, BigDecimal> coverages = new HashMap<>();
    coverages.put("Incendio", new BigDecimal("123.45"));
    b.setCoverages(coverages);

    List<String> assistances = new ArrayList<>();
    assistances.add("Chaveiro 24h");
    b.setAssistances(assistances);

    List<StatusHistory> history = new ArrayList<>();
    history.add(new StatusHistory(null, Status.RECEBIDO, b.getCreatedAt(), b));
    b.setHistory(history);

    when(repository.findByCustomerId(customer)).thenReturn(List.of(a, b));

    List<Solicitation> out = service.findByCustomerId(customer);

    assertEquals(2, out.size());
    // Apenas sanidade: os dados do item B continuam acessíveis
    assertEquals(new BigDecimal("123.45"), out.get(1).getCoverages().get("Incendio"));
    assertEquals("Chaveiro 24h", out.get(1).getAssistances().get(0));
    verify(repository).findByCustomerId(customer);
  }
}
