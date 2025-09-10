# Itaú --- Policy Service (MVP EDA)

Microsserviço para gerenciar o ciclo de vida de solicitações de apólice,
orientado a eventos.

## Stack

-   Java 17, Spring Boot 3.3
-   PostgreSQL 16 (Docker)
-   RabbitMQ 3-management (Docker)
-   WireMock 3 (Docker) --- mock da API de Fraudes
-   Maven, Testcontainers, Actuator
-   JaCoCo (cobertura de testes)
-   Postman (coleção de testes)

## Como subir a infraestrutura

``` bash
docker compose up -d
```

Serviços disponíveis: - **Postgres**: `localhost:5432` (user: `itau` /
pwd: `itau`, db: `policydb`) - **RabbitMQ UI**: <http://localhost:15672>
(guest/guest) - **WireMock**: <http://localhost:8081/__admin>

## Como rodar a aplicação (local)

``` bash
mvn spring-boot:run
```

Health check:

``` bash
curl -s http://localhost:8080/actuator/health
```

## Como rodar a aplicação (via Docker Compose, incluindo app)

``` bash
mvn clean package -DskipTests
docker compose up -d --build
```

A aplicação sobe junto com Postgres, RabbitMQ e WireMock.

## Testes e cobertura

Rodar todos os testes + gerar relatório de cobertura:

``` bash
mvn clean verify
```

Abrir relatório JaCoCo:

    target/site/jacoco/index.html

Thresholds configurados: linhas ≥ 80%, branches ≥ 70%.

## Eventos no RabbitMQ

-   Exchange: `policy.lifecycle` (topic)
-   Eventos publicados:
    -   `SolicitationValidatedEvent`
    -   `SolicitationRejectedEvent`
    -   `SolicitationApprovedEvent`

Exemplo inspeção via cURL:

``` bash
curl -u guest:guest http://localhost:15672/api/queues/%2f/validatedQueue/get -d'{"count":5,"requeue":true,"encoding":"auto"}'
```

Ou pela interface de gerenciamento (http://localhost:15672).

## Postman (fluxo end-to-end)

A coleção está em:
`src/main/resources/itau-policy-service.postman_collection.json`

Fluxo suportado: 1. Criar solicitação (`POST /solicitations`) 2.
Consultar por ID (`GET /solicitations/{id}`) 3. Consultar por cliente
(`GET /solicitations?customerId=...`) 4. Validar fraude
(`POST /solicitations/{id}/validate`)

Variáveis no Postman: - `baseUrl` (ex: `http://localhost:8080`) -
`solicitationId` (definido no create e usado nos próximos passos)

## Decisões de arquitetura

-   Arquitetura orientada a eventos (EDA) com RabbitMQ
-   Microsserviço focado no ciclo de vida de solicitações
-   Eventos desacoplados para integração futura (pagamento, subscrição,
    notificação)
-   API de Fraudes mockada com WireMock (`POST /fraud/check`)
-   Estados principais:
    -   RECEIVED → VALIDATED/REJECTED
    -   VALIDATED → PENDING
    -   PENDING → APPROVED/REJECTED/CANCELLED

## Decisões técnicas / trade-offs

-   Durante desenvolvimento e testes:
    `spring.jpa.hibernate.ddl-auto=create` para facilitar a criação do
    schema.
-   **Produção**: usar **Flyway** ou **Liquibase** para versionamento
    controlado.
-   Testes automatizados com cobertura mínima validada por JaCoCo
    (linhas ≥ 80%, branches ≥ 70%).
-   Uso de **Testcontainers** para isolar dependências em testes de
    integração.

## Próximos passos

-   Implementar consumidores para eventos de pagamento/subscrição.
-   Expandir transições de estado (PENDING → APPROVED/REJECTED).
-   Implementar cancelamento com regras de bloqueio em estados
    terminais.
-   Adicionar métricas e logs estruturados (Micrometer/Actuator).
-   Criar seção de troubleshooting no README.

------------------------------------------------------------------------

### Requisitos Atendidos até 09/09/2025

-   Persistência com JPA (Solicitação, Histórico, Cliente).
-   API REST: criar e consultar solicitações.
-   Integração com API de Fraudes (mock via WireMock).
-   Transição RECEIVED → VALIDATED/REJECTED com publicação de eventos.
-   Cobertura mínima de testes garantida com JaCoCo.
-   Docker Compose ponta a ponta (Postgres, RabbitMQ, WireMock, App).

### Pontos Críticos já cumpridos

-   Arquitetura orientada a eventos implementada.
-   Testes com cobertura mínima atendida.
-   Documentação inicial no README.
-   Docker Compose ponta a ponta validado.
