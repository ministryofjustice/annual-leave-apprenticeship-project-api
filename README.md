# Annual leave API (prototype for apprenticeship project)

### LOCAL API DOCS:
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](http://localhost:8080/swagger-ui/index.html)

A prototype REST API for managing employee annual leave requests, built with Kotlin and Spring Boot.

Employees can submit, view, and cancel leave requests. Managers can view requests assigned to them and approve or reject them. The API tracks leave balances, accounting for both pending and approved requests against each employee's annual entitlement.

## Authentication

This is a prototype application with simplified authentication. There is no JWT, OAuth, or session-based auth. Instead:

- **Login** (`POST /auth/login`) validates email and password against the database with plaintext comparison (no hashing).
- After login, the client receives the user's UUID and must pass it as an `X-User-Id` header on all subsequent requests.
- There is no token expiry, session management, or middleware-level authentication.

This approach is intentional for prototyping purposes.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/login` | Log in with email and password |
| GET | `/auth/me` | Get current user details |
| GET | `/requests` | Get all leave requests for the current user |
| POST | `/requests` | Submit a new leave request |
| DELETE | `/requests/{id}` | Delete a pending leave request |
| GET | `/requests/assigned` | Get all requests assigned to the user as approver |
| PATCH | `/requests/assigned/{id}` | Approve or reject an assigned leave request |
| GET | `/balance` | Get the current user's leave balance |

All endpoints (except `/auth/login`) require an `X-User-Id` header with a valid UUID.

API documentation is available at `/swagger-ui/index.html` when the app is running.

## Tech Stack

- Kotlin
- Spring Boot
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway (database migrations)
- Gradle

## Prerequisites

- Java 25+
- Docker (for PostgreSQL)

## Running Locally

### 1. Start PostgreSQL

```bash
docker compose up postgres
```

This starts a PostgreSQL instance on `localhost:5432` with user `root`, password `dev`.

### 2. Run the application

```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.

### Running in IntelliJ

Start PostgreSQL with Docker as above, then run the `AnnualLeaveApi.kt` main class with the `dev` Spring profile active.

## Useful Gradle Commands

| Command | Description |
|---------|-------------|
| `./gradlew bootRun` | Run the application |
| `./gradlew test` | Run all tests |
| `./gradlew check` | Run all tests, linting, and verification tasks |
| `./gradlew build` | Full build (compile, test, check, assemble jar) |
| `./gradlew clean build` | Clean and do a full build from scratch |
| `./gradlew clean assemble` | Build the jar without running tests |
| `./gradlew ktlintFormat` | Auto-format code to match Kotlin style rules |
| `./gradlew ktlintCheck` | Check code formatting without fixing |

## Running Everything in Docker

To run both the database and the application in Docker:

```bash
docker compose up
```

## Project Structure

```
src/main/kotlin/uk/gov/justice/digital/hmpps/annualleaveapi/
├── config/         # CORS, exception handling, OpenAPI config
├── controller/     # REST controllers
│   ├── request/    # Request DTOs
│   └── response/   # Response DTOs
├── model/          # JPA entities (User, LeaveRequest, Status)
├── repository/     # Spring Data JPA repositories
├── service/        # Business logic
└── AnnualLeaveApi.kt
```

## Database

The application uses Flyway for database migrations. Migration scripts are in `src/main/resources/db/migration/`.

- **dev/local:** PostgreSQL
- **Tests:** H2 in-memory database
