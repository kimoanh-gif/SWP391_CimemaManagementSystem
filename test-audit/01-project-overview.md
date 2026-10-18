# 01 — Project Architecture & Overview

## 1. Project Information
- **Project Name:** Cinema Management System (`cinema-management-system`)
- **Course / Context:** SWP391 - Software Development Project (Iteration 2)
- **Purpose:** Comprehensive cinema operational platform supporting cinema managers in handling movies, genres, theater rooms, seat layouts, seat types, showtime scheduling, dynamic ticket pricing, food & beverage (F&B) inventory, and customer profile management.
- **Repository Branch:** `hung`

---

## 2. Technology Stack & Architectural Summary

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Backend Runtime** | Java 17 | OpenJDK 17 / Eclipse Temurin |
| **Backend Framework** | Spring Boot 3.5.7 | Web (`spring-boot-starter-web`), JDBC (`spring-boot-starter-jdbc`), Validation (`spring-boot-starter-validation`) |
| **Build Tool** | Apache Maven 3.x | Maven project definition (`pom.xml`) |
| **Database System** | Microsoft SQL Server 2019+ | Driver: `mssql-jdbc` |
| **Persistence Style** | Spring `JdbcTemplate` | Direct SQL queries and mapped `RowMapper` implementations (no Hibernate/JPA) |
| **Server-Side UI** | Thymeleaf 3 | `spring-boot-starter-thymeleaf` |
| **Client-Side Scripts** | Vanilla JavaScript (ES6+) | Direct DOM manipulation, Fetch API, no external client frameworks (React/Vue/Angular not used) |
| **Client-Side Styling** | CSS3 | Dark Theme custom variables (`--bg-main`, `--accent-red`, `--accent-gold`), Flexbox, CSS Grid |
| **Testing Frameworks** | JUnit 5, Mockito, Spring Test | `spring-boot-starter-test` (using Spring `MockMvc` for slice testing, `@ExtendWith(MockitoExtension.class)` for unit testing) |

---

## 3. Directory & Folder Structure

```
c:\Users\hoang\Downloads\SWP_RapPhim\
├── database/                                # Database migration scripts and schema definitions
│   ├── README.md
│   ├── schema_fb.sql                        # Food & Beverage items and Combo relationships
│   ├── schema_genre.sql                     # Genre and MovieGenre mapping
│   ├── schema_movie.sql                     # Movie entity definitions
│   ├── schema_pricing.sql                   # SeatType and TicketPricing matrix
│   └── schema_room.sql                      # Room and Seat entities
├── docs/                                    # System documentation
│   ├── README.md
│   └── SRS_CinemaManagementSystem_SWP391_G5.docx.md # System Requirements Specification (SRS)
├── src/
│   ├── main/
│   │   ├── java/com/cinema/
│   │   │   ├── CinemaManagementApplication.java  # Spring Boot Main Entry Point
│   │   │   ├── controller/                  # MVC Page Controllers (Screen 08, 53 - 61)
│   │   │   │   ├── api/                     # REST API Controllers (/api/manager/...)
│   │   │   ├── dao/                         # Data Access Objects (JdbcTemplate)
│   │   │   ├── exception/                   # Custom Domain & Validation Exceptions
│   │   │   ├── model/                       # Data Entities, Forms & DTOs
│   │   │   ├── service/                     # Core Business Logic & Rule Enforcement
│   │   │   └── util/                        # Security Utilities (ManagerSecurityUtils)
│   │   └── resources/
│   │       ├── application.properties       # Base environment configuration
│   │       ├── application-local.properties # Local profile (SQL Server credentials [REDACTED])
│   │       ├── static/                      # Static assets
│   │       │   ├── css/                     # Per-screen stylesheets (manager-*.css, customer-profile.css)
│   │       │   ├── js/                      # Per-screen JavaScript scripts (manager-*.js)
│   │       │   └── images/
│   │       └── templates/                   # Thymeleaf templates
│   │           ├── admin/
│   │           ├── auth/
│   │           ├── customer/                # profile.html, profile-edit.html
│   │           ├── manager/                 # dashboard.html, movies.html, genres.html, rooms.html,
│   │           │                            # seat-types.html, seats.html, showtimes.html, pricing.html, fb.html
│   │           ├── staff/
│   │           └── index.html               # Welcome root template
│   └── test/
│       └── java/com/cinema/
│           ├── controller/                  # WebMvc slice tests (Controllers)
│           │   ├── api/                     # REST API WebMvc tests
│           └── service/                     # Mockito unit tests (Services)
└── test-audit/                              # Comprehensive QA Audit Documentation
```

---

## 4. Database Architecture & Schema Location
- **Database Engine:** Microsoft SQL Server (target DB name: `CinemaManagementDB`).
- **Schema Scripts Location:** `database/` directory.
  - `schema_movie.sql`: Movie catalog table (`Movie`).
  - `schema_genre.sql`: Genre classification (`Genre`) and many-to-many relationship (`MovieGenre`).
  - `schema_room.sql`: Cinema screening rooms (`Room`) and individual seat matrix (`Seat`).
  - `schema_pricing.sql`: Seat types with multipliers (`SeatType`) and base pricing matrix rules (`TicketPricing`).
  - `schema_fb.sql`: Food & Beverage catalogue (`FBItem`) and combo recipe breakdown (`ComboItem`).
- **Data Access Layer:** Direct Spring `JdbcTemplate` instances injected into specialized DAOs. No automated database migration tool (e.g., Flyway or Liquibase) is currently configured in `pom.xml`.

---

## 5. Authentication & Authorization Mechanism

### Current Implementation Findings:
1. **No Spring Security:** `spring-boot-starter-security` is **not** present in `pom.xml`.
2. **Custom Security Utility:** Manager role verification is enforced through `com.cinema.util.ManagerSecurityUtils.verifyManagerAccess(HttpServletRequest)`.
   - **Role Resolution Order:**
     1. HTTP Session: Attribute `role` or `userRole`.
     2. HTTP Request Header: `X-User-Role`.
     3. Request Query Parameter: `?role=...`.
     4. **Default Fallback:** If no session, header, or query param is provided, it defaults to `"MANAGER"` to facilitate UI access in local development.
   - **Access Control Rule:** If a role is resolved, only `"MANAGER"` and `"ADMIN"` (case-insensitive) are allowed. Any other role (e.g., `"CUSTOMER"`) triggers an `UnauthorizedManagerException` (HTTP 403 Forbidden).
3. **Customer Module Security Gap:**
   - `CustomerProfileController` has **no authentication or session checks**. It directly loads and updates profiles using the query parameter `?accountId=...`. Anyone can inspect or modify any customer profile by changing the `accountId`.
4. **Missing Authentication Modules:**
   - No login form, registration flow, password hashing (`BCrypt`), or token handling (JWT/OAuth) exists in the active codebase.

---

## 6. External Services & Third-Party Integrations
- **CDN / External Images:** Unsplash image URLs are used in seed scripts (`schema_fb.sql`) and sample movie posters.
- **Third-Party APIs:** No payment gateways (VNPay, Momo), email services (SendGrid, JavaMail), or SMS OTP providers are currently integrated in the backend codebase.

---

## 7. Environment Configuration
Configuration files located in `src/main/resources/`:
1. `application.properties`:
   - `spring.application.name=cinema-management-system`
   - `spring.profiles.default=local`
   - `spring.datasource.url=${DB_URL}`
   - `spring.datasource.username=${DB_USERNAME}`
   - `spring.datasource.password=${DB_PASSWORD}`
   - `spring.thymeleaf.cache=false`
2. `application-local.properties`:
   - Contains local connection strings to `localhost:1433`, `CinemaManagementDB` with database user `[REDACTED]` and password `[REDACTED]`.

---

## 8. Application Startup Instructions

### Prerequisites:
- Java JDK 17 installed and configured (`JAVA_HOME`).
- Microsoft SQL Server running on `localhost:1433` with database `CinemaManagementDB` created and schemas executed.
- Maven 3.8+ (or Maven wrapper if available).

### Build & Run Commands:
```bash
# 1. Compile and execute automated test suite
mvn clean test

# 2. Package application jar
mvn clean package -DskipTests

# 3. Run application with local profile
mvn spring-boot:run
```
- **Access URLs:**
  - Manager Dashboard: `http://localhost:8080/manager` or `http://localhost:8080/manager/dashboard`
  - Customer Profile: `http://localhost:8080/customer/profile?accountId=1`
  - Landing page: `http://localhost:8080/`

---

## 9. Test Frameworks & Browser Automation Tools Installed
- **Unit & Slice Testing:**
  - JUnit Jupiter (JUnit 5)
  - Mockito & `org.springframework.test.context.bean.override.mockito.MockitoBean`
  - Spring Test `MockMvc`
- **Browser Automation Tools:**
  - **None installed.** Selenium WebDriver, Playwright, Cypress, and Puppeteer are **absent** from `pom.xml` and the repository.
- **Automated Test Execution Status:**
  - All existing 101 tests execute cleanly via `mvn test`.
