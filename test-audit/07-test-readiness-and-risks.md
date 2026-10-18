# 07 — Test Readiness & Risk Classification

This report categorizes all technical, security, architectural, and data risks discovered during the source code audit into prioritized severity tiers.

---

## 1. Risk Classification Matrix

| Risk ID | Severity | Category | Risk Description | Impact |
|:---|:---:|:---|:---|:---|
| **RISK-01** | **CRITICAL** | Security / Auth | **Default Manager Privilege Fallback:** `ManagerSecurityUtils.resolveRole()` falls back to `"MANAGER"` when no session or header is provided. | Any unauthenticated user accessing `/manager/*` in local or staging environments is automatically granted Manager access. |
| **RISK-02** | **CRITICAL** | Security / IDOR | **Insecure Direct Object Reference (IDOR) in Customer Profile:** `CustomerProfileController` accepts `?accountId=X` with no authentication or ownership verification. | Any external user can view and overwrite any customer's personal data (phone, address, loyalty points). |
| **RISK-03** | **CRITICAL** | Database Schema | **Missing Core Tables in Schema Migrations:** Tables `Account`, `Customer`, `MembershipRank`, and `Booking` exist in DAO SQL queries but are absent from `database/*.sql`. | Clean database initialization will crash at runtime with SQL Server error `Invalid object name 'Account'`. |
| **RISK-04** | **HIGH** | Data Integrity | **Destructive Seat Layout Replacement:** `SeatService.saveRoomLayout` executes `DELETE FROM Seat WHERE RoomID = ?`. | Once customer bookings are implemented, updating a room's seat layout will fail due to FK constraints or delete booked seats. |
| **RISK-05** | **HIGH** | Data Integrity | **Soft Seat-to-SeatType Relationship:** `Seat.SeatTypeName` is stored as an arbitrary `NVARCHAR(50)` rather than a Foreign Key referencing `SeatType.SeatTypeID`. | Inconsistent seat types, broken multiplier calculations, or orphaned records if a SeatType is renamed or removed. |
| **RISK-06** | **HIGH** | Test Automation | **Absence of E2E Browser Automation:** No Selenium, Playwright, or Cypress frameworks exist in the project. | Interactive visual features (seat grid selection, modal dialogs, schedule buffer visualizers) cannot be verified in real browsers automatically. |
| **RISK-07** | **MEDIUM** | API Architecture | **Dual Path Divergence (REST vs MVC):** Frontend forms submit to MVC endpoints; dedicated REST APIs are largely unused by the UI. | Risk of behavioral divergence where bug fixes or rule changes in REST APIs are not reflected in MVC controllers (or vice versa). |
| **RISK-08** | **MEDIUM** | Security / Secrets | **Database Credentials in Repository:** `application-local.properties` contains plaintext database passwords. | Exposure of local database credentials in source control history. |
| **RISK-09** | **MEDIUM** | Media Handling | **Unvalidated Image URLs:** Movie posters and F&B images rely on raw external URL strings without format or availability validation. | Broken images or malicious URL injection in UI display. |
| **RISK-10** | **LOW** | QA Governance | **Missing Code Coverage Tooling:** JaCoCo plugin is not configured in `pom.xml`. | Inability to systematically measure line and branch coverage during automated builds. |

---

## 2. Test Readiness Assessment

### Summary Status: **READY FOR BACKEND/API AUTOMATION; BLOCKED FOR BROWSER E2E**

| Area | Readiness Rating | Current Status & Notes |
|:---|:---:|:---|
| **Unit Testing (Service Rules)** | **100% READY** | Mockito unit tests cover all business rules (BR-07, BR-08, BR-09, BR-37, BR-50, BR-52, BR-55). |
| **Controller Slice Testing** | **100% READY** | Spring `MockMvc` tests cover all 10 manager MVC controllers and REST APIs. |
| **Database Integration Testing** | **PARTIALLY READY** | Requires a running Microsoft SQL Server instance with executed schema scripts. |
| **E2E Browser Functional Testing** | **NOT READY / BLOCKED** | Requires installing and configuring a browser automation engine (e.g. Playwright or Selenium). |
| **Security / Auth Testing** | **BLOCKED** | Real user login and session flows cannot be tested as no authentication module exists. |

---

## 3. Immediate Testing Blockers
1. **No Login Interface:** QA cannot test role transitions (Guest -> Customer -> Manager) through the UI. Testing must simulate roles using HTTP headers (`X-User-Role: MANAGER`) or query parameters (`?role=CUSTOMER`).
2. **Missing Customer Database Seed:** Executing functional tests on `/customer/profile` requires manually executing DDL for `Account`, `Customer`, and `MembershipRank`.
3. **No Browser Automation Harness:** Visual verification of the interactive seat matrix (Screen 58) requires manual exploratory testing until an E2E framework is introduced.
