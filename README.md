# User Management Spring Boot Application

This is a domain-centric Spring Boot application that follows clean architecture principles, implementing a REST API backend for a personal budgeting app.

## Architecture

The application follows the domain-centric architecture pattern with clear separation of concerns:

- **Domain Layer**: Contains business logic, entities, and contracts
- **Application Layer**: REST endpoints and controllers
- **Infrastructure Layer**: Data access, security, and external integrations

## Features

- PostgreSQL database integration
- Comprehensive integration tests with Testcontainers
- Async data operations
- Input validation

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for database)

## Steps for implementing new endpoints

1. Create/modify `domain/model` to represent database table
2. Create a repository for the model in `data/context` (if not existent)
3. Specify DTOs needed for endpoints in `domain/dtos`
4. Create extensions for dto/entity conversion in `domain/extensions`
5. Add custom Exception to `shared/exceptions` if needed
6. Specify needed methods in `IDomainService` & `IDataService`
7. Implement specified methods in respective Service
8. Write integration tests for planned endpoints
9. Create endpoints in `api/endpoints`, including OpenAPI annotations

## Setup

1. **Start PostgreSQL database:**

   ```bash
   docker-compose up -d
   ```

## API Endpoints

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user
- `GET /api/users/{id}` - Get user by ID (authenticated)
- `GET /api/users` - Get all users (authenticated)
- `DELETE /api/users/{id}` - Delete user (authenticated)

## Testing

Run integration tests with Testcontainers:

```bash
mvn test
```

The tests automatically start a PostgreSQL container and test the complete application flow.

## Project Structure

```
src/
├── main/java/org/axelnyman/main/
│   ├── MainApplication.java
│   ├── application/endpoints/
│   ├── domain/
│   │   ├── abstracts/
│   │   ├── dtos/
│   │   ├── extensions/
│   │   ├── model/
│   │   └── services/
│   ├── infrastructure/
│   │   ├── data/
│   │   │   ├── context/
│   │   │   └── services/
│   │   └── security/
│   └── shared/exceptions/
└── test/java/org/axelnyman/main/
    ├── integration/
    └── unit/
```

## Design Patterns Used

- **Domain-Centric Architecture**: Business logic isolated from external concerns
- **Dependency Inversion**: Abstractions define contracts, implementations depend on abstractions
- **Repository Pattern**: Data access abstraction
- **Extension Methods**: Domain entity mapping

This architecture makes the application highly testable, maintainable, and allows easy swapping of external dependencies.
