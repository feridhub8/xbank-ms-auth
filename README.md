# xbank-ms-auth

Registration and sign-in service for XBank. Users register through it; Keycloak holds them. Sign-in endpoints arrive one change at a time.

## Requirements

- Java 25 (local runs and tests)
- Docker (dev, prod, and the tests)
- `xbank-infrastructure` running, with Vault unseal and its auth setup done. The service does not start when Vault is sealed, unreachable, or denies access.

## Environments

| Profile | Runs with | Host port | Inside the container |
|---|---|---|---|
| `local` | IDE or `./gradlew bootRun` | 9001 | - |
| `dev` | `docker-compose.dev.yml` | 9101 | 8080 |
| `prod` | `docker-compose.yml` | 9201 | 8080 |

Health: `GET /actuator/health` on the host port, for example http://localhost:9001/actuator/health

## Configuration

At startup the service signs in to Vault with AppRole `ms-auth-<env>` and reads `secret/xbank/<env>/ms-auth` (key `keycloak-client-secret`). The AppRole credentials live in files that are never committed:

| File | Used by | AppRole |
|---|---|---|
| `.env` | `local` (the app reads it itself) | `ms-auth-local` |
| `.env.dev` | `docker-compose.dev.yml` | `ms-auth-dev` |
| `.env.prod` | `docker-compose.yml` | `ms-auth-prod` |

Create each from `.env.example` and fill in the values. Run these in `xbank-infrastructure`, logged in to Vault as `admin`, replacing `local` with `dev` or `prod`:

```bash
docker compose exec vault vault read -field=role_id auth/approle/role/ms-auth-local/role-id
docker compose exec vault vault write -f -field=secret_id auth/approle/role/ms-auth-local/secret-id
```

`VAULT_URL` overrides the Vault address (default `http://localhost:8200` for local, `http://vault:8200` in Docker). `KEYCLOAK_URL` overrides the Keycloak address (default `http://localhost:8180` for local, `http://keycloak:8080` in Docker). The realm is `xbankDev` for `local` and `dev`, `xbank` for `prod`.

## API

In `local` and `dev` the API is documented and can be tried in the browser:

- Swagger UI: `/swagger-ui.html`, for example http://localhost:9001/swagger-ui.html
- OpenAPI JSON: `/v3/api-docs`

Both are turned off in `prod` (they return `404`).

### `POST /api/v1/auth/register`

No token needed. Creates a user in Keycloak with role `USER`; the FIN becomes the username.

```json
{
  "fin": "AB12C3D",
  "email": "user@example.com",
  "password": "Passw0rd",
  "firstName": "Ali",
  "lastName": "Aliyev",
  "phoneNumber": "+994501234567"
}
```

| Field | Rule |
|---|---|
| `fin` | 7 uppercase letters or digits |
| `email` | valid address, unique |
| `password` | 8+ characters, an uppercase letter, a lowercase letter, a digit |
| `firstName`, `lastName` | required |
| `phoneNumber` | `+994` followed by 9 digits, unique |

`201 Created`:

```json
{
  "fin": "AB12C3D",
  "email": "user@example.com",
  "firstName": "Ali",
  "lastName": "Aliyev",
  "phoneNumber": "+994501234567",
  "roles": ["USER"]
}
```

Errors: `400 VALIDATION_FAILED`, `409 FIN_ALREADY_EXISTS`, `409 EMAIL_ALREADY_EXISTS`, `409 PHONE_ALREADY_EXISTS`, `503 UPSTREAM_UNAVAILABLE`.

## Errors

Every error response is a ProblemDetail (RFC 9457) with content type `application/problem+json`:

```json
{
  "type": "https://xbank.local/errors/validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/v1/...",
  "code": "VALIDATION_FAILED",
  "errors": [{ "field": "email", "message": "..." }]
}
```

Clients recognize errors by `code`, never by the message text. `errors` appears only for validation failures. Unexpected errors never include stack traces or internal messages; they are logged instead.

| Code | Status | When |
|---|---|---|
| `VALIDATION_FAILED` | 400 | The request body has invalid fields (listed in `errors`) |
| `MALFORMED_REQUEST` | 400 | The request body cannot be read, for example invalid JSON |
| `NOT_FOUND` | 404 | No resource at this path |
| `METHOD_NOT_ALLOWED` | 405 | The path does not support this HTTP method |
| `FIN_ALREADY_EXISTS` | 409 | A user with this FIN already exists |
| `EMAIL_ALREADY_EXISTS` | 409 | A user with this email already exists |
| `PHONE_ALREADY_EXISTS` | 409 | A user with this phone number already exists |
| `UPSTREAM_UNAVAILABLE` | 503 | A service the request depends on is not reachable |
| `INTERNAL_ERROR` | 500 | Anything unexpected |

Features add their own codes to `common/exception/v1/ErrorCode.java`.

## Build

```bash
./gradlew build
```

## Run

Local:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Dev and prod run in Docker and need the shared network `xbank_network` (create it once per machine with `docker network create xbank_network`):

```bash
docker compose -f docker-compose.dev.yml up -d --build
docker compose up -d --build
```

Stop them with the same command and `down` instead of `up -d --build`.

## Test

```bash
./gradlew test
```

Docker must be running. The tests start their own Vault and Keycloak containers, set up with `../xbank-infrastructure/vault/setup/init-auth.sh` and `../xbank-infrastructure/keycloak/realms/xbankDev.json` (override with `./gradlew test -PinfrastructureDir=<path>`). They never use the running infrastructure. `ContainerIT` builds the Docker image; the first run takes about a minute and a half.
