package br.com.danieldomingues.itau.policy.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.*;

@Entity
@Table(
    name = "solicitation",
    indexes = {
      @Index(name = "idx_solicitation_customer", columnList = "customerId"),
      @Index(name = "idx_solicitation_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // exigido pelo JPA
@AllArgsConstructor
@Builder
public class Solicitation {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  private UUID customerId;

  @Column(nullable = false, length = 64)
  private String productId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Category category;

  @Column(nullable = false)
  private String salesChannel;

  @Column(nullable = false)
  private String paymentMethod;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal totalMonthlyPremiumAmount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal insuredAmount;

  @ElementCollection
  @CollectionTable(
      name = "solicitation_coverages",
      joinColumns = @JoinColumn(name = "solicitation_id"))
  @MapKeyColumn(name = "name")
  @Column(name = "amount", nullable = false, precision = 14, scale = 2)
  @Builder.Default
  private Map<String, BigDecimal> coverages = new LinkedHashMap<>(); // NÃO final

  @ElementCollection
  @CollectionTable(
      name = "solicitation_assistances",
      joinColumns = @JoinColumn(name = "solicitation_id"))
  @Column(name = "assistance", nullable = false)
  @Builder.Default
  private List<String> assistances = new ArrayList<>(); // NÃO final

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  private OffsetDateTime finishedAt;

  @OneToMany(mappedBy = "solicitation", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("timestamp ASC")
  @Builder.Default
  private List<StatusHistory> history = new ArrayList<>(); // NÃO final

  public void addHistory(Status s, OffsetDateTime when) {
    StatusHistory h = new StatusHistory();
    h.setStatus(s);
    h.setTimestamp(when);
    h.setSolicitation(this);
    this.history.add(h);
  }

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = OffsetDateTime.now();
  }
}
