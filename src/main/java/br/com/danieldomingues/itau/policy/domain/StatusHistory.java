package br.com.danieldomingues.itau.policy.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "status_history")
public class StatusHistory {
  @Id @GeneratedValue private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  @Column(nullable = false)
  private OffsetDateTime timestamp;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "solicitation_id", nullable = false)
  private Solicitation solicitation;

  public UUID getId() {
    return id;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public Solicitation getSolicitation() {
    return solicitation;
  }

  public void setSolicitation(Solicitation solicitation) {
    this.solicitation = solicitation;
  }
}
