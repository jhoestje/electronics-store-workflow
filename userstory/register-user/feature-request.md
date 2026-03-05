# Feature Request: User Registration

## Metadata
- **Feature Name**: User Registration
- **Feature ID**: register-user
- **Created**: Phase 0 - Intake & Triage
- **Status**: ✅ COMPLETE — All Phases Done
- **Next Phase**: N/A — Feature Delivered

---

## Git Environment Strategy
- **Strategy**: Worktree-optimized workflow
- **Approach**: Virtual worktree structure within `userstory/` directory
- **Benefits**: Clean feature isolation, easier file management, parallel development support
- **Implementation**: Worktree-optimized file organization within `userstory/register-user/`

---

## Feature Description

### User Story (Provided by User)
> As an unregistered user
> I want to register on the website with my email, password and username by clicking on the register button
> So that I can be a registered user and be able to login and make purchases

### Source File
`userstory/RegisterUserStory.txt`

---

## Clarification Questions & Answers

### Q1: Functional Requirements
**Question**: What specific registration functionality should be implemented?

**Answer** *(Code Analysis)*: The backend already has:
- `POST /api/auth/register` endpoint in `AuthController`
- `RegisterRequest` DTO with `username`, `email`, `password` fields (basic `@NotBlank`/`@Email` validation)
- `AuthService.register()` that saves a `User` entity with `ROLE_CUSTOMER`, promotes first user to `ROLE_ADMIN`
- Returns an `AuthResponse` with `token` (currently a "dummy-token" placeholder) and `user` object

**Gaps**:
- JWT is not implemented — `AuthService` returns `"dummy-token"` instead of a real JWT
- No `Register.tsx` frontend page exists
- No `/register` route in `App.tsx`
- No Register link/button in `Navbar.tsx`
- No Redux actions for registration in `authSlice.ts`
- No password strength validation
- Error handling uses bare `RuntimeException` (produces generic 500 errors)

**Status**: ✅ Answered

---

### Q2: User Interface
**Question**: How should users interact with this feature?

**Answer** *(Code Analysis + Inferred from existing implementation)*:
- A "Register" button/link should appear in the `Navbar` alongside the existing "Login" button
- Clicking "Register" navigates to a `/register` route
- The registration form should have fields: Username, Email, Password (and optionally Confirm Password)
- On success, the user is automatically logged in and redirected to the home page
- On failure, an error message is displayed inline on the form
- The existing `Login.tsx` uses Formik + Yup + Material UI — the Register page should follow the same pattern

**Status**: ✅ Answered

---

### Q3: Business Rules
**Question**: What constraints or validation rules apply?

**Answer** *(Code Analysis)*:
- `username`: required, not blank
- `email`: required, valid email format
- `password`: required, not blank
- Username must be unique (enforced in `AuthService`)
- Email must be unique (enforced in `AuthService`)
- First registered user automatically gets `ROLE_ADMIN`; all others get `ROLE_CUSTOMER`

**Answer (User)**: Strong password rules required:
- Minimum 8 characters
- At least one uppercase letter
- At least one digit
- At least one special character

This must be enforced on both the backend (`@Pattern` constraint on `RegisterRequest.password`) and the frontend (Yup validation schema in `Register.tsx`).

**Status**: ✅ Answered

---

### Q4: Data Requirements
**Question**: What data needs to be stored, processed, or displayed?

**Answer** *(Code Analysis)*:
- **Stored**: `User` entity in `users` table — `id`, `username`, `password` (BCrypt hashed), `email`, roles in `user_roles` table
- **Processed**: Password encoded with `BCryptPasswordEncoder` before persistence
- **Returned**: `AuthResponse` with JWT `token` and `User` object (`id`, `username`, `email`, `roles`)
- The `User.password` field is annotated `@JsonIgnore` so it is never exposed in API responses

**Status**: ✅ Answered

---

### Q5: Integration Points
**Question**: Does this feature interact with existing systems or APIs?

**Answer** *(Code Analysis)*:
- **Backend**: Integrates with `UserRepository`, `PasswordEncoder`, and will need a real `JwtService` for token generation
- **Frontend**: `authAPI.register()` already exists in `api.ts`. `authSlice.ts` has login actions but needs registration-specific actions (`registerStart`, `registerSuccess`, `registerFailure`)
- **Navbar**: `Navbar.tsx` needs a "Register" button added

**Status**: ✅ Answered

---

### Q6: Performance Requirements
**Question**: Are there specific performance or scalability needs?

**Answer** *(Inferred from existing implementation)*: No special performance requirements beyond standard web app expectations. H2 in-memory database is for development only. BCrypt hashing adds ~100–200ms per registration which is acceptable.

**Status**: ✅ Answered

---

### Q7: Security Considerations
**Question**: Are there authentication, authorization, or data protection requirements?

**Answer** *(Code Analysis)*:
- Passwords must be BCrypt-hashed before storage — already configured via `BCryptPasswordEncoder`
- JWT token must be returned on successful registration so the user is immediately logged in
- JWT is currently a placeholder (`"dummy-token"`) — a real `JwtService` must be implemented
- `SecurityConfig` currently permits all requests (`/**`) — registration endpoint must remain public
- `SecurityConfig` is stateless (no sessions) — JWT must be used for all authenticated calls

**Status**: ✅ Answered

---

### Q8: Edge Cases
**Question**: What special scenarios or error conditions should be handled?

**Answer** *(Code Analysis)*:
- Duplicate username → currently throws `RuntimeException("Username already exists")` → must return `400 Bad Request` with meaningful message
- Duplicate email → currently throws `RuntimeException("Email already exists")` → must return `400 Bad Request` with meaningful message
- Blank/invalid fields → handled by Bean Validation (`@NotBlank`, `@Email`) → must return `400 Bad Request`
- Network error on frontend → must show user-friendly error message
- User already logged in navigating to `/register` → should redirect to home page

**Status**: ✅ Answered

---

### Q9: Success Criteria
**Question**: How will we know the feature is working correctly?

**Answer** *(Code Analysis + Inferred)*:
- A user can submit the registration form with valid data and receive a JWT token
- The user is automatically logged in and redirected to the home page after registration
- Attempting to register with a duplicate username or email returns a 400 error with a clear message
- Attempting to register with invalid/blank fields returns a 400 validation error
- The JWT token is stored in `localStorage` and used for subsequent authenticated API calls
- A "Register" button is visible in the Navbar for unauthenticated users

**Status**: ✅ Answered

---

### Q10: Dependencies
**Question**: Are there any prerequisites or external dependencies?

**Answer** *(Code Analysis)*:
- `io.jsonwebtoken:jjwt-api:0.11.5`, `jjwt-impl`, `jjwt-jackson` are already in `pom.xml` — JWT libraries are available but not configured
- A `jwt.secret` and `jwt.expiration` must be added to `application.yml`
- Frontend: `formik`, `yup`, `@mui/material` are all already installed (used by `Login.tsx`)
- No new dependencies needed for either backend or frontend

**Status**: ✅ Answered

---

## Classification
- **Type**: New Feature — requires full lifecycle (Phases 0–5)
- **Reason**: Registration page does not exist on the frontend; JWT is not implemented on the backend; error handling is incomplete

---

## Scope
- **API Layer**: `AuthController` (minor update), `JwtService` (new), `JwtAuthFilter` (new), `GlobalExceptionHandler` (new)
- **Service Layer**: `AuthService` (update to return real JWT), password validation
- **Data Layer**: No schema changes needed — `User` and `user_roles` tables exist
- **Security Layer**: `SecurityConfig` (add JWT filter chain)
- **Frontend**: `Register.tsx` (new), `App.tsx` (add route), `Navbar.tsx` (add Register button), `authSlice.ts` (add register actions)
- **Configuration**: `application.yml` (add JWT secret + expiration)

---

## Complexity Estimate
**M — Half Day**

Rationale:
- Backend JWT implementation is well-scoped (jjwt library already included)
- Frontend registration form follows established Login.tsx pattern
- No new database entities or schema migrations needed
- Error handling requires a new `GlobalExceptionHandler` class

---

## Business User Stories

### Epic User Story
> **As an electronics store**, we want unregistered visitors to be able to create accounts so they can log in and make purchases, growing our registered customer base.

### Feature User Stories

#### US-REG-01: Registration Form
> As an unregistered user, I want to see a "Register" button in the navigation bar so that I can easily find the registration page.

**Acceptance Criteria**:
- [x] A "Register" button/link is visible in the Navbar when no user is logged in
- [x] Clicking "Register" navigates to `/register`
- [x] The form has Username, Email, and Password fields
- [x] The form has a submit button labeled "Register"
- [x] The form has a link to the Login page for users who already have an account

#### US-REG-02: Successful Registration
> As an unregistered user, I want to submit the registration form and be automatically logged in so that I don't have to log in separately after registering.

**Acceptance Criteria**:
- [x] Submitting valid credentials creates a new user account
- [x] A real JWT token is returned and stored in `localStorage`
- [x] The user is automatically authenticated and redirected to the home page
- [x] The Navbar shows the logged-in state (user name / profile link)

#### US-REG-03: Registration Validation
> As an unregistered user, I want to see clear error messages when my input is invalid so that I know what to correct.

**Acceptance Criteria**:
- [x] Empty username shows "Username is required"
- [x] Empty or invalid email shows "Valid email is required"
- [x] Empty password shows "Password is required"
- [x] Weak password shows "Password must be 8+ characters with uppercase, digit, and special character"
- [x] Duplicate username shows "Username already exists"
- [x] Duplicate email shows "Email already exists"

### Technical User Stories

#### TS-REG-01: JWT Token Generation
> As a developer, I want the backend to generate real JWT tokens on registration and login so that authenticated API calls work correctly.

**Acceptance Criteria**:
- [x] `JwtService` generates signed JWT tokens with configurable secret and expiration
- [x] `JwtAuthFilter` validates tokens on incoming requests
- [x] `AuthService.register()` and `AuthService.login()` return real JWT tokens
- [x] JWT secret and expiration are configured in `application.yml`

#### TS-REG-02: Error Handling
> As a developer, I want a `GlobalExceptionHandler` that maps exceptions to proper HTTP responses so that clients receive meaningful error messages.

**Acceptance Criteria**:
- [x] Duplicate username → `400 Bad Request` with `{ "error": "Username already exists" }`
- [x] Duplicate email → `400 Bad Request` with `{ "error": "Email already exists" }`
- [x] Validation failures → `400 Bad Request` with field-level error messages

---

## Business Value
- **Customer Acquisition**: Enables new users to create accounts and make purchases
- **Security**: Real JWT tokens replace the insecure "dummy-token" placeholder, securing all authenticated endpoints
- **UX**: Consistent registration experience matching the existing login pattern

---

## Planning Context
- **System Stack**: Spring Boot 3.2.0, Java 17, JWT (jjwt 0.11.5), H2, React 18.2.0, TypeScript, Redux Toolkit, Material UI, Formik+Yup
- **Package**: `com.store.electronics`
- **Available Agents**: `/full-stack-engineer`, `/test-engineer`, `/documentation-writer`
- **Artifacts Location**: `userstory/register-user/`

---

## Feature Gate Status
- **Phase 0**: ✅ Complete — Intake & Triage
- **Phase 1**: ✅ Complete — Planning & Design
- **Phase 2**: ✅ Complete — Full Stack Development
- **Phase 3**: ✅ Complete — 31/31 tests passing
- **Phase 4**: ✅ Complete — 5 QA issues fixed
- **Phase 5**: ✅ Complete — Documentation delivered

**Overall Status: FEATURE COMPLETE ✅**
- **Password Strength Rules**: 8+ chars, uppercase, digit, special character — enforced on backend (`@Pattern`) and frontend (Yup)
- **Output Artifact**: `userstory/register-user/RegisterUserStoryOutput.txt`
