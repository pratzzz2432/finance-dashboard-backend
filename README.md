# Finance Dashboard Backend

Backend for a finance dashboard where users with different roles (Viewer, Analyst, Admin) interact with financial records. Built with Spring Boot 3, JWT auth, and role-based access control.

---

## Table of Contents

1.  [What This Does](#what-this-does)
2.  [Tech Stack](#tech-stack)
3.  [Project Structure](#project-structure)
4.  [Architecture](#architecture)
5.  [Request Lifecycle](#request-lifecycle)
6.  [Authentication Flow](#authentication-flow)
7.  [Role-Based Access Control](#role-based-access-control)
8.  [Database Schema](#database-schema)
9.  [Getting Started](#getting-started)
10. [How to Verify Everything Works](#how-to-verify-everything-works)
11. [Verification Results](#verification-results)
12. [API Reference](#api-reference)
13. [Filtering, Pagination & Sorting](#filtering-pagination--sorting)
14. [Dashboard Analytics](#dashboard-analytics)
15. [Validation & Error Handling](#validation--error-handling)
16. [Running Tests](#running-tests)
17. [Swagger UI & H2 Console](#swagger-ui--h2-console)
18. [Design Decisions](#design-decisions)
19. [What I Would Improve Next](#what-i-would-improve-next)
20. [Assumptions & Notes](#assumptions--notes)

---

## What This Does

- JWT-based authentication (register, login, token-based access)
- Financial records CRUD with filtering, pagination, and soft deletes
- Dashboard analytics — totals, category breakdowns, monthly trends
- Role-based access control enforced at both URL and method level
- Input validation with consistent error responses
- Swagger UI for interactive API documentation
- Automated tests (unit + integration, 9 tests passing)

---

## Tech Stack

| Component     | Choice                            | Why                                                   |
|---------------|-----------------------------------|-------------------------------------------------------|
| Language      | Java 17                           | LTS, Spring Boot 3 compatible                         |
| Framework     | Spring Boot 3.2.5                 | Handles DI, security, data access out of the box      |
| Security      | Spring Security 6 + JWT (jjwt)    | Stateless auth, no session management needed          |
| Database      | H2 (in-memory)                    | Zero setup, schema auto-generated from entities       |
| ORM           | Spring Data JPA + Hibernate       | Repository pattern, custom JPQL for aggregation       |
| Build         | Maven 3.9                         | Standard dependency management                        |
| API Docs      | SpringDoc OpenAPI 2.5             | Auto-generated Swagger UI from annotations            |
| Testing       | JUnit 5 + Mockito + MockMvc       | Unit and integration tests                            |

---

## Project Structure

```
finance-dashboard/
|
+-- pom.xml                                  # dependencies and build config
+-- README.md
+-- src/
    +-- main/
    |   +-- java/com/zorvyn/finance/
    |   |   +-- FinanceDashboardApplication.java       # entry point
    |   |   |
    |   |   +-- config/
    |   |   |   +-- SecurityConfig.java                # URL-to-role mappings, JWT filter setup
    |   |   |   +-- OpenApiConfig.java                 # Swagger with JWT auth header
    |   |   |   +-- DataSeeder.java                    # seeds roles, users, sample records on startup
    |   |   |
    |   |   +-- security/
    |   |   |   +-- JwtTokenProvider.java              # create, parse, validate JWT tokens
    |   |   |   +-- JwtAuthenticationFilter.java       # servlet filter — checks JWT on every request
    |   |   |   +-- CustomUserDetailsService.java      # loads user from DB for Spring Security
    |   |   |
    |   |   +-- domain/
    |   |   |   +-- entity/
    |   |   |   |   +-- User.java                      # username, email, password, role, status
    |   |   |   |   +-- Role.java                      # VIEWER / ANALYST / ADMIN
    |   |   |   |   +-- FinancialRecord.java           # amount, type, category, date, soft-delete flag
    |   |   |   +-- enums/
    |   |   |       +-- RoleType.java, RecordType.java, UserStatus.java
    |   |   |
    |   |   +-- repository/
    |   |   |   +-- UserRepository.java
    |   |   |   +-- RoleRepository.java
    |   |   |   +-- FinancialRecordRepository.java     # custom JPQL for filtering + dashboard aggregation
    |   |   |
    |   |   +-- dto/
    |   |   |   +-- request/                           # LoginRequest, RegisterRequest, Create/UpdateRecordRequest, UpdateUserRequest
    |   |   |   +-- response/                          # AuthResponse, UserResponse, RecordResponse, DashboardSummary, etc.
    |   |   |
    |   |   +-- service/
    |   |   |   +-- AuthService.java                   # login + registration logic
    |   |   |   +-- UserService.java                   # user CRUD, role changes, deactivation
    |   |   |   +-- FinancialRecordService.java        # record CRUD with filtering and soft delete
    |   |   |   +-- DashboardService.java              # aggregation — totals, breakdowns, trends
    |   |   |
    |   |   +-- controller/
    |   |   |   +-- AuthController.java                # POST /api/auth/login, /register
    |   |   |   +-- UserController.java                # /api/users (admin only)
    |   |   |   +-- FinancialRecordController.java     # /api/records
    |   |   |   +-- DashboardController.java           # /api/dashboard/summary
    |   |   |
    |   |   +-- exception/
    |   |       +-- GlobalExceptionHandler.java        # catches all errors, returns consistent ApiError
    |   |       +-- ResourceNotFoundException.java     # -> 404
    |   |       +-- BadRequestException.java           # -> 400
    |   |       +-- UnauthorizedException.java         # -> 401
    |   |
    |   +-- resources/
    |       +-- application.properties                 # DB, JWT, server, logging config
    |
    +-- test/java/com/zorvyn/finance/
        +-- FinanceDashboardApplicationTests.java      # context loads
        +-- service/
        |   +-- FinancialRecordServiceTest.java        # unit tests for CRUD + soft delete
        |   +-- DashboardServiceTest.java              # unit tests for balance calculations
        +-- controller/
            +-- AuthControllerTest.java                # integration tests for login endpoint
```

**Why this structure?** Each package has one job. Controllers handle HTTP, services handle business logic, repositories handle data. DTOs sit at the boundary to keep entities out of API responses. If I need to swap H2 for PostgreSQL, I change `application.properties` and nothing else. If I need to change how auth works, I touch `security/` without affecting business logic.

---

## Architecture

How requests flow through the system:

```
+------------------------------------------------------------------+
|                     CLIENT (curl / Postman / Frontend)            |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                     SECURITY FILTER CHAIN                        |
|                                                                  |
|  JwtAuthenticationFilter:                                        |
|    1. Extract JWT from Authorization header                      |
|    2. Validate token signature + expiry                          |
|    3. Load user from DB, set SecurityContext                     |
|    4. SecurityConfig checks URL-level role permissions           |
|                                                                  |
|  Public endpoints (/api/auth/**, /swagger-ui/**) skip this.     |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                     CONTROLLER LAYER                             |
|                                                                  |
|  - Validates input (@Valid)                                      |
|  - Checks method-level permissions (@PreAuthorize)               |
|  - Delegates to Service, returns response with status code       |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                      SERVICE LAYER                               |
|                                                                  |
|  - Business logic and rules                                      |
|  - Converts between DTOs and entities                            |
|  - Handles transactions                                          |
|  - Throws business exceptions                                    |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                    REPOSITORY LAYER                              |
|                                                                  |
|  - Spring Data JPA interfaces                                    |
|  - Simple queries from method names                              |
|  - Complex aggregation via JPQL (@Query)                         |
+------------------------------------------------------------------+
                                |
                                v
+------------------------------------------------------------------+
|                     DATABASE (H2 In-Memory)                      |
|  Tables: roles, users, financial_records                         |
+------------------------------------------------------------------+

Error flow: exceptions bubble up to GlobalExceptionHandler -> consistent ApiError JSON
```

---

## Request Lifecycle

Concrete example — what happens when someone calls `GET /api/records?type=EXPENSE&page=0&size=5`:

```
Client sends:
  GET /api/records?type=EXPENSE&page=0&size=5
  Authorization: Bearer eyJhbG...

     |
     v
[JwtAuthenticationFilter]
  -> Extracts token from header
  -> Validates signature and expiry
  -> Extracts username "analyst" from claims
  -> Loads user from DB -> sets SecurityContext with ROLE_ANALYST

     |
     v
[SecurityConfig]
  -> GET /api/records/** needs VIEWER, ANALYST, or ADMIN
  -> ROLE_ANALYST matches -> PASS

     |
     v
[FinancialRecordController.getRecords()]
  -> @PreAuthorize check passes
  -> Parses type="EXPENSE" -> RecordType.EXPENSE
  -> Calls service with filters + pageable

     |
     v
[FinancialRecordService]
  -> Calls repository.findWithFilters(EXPENSE, null, null, null, pageable)

     |
     v
[FinancialRecordRepository]
  -> Runs JPQL: SELECT r FROM FinancialRecord r WHERE deleted=false AND type='EXPENSE'
  -> Returns Page<FinancialRecord>

     |
     v
[Back up the chain]
  -> Entities mapped to RecordResponse DTOs
  -> HTTP 200 with paginated JSON
```

---

## Authentication Flow

Three flows: register, login, then use the token.

```
REGISTRATION
============
  Client                              Server
    |  POST /api/auth/register           |
    |  { username, email, password,      |
    |    fullName, role: "ANALYST" }      |
    |------------------------------------>|
    |                                     |
    |                  1. Check username/email uniqueness
    |                  2. Parse role (default: VIEWER)
    |                  3. Hash password with BCrypt
    |                  4. Save user to DB
    |                                     |
    |  HTTP 201                           |
    |  { id, username, email, role }      |
    |<------------------------------------|


LOGIN
=====
  Client                              Server
    |  POST /api/auth/login              |
    |  { username, password }            |
    |------------------------------------>|
    |                                     |
    |                  1. AuthenticationManager verifies credentials
    |                     (loads user, BCrypt compares hash)
    |                  2. Generate JWT (subject=username, expiry=24h)
    |                  3. Return token + role info
    |                                     |
    |  HTTP 200                           |
    |  { accessToken: "eyJ...",           |
    |    tokenType: "Bearer",             |
    |    username, role }                 |
    |<------------------------------------|


USING THE TOKEN
===============
  Client                              Server
    |  GET /api/dashboard/summary        |
    |  Authorization: Bearer eyJ...      |
    |------------------------------------>|
    |                                     |
    |                  1. Filter extracts + validates JWT
    |                  2. Loads user, sets SecurityContext
    |                  3. SecurityConfig: /api/dashboard needs ANALYST+
    |                  4. User has ROLE_ANALYST -> OK
    |                  5. DashboardService computes summary
    |                                     |
    |  HTTP 200                           |
    |  { totalIncome, totalExpenses, ... }|
    |<------------------------------------|
```

---

## Role-Based Access Control

I chose three roles that map naturally to a finance dashboard:
- **VIEWER** — can browse records (read-only)
- **ANALYST** — can also access dashboard insights
- **ADMIN** — full access: create/edit/delete records, manage users

```
ACCESS MATRIX
=============
  Endpoint                       VIEWER   ANALYST   ADMIN
  ----------------------------  -------   -------   ------
  POST /api/auth/*                Yes       Yes      Yes
  GET  /api/records               Yes       Yes      Yes
  GET  /api/records/{id}          Yes       Yes      Yes
  GET  /api/records/categories    Yes       Yes      Yes
  POST /api/records               --        --       Yes
  PUT  /api/records/{id}          --        --       Yes
  DELETE /api/records/{id}        --        --       Yes
  GET  /api/dashboard/*           --        Yes      Yes
  GET/PUT/DELETE /api/users/*     --        --       Yes

  "--" = HTTP 403 Forbidden
```

Access control is enforced at **two layers** as defense-in-depth:

1. **URL level** — `SecurityConfig.java` checks role before any controller code runs
2. **Method level** — `@PreAuthorize` on each controller method as a safety net

```
What happens when a VIEWER tries to create a record:

  1. VIEWER logs in, gets JWT with ROLE_VIEWER
  2. Sends POST /api/records
  3. JwtAuthenticationFilter validates token -> OK
  4. SecurityConfig: POST /api/records needs ADMIN
  5. User has VIEWER -> REJECTED at step 4
  6. Returns 403 Forbidden (controller code never executes)
```

---

## Database Schema

Kept the schema simple — three tables with clear relationships. Used soft deletes for records because financial data shouldn't just disappear.

```
+-------------------+         +-------------------+         +------------------------+
|      roles        |         |      users        |         |   financial_records    |
+-------------------+         +-------------------+         +------------------------+
| id       BIGINT PK|<---+    | id       BIGINT PK|<---+   | id          BIGINT PK  |
| name     VARCHAR  |    |    | username VARCHAR  |    |   | amount      DECIMAL    |
|  (VIEWER/ANALYST/ |    +----| role_id  BIGINT FK|    |   | type        VARCHAR    |
|   ADMIN)          |         | email    VARCHAR  |    |   |  (INCOME/EXPENSE)      |
+-------------------+         | password VARCHAR  |    |   | category    VARCHAR    |
                              | full_name VARCHAR |    |   | date        DATE       |
                              | status   VARCHAR  |    +---| created_by  BIGINT FK  |
                              |  (ACTIVE/INACTIVE)|        | description VARCHAR    |
                              | created_at TIMESTAMP       | deleted     BOOLEAN    |
                              | updated_at TIMESTAMP       | created_at  TIMESTAMP  |
                              +-------------------+        | updated_at  TIMESTAMP  |
                                                           +------------------------+

Relationships:
  users.role_id -> roles.id          (many users per role)
  financial_records.created_by -> users.id  (many records per user)
```

Key choices:
- Passwords stored as BCrypt hashes
- `deleted` flag for soft delete (records hidden from queries but data preserved)
- Timestamps auto-set via JPA lifecycle callbacks
- `BigDecimal` for amounts (never use float/double for money)

---

## Getting Started

### Prerequisites

- Java 17+ (JDK, not just JRE)
- Maven 3.6+

```bash
java -version    # should show 17.x.x or higher
mvn --version    # should show 3.6+
```

### Build

```bash
cd finance-dashboard
mvn clean install          # full build + tests
mvn clean package -DskipTests  # just build, skip tests
```

### Run

```bash
# Option 1: via Maven
mvn spring-boot:run

# Option 2: via JAR
java -jar target/finance-dashboard-1.0.0.jar

# Option 3: custom port (if 8080 is taken)
java -jar target/finance-dashboard-1.0.0.jar --server.port=8085
```

### What Happens on Startup

```
1. H2 in-memory database created
2. Hibernate auto-generates tables from entity annotations
3. DataSeeder runs:
   - Creates 3 roles: VIEWER, ANALYST, ADMIN
   - Creates 3 users:
       admin / admin123 (ADMIN)
       analyst / analyst123 (ANALYST)
       viewer / viewer123 (VIEWER)
   - Seeds 15 sample records (Jan-Mar 2026, mix of income/expenses)
4. Tomcat starts on configured port
```

Quick sanity check:
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```
If you get an `accessToken` back, it's running.

---

## How to Verify Everything Works

Step-by-step walkthrough with `curl`. Replace port if needed.

### 1. Get a Token

```bash
# shortcut to save the token in a variable
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
```

### 2. Registration

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","email":"new@test.com","password":"pass123","fullName":"Test User","role":"ANALYST"}'
# -> 201 Created

# duplicate username should fail
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","email":"x@test.com","password":"pass123"}'
# -> 400 "Username 'admin' is already taken"
```

### 3. Financial Records CRUD

```bash
# list records
curl -s "http://localhost:8080/api/records?size=3" -H "Authorization: Bearer $TOKEN"

# create
curl -s -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":3500,"type":"INCOME","category":"Consulting","date":"2026-04-01","description":"Q1 payment"}'
# -> 201

# update (partial — only fields you send get changed)
curl -s -X PUT http://localhost:8080/api/records/1 \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":5500,"description":"Updated amount"}'
# -> 200

# soft delete
curl -s -X DELETE http://localhost:8080/api/records/1 -H "Authorization: Bearer $TOKEN" -w "\n%{http_code}"
# -> 204

# verify it's gone from queries
curl -s http://localhost:8080/api/records/1 -H "Authorization: Bearer $TOKEN"
# -> 404 (soft-deleted, hidden from all queries)
```

### 4. Filtering

```bash
# by type
curl -s "http://localhost:8080/api/records?type=EXPENSE" -H "Authorization: Bearer $TOKEN"

# by date range
curl -s "http://localhost:8080/api/records?startDate=2026-02-01&endDate=2026-02-28" -H "Authorization: Bearer $TOKEN"

# combined with sorting
curl -s "http://localhost:8080/api/records?type=INCOME&sort=amount,desc&size=5" -H "Authorization: Bearer $TOKEN"

# available categories
curl -s "http://localhost:8080/api/records/categories" -H "Authorization: Bearer $TOKEN"
```

### 5. Dashboard

```bash
# full summary
curl -s http://localhost:8080/api/dashboard/summary -H "Authorization: Bearer $TOKEN"

# specific date range
curl -s "http://localhost:8080/api/dashboard/summary/range?startDate=2026-02-01&endDate=2026-02-28" \
  -H "Authorization: Bearer $TOKEN"
```

### 6. User Management (Admin)

```bash
curl -s http://localhost:8080/api/users -H "Authorization: Bearer $TOKEN"
curl -s -X PUT http://localhost:8080/api/users/3 \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"role":"ANALYST"}'
curl -s -X DELETE http://localhost:8080/api/users/3 -H "Authorization: Bearer $TOKEN" -w "\n%{http_code}"
# -> 204 (deactivated, not hard-deleted)
```

### 7. Verify Access Control

```bash
VIEWER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"viewer","password":"viewer123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# viewer CAN read records
curl -s "http://localhost:8080/api/records?size=2" -H "Authorization: Bearer $VIEWER_TOKEN" -w "\n%{http_code}"
# -> 200

# viewer CANNOT create records
curl -s -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" -H "Authorization: Bearer $VIEWER_TOKEN" \
  -d '{"amount":100,"type":"EXPENSE","category":"Test","date":"2026-04-01"}' -w "\n%{http_code}"
# -> 403

# viewer CANNOT access dashboard
curl -s http://localhost:8080/api/dashboard/summary -H "Authorization: Bearer $VIEWER_TOKEN" -w "\n%{http_code}"
# -> 403
```

### 8. Validation Errors

```bash
# missing field
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"password":"admin123"}'
# -> 400 with fieldErrors

# invalid amount
curl -s -X POST http://localhost:8080/api/records \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":-50,"type":"EXPENSE","category":"Test","date":"2026-04-01"}'
# -> 400

# wrong password
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"wrong"}'
# -> 401
```

---

## Verification Results

I tested every endpoint and edge case manually. Here are the results (all 33 passing):

### Authentication & Registration

| #  | Test                              | Expected | Result |
|----|-----------------------------------|----------|--------|
| 1  | Admin login                       | 200 + token | 200 |
| 2  | Analyst login                     | 200 + token | 200 |
| 3  | Viewer login                      | 200 + token | 200 |
| 4  | Register new user                 | 201      | 201    |
| 5  | Register duplicate username       | 400      | 400    |

### Financial Records CRUD

| #  | Test                              | Expected | Result |
|----|-----------------------------------|----------|--------|
| 6  | GET records (paginated)           | 200      | 200    |
| 7  | GET records filtered by EXPENSE   | 200 + correct count | 200 |
| 8  | GET records by date range         | 200 + correct count | 200 |
| 9  | GET record by ID                  | 200      | 200    |
| 10 | GET categories                    | 7 categories | 7 categories |
| 11 | CREATE record (admin)             | 201      | 201    |
| 12 | UPDATE record (admin)             | 200      | 200    |
| 13 | DELETE record (soft delete)        | 204      | 204    |
| 14 | GET soft-deleted record           | 404      | 404    |

### Dashboard Analytics

| #  | Test                              | Expected | Result |
|----|-----------------------------------|----------|--------|
| 15 | Dashboard summary                 | totals + breakdowns + trends | All present |
| 16 | Dashboard date range              | 200      | 200    |

### User Management

| #  | Test                              | Expected | Result |
|----|-----------------------------------|----------|--------|
| 17 | GET all users                     | paginated list | 4 users returned |
| 18 | UPDATE user role                  | 200      | 200    |
| 19 | DEACTIVATE user                   | 204      | 204    |

### Role-Based Access Control

| #  | Test                              | Expected | Result |
|----|-----------------------------------|----------|--------|
| 20 | VIEWER GET records                | 200      | 200    |
| 21 | VIEWER POST records               | 403      | 403    |
| 22 | VIEWER GET dashboard              | 403      | 403    |
| 23 | VIEWER GET users                  | 403      | 403    |
| 24 | VIEWER DELETE records             | 403      | 403    |
| 25 | ANALYST GET records               | 200      | 200    |
| 26 | ANALYST GET dashboard             | 200      | 200    |
| 27 | ANALYST POST records              | 403      | 403    |
| 28 | ANALYST GET users                 | 403      | 403    |
| 29 | No token at all                   | 403      | 403    |

### Validation & Error Handling

| #  | Test                              | Expected | Result |
|----|-----------------------------------|----------|--------|
| 30 | Missing required field            | 400      | 400    |
| 31 | Negative amount                   | 400      | 400    |
| 32 | Invalid record type               | 400      | 400    |
| 33 | Wrong password                    | 401      | 401    |
| 34 | Non-existent record               | 404      | 404    |

**All 34 tests pass.** Every endpoint, every role, every validation rule, every error case — verified.

---

## API Reference

### Auth (public)

| Method | Endpoint             | Body                                                   | Response              |
|--------|----------------------|--------------------------------------------------------|-----------------------|
| POST   | `/api/auth/login`    | `{ username, password }`                               | `{ accessToken, role }` |
| POST   | `/api/auth/register` | `{ username, email, password, fullName?, role? }`      | `{ id, username, ... }` |

### Records

| Method | Endpoint                  | Who           | Body / Params                                                     |
|--------|---------------------------|---------------|-------------------------------------------------------------------|
| GET    | `/api/records`            | All roles     | Query: type, category, startDate, endDate, page, size, sort       |
| GET    | `/api/records/{id}`       | All roles     | --                                                                |
| GET    | `/api/records/categories` | All roles     | --                                                                |
| POST   | `/api/records`            | Admin         | `{ amount, type, category, date, description? }`                  |
| PUT    | `/api/records/{id}`       | Admin         | `{ amount?, type?, category?, date?, description? }`              |
| DELETE | `/api/records/{id}`       | Admin         | -- (soft delete)                                                  |

### Dashboard (Analyst + Admin)

| Method | Endpoint                         | Params                                |
|--------|----------------------------------|---------------------------------------|
| GET    | `/api/dashboard/summary`         | --                                    |
| GET    | `/api/dashboard/summary/range`   | `startDate`, `endDate` (required)     |

### Users (Admin only)

| Method | Endpoint          | Body                                          |
|--------|-------------------|-----------------------------------------------|
| GET    | `/api/users`      | --                                            |
| GET    | `/api/users/{id}` | --                                            |
| PUT    | `/api/users/{id}` | `{ fullName?, email?, role?, status? }`       |
| DELETE | `/api/users/{id}` | -- (deactivates user)                         |

---

## Filtering, Pagination & Sorting

`GET /api/records` supports:

| Param       | Example                   | What it does                              |
|-------------|---------------------------|-------------------------------------------|
| `type`      | `?type=EXPENSE`           | INCOME or EXPENSE                         |
| `category`  | `?category=Rent`          | exact category match                      |
| `startDate` | `?startDate=2026-01-01`   | records on or after (YYYY-MM-DD)          |
| `endDate`   | `?endDate=2026-03-31`     | records on or before                      |
| `page`      | `?page=0`                 | 0-based page number                       |
| `size`      | `?size=10`                | results per page (default 20)             |
| `sort`      | `?sort=amount,desc`       | field + direction                         |

All filters can be combined. The filter query uses a single dynamic JPQL query where null parameters are ignored — so you can pass any combination without needing separate endpoints.

Pagination response includes `totalElements`, `totalPages`, `first`, `last`, `numberOfElements`.

---

## Dashboard Analytics

`GET /api/dashboard/summary` returns:

```json
{
  "totalIncome": 19300.00,
  "totalExpenses": 5400.00,
  "netBalance": 13900.00,
  "incomeByCategory": [
    { "category": "Salary", "total": 15000.00 },
    { "category": "Freelance", "total": 2300.00 },
    { "category": "Investments", "total": 2000.00 }
  ],
  "expenseByCategory": [
    { "category": "Rent", "total": 3600.00 },
    { "category": "Utilities", "total": 950.00 },
    { "category": "Marketing", "total": 500.00 },
    { "category": "Office Supplies", "total": 350.00 }
  ],
  "recentActivity": [ /* last 10 records */ ],
  "monthlyTrends": [
    { "year": 2026, "month": 1, "income": 5800.00, "expense": 1700.00, "net": 4100.00 },
    { "year": 2026, "month": 2, "income": 7000.00, "expense": 1980.00, "net": 5020.00 },
    { "year": 2026, "month": 3, "income": 6500.00, "expense": 1720.00, "net": 4780.00 }
  ]
}
```

All aggregation is done at the database level via JPQL — I push SUM/GROUP BY to the DB instead of loading all records into Java. Much more efficient if the dataset grows.

---

## Validation & Error Handling

Request DTOs use Jakarta Bean Validation (`@NotBlank`, `@Email`, `@DecimalMin`, `@Size`). Invalid input returns structured errors:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-04-03T12:00:00",
  "fieldErrors": {
    "username": "Username is required",
    "amount": "Amount must be greater than zero"
  }
}
```

| Code | When                                        |
|------|---------------------------------------------|
| 200  | Successful GET/PUT                          |
| 201  | Successful POST (resource created)          |
| 204  | Successful DELETE                           |
| 400  | Validation error, bad input, duplicates     |
| 401  | Wrong credentials or expired token          |
| 403  | Valid token but wrong role                  |
| 404  | Resource not found or soft-deleted          |
| 500  | Unexpected server error                     |

All exceptions are caught by `GlobalExceptionHandler` so the response format is always consistent — no matter what goes wrong, the client gets the same `ApiError` shape.

---

## Running Tests

```bash
mvn test
```

| Test                            | Type        | What it checks                                     |
|---------------------------------|-------------|-----------------------------------------------------|
| `FinanceDashboardApplicationTests` | Integration | Spring context loads without errors              |
| `FinancialRecordServiceTest`    | Unit (4)    | Get by ID, not-found exception, create, soft delete |
| `DashboardServiceTest`          | Unit (2)    | Net balance math, zero-data edge case               |
| `AuthControllerTest`            | Integration (2) | Login returns token, missing fields -> 400      |

Expected: `Tests run: 9, Failures: 0, Errors: 0`

The unit tests mock the repository layer so they run fast without a database. The integration tests boot the full Spring context with an in-memory H2 and test real HTTP flows via MockMvc.

---

## Swagger UI & H2 Console

### Swagger (interactive API docs)
```
http://localhost:8080/swagger-ui.html
```
Click "Authorize", enter `Bearer <token>`, then use "Try it out" on any endpoint. This is probably the easiest way to explore the API without writing curl commands.

### H2 Console (inspect the database)
```
http://localhost:8080/h2-console
```
- JDBC URL: `jdbc:h2:mem:financedb`
- Username: `sa`
- Password: _(empty)_

Useful queries:
```sql
SELECT u.username, r.name as role, u.status FROM users u JOIN roles r ON u.role_id = r.id;
SELECT type, category, SUM(amount) FROM financial_records WHERE deleted=false GROUP BY type, category;
```

---

## Design Decisions

I tried to keep things practical — enough structure to be maintainable, not so much that it's over-engineered for the scope.

**H2 in-memory database** — Zero setup, great for dev and review. Schema auto-generated from JPA entities. Swapping to PostgreSQL later means changing only `application.properties` — no Java code changes needed.

**Soft deletes** — Financial records use a `deleted` boolean flag instead of actual deletion. Financial data shouldn't just vanish. All queries filter by `deleted = false` consistently at the repository level.

**JWT over sessions** — Stateless auth. No server-side session store, scales horizontally. Trade-off: can't revoke tokens before expiry. For this scope that's fine; in production I'd add a token blacklist or use short-lived tokens + refresh tokens.

**DB-level aggregation** — Dashboard queries use JPQL `SUM`/`GROUP BY` instead of loading all records into Java and computing totals. This is significantly more efficient as data grows — the database is built for this kind of work.

**Two-layer access control** — URL-level rules in `SecurityConfig` + method-level `@PreAuthorize` on each controller method. Intentionally redundant — defense in depth. If someone changes the security config, the annotations still protect endpoints.

**DTOs everywhere** — Entities never leak into API responses. Passwords, the `deleted` flag, and JPA-specific details stay internal. Request and response shapes can evolve independently of the database schema.

**Pagination by default** — All list endpoints return paginated results (default 20). This prevents accidentally returning thousands of records if the dataset grows.

**BCrypt for passwords** — Industry standard one-way hashing. Even if the database is compromised, passwords can't be reversed.

**Single dynamic filter query** — Instead of separate endpoints for each filter combination, `findWithFilters()` uses a single JPQL query where null params are ignored. Cleaner than writing N separate repository methods.

---

## What I Would Improve Next

If I had more time or this was headed to production, here's what I'd tackle:

1. **Refresh tokens** — Right now JWT tokens last 24 hours and can't be revoked. I'd add short-lived access tokens (15 min) with a refresh token flow to improve security.

2. **Externalize the JWT secret** — Currently hardcoded in `application.properties` for simplicity. Would move to environment variables or a secrets manager.

3. **Switch to PostgreSQL** — H2 is great for dev but not production. The switch is just a config change since I'm using JPA.

4. **Restrict ADMIN role self-assignment** — Right now anyone registering can request any role. In production, ADMIN/ANALYST assignment should require an existing admin's approval.

5. **Rate limiting** — No protection against brute-force login attempts currently. Would add Spring's rate limiter or put the API behind a gateway.

6. **Caching for dashboard** — The dashboard hits multiple aggregate queries on every call. For a large dataset, I'd cache the summary with a short TTL (e.g., Redis with 5-minute expiry).

7. **Audit logging** — Track who did what and when. The `createdBy` field is a start, but a proper audit log table would capture updates and deletes too.

8. **Search endpoint** — Currently filtering is exact-match only. A keyword search across descriptions and categories would be useful.

9. **Docker + docker-compose** — For easier deployment. One `docker-compose up` to start the app + database.

10. **More comprehensive tests** — Current coverage hits the main flows, but I'd add tests for edge cases like concurrent updates, token expiry behavior, and pagination boundaries.

---

## Assumptions & Notes

1. **Open registration** — anyone can register, defaults to VIEWER role. In production I'd restrict who can assign ADMIN/ANALYST.
2. **JWT secret hardcoded** — fine for dev, would use environment variables or a secrets manager in production.
3. **H2 resets on restart** — data lives in memory only. DataSeeder re-populates on each startup. Intentional for assessment/dev.
4. **Soft-deleted records excluded everywhere** — from queries, dashboard, categories. No "undelete" endpoint yet (data is still in DB though).
5. **Monthly trends default to 6 months** — the `/summary/range` endpoint allows custom date ranges.
6. **No rate limiting** — would add it via Spring's rate limiter or an API gateway in production.
7. **BigDecimal for money** — never use float/double for financial amounts due to precision issues.
8. **Single role per user** — kept it simple with a ManyToOne relationship. A ManyToMany setup would support users having multiple roles, but it's overkill for this scope.

---
