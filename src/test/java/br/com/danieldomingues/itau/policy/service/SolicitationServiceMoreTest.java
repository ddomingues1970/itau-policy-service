package br.com.danieldomingues.itau.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import br.com.danieldomingues.itau.policy.factory.SolicitationFactory;
import br.com.danieldomingues.itau.policy.repo.SolicitationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SolicitationServiceMoreTest {

  @Mock private SolicitationRepository repository;
  // Caso o service NÃO receba a factory, o Mockito simplesmente ignora este mock.
  @Mock private SolicitationFactory factory;

  @InjectMocks private SolicitationService service;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @DisplayName("getWithHistoryById → Optional.empty quando não encontrado")
  void getWithHistoryById_shouldReturnEmpty_whenNotFound() {
    UUID id = UUID.randomUUID();
    when(repository.findWithHistoryById(id)).thenReturn(Optional.empty());

    Optional<?> out = service.getWithHistoryById(id);

    assertThat(out).isEmpty();
    verify(repository).findWithHistoryById(id);
    verifyNoMoreInteractions(repository);
  }

  @Test
  @DisplayName("findByCustomerId → lista vazia quando não há dados")
  void findByCustomerId_shouldReturnEmptyList_whenNoData() {
    UUID customerId = UUID.randomUUID();
    when(repository.findByCustomerId(customerId)).thenReturn(List.of());

    List<?> out = service.findByCustomerId(customerId);

    assertThat(out).isEmpty();
    verify(repository).findByCustomerId(customerId);
    verifyNoMoreInteractions(repository);
  }
}
