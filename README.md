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

## 📋 API Endpoints

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe", 
  "email": "john.doe@example.com",
  "password": "securePassword123"
}
```

**Success Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "householdId": 1,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "error": "User with email john.doe@example.com already exists",
  "details": {
    "email": ["Email already exists"]
  }
}
```

#### Login User
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "securePassword123"
}
```

**Success Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe", 
    "email": "john.doe@example.com",
    "householdId": 1,
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

**Error Response (401 Unauthorized):**
```json
{
  "error": "Invalid credentials"
}
```

### User Management Endpoints
*All user endpoints require authentication*

#### Get Current User
```http
GET /api/users/me
Authorization: Bearer <jwt-token>
```

**Success Response (200 OK):**
```json
{
  "userId": 1,
  "householdId": 1,
  "email": "john.doe@example.com"
}
```

#### Get User by ID
```http
GET /api/users/{id}
Authorization: Bearer <jwt-token>
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com"
}
```

#### Get All Users
```http
GET /api/users
Authorization: Bearer <jwt-token>
```

**Success Response (200 OK):**
```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  },
  {
    "id": 2,
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@example.com"
  }
]
```

#### Delete User
```http
DELETE /api/users/{id}
Authorization: Bearer <jwt-token>
```

**Success Response (204 No Content):** *(Empty body)*

**Error Response (404 Not Found):** *(Empty body)*

## 🔐 Authentication Flow

### 1. User Registration
1. Send `POST /api/auth/register` with user details
2. Receive JWT token and user data
3. Store JWT token securely (localStorage/sessionStorage)
4. Include token in Authorization header for subsequent requests

### 2. User Login
1. Send `POST /api/auth/login` with email/password
2. Receive JWT token and user data
3. Store JWT token securely
4. Include token in Authorization header for subsequent requests

### 3. Authenticated Requests
Include the JWT token in every request to protected endpoints:
```javascript
const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('jwtToken')}`
};
```

### 4. Token Expiration
- JWT tokens expire after 24 hours
- When you receive a 401 Unauthorized response, redirect user to login

## 🛠 Frontend Integration Examples

### JavaScript/Fetch API
```javascript
// Register user
async function registerUser(userData) {
  const response = await fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(userData)
  });
  
  if (response.ok) {
    const data = await response.json();
    localStorage.setItem('jwtToken', data.token);
    return data;
  } else {
    const error = await response.json();
    throw new Error(error.error);
  }
}

// Login user
async function loginUser(credentials) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials)
  });
  
  if (response.ok) {
    const data = await response.json();
    localStorage.setItem('jwtToken', data.token);
    return data;
  } else {
    const error = await response.json();
    throw new Error(error.error);
  }
}

// Get current user (authenticated)
async function getCurrentUser() {
  const token = localStorage.getItem('jwtToken');
  const response = await fetch('http://localhost:8080/api/users/me', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (response.ok) {
    return await response.json();
  } else if (response.status === 401) {
    // Token expired, redirect to login
    localStorage.removeItem('jwtToken');
    window.location.href = '/login';
  } else {
    throw new Error('Failed to fetch user data');
  }
}
```

### React/Axios Example
```javascript
import axios from 'axios';

// Configure axios instance
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwtToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor to handle 401 errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('jwtToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// API functions
export const authAPI = {
  register: (userData) => api.post('/auth/register', userData),
  login: (credentials) => api.post('/auth/login', credentials),
};

export const userAPI = {
  getCurrentUser: () => api.get('/users/me'),
  getUserById: (id) => api.get(`/users/${id}`),
  getAllUsers: () => api.get('/users'),
  deleteUser: (id) => api.delete(`/users/${id}`),
};
```

## 🐛 Error Handling

### Common Error Responses

#### 400 Bad Request
```json
{
  "error": "User with email john.doe@example.com already exists",
  "details": {
    "email": ["Email already exists"]
  }
}
```

#### 401 Unauthorized  
```json
{
  "error": "Invalid credentials"
}
```

#### 404 Not Found
Empty response body with 404 status code.

#### 500 Internal Server Error
```json
{
  "error": "An unexpected error occurred"
}
```

### Error Handling Best Practices
1. Always check response status codes
2. Handle 401 errors by redirecting to login
3. Display user-friendly error messages from `error` field
4. Use `details` field for form validation errors

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

### Quick Start
1. **Start the database:**
   ```bash
   docker-compose up -d
   ```

2. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
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

### Running Tests
```bash
./mvnw test
```

## 📚 Additional Resources

- **OpenAPI Documentation:** Available at `http://localhost:8080/swagger-ui.html` when running locally
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