# Accounts Microservices

Technical test on microservice architecture: two Spring Boot 4 services (WebFlux
+ JPA) on MariaDB, one schema per service (*database per service*), with the
customer validated through a REST call between the services.

| Service | Module | Port | Database | Base path |
|---|---|---|---|---|
| `ms-customers` | [customerms/](customerms/) | 8081 | `customers_ms` | `/api/v1` |
| `ms-accounts` | [accountms/account-service/](accountms/account-service/) | 8082 | `accounts_ms` | `/api/v1` |

There is no FK between the two schemas: `account.customer_id` is validated on
write by calling `ms-customers`.

## Deliverables

| Deliverable | Where |
|---|---|
| Database script | [schemas/BaseDatos.sql](schemas/BaseDatos.sql) — both schemas, tables, view and seed data |
| OpenAPI specification | [openapi/](openapi/) — both contracts, OpenAPI 3.0.3 |
| Endpoint test collection | [postman/bank-microservices.postman_collection.json](postman/bank-microservices.postman_collection.json) — 35 requests, 71 assertions |
| Docker deployment | [docker-compose.yml](docker-compose.yml) — `docker compose up --build` |

---

## Requirements

- **Docker Desktop** (with Compose v2) — the only thing needed to bring
  everything up.
- To run locally without Docker: **JDK 17** and **MariaDB 10.4+** (or MySQL 8.x)
  listening on `localhost:3306`. Maven is not needed: each module ships its own
  wrapper (`mvnw` / `mvnw.cmd`).

---

## Option 1 — Bring everything up with Docker Compose (recommended)

From the root of the repository, create the environment file Compose reads and
start everything:

```bash
cp .env.example .env
docker compose up --build
```

On Windows (PowerShell): `Copy-Item .env.example .env`.

`.env` is git-ignored because it is the file that would hold real credentials;
[.env.example](.env.example) ships the local development defaults and is the one
under version control.

This starts three containers on the `bank-net` network:

1. **`bank-db`** — `mariadb:11.4`, published on the host on port **3307** (so it
   does not clash with a local XAMPP/MySQL on 3306). On its first start it runs
   [docker/init-schemas.sql](docker/init-schemas.sql), which creates the empty
   `customers_ms` and `accounts_ms` schemas. The tables are created by **Flyway**
   when each service starts.
2. **`ms-customers`** — built from [customerms/Dockerfile](customerms/Dockerfile)
   (multi-stage build: Maven 3.9 + Temurin 17 → JRE 17 alpine). It waits for the
   database to pass its healthcheck.
3. **`ms-accounts`** — built from
   [accountms/account-service/Dockerfile](accountms/account-service/Dockerfile).
   It reaches `http://ms-customers:8081/api/v1` by service name.

The first build downloads the Maven dependencies and takes several minutes.

### Check that it is up

```bash
curl http://localhost:8081/api/v1/actuator/health
curl http://localhost:8082/api/v1/actuator/health
```

Both must answer `{"status":"UP"}` with the detail of the `db` component.

### Environment variables

Compose reads the .env file at the root (created from [.env.example](.env.example)):

| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_CUSTOMER_URL` | `jdbc:mysql://db:3306/customers_ms?...` |
| `SPRING_DATASOURCE_ACCOUNT_URL` | `jdbc:mysql://db:3306/accounts_ms?...` |
| `SPRING_DATASOURCE_USERNAME` | `root` |
| `SPRING_DATASOURCE_PASSWORD` | *(empty)* |
| `CUSTOMERS_SERVICE_URL` | `http://ms-customers:8081/api/v1` |

The database container starts with `MARIADB_ALLOW_EMPTY_ROOT_PASSWORD=yes`, which
is why the password is empty. That is a development setting: if this is ever
deployed outside the local machine, a real password has to be set in `.env` and
in the `environment` of the `db` service.

The JDBC URL uses the `jdbc:mysql://` scheme and the `mysql-connector-j` driver
even though the engine is MariaDB — deliberately, so dev, tests and deployment
all use the same driver. The dialect is `MariaDBDialect`, because `MySQLDialect`
emits `FOR UPDATE OF <alias>`, syntax MariaDB rejects on the pessimistic lock.

### Stop and clean up

```bash
docker compose down          # stops the containers, keeps the data
docker compose down -v       # also drops the db-data volume (blank database)
```

---

## Option 2 — Local run (no Docker for the services)

Requires a MariaDB/MySQL listening on `localhost:3306` with user `root` and an
empty password (XAMPP, for instance). The URLs in `application.properties` carry
`createDatabaseIfNotExist=true`, so the schemas create themselves; the tables are
applied by Flyway on startup.

Terminal 1 — customers:

```bash
cd customerms
./mvnw spring-boot:run
```

Terminal 2 — accounts:

```bash
cd accountms/account-service
./mvnw spring-boot:run
```

On Windows (PowerShell / CMD) use `.\mvnw.cmd` instead of `./mvnw`.

Start `customerms` first: `account-service` calls it to validate the customer
when an account is created (3 s timeout, configurable with
`customers.service.timeout-ms`).

Override the credentials without touching the files:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.datasource.password=myPassword
```

or through the standard Spring Boot environment variables
(`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
`SPRING_DATASOURCE_URL`, `CUSTOMERS_SERVICE_URL`).

### Bring up only the database with Docker

Useful to develop the services from the IDE:

```bash
docker compose up -d db
```

It is exposed on `localhost:3307`, so the port has to be adjusted in the JDBC URL
of each `application.properties` (they point at 3306 by default).

---

## Build the JARs

```bash
cd customerms && ./mvnw clean package
cd accountms/account-service && ./mvnw clean package
```

The artifacts land in `target/*.jar` of each module and run with
`java -jar target/<name>.jar`.

---

## Tests

Both modules split their tests the same way: Surefire runs the unit tests
(`*Test`), Failsafe runs the integration tests (`*IT`) in the `verify` phase.

Unit tests — no Spring, no database, no Docker:

```bash
./mvnw test
```

`account-service`: 47 tests (`AccountServiceTest`, `MovementServiceTest`).
`customerms`: 9 (`CustomerServiceTest`).

Integration tests (`*IT.java`) — they start a **`mariadb:11.4`** container
through Testcontainers, so **Docker has to be running**:

```bash
./mvnw verify
```

In `account-service` the integration suite concentrates on rule F3
(`MovementInsufficientBalanceIT`): it validates the 422 carrying the literal
"Saldo no disponible" end-to-end, that the rejection leaves the database
untouched, that the rule covers the three doors moving the balance (create, edit,
revert) and that two concurrent debits cannot slip past it. `customerms` covers
the customer CRUD in `CustomerIT`. Both modules also ship a context-startup smoke
test (`*ApplicationIT`).

One relevant detail of the `account-service` suite: the schema of the tests does
not come from the Flyway migrations but from
[schemas/BaseDatos.sql](schemas/BaseDatos.sql), the deliverable script — it is
mounted at container startup and Flyway is switched off in the tests
(`spring.flyway.enabled=false`). With `ddl-auto=validate`, if the script and the
entities drift apart, the context does not start and the first IT fails. The
script is located by walking up the parent directories, so the suite works the
same launched from the module or from the root of the repo.

---

## API

Interactive documentation (springdoc / Swagger UI) with the services up:

- Customers: http://localhost:8081/api/v1/swagger-ui.html
- Accounts: http://localhost:8082/api/v1/swagger-ui.html

### OpenAPI specification

Both contracts, together for delivery, in [openapi/](openapi/):

| File | Service | Operations |
|---|---|---|
| [openapi/customer_openapi.yaml](openapi/customer_openapi.yaml) | Customer Microservice (8081) | 6 over 2 paths |
| [openapi/account_openapi.yaml](openapi/account_openapi.yaml) | Account/Movement Microservice (8082) | 13 over 5 paths |

Both are OpenAPI 3.0.3. **Contract First**: they do not document the code, they
generate it — `openapi-generator-maven-plugin` produces, in `generate-sources`,
the interfaces (`CustomersApi`, `AccountsApi`, `MovementsApi`, `ReportsApi`) the
controllers implement, plus the DTOs with their Bean Validation. That is why
there is not a single `@GetMapping` in the code: if the contract and the code
drift apart, the module does not compile.

The canonical copy is the one living inside each module — it is the path the
plugin declares in `<inputSpec>` and the only one that enters the Docker build
context:

```
customerms/src/main/resources/openapi/customer_openapi.yaml
accountms/account-service/src/main/resources/openapi/account_openapi.yaml
```

`openapi/` holds the delivery copies. After editing a contract, refresh them from
the module before delivering:

```powershell
Copy-Item customerms\src\main\resources\openapi\customer_openapi.yaml openapi\
Copy-Item accountms\account-service\src\main\resources\openapi\account_openapi.yaml openapi\
```

To check a delivery copy still matches what its module compiles (no output means
they are identical):

```powershell
Compare-Object (Get-Content customerms\src\main\resources\openapi\customer_openapi.yaml) (Get-Content openapi\customer_openapi.yaml)
```

To read a contract without starting anything, paste the YAML into
https://editor.swagger.io.

### Endpoints

**ms-customers** — `http://localhost:8081/api/v1`

| Method | Path |
|---|---|
| `GET` / `POST` | `/customers` |
| `GET` / `PUT` / `PATCH` / `DELETE` | `/customers/{customerId}` |

**ms-accounts** — `http://localhost:8082/api/v1`

| Method | Path |
|---|---|
| `GET` / `POST` | `/accounts` |
| `GET` / `PUT` / `PATCH` / `DELETE` | `/accounts/{accountNumber}` |
| `GET` / `POST` | `/movements` |
| `GET` / `PUT` / `PATCH` / `DELETE` | `/movements/{movementId}` |
| `GET` | `/reports/{client-id}` |

### Endpoint test collection

[postman/bank-microservices.postman_collection.json](postman/bank-microservices.postman_collection.json)
— 35 requests with 71 assertions, Postman v2.1 format. Imported with
*Import → File*.

**Order matters.** The folders are numbered and chain through collection
variables: `01 · Customers` stores `{{customerId}}`, `02 · Accounts` stores
`{{accountNumber}}` and `03 · Movements` stores `{{movementId}}`. Running it top
to bottom (Runner or Newman) walks the happy path, every error code of the
contract and rules F2/F3, then leaves the database as it was found.

| Folder | What it covers |
|---|---|
| `00 · Health` | Actuator of both services |
| `01 · Customers` | CRUD + 409 duplicate identification, 400 size out of range, 404 |
| `02 · Accounts` | CRUD + 404 unknown customer (the inter-service call) |
| `03 · Movements` | F2 (a debit subtracts, a credit adds, value > 0) and **F3: 422 "Saldo no disponible"** |
| `04 · Reports` | Account statement + 404 and 400 on an inverted range |
| `05 · Teardown` | 409 deleting an account holding a balance, debit down to zero, cascading delete |

The `customersUrl` and `accountsUrl` variables point by default at
`localhost:8081` and `localhost:8082`, the ports published by docker-compose.

Since every request carries its assertions, it also runs headless:

```bash
newman run postman/bank-microservices.postman_collection.json
```

---

## Repository layout

```
.
├── accountms/account-service/   # accounts and movements microservice (8082)
├── customerms/                  # customers microservice (8081)
├── docker/init-schemas.sql      # creates customers_ms and accounts_ms on database startup
├── schemas/
│   ├── BaseDatos.sql            # deliverable script; basis of the integration tests
│   └── init-schemas.sql
├── docs/                        # requirements and design diagrams
├── openapi/                     # deliverable: both OpenAPI contracts
├── postman/                     # deliverable: endpoint test collection
├── docker-compose.yml
└── .env.example                 # template for the .env Compose reads (.env is git-ignored)
```

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `Port 3307 is already allocated` | Another process holds the port. Change the mapping of the `db` service in `docker-compose.yml`. |
| `ms-accounts` fails to create accounts | `ms-customers` is not up, or `CUSTOMERS_SERVICE_URL` points elsewhere. Check its `/actuator/health`. |
| `Schema-validation: missing table [...]` | The schema does not match the entities. Locally, check that Flyway applied the migrations; in the tests, that `schemas/BaseDatos.sql` is up to date. |
| `mvnw verify` fails to start the container | Docker is not running, or the `mariadb:11.4` image still has to be pulled. |
| Code changes do not show up in Docker | Compose reuses the image it already built. Relaunch with `docker compose up --build`. |
