# xbank-ms-auth

Registration and sign-in service for XBank. At this stage it has a health endpoint and loads its Keycloak client secret from Vault at startup; features arrive one change at a time.

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

`VAULT_URL` overrides the Vault address (default `http://localhost:8200` for local, `http://vault:8200` in Docker).

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

Docker must be running. The tests start their own Vault container, set up with `../xbank-infrastructure/vault/setup/init-auth.sh` (override with `./gradlew test -PinfrastructureDir=<path>`). `ContainerIT` builds the Docker image; the first run takes about a minute and a half.
