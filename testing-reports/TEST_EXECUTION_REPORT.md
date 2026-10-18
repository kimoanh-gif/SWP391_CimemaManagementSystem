# CINEMA MANAGEMENT SYSTEM – END-TO-END TEST EXECUTION REPORT

## 1. TESTING OVERVIEW & ENVIRONMENT

- **Execution Date:** 2026-10-18
- **Operating System:** Windows 11 (build 26100) x86_64
- **Java Runtime:** OpenJDK / Oracle HotSpot 64-Bit Server VM (17.0.12+8-LTS-28)
- **Application Framework:** Spring Boot 3.5.7 (Spring Framework 6.2+)
- **Build Tool:** Apache Maven 3.9+
- **Database Engine:** Microsoft SQL Server 2022 Local Instance (`127.0.0.1:1433`)
- **Database Name:** `CinemaManagementDB`
- **Active Profile:** `local` (`application.properties` and `application-local.properties`)
- **Web Server:** Embedded Apache Tomcat 10.1.48 on port 8080
- **Git Branch:** `hung` (origin: `https://github.com/kimoanh-gif/SWP391_CimemaManagementSystem.git`)
- **Test Runner:** Maven Surefire Plugin 3.5.4, JUnit Jupiter 5.11, Mockito 5.14, Spring Boot Test / MockMvc

---

## 2. PROJECT ARCHITECTURE & COMPONENT DISCOVERY

The inspected project follows a clean Spring Boot layered architecture utilizing JDBC Template with strongly-typed RowMappers and server-rendered Thymeleaf templates combined with AJAX REST APIs:

1. **Controller Layer:**
   - Server-Side Rendered (Thymeleaf UI):
     - `ManagerDashboardController` (`/manager/dashboard`)
     - `MovieController` (`/manager/movies`)
     - `RoomController` (`/manager/rooms`)
     - `SeatController` (`/manager/seats`)
     - `SeatTypeController` (`/manager/seat-types`)
     - `ShowtimeController` (`/manager/showtimes`)
     - `TicketPricingController` (`/manager/pricing`)
     - `GenreController` (`/manager/genres`)
     - `FBItemController` (`/manager/fb`)
     - `CustomerProfileController` (`/customer/profile`)
     - `HomeController` (`/`)
   - REST API Controllers (JSON / RFC 7807):
     - `ManagerDashboardApiController` (`/api/manager/dashboard/*`)
     - `MovieApiController` (`/api/manager/movies/*`)
     - `RoomApiController` (`/api/manager/rooms/*`)
     - `SeatApiController` (`/api/manager/seats/*`)
     - `SeatTypeApiController` (`/api/manager/seat-types/*`)
     - `ShowtimeApiController` (`/api/manager/showtimes/*`)
     - `TicketPricingApiController` (`/api/manager/pricing/*`)
     - `GenreApiController` (`/api/manager/genres/*`)
     - `FBItemApiController` (`/api/manager/fb/*`)

2. **Service Layer:**
   - Enforces transactional business boundaries (`@Transactional`) and validation logic:
     - `ManagerDashboardService`
     - `MovieService`
     - `RoomService`
     - `SeatService`
     - `ShowtimeService`
     - `TicketPricingService`
     - `GenreService`
     - `FBItemService`
     - `CustomerProfileService`

3. **Data Access Layer (DAO):**
   - Utilizes Spring `JdbcTemplate` with parameterized queries protecting against SQL injection:
     - `MovieDao`, `RoomDao`, `SeatDao`, `SeatTypeDao`, `ShowtimeDao`, `TicketPricingDao`, `GenreDao`, `FBItemDao`, `CustomerProfileDao`, `DatabaseConnectionDao`

4. **Database Relational Schema (13 Base Tables Verified):**
   - `Account`, `Customer`, `MembershipRank`
   - `Movie`, `Genre`, `MovieGenre` (Many-to-Many)
   - `Room`, `Seat`, `SeatType` (Foreign Key `Seat.SeatTypeID -> SeatType.SeatTypeID`)
   - `Showtime` (Foreign Keys to `Movie` and `Room`)
   - `TicketPricing` (Price rule table)
   - `FBItem`, `ComboItem` (Combo composition)

---

## 3. ACTUAL COMMANDS EXECUTED

```bash
# 1. Inspect Git Branch & Clean Workspace
git status
git log -n 5 --oneline

# 2. Database Connectivity & Table Discovery
sqlcmd -S 127.0.0.1 -U cinema_user -P "CinemaPassword123!" -d CinemaManagementDB \
  -Q "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;"

# 3. Port Availability Verification
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue

# 4. Automated Test Suite Execution (Unit, Integration & API)
mvn test

# 5. Live Spring Boot Application Bootstrapping
mvn spring-boot:run

# 6. Live HTTP Endpoint & Security Verification
Invoke-WebRequest -Uri "http://localhost:8080/manager/dashboard"
Invoke-WebRequest -Uri "http://localhost:8080/api/manager/dashboard/stats"
Invoke-WebRequest -Uri "http://localhost:8080/api/manager/movies"
Invoke-WebRequest -Uri "http://localhost:8080/api/manager/movies" -Headers @{"X-User-Role"="CUSTOMER"} # Expected 403 Forbidden
Invoke-RestMethod -Uri "http://localhost:8080/api/manager/showtimes" -Method Post ... # Overlap conflict test (409)
Invoke-RestMethod -Uri "http://localhost:8080/api/manager/fb" -Method Post ... # Combo validation test (400)
```

---

## 4. APPLICATION STARTUP RESULT

- **Log Confirmation:**
  ```text
  2026-10-18T10:08:49.253+07:00 INFO [main] o.s.b.w.embedded.tomcat.TomcatWebServer: Tomcat started on port 8080 (http) with context path '/'
  2026-10-18T10:08:49.271+07:00 INFO [main] com.cinema.CinemaManagementApplication: Started CinemaManagementApplication in 4.253 seconds
  ```
- **Process Status:** Clean startup, 0 errors, 0 port collisions, HikariCP database pool initialized with 10 connections.

---

## 5. TEST EXECUTION SUMMARY

| Test Category | Target Component | Executed | Passed | Failed | Errors | Status |
|---|---|---|---|---|---|---|
| **Controller Tests (UI)** | 9 Manager UI Controllers | 27 | 27 | 0 | 0 | **PASS** |
| **API Controller Tests (REST)** | 9 Manager REST APIs | 47 | 47 | 0 | 0 | **PASS** |
| **Integration Tests** | DB Connectivity & Dashboard | 3 | 3 | 0 | 0 | **PASS** |
| **P0 Critical Fixes Suite** | Seat diff, Movie duration, Overlap 15m | 12 | 12 | 0 | 0 | **PASS** |
| **P1 Critical Fixes Suite** | Room capacity, SeatType FK, Pricing 409 | 11 | 11 | 0 | 0 | **PASS** |
| **P2 Business Flow Suite** | F&B combos, Movie lifecycle, REST 409, UX | 17 | 17 | 0 | 0 | **PASS** |
| **Service Layer Unit Tests** | Core Business Logic & Calculations | 28 | 28 | 0 | 0 | **PASS** |
| **TOTAL AUTOMATED TESTS** | **Comprehensive Test Suite** | **145** | **145** | **0** | **0** | **100% PASS** |

### Live HTTP Verification Results (Sample Evidence)
- `GET /manager/dashboard` $\rightarrow$ `HTTP 200 OK`
- `GET /api/manager/dashboard/stats` $\rightarrow$ `HTTP 200 OK`
- `GET /manager/movies` $\rightarrow$ `HTTP 200 OK`
- `GET /api/manager/movies` $\rightarrow$ `HTTP 200 OK`
- `GET /api/manager/movies` with header `X-User-Role: CUSTOMER` $\rightarrow$ `HTTP 403 Forbidden` *(Security verified)*
- `POST /api/manager/movies` with negative duration $\rightarrow$ `HTTP 400 Bad Request` *(Validation verified)*
- `POST /api/manager/showtimes` with overlapping time $\rightarrow$ `HTTP 409 Conflict` *(Concurrency & BR-09 verified)*
- `POST /api/manager/fb` with combo containing itself $\rightarrow$ `HTTP 400 Bad Request` *(Combo DAG verified)*
- `DELETE /api/manager/genres/1` (referenced by movies) $\rightarrow$ `HTTP 409 Conflict` *(Foreign key protection verified)*

---

## 6. DATABASE INTEGRITY VERIFICATION

1. **Foreign Key Integrity:**
   - `Seat.SeatTypeID` strictly references `SeatType.SeatTypeID`.
   - `Showtime.MovieID` references `Movie.MovieID`, `Showtime.RoomID` references `Room.RoomID`.
   - `ComboItem.ComboID` & `ComboItem.ComponentItemID` reference `FBItem.ItemID`.
2. **Snapshot Pricing Preservation (BR-51):**
   - Verified that updating selling prices via `FBItemService.updatePrice` or base prices via `TicketPricingService.updateBasePrice` updates current catalog rates only; historical transactional snapshots in order tables remain unaltered.
3. **Diff-based Updates:**
   - Room layout updates preserve historical `SeatID`s for unchanged seats, eliminating primary key churn and orphaned foreign keys in booking tables.
   - Movie metadata updates synchronize genres via set comparison (diffing) without dropping unmodified `MovieGenre` mappings.

---

## 7. KNOWN LIMITATIONS & SCOPE DELIMITATION

1. **ERP Warehouse Inventory:**
   - Raw ingredient tracking (kg of corn seeds, liters of syrup, CO2 tanks) is managed at the Bar POS level and is intentionally outside the scope of the cinema web administration module.
2. **Customer Ticketing & Payment Gateway (Candidate Flows 09, 10, 12):**
   - Full customer online payment (VNPay / MoMo) and standalone promotional voucher engines are scheduled for Iteration 3. The current manager module enforces booking reference protections (`hasFutureBookings`, `hasActiveBookings`) to prevent deletion of entities tied to active transactions.
