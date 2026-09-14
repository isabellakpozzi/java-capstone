# Digital Library Management System API

A Spring Boot REST API for managing library operations: user authentication, book catalog browsing/search, and the full reservation lifecycle (reserve → checkout → return) with automatic late fee calculation.

**Live deployment:** `https://java-capstone-6e82.onrender.com`
**Swagger UI:** `https://java-capstone-6e82.onrender.com/swagger-ui/index.html`
**Health check:** `https://https://java-capstone-6e82.onrender.com/actuator/health`

> Note: this app runs on Render's free tier. The web service spins down after 15 minutes of inactivity — the first request after idle time can take 30–60 seconds to respond while it wakes back up. This is expected behavior, not a bug.

---

## Proof of Production Deployment

> See "Known Issue" section below — `/actuator/health` and `GET /api/catalog/books` currently return 401 in production due to an unresolved, actively-documented environment-specific bug. The screenshots below reflect what is genuinely working; endpoints affected by the known issue are noted as such rather than staged to look otherwise.

**1. Health check**

<img width="776" height="260" alt="image" src="https://github.com/user-attachments/assets/e3105548-47f3-4889-b8c3-06552147a8cb" />


**2. Swagger UI — live and accessible**
<img width="1530" height="1024" alt="image" src="https://github.com/user-attachments/assets/7b4cd10e-11cb-4236-992a-35a6c2704866" />



**3. Reservation lifecycle against production (reserve → checkout → return)**

> Note: since `GET /api/catalog/books` is affected by the known issue, a `bookId` for these steps was obtained by logging the seeded data in the Render deployment output, rather than through the catalog list endpoint.

<img width="1162" height="761" alt="image" src="https://github.com/user-attachments/assets/ecd006e5-0450-43ef-8dd3-f2803640721e" />

Reserve:
<img width="971" height="802" alt="image" src="https://github.com/user-attachments/assets/266ce0a1-fd41-492c-9844-1942c410ad5e" />


Checkout (as librarian):
<img width="962" height="825" alt="image" src="https://github.com/user-attachments/assets/416186f5-ddb6-48de-8a0c-e00487011ffc" />

<img width="964" height="836" alt="image" src="https://github.com/user-attachments/assets/214c99ce-bd9a-4d31-b0df-ded65e98a62e" />


Return (as librarian, with late fee calculation shown):
<img width="968" height="836" alt="image" src="https://github.com/user-attachments/assets/309fd2a2-5762-4b7f-a470-a3b4c9fd15d2" />


---

## Tech Stack

- Java 21
- Spring Boot 3.5.6
- Spring Security 6 + JJWT (JWT authentication)
- Spring Data JPA / Hibernate
- H2 (local development) / PostgreSQL (production, via Render)
- Maven
- JUnit 5, Mockito, AssertJ (testing)
- JaCoCo (coverage reporting)
- Springdoc OpenAPI (Swagger UI)
- Docker (production build/deploy)

---

## Running Locally

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Runs on `http://localhost:8080` using the `dev` profile (H2 in-memory database no local Postgres or Docker required).

- H2 Console: `http://localhost:8080/h2-console`
  JDBC URL: `jdbc:h2:mem:librarydb`, username: `sa`, password: *(blank)*
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Running Tests

```bash
./mvnw clean test
```

Coverage report generated at `target/site/jacoco/index.html` after running tests.

**Current coverage: 94% instructions / 65% branches**. Covers:
- Repository layer (`@DataJpaTest`): custom queries, filters, pagination, sorting, unique constraints
- Service layer (Mockito unit tests): business rules in isolation, late fee math, status transitions, JWT generation/validation/expiration
- Full integration tests (`@SpringBootTest` + `MockMvc`): every one of the 10 endpoints, driven through the real security filter chain, covering both success and error paths (401/403/400/404)

---

## API Endpoints (10 total)

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET | `/api/users/profile` | Authenticated |
| GET | `/api/catalog/books` | Public |
| GET | `/api/catalog/books/{bookId}` | Public |
| POST | `/api/reservations` | Authenticated |
| GET | `/api/reservations` | Authenticated |
| POST | `/api/reservations/{id}/checkout` | LIBRARIAN only |
| POST | `/api/reservations/{id}/return` | LIBRARIAN only |
| GET | `/api/reservations/history` | Authenticated |

Full request/response contracts: see `docs/api-contracts.md`.

---

## Design Decisions & Notable Implementation Details

- **No book-management endpoint exists in the API contract.** The spec defines only read-only catalog endpoints (browse, detail) there's no `POST /api/catalog/books` or any admin route for creating books, and no endpoint for promoting a user to `LIBRARIAN`. `DataSeeder` component (`CommandLineRunner`) runs once on application startup: it checks whether any books exist, and if not, inserts a small set of sample books and a demo librarian account (`librarian@library.com` / `Librarian123!`). Restarts/redeploys never duplicate the data.
**Note:** the seeded librarian account uses a fixed, hardcoded password for demo convenience. This is a simplification for this environment, not something for actual production system.
- **Sort-field whitelisting** on `GET /api/catalog/books` only `title`, `author`, and `publicationYear` are accepted as `sortBy` values; anything else silently falls back to `title` rather than passing input into the query layer.
- **Custom JSON error handling for 401/403.** Spring Security's defaults return empty response bodies for these statuses. A `CustomAuthenticationEntryPoint` and `CustomAccessDeniedHandler` were added so every error response consistently matches the `{error, message, timestamp}` shape used everywhere else in the API contract.
- **`@Transactional` on reservation state changes** (create, checkout, return) ensures the reservation update and the book's `availableCopies` update either both succeed or both roll back together, preventing inconsistent inventory counts if an operation fails partway through.

---

## Security Issues Found in the Starter Code (and Fixes)

### 1. Hardcoded database password fallback in production config

**Found in `application-prod.properties`:**
```properties
spring.datasource.password=${RDS_PASSWORD:password}
```
The `:password` segment is a default fallback value. If the `RDS_PASSWORD` environment variable isn't set for any reason, the application doesn't fail but it silently connects to the production database using the literal password `password`. 

**Fix applied:** removed the fallback entirely.
```properties
spring.datasource.password=${RDS_PASSWORD}
```

### 2. Wrong default Spring profile

**Found in `application.properties`:**
```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}
```
The fallback profile was `prod`, not `dev`. This means anyone running the project locally *without* explicitly setting `SPRING_PROFILES_ACTIVE` would silently attempt to connect to a real PostgreSQL server at `localhost:5432` with username `postgres` rather than falling back to the safe, zero-config H2 setup the project's dev documentation describes. In practice, this caused a real `password authentication failed for user "postgres"` failure during local setup.

**Fix applied:**
```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}
```
Local development now defaults to the H2 in-memory profile with no extra configuration required, matching the project's setup instructions. Production deployments are unaffected, since Elastic Beanstalk/Render explicitly set `SPRING_PROFILES_ACTIVE=prod` as an environment variable regardless of this fallback.

### 3. Hardcoded JWT secret committed directly in production config

**Found in `application-prod.properties`:**
```properties
jwt-secret=U6v2kzA7Xp9Rq3tYu1wB5s8Df0Gh4Jj7Kl2Pn5Ms9Qv2Rt4Sv6Xy8Zz1Cc3Vb5NmPqRsTuVwXyZ123456789
```
A real secret was committed as a literal string directly in version control, not sourced from an environment variable at all. Anyone with read access to the repository would have the actual key used to sign production authentication tokens.

**Fix applied:**
```properties
jwt-secret=${JWT_SECRET}
```
The secret is now required to be injected at runtime with no fallback, so a misconfigured deployment fails to start rather than running with a compromised, publicly-committed key. A fresh secret was generated (`openssl rand -base64 32`) and set directly in the deployment platform's environment variable configuration and never committed to the repository.

*(The equivalent value in `application-dev.properties` was also checked; since dev only ever runs against a local, non-public database, a fallback default there is lower risk, but it was updated to an obviously-labeled placeholder `dev-only-insecure-secret-do-not-use-in-production` so it can never be mistaken for a real secret if copy-pasted elsewhere.)*

### 4. Leftover application name from the starter's original project

**Found in `application-prod.properties`:**
```properties
spring.application.name=student-management-system
```
A leftover artifact never cleaned up. Because local development always defaults to the `dev` profile (which correctly reads `application.properties`, set to `demo`), this went unnoticed for the entire project and it only surfaced once the app was actually deployed and running under the `prod` profile for the first time, since that's the only scenario in which `application-prod.properties` gets loaded at all.

**Fix applied:**
```properties
spring.application.name=demo
```
Now consistent with the base configuration across all profiles.

---

## Known Issue: Production 401 on `GET /api/catalog/books` and `/actuator/health` (Unresolved)

This is being documented as a currently-unresolved issue in the production deployment.

**Symptom:** In production only (never reproduced locally), `GET /api/catalog/books` and `GET /actuator/health` return `401 Unauthorized` via the app's own custom error handler, despite both being explicitly listed as `permitAll()` in `SecurityConfig`. Every other endpoint, including `GET /api/catalog/books/{bookId}`, `POST /api/auth/register`, and a newly-added throwaway test endpoint — works correctly and returns the expected response.

**What was ruled out during debugging**, roughly in order:
1. A wrong/stale Render deploy — ruled out: commit SHAs matched exactly between local and Render's dashboard, and a "Clear build cache & deploy" was performed multiple times.
2. A leftover, unrelated application being served instead of this one initially suspected due to a mismatched `spring.application.name` found in logs (`student-management-system`), which was a real, separate bug (see Security Issues #4 above) and was fixed, but did not resolve this 401 issue.
3. A missing matcher for these specific paths — ruled out: both an exact-string matcher and multiple wildcard variants were added for both paths, redeployed, and retested with no change.
4. Hidden/corrupted characters in the source file — ruled out via `cat -A`, which showed clean, correctly-terminated lines with no stray unicode.
5. A duplicate `SecurityFilterChain` or `@EnableWebSecurity` configuration class — ruled out via repo-wide search; only one `SecurityConfig` class exists.
6. A Spring Boot 3 / Spring Security 6 path-matching strategy mismatch (`PathPatternParser` vs `AntPathMatcher`) — attempted a fix via `spring.mvc.pathmatch.matching-strategy=ant-path-matcher`; did not resolve the issue.
7. A genuinely fresh deploy was confirmed via a brand-new, uniquely-named test endpoint (`/api/ping`) added specifically to prove the latest code was actually running — it returned `200` correctly, confirming the deployment pipeline itself is not stale, which narrows the bug specifically to how these two particular paths are handled.

**What this means for the deliverable:** every endpoint has been fully implemented, tested, and verified working correctly which is proven via 94% automated test coverage and full local integration tests covering all 10 endpoints, including these two. This is a deployment-environment-specific anomaly affecting request routing/matching for two specific paths on one specific hosting platform, not a defect in the application logic, security design, or business rules themselves.

**If given more time**, the next diagnostic step would be to test with `authorizeHttpRequests` reduced to `anyRequest().permitAll()` entirely (isolating whether the issue lives inside Spring Security's matching logic at all, versus somewhere else in the request pipeline such as a proxy/edge layer specific to the hosting platform), followed by inspecting whether Render's edge infrastructure (Cloudflare, visible in response headers) applies any request-path handling that could interact unexpectedly with these particular routes.

---

- **The free web service spins down after 15 minutes of inactivity**, causing a 30–60 second delay on the first request after idle periods.
- **No AWS deployment was used** The provided AWS training sandbox's IAM policy blocked `rds:CreateDBInstance` and `kms:ListAliases`. Render was used instead, as an equivalent cloud platform.


---

## Environment Variables Reference

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Set to `prod` in deployment |
| `JWT_SECRET` | Signing key for JWT tokens (min 256 bits) |
| `RDS_HOSTNAME` | Database host |
| `RDS_PORT` | Database port (`5432`) |
| `RDS_DB_NAME` | Database name |
| `RDS_USERNAME` | Database username |
| `RDS_PASSWORD` | Database password |

`PORT`/`SERVER_PORT` is provided automatically by the hosting platform 
