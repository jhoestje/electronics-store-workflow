# Design Document: User Registration

## Overview
This feature implements end-to-end user registration for the Electronics Store application. The backend already exposes `POST /api/auth/register` but returns a placeholder `"dummy-token"`. This design replaces the placeholder with a real JWT implementation, adds proper error handling, enforces password strength rules, and adds a `Register.tsx` frontend page wired to a new `/register` route. The Navbar already contains a "Register" button navigating to `/register` — no Navbar changes are required.

---

## 1a. Codebase Analysis

### Backend — What Exists
| File | Status | Notes |
|------|--------|-------|
| `AuthController` | ✅ Exists | `/api/auth/register` and `/api/auth/login` endpoints |
| `AuthService` | ✅ Exists | `register()` and `login()` — returns `"dummy-token"` |
| `User` entity | ✅ Exists | `id`, `username`, `password` (@JsonIgnore), `email`, `roles` |
| `RegisterRequest` DTO | ✅ Exists | `@NotBlank`/`@Email` — no password strength validation |
| `AuthResponse` DTO | ✅ Exists | `token` + `user` |
| `SecurityConfig` | ✅ Exists | Stateless, BCrypt, permits all — needs JWT filter |
| `CustomUserDetailsService` | ✅ Exists | Loads user by username |
| `UserRepository` | ✅ Exists | `existsByUsername`, `existsByEmail`, `findByUsername` |
| `JwtService` | ❌ Missing | JWT generation/validation |
| `JwtAuthFilter` | ❌ Missing | Per-request JWT validation filter |
| `GlobalExceptionHandler` | ❌ Missing | Custom exception → HTTP response mapping |
| Custom exceptions | ❌ Missing | `UsernameAlreadyExistsException`, `EmailAlreadyExistsException` |

### Frontend — What Exists
| File | Status | Notes |
|------|--------|-------|
| `Navbar.tsx` | ✅ Exists | Already has "Register" button → `/register` (lines 91–93) |
| `Login.tsx` | ✅ Exists | Pattern to follow for Register page |
| `api.ts` → `authAPI.register()` | ✅ Exists | `POST /api/auth/register` call |
| `authSlice.ts` | ✅ Exists | `loginStart/Success/Failure/logout` — reusable for register |
| `App.tsx` | ✅ Exists | No `/register` route — needs adding |
| `Register.tsx` | ❌ Missing | Registration form page |

---

## 2. Package Structure

### New Backend Files
```
src/main/java/com/store/electronics/
├── exception/
│   ├── UsernameAlreadyExistsException.java     (new)
│   ├── EmailAlreadyExistsException.java        (new)
│   └── GlobalExceptionHandler.java             (new)
└── security/
    ├── JwtService.java                         (new)
    └── JwtAuthFilter.java                      (new)
```

### Modified Backend Files
```
src/main/java/com/store/electronics/
├── dto/RegisterRequest.java                    (add @Pattern password validation)
├── service/AuthService.java                    (use JwtService, throw custom exceptions)
└── security/SecurityConfig.java               (wire JwtAuthFilter)
src/main/resources/application.yml             (add jwt.secret, jwt.expiration)
```

### New Frontend Files
```
frontend/src/pages/
└── Register.tsx                               (new registration form page)
```

### Modified Frontend Files
```
frontend/src/
├── App.tsx                                    (add /register route)
└── store/slices/authSlice.ts                 (add registerSuccess action if distinct from loginSuccess)
```

---

## 3. Component Diagram

### Backend Components

```
AuthController
  └── POST /api/auth/register (@Valid RegisterRequest)
        └── AuthService.register()
              ├── UserRepository.existsByUsername() → UsernameAlreadyExistsException
              ├── UserRepository.existsByEmail()    → EmailAlreadyExistsException
              ├── PasswordEncoder.encode()
              ├── UserRepository.save()
              └── JwtService.generateToken()        → AuthResponse(token, user)

GlobalExceptionHandler (@ControllerAdvice)
  ├── UsernameAlreadyExistsException → 400 { "error": "Username already exists" }
  ├── EmailAlreadyExistsException    → 400 { "error": "Email already exists" }
  └── MethodArgumentNotValidException → 400 { field: message, ... }

JwtAuthFilter (OncePerRequestFilter)
  ├── Reads "Authorization: Bearer <token>" header
  ├── JwtService.extractUsername()
  ├── CustomUserDetailsService.loadUserByUsername()
  ├── JwtService.isTokenValid()
  └── Sets SecurityContextHolder authentication
```

### Frontend Components

```
Register.tsx
  ├── Formik form (username, email, password, confirmPassword)
  ├── Yup validation (required + email format + password strength)
  ├── authAPI.register() → POST /api/auth/register
  ├── dispatch(loginSuccess()) → stores token + user in Redux + localStorage
  └── navigate('/') on success

App.tsx
  └── <Route path="/register" element={<Register />} />
```

---

## 4. API Contracts

### POST /api/auth/register
**Request**:
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "Secure1@pass"
}
```

**Success Response** `200 OK`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "roles": ["ROLE_CUSTOMER"]
  }
}
```

**Error Responses** `400 Bad Request`:
```json
{ "error": "Username already exists" }
{ "error": "Email already exists" }
{ "username": "must not be blank", "email": "must be a well-formed email address", "password": "..." }
```

### POST /api/auth/login
No changes to the contract. Will now return real JWT tokens (was returning `"dummy-token"`).

---

## 5. Data Model

No schema changes required. Existing `users` and `user_roles` tables are sufficient.

```sql
-- Existing tables (no migration needed)
CREATE TABLE users (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role    VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 6. Security Flow

### Registration Flow
```
Client → POST /api/auth/register
       → JwtAuthFilter (no token → skips auth, endpoint is public)
       → AuthController.register()
       → AuthService.register() → saves user + BCrypt password
       → JwtService.generateToken(username) → signed JWT (HS256)
       → AuthResponse(token, user)
Client stores token in localStorage
```

### Subsequent Authenticated Requests
```
Client → GET /api/* + "Authorization: Bearer <jwt>"
       → JwtAuthFilter
           → JwtService.extractUsername(token)
           → CustomUserDetailsService.loadUserByUsername()
           → JwtService.isTokenValid(token, userDetails)
           → Sets UsernamePasswordAuthenticationToken in SecurityContext
       → Controller handles request
```

### JWT Configuration
- **Algorithm**: HMAC-SHA256 (HS256)
- **Secret**: Configurable via `application.yml` (base64-encoded, 256-bit minimum)
- **Expiration**: Configurable via `application.yml` (default: 86400000 ms = 24 hours)
- **Claims**: `sub` (username), `iat` (issued at), `exp` (expiration)

---

## 7. Error Handling Strategy

| Scenario | Exception | HTTP Status | Response Body |
|----------|-----------|-------------|---------------|
| Duplicate username | `UsernameAlreadyExistsException` | 400 | `{"error": "Username already exists"}` |
| Duplicate email | `EmailAlreadyExistsException` | 400 | `{"error": "Email already exists"}` |
| Blank/invalid fields | `MethodArgumentNotValidException` | 400 | `{field: message, ...}` |
| Invalid JWT | — | 403 (Spring Security default) | — |

**Password validation pattern** (backend `@Pattern`):
```
^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$
```
Message: `"Password must be at least 8 characters and contain uppercase, digit, and special character"`

---

## 8. Configuration (`application.yml` additions)

```yaml
jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration: 86400000
```

---

## 9. Frontend Integration

### Register.tsx
- **Pattern**: Mirrors `Login.tsx` — same `Container`, `Paper`, `TextField`, `Button` layout
- **Form fields**: Username, Email, Password, Confirm Password
- **Validation** (Yup):
  - `username`: required
  - `email`: required + valid email
  - `password`: required + min 8 + matches strength pattern
  - `confirmPassword`: required + must match `password`
- **On submit**: calls `authAPI.register()` → on success dispatches `loginSuccess` (reuse) → `navigate('/')`
- **Error display**: Shows API error message in a Material UI `Alert` component
- **Link**: "Already have an account? Login" link to `/login`

### App.tsx change
Add one route:
```tsx
<Route path="/register" element={<Register />} />
```

### authSlice.ts
The existing `loginStart`, `loginSuccess`, `loginFailure` actions are sufficient for registration flow — registration immediately logs in the user with the same token+user payload. No new actions needed.

---

## 10. Task Breakdown

| # | Task | Layer | Complexity | Dependency |
|---|------|-------|------------|------------|
| 1 | Add JWT config to `application.yml` | Config | S | None |
| 2 | Create `JwtService` (generate + validate tokens) | Security | S | Task 1 |
| 3 | Create `JwtAuthFilter` (OncePerRequestFilter) | Security | S | Task 2 |
| 4 | Update `SecurityConfig` to add `JwtAuthFilter` | Security | S | Task 3 |
| 5 | Create `UsernameAlreadyExistsException` + `EmailAlreadyExistsException` | Exception | S | None |
| 6 | Create `GlobalExceptionHandler` | Exception | S | Task 5 |
| 7 | Add `@Pattern` password validation to `RegisterRequest` | DTO | S | None |
| 8 | Update `AuthService` to use `JwtService` + custom exceptions | Service | S | Tasks 2, 5 |
| 9 | Create `Register.tsx` frontend page | Frontend | M | None |
| 10 | Add `/register` route to `App.tsx` | Frontend | S | Task 9 |

**Execution order**: 1→2→3→4 (JWT chain), 5→6 (exception chain), 7, 8 (depends on 2+5), 9→10 (frontend)

---

## 11. Risks and Open Questions

| Risk | Mitigation |
|------|-----------|
| JWT secret must be ≥256 bits for HS256 — short secret causes JJWT exception | Use pre-generated 64-char hex string in config |
| `JwtAuthFilter` runs on all requests — must skip `/api/auth/**` endpoints cleanly | Check for missing/null token and skip filter gracefully |
| `AuthService` admin-promotion logic: `userRepository.count() == 1` check runs AFTER save | This is a pre-existing logic quirk — preserve as-is, document in code |
| CORS: `SecurityConfig` allows `http://localhost:5173` — `Authorization` header is in `exposedHeaders` | Already configured correctly — no changes needed |

---

## Status
- **Phase 0**: ✅ Complete (Approved)
- **Phase 1**: ✅ Complete — awaiting user approval to proceed to Phase 2
- **Phase 0 Gate Decision**: Approved by user
- **Phase 1 Gate**: Pending user review
