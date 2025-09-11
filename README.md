# Itaú — Policy Service (MVP EDA)

Microsserviço para gerenciar o ciclo de vida de solicitações de apólice, orientado a eventos.

## Stack
- Java 17, Spring Boot 3.3
- PostgreSQL 16 (Docker)
- RabbitMQ 3-management (Docker)
- WireMock 3 (Docker) — mock da API de Fraudes
- Maven, Testcontainers, Actuator
- JaCoCo (coverage)
- Postman (coleção)

## Infra (Docker)
```bash
docker compose up -d
```
Serviços:
- **Postgres**: `localhost:5432` (itau/itau, db: `policydb`)
- **RabbitMQ UI**: http://localhost:15672 (guest/guest)
- **WireMock**: http://localhost:8081/__admin

## Executar a aplicação
```bash
mvn spring-boot:run
```
Health:
```bash
curl -s http://localhost:8080/actuator/health
```

## Build com app via Compose
```bash
mvn clean package -DskipTests
docker compose up -d --build
```

## API
- **POST** `/solicitations` — cria solicitação
- **GET** `/solicitations/{id}` — consulta por ID
- **GET** `/solicitations?customerId={uuid}` — lista por cliente
- **POST** `/solicitations/{id}/validate` — valida fraude (WireMock)
- **DELETE** `/solicitations/{id}` — **cancela** a solicitação  
  Regras:
  - `APPROVED`/`REJECTED` → **400** (terminal)
  - inexistente → **404**
  - idempotente em `CANCELLED` → **204**

## Observabilidade
- Actuator expõe: `/actuator/health`, `/actuator/info`, `/actuator/metrics`
- Logs estruturados (logfmt) no console:
  ```
  ts=2025-09-11T18:20:01-03:00 level=INFO logger=b.c.d.i.p... thread=main msg="Started ..."
  ```

## Testes & cobertura
```bash
mvn clean verify
# Relatório: target/site/jacoco/index.html
```
Gate: **linhas ≥ 80%**, **branches ≥ 70%** (UT + IT).

## Postman
Coleção: `src/main/resources/itau-policy-service.postman_collection.json`  
Fluxo:
1. Health
2. Criar
3. Consultar por ID
4. Consultar por cliente
5. Validar
6. **Cancelar**

Variáveis: `baseUrl`, `solicitationId`, `customerId`, `productId`.

## Eventos (RabbitMQ)
- Exchange: `policy.lifecycle` (topic)
- Eventos publicados: `SolicitationValidatedEvent`, `SolicitationRejectedEvent`  
(Consumo será expandido em etapas futuras.)

## Decisões & trade-offs
- EDA com RabbitMQ para desacoplamento e evolução incremental.
- Mock de fraudes com WireMock (`POST /fraud/check`) para testes determinísticos.
- **JaCoCo gate** para garantir qualidade mínima contínua.
- Log fmt simples sem dependências extras para facilitar parsing local e CI.

## Troubleshooting
- **WireMock 404**: verifique `infra/wiremock/mappings/*.json` e a porta **8081** no Compose.
- **RabbitMQ indisponível**: confira `docker compose ps` e credenciais.
- **DB schema**: em `dev/test`, `ddl-auto=update`; em produção use migrações (Flyway/Liquibase).
- **Coverage ausente**: certifique-se do POM com `prepare-agent` (UT/IT) e execute `mvn clean verify`.
