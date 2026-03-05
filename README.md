# Electronics Store Application - For Agentic Digital Twins

A POC project that represents an online electronics store to prove out Agentic Driven Development ideas.

Main branch is the baseline code used for repeating the process.  The other branches are the results of running the agentic tools with different scenarios.

Using an agentic IDE or other agentic tools, the user will be able to provide the prompt in UserStoryExpanded.prompt.md and the agentic tools will generate the code and tests based on the prompt.  A user story and test feature file will be the inputs to the agentic tool.

## User Story Expanded Prompt
+---------------------+
|  Agentic Digital   |
|        Twin         |
+----------+----------+
           |
           | receive
           | user story,
           | test file,
           | and prompt
           |
           v
+----------+----------+
|   Generate        |
|   Solution        |
|   based on       |
|   inputs         |
+-------------------+

### Branches
user-story-add-user-registration branch- agentic produced implementation of a user registration user story

user-story-add-user-registration-with-tests branch- agentic produced implementation of a user registration user story with tests


A full-stack e-commerce application for an electronics store, featuring secure user authentication, product catalog management, shopping cart functionality, and administrative capabilities.

## Features

### User Features
- **User Registration and Authentication**
- **Product Management**
- **Shopping Cart**
- **User Profile**

### Admin Features
- **Product Management**
- **User Management**

## Tech Stack

### Backend
- Java Spring Boot
- Spring Security for authentication and authorization
- Spring Data JPA for database access
- H2 Database (for development)
- JUnit and Spring Test for integration testing

### Frontend
- React with TypeScript
- Redux for state management
- Material-UI for component library
- React Hook Form for form validation
- Yup for schema validation
- Axios for API requests

## Project Structure

```
electronics-store/
├── backend/                   # Spring Boot application
├── frontend/                  # React application
└── userstory/                 # User stories and documentation
```

## Getting Started

### Prerequisites
- Java 17+
- Node.js 16+
- Maven

### Backend Setup
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
The backend will run on http://localhost:8080

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```
The frontend will run on http://localhost:5173

## User Registration Feature

The user registration feature is fully implemented end-to-end. See `userstory/register-user/RegisterUserStoryOutput.txt` for the complete implementation summary.

### Registration Endpoint

**POST** `/api/auth/register`

```json
// Request
{ "username": "johndoe", "email": "john@example.com", "password": "Secure1@pass" }

// 200 OK Response
{ "token": "<jwt>", "user": { "id": 1, "username": "johndoe", "email": "john@example.com", "roles": ["ROLE_CUSTOMER"] } }

// 400 Error — duplicate username / email / weak password
{ "error": "Username already exists" }
{ "password": "Password must be at least 8 characters and contain uppercase, digit, and special character" }
```

**Password Rules**: minimum 8 characters, at least one uppercase letter, one digit, and one special character (`@$!%*?&`).

**First registered user** is automatically promoted to `ROLE_ADMIN`.

### Login Endpoint

**POST** `/api/auth/login`

```json
// Request
{ "username": "johndoe", "password": "Secure1@pass" }

// 200 OK — returns same AuthResponse shape as register
// 401 — { "error": "Invalid username or password" }
```

---

## Configuration Reference

| Key | Default | Description |
|-----|---------|-------------|
| `spring.datasource.url` | `jdbc:h2:mem:electronicsdb` | In-memory H2 database URL |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema auto-update on startup |
| `spring.h2.console.enabled` | `true` | H2 web console (dev only) |
| `spring.h2.console.path` | `/h2-console` | H2 console URL path |
| `server.port` | `8080` | Backend HTTP port |
| `jwt.secret` | *(base64-encoded 256-bit key)* | HMAC-SHA256 signing secret — **externalise in production** via `JWT_SECRET` env var |
| `jwt.expiration` | `86400000` | Token lifetime in milliseconds (24 hours) |
| `logging.level.com.store.electronics` | `INFO` | Application log level |

---

## Testing

### Backend Tests
```bash
cd backend
mvn test
```

Runs 21 tests across three suites:

| Suite | Count | Type |
|-------|-------|------|
| `AuthControllerIntegrationTest` | 9 | Integration (MockMvc + H2) |
| `JwtServiceTest` | 6 | Unit |
| `AuthServiceTest` | 6 | Unit (Mockito) |

Behavioral (Cucumber/Gherkin) tests are in:
- Feature file: `backend/src/test/resources/features/user_registration.feature`
- Runner: `UserRegistrationRunner` (included in `mvn test`)

### Frontend Tests
```bash
cd frontend
npm test
```

Runs 10 component tests for `Register.tsx` using Vitest + React Testing Library.

### Run with coverage
```bash
# Backend
mvn test -f backend/pom.xml

# Frontend
cd frontend && npm run test:coverage
```

> **Java 25 note**: The project uses Lombok 1.18.38 and a `mockito-extensions` subclass mock maker config to work correctly on Java 25. No action needed — this is already configured in `pom.xml` and `src/test/resources/mockito-extensions/`.

