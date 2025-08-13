# Personal Budgeting App - Backend API

A Spring Boot REST API backend for a personal budgeting application, designed for easy frontend integration.

## 🚀 Quick Start for Frontend Engineers

### Base URL

```
http://localhost:8080/api
```

### Authentication

This API uses JWT Bearer token authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

## 📋 API Overview

### Available Endpoints

#### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - Authenticate user

#### User Management _(Authentication required)_
- `GET /api/users/me` - Get current user profile
- `GET /api/users/{id}` - Get user by ID  
- `GET /api/users` - Get all users
- `DELETE /api/users/{id}` - Delete user

### 📚 Complete API Documentation

**All detailed endpoint documentation, schemas, and interactive testing available at:**

```
http://localhost:8080/swagger-ui.html
```

## 🔐 Authentication Flow

### Quick Start
1. **Register:** `POST /api/auth/register` with user details
2. **Login:** `POST /api/auth/login` with email/password  
3. **Store token:** Save JWT token from response securely
4. **Use token:** Include `Authorization: Bearer <token>` header in requests

### Token Details
- JWT tokens expire after 24 hours
- 401 Unauthorized responses indicate expired/invalid tokens

## 🛠 Frontend Integration

### Quick Integration Example

```javascript
// Basic authentication flow
const response = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'user@example.com', password: 'password' })
});

const { token } = await response.json();

// Use token for authenticated requests
fetch('http://localhost:8080/api/users/me', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

**For complete integration examples and code snippets, see the Swagger UI documentation.**

## 🐛 Error Handling

### HTTP Status Codes
- **400** - Bad Request (validation errors, duplicates)
- **401** - Unauthorized (authentication required/invalid)
- **404** - Not Found (resource doesn't exist)  
- **500** - Internal Server Error

### Error Response Format
```json
{
  "error": "Human-readable error message",
  "details": {
    "field": ["Field-specific validation errors"]
  }
}
```

**See detailed error examples in Swagger UI documentation.**

## 🏗 Data Models

### User Object

```typescript
interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  householdId?: number;
  createdAt?: string; // ISO 8601 datetime
}
```

### Auth Response

```typescript
interface AuthResponse {
  token: string;
  user: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    householdId: number;
    createdAt: string;
  };
}
```

### Current User Response

```typescript
interface CurrentUser {
  userId: number;
  householdId: number;
  email: string;
}
```

## 🚀 Development Setup

### Prerequisites

- Java 17+
- Docker (for database)

### Quick Start (Recommended: Native Development)

This setup provides the fastest development experience with instant code reloads.

1. **Start database only:**

   ```bash
   docker-compose -f docker-compose.dev.yml up -d
   ```

2. **Run the application natively:**

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **API will be available at:**

   ```
   http://localhost:8080/api
   ```

4. **Database admin interface:**

   ```
   http://localhost:8081 (Adminer)
   Username: user
   Password: password
   Database: mydatabase
   ```

5. **Stop database when done:**
   ```bash
   docker-compose -f docker-compose.dev.yml down
   ```

### Environment Configuration

For custom configurations, copy `.env.example` to `.env` and modify as needed:

```bash
cp .env.example .env
# Edit .env with your preferred settings
```

### Running Tests

```bash
./mvnw test
```

## 📚 API Documentation & Resources

### Interactive API Documentation (Swagger UI)

When the application is running locally, you can access interactive API documentation at:

```
http://localhost:8080/swagger-ui.html
```

**Features:**
- **Interactive Testing:** Test all endpoints directly from the browser
- **Authentication Support:** Use the "Authorize" button to add your JWT token
- **Request/Response Examples:** See actual request/response formats
- **Schema Definitions:** View all data models and their properties

### OpenAPI Specification

Raw OpenAPI 3.0 specification (JSON format) available at:

```
http://localhost:8080/v3/api-docs
```

### How to Use Swagger UI for Testing

1. **Start the application** (database + app)
2. **Open Swagger UI:** http://localhost:8080/swagger-ui.html
3. **Register a test user** using the `/api/auth/register` endpoint
4. **Copy the JWT token** from the registration response
5. **Click "Authorize"** button (🔒 icon) at the top right
6. **Enter:** `Bearer <your-jwt-token>` in the authorization field
7. **Test protected endpoints** like `/api/users/me`

### Additional Resources

- **Database Schema:** Auto-generated from JPA entities
- **CORS:** Currently configured for development (adjust for production)

## 🔒 Security Considerations

- Passwords are hashed using BCrypt
- JWT tokens expire after 24 hours
- Always use HTTPS in production
- Store JWT tokens securely (avoid localStorage for sensitive applications)
- Implement proper CORS policies for production

## 🏛 Architecture Overview

This Spring Boot application follows clean architecture principles:

- **Domain Layer:** Business logic and entities
- **API Layer:** REST controllers
- **Infrastructure Layer:** Database access and security
- **Shared Layer:** Exception handling and utilities

The application uses:

- **PostgreSQL** for data persistence
- **Spring Security** with JWT authentication
- **JPA/Hibernate** for ORM
- **Testcontainers** for integration testing
