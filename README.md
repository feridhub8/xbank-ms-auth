# xbank-ms-auth

Registration and sign-in service for XBank. At this stage it is an empty Spring Boot service with a health endpoint; features arrive one change at a time.

## Requirements

- Java 25

## Build

```bash
./gradlew build
```

## Run

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

The service listens on port 9001. Health: http://localhost:9001/actuator/health

## Test

```bash
./gradlew test
```
