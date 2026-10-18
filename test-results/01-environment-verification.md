# 01 — Test Environment Verification & Readiness Report

**Execution Timestamp:** 2026-10-14 15:24:00  
**Test Auditor:** Senior QA Automation Engineer / Test Architect  
**Project:** Cinema Management System (`SWP_RapPhim`)  

---

## 1. Source Control & Working Tree Status

| Item | Verified Value | Status |
|:---|:---|:---:|
| **Current Branch** | `hung` | PASS |
| **Tracking Remote** | `origin/hung` (Up to date) | PASS |
| **Working Tree** | Clean (untracked `test-audit/` and `test-results/`) | PASS |
| **Commit Hash** | `3095b05` (*feat: Hoan thien man hinh Tong quan Quan ly - Screen 53 (Manager Dashboard)*) | PASS |

---

## 2. Runtime Environment & Toolchain

| Component | Version | Verification Output / Notes | Status |
|:---|:---|:---|:---:|
| **Java Runtime (JDK)** | Java 17.0.12 LTS | `Java(TM) SE Runtime Environment (build 17.0.12+8-LTS-286)` | PASS |
| **Build Automation** | Apache Maven 3.9.16 | `Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)` | PASS |
| **Operating System** | Windows 11 (10.0, amd64) | Native execution environment | PASS |
| **Spring Boot Framework** | 3.5.7 | Web, Thymeleaf, Validation, JDBC | PASS |

---

## 3. Database Connectivity & Schema Inspection

| Check | Target / Command | Result | Status |
|:---|:---|:---|:---:|
| **Database Engine** | Microsoft SQL Server | `localhost:1433` | PASS |
| **TCP Port Reachability** | `Test-NetConnection -Port 1433` | `TcpTestSucceeded : True` | PASS |
| **Database Name** | `CinemaManagementDB` | Profile: `local` (Credentials `[REDACTED]`) | PASS |
| **Connection Pool** | HikariCP (`HikariDataSource`) | Connected (`HikariPool-1 - Start completed`) | PASS |
| **Ping Query** | `SELECT 1` via `DatabaseConnectionDao` | Returned `1` successfully | PASS |
| **Table Schema Discovery** | `INFORMATION_SCHEMA.TABLES` | Discovered: `[SeatType, TicketPricing, FBItem, ComboItem, Account, MembershipRank, Customer, Movie, Genre, MovieGenre, Showtime, Room, Seat]` | PASS |

---

## 4. Application Startup & Context Health

- **Spring Boot Context Initialization:** Verified via `DatabaseConnectivityIntegrationTest` using `@SpringBootTest` and `@ActiveProfiles("local")`.
- **Thymeleaf Template Resolver:** Verified template mappings: `index`, `customer/profile`, `customer/profile-edit`, `manager/dashboard`, `manager/movies`, `manager/genres`, `manager/rooms`, `manager/seat-types`, `manager/seats`, `manager/showtimes`, `manager/pricing`, `manager/fb`.
- **Startup Time:** ~4.38 seconds without errors.

---

## 5. Existing Automated Test Suite Execution

- **Command Executed:** `mvn clean test`
- **Execution Log Summary:**
  ```
  [INFO] -------------------------------------------------------
  [INFO]  T E S T S
  [INFO] -------------------------------------------------------
  [INFO] Running com.cinema.controller.api.FBItemApiControllerTest -> Tests run: 3, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.api.GenreApiControllerTest -> Tests run: 6, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.api.ManagerDashboardApiControllerTest -> Tests run: 2, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.api.MovieApiControllerTest -> Tests run: 6, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.api.RoomApiControllerTest -> Tests run: 6, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.api.SeatApiControllerTest -> Tests run: 2, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.api.SeatTypeApiControllerTest -> Tests run: 2, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.api.ShowtimeApiControllerTest -> Tests run: 5, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.api.TicketPricingApiControllerTest -> Tests run: 3, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.GenreControllerTest -> Tests run: 4, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.ManagerDashboardControllerTest -> Tests run: 3, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.MovieControllerTest -> Tests run: 5, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.SeatControllerTest -> Tests run: 3, Failures: 0, Errors: 0
  [INFO] Running com.cinema.controller.SeatTypeControllerTest -> Tests run: 3, Failures: 0, Errors: 0
  [INFO] Running com.cinema.service.FBItemServiceTest -> Tests run: 7, Failures: 0, Errors: 0
  [INFO] Running com.cinema.service.GenreServiceTest -> Tests run: 11, Failures: 0, Errors: 0
  [INFO] Running com.cinema.service.ManagerDashboardServiceTest -> Tests run: 1, Failures: 0, Errors: 0
  [INFO] Running com.cinema.service.MovieServiceTest -> Tests run: 12, Failures: 0, Errors: 0
  [INFO] Running com.cinema.service.RoomServiceTest -> Tests run: 5, Failures: 0, Errors: 0
  [INFO] Running com.cinema.service.SeatServiceTest -> Tests run: 4, Failures: 0, Errors: 0
  [INFO] Running com.cinema.service.ShowtimeServiceTest -> Tests run: 5, Failures: 0, Errors: 0
  [INFO] Running com.cinema.service.TicketPricingServiceTest -> Tests run: 9, Failures: 0, Errors: 0
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] Total time: 35.891 s
  [INFO] Tests run: 101, Failures: 0, Errors: 0, Skipped: 0
  ```
- **Result:** **101/101 PASS (100% Pass Rate)**

---

## 6. Browser Automation Tooling Check

- **Search Command:** `Get-Command playwright, chromedriver, selenium-server, geckodriver`
- **Result:** None found in the local environment PATH.
- **Impact:** Real browser-driven UI end-to-end automation cannot execute directly. Phase 7 (Browser E2E Testing) must be recorded as **BLOCKED** with a proposal for headless Playwright Java integration.

---

## 7. Phase 1 Readiness Decision

The local test environment is verified, stable, and ready to execute:
- Backend functional verification
- REST API integration verification
- Business flow validation
- Security & boundary tests
- Database integrity inspection against `CinemaManagementDB`
