# xbank-ms-auth

Registration and sign-in service for XBank. At this stage it is an empty Spring Boot service with a health endpoint; features arrive one change at a time.

## Requirements

- Java 25 (local runs and tests)
- Docker (dev, prod, and the container tests)

## Environments

| Profile | Runs with | Host port | Inside the container |
|---|---|---|---|
| `local` | IDE or `./gradlew bootRun` | 9001 | - |
| `dev` | `docker-compose.dev.yml` | 9101 | 8080 |
| `prod` | `docker-compose.yml` | 9201 | 8080 |

Health: `GET /actuator/health` on the host port, for example http://localhost:9001/actuator/health

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

`ContainerIT` builds the Docker image, so Docker must be running; the first run takes about a minute and a half.
