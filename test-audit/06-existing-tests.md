# 06 — Existing Test Suite & Automation Capabilities

This document audits the current test suite, test dependencies, execution capabilities, coverage, and limitations discovered in the repository.

---

## 1. Test Architecture & Dependencies

Defined in `pom.xml`:
- `spring-boot-starter-test` (scope: `test`):
  - JUnit Jupiter (JUnit 5.11+)
  - Mockito (Mockito Core 5.x)
  - Spring Test Context Framework & Spring `MockMvc`
  - AssertJ, Hamcrest, JSONassert
- **Absent Automation Libraries:**
  - Selenium WebDriver: **Not present**
  - Playwright / Cypress / Puppeteer: **Not present**
  - Cucumber / BDD: **Not present**
  - RestAssured: **Not present**
  - JaCoCo (Code Coverage Maven Plugin): **Not present** in `pom.xml`

---

## 2. Test Suite Breakdown (101 Automated Tests)

All automated tests are written in Java using JUnit 5 and Spring Boot Test utilities.

### A. MVC Controller Slice Tests (`@WebMvcTest`)
| Test Class | Focus / Scenarios Tested | Test Count |
|:---|:---|:---:|
| `GenreControllerTest` | List view rendering, authorization, valid save redirect, invalid input error | 4 |
| `MovieControllerTest` | Catalog rendering, search filters, form creation redirect, status change, delete guard | 5 |
| `SeatControllerTest` | Room layout view rendering, 403 authorization guard, individual seat updates | 3 |
| `SeatTypeControllerTest` | Seat type listing, customer role rejection (403), multiplier update redirect | 3 |
| `ManagerDashboardControllerTest` | Dashboard rendering with stats, `/manager` alias mapping, 403 Forbidden check | 3 |

### B. REST API Slice Tests (`@WebMvcTest`)
| Test Class | Focus / Scenarios Tested | Test Count |
|:---|:---|:---:|
| `FBItemApiControllerTest` | List retrieval, item detail, item creation, price updates, deletion guard | 3 |
| `GenreApiControllerTest` | CRUD endpoints, JSON serialization, status toggle, duplicate conflict (409) | 6 |
| `ManagerDashboardApiControllerTest` | Live JSON stats endpoint, customer role rejection | 2 |
| `MovieApiControllerTest` | REST CRUD, query filters, status patch, active showtime delete block | 6 |
| `RoomApiControllerTest` | Room CRUD, room search, maintenance transition, upcoming showtimes block | 6 |
| `SeatApiControllerTest` | Room seat layout retrieval, individual seat patch | 2 |
| `SeatTypeApiControllerTest` | Seat type list JSON, quick multiplier update PUT | 2 |
| `ShowtimeApiControllerTest` | Showtime schedule list, showtime creation, conflict handling, status change | 5 |
| `TicketPricingApiControllerTest` | Pricing rules list, base price update patch, pricing matrix retrieval | 3 |

### C. Domain Service Unit Tests (`@ExtendWith(MockitoExtension.class)`)
| Test Class | Business Rules & Logic Tested | Test Count |
|:---|:---|:---:|
| `FBItemServiceTest` | Price positivity, SKU uniqueness, combo breakdown, soft-delete rules | 7 |
| `GenreServiceTest` | Name uniqueness case-insensitivity, association checks before deletion | 11 |
| `ManagerDashboardServiceTest` | KPI metric aggregations across all underlying DAOs | 1 |
| `MovieServiceTest` | Status transitions (BR-07), runtime positivity, active showtime guards (BR-37) | 12 |
| `RoomServiceTest` | Capacity validation, maintenance deactivation with future showtimes (BR-52) | 5 |
| `SeatServiceTest` | Position uniqueness, capacity boundary checks, default zoning generation | 4 |
| `ShowtimeServiceTest` | BR-07 movie status, BR-08 room status, BR-09 overlap with 15-min cleaning buffer | 5 |
| `TicketPricingServiceTest` | BR-50 pricing formula, rounding to nearest 1,000 VND, multiplier range `[0.50, 3.00]` | 9 |

**Total Tests:** **101 Tests** (100% Pass Rate).

---

## 3. Test Execution Verification

- **Command Executed:** `mvn test`
- **Result:**
  ```
  [INFO] Results:
  [INFO] Tests run: 101, Failures: 0, Errors: 0, Skipped: 0
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] Total time: 20.774 s
  ```
- **Observations:**
  - MockMvc slice tests and Mockito unit tests execute in under 21 seconds.
  - No active database connection is required to run the current unit/slice tests (all database calls are mocked via `@MockitoBean` or `@Mock`).

---

## 4. Test Coverage & Automation Gaps

1. **No End-to-End (E2E) Browser Tests:**
   - No browser automation exists to test real user interactions in Chrome/Firefox (e.g., clicking on seat buttons in the visual seat layout, opening modals, submitting forms).
2. **Missing Customer Profile Tests:**
   - Neither `CustomerProfileController` nor `CustomerProfileService` has any test classes in `src/test/java/`.
3. **No Integration Tests against Real Database:**
   - No `@SpringBootTest(webEnvironment = RANDOM_PORT)` or Testcontainers tests exist to verify actual SQL queries against SQL Server.
4. **No CI/CD Pipeline Configured:**
   - No `.github/workflows/` or GitLab CI configuration exists in the repository. Testing is currently triggered manually via Maven.
