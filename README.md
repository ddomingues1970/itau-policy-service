# Itaú — Policy Service (MVP EDA)

Microsserviço para gerenciar o ciclo de vida de solicitações de apólice, orientado a eventos.

## Stack

- Java 17, Spring Boot 3.3
- PostgreSQL 16 (Docker)
- RabbitMQ 3-management (Docker)
- WireMock 3 (Docker) — mock da API de Fraudes
- Maven, Testcontainers, Actuator

## Como subir a infraestrutura

```bash
docker compose up -d
# Postgres: localhost:5432 (itau/itau, db: policydb)
# RabbitMQ UI: http://localhost:15672 (guest/guest)
# WireMock:   http://localhost:8081/__admin

## Como rodar a aplicação
mvn spring-boot:run
# health:
curl -s http://localhost:8080/actuator/health

## Decisões iniciais
EDA com RabbitMQ; eventos de ciclo de vida publicados em exchange policy.lifecycle (topic).
Mock de Fraudes com WireMock via /frauds/{orderId}?c=REGULAR|HIGH_RISK|PREFERENTIAL|NO_INFO.
Estados: RECEIVED → VALIDATED/REJECTED → PENDING → APPROVED/REJECTED/CANCELLED.

## Próximos passos
POST /solicitacoes (persistência + retorno id/createdAt)
Consultas por ID da solicitação e por customerId
Integração com mock de Fraudes e regras por classificação
Consumidores de eventos (pagamento/subscrição) e publicações de status