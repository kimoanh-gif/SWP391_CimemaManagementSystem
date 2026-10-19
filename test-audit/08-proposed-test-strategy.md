# 08 — Proposed Quality Assurance & Test Strategy

This document defines the structured test execution strategy, prioritized flow sequence, test data preparation, and recommended automation roadmap for functional verification.

---

## 1. Testing Scope & Boundaries

### In-Scope (Implemented Modules):
1. **Screen 53: Manager Dashboard** (Operational metrics, KPI cards, navigation routes).
2. **Screen 54: Movie Management** (CRUD, genre mapping, age rating, active showtime guards - BR-07, BR-37, BR-44).
3. **Screen 55: Genre Management** (CRUD, unique name validation, movie association integrity).
4. **Screen 56: Room Management** (Screening hall setup, capacity constraints, deactivation guards - BR-08, BR-52).
5. **Screen 57: Seat Type Management** (Multiplier range validation `[0.50, 3.00]`, type uniqueness - BR-50).
6. **Screen 58: Seat Layout Management** (Grid generation, capacity boundary, duplicate positions, maintenance status).
7. **Screen 59: Showtime Management** (Schedule creation, BR-07 movie status, BR-08 room status, BR-09 overlap with 15-min cleaning buffer, BR-54 booking lock, BR-55 duration calculation).
8. **Screen 60: Ticket Pricing Management** (Base price rules, BR-50 formula evaluation, 1,000 VND rounding, matrix generation).
9. **Screen 61: Food & Beverage Management** (Concessions CRUD, combo assembly, price positivity, soft delete).
10. **Screen 08: Customer Profile** (Profile view & update, field validation).

### Out-of-Scope (Not Implemented):
- Online Booking & Payment Processing (Screens 15-22).
- Member Login & Self-Registration (Screens 01-05).
- Promotions & Vouchers Management (Screen 62).
- Financial Reports & Statistical BI (Screen 63).
- News Content Publishing (Screen 64).

---

## 2. Test Execution Order (Prioritized Sequence)

Tests must be executed in dependency order so that prerequisite records exist before downstream dependent entities are configured:

```
[Phase 1: Foundation]
  1. Room Management (Screen 56)          --> Create testing rooms (Standard, IMAX)
  2. Genre Management (Screen 55)         --> Create base genres (Action, Drama, Sci-Fi)
  3. Seat Type Management (Screen 57)     --> Verify multipliers (STD, VIP, COUPLE, FRONT)

[Phase 2: Core Infrastructure]
  4. Seat Layout Management (Screen 58)   --> Generate & verify seat grids for created rooms
  5. Movie Management (Screen 54)         --> Create Upcoming & Now Showing movies linked to genres
  6. Ticket Pricing Management (Screen 60)--> Configure base rates & verify BR-50 price matrix

[Phase 3: Operational Scheduling]
  7. Showtime Management (Screen 59)      --> Schedule showtimes, verify buffer & overlap guards
  8. Food & Beverage Management (Screen 61)--> Setup concessions & combo recipes

[Phase 4: Synthesis & Monitoring]
  9. Manager Dashboard (Screen 53)        --> Verify live KPI metrics reflect all above operations
 10. Customer Profile (Screen 08)         --> Inspect profile view & update (with mock Account ID)
```

---

## 3. Detailed Test Scenarios per Flow

### Priority 1: Showtime Conflict & Cleaning Buffer (Screen 59 / BR-07, BR-08, BR-09, BR-55)
- **TC-SHW-01 (Positive):** Schedule a valid showtime for an active movie in Room 1 from 14:00 to 16:00. Verify status is `Scheduled`.
- **TC-SHW-02 (BR-09 Overlap):** Attempt to schedule a showtime in Room 1 from 15:30 to 17:30. Verify system blocks creation with `ShowtimeConflictException`.
- **TC-SHW-03 (15-min Buffer Guard):** Attempt to schedule a showtime in Room 1 starting at 16:10 (less than 15 minutes after 16:00). Verify conflict is flagged due to mandatory cleaning buffer.
- **TC-SHW-04 (Safe Adjacent):** Schedule a showtime in Room 1 starting at 16:15 (exactly 15 minutes after 16:00). Verify creation succeeds.
- **TC-SHW-05 (BR-07 Rejection):** Attempt to schedule an `Ended` or `Archived` movie. Verify validation rejects the request.

### Priority 2: Ticket Pricing Calculation & Rounding (Screen 60 / BR-50, BR-51)
- **TC-PRC-01 (Formula Verification):**
  - Base ticket price = 85,000 VND
  - Format surcharge (3D) = 20,000 VND
  - Seat Type (VIP) multiplier = 1.25
  - Expected Unit Price: $(85,000 + 20,000) \times 1.25 = 105,000 \times 1.25 = 131,250 \approx 131,000\text{ VND}$ (rounded to nearest 1,000 VND).
- **TC-PRC-02 (Multiplier Boundary):**
  - Attempt to set multiplier to `0.45` (< 0.50) -> Rejected.
  - Attempt to set multiplier to `3.50` (> 3.00) -> Rejected.
  - Set multiplier to `0.50` and `3.00` -> Accepted.

### Priority 3: Seat Layout & Capacity Constraints (Screen 58)
- **TC-SEAT-01 (Auto-Generate):** Trigger auto-generation for a room with 8 rows x 12 seats (96 seats total). Verify Row A is `Front`, central rows are `VIP`, back row is `Couple`, rest are `Standard`.
- **TC-SEAT-02 (Duplicate Position Guard):** Submit layout containing two seats at `(Row A, Number 5)` in the same room. Verify rejection with `InvalidSeatLayoutException`.
- **TC-SEAT-03 (Exceed Capacity Guard):** Submit 101 seats for a room with capacity 100. Verify rejection.

### Priority 4: Referential Integrity & Deletion Guards (Screens 54, 55, 56)
- **TC-DEL-01 (BR-37 Movie Guard):** Attempt to delete a movie currently scheduled in future showtimes. Verify deletion is rejected.
- **TC-DEL-02 (BR-52 Room Guard):** Attempt to put a room into `Maintenance` while it has future showtimes. Verify operation is blocked.
- **TC-DEL-03 (Genre Guard):** Attempt to delete a genre mapped to active movies. Verify operation is rejected.

---

## 4. Test Environment Setup & Prerequisites

1. **Target Database:** Microsoft SQL Server with database `CinemaManagementDB`.
2. **Schema Execution Order:**
   1. `database/schema_genre.sql`
   2. `database/schema_movie.sql`
   3. `database/schema_room.sql`
   4. `database/schema_pricing.sql`
   5. `database/schema_fb.sql`
3. **Application Profile:** Run with profile `local` (`application-local.properties`).

---

## 5. Recommended Test Automation Roadmap

To achieve full end-to-end automation without manual testing:
1. **API Automation:** Leverage RestAssured or Spring `MockMvc` integration tests to automate all 48 REST API endpoints in regression pipelines.
2. **Browser E2E Automation:** Introduce **Playwright Java** into `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.microsoft.playwright</groupId>
       <artifactId>playwright</artifactId>
       <version>1.49.0</version>
       <scope>test</scope>
   </dependency>
   ```
   Playwright allows headless Chromium testing of the visual seat map and modal interactions without external driver installation.
3. **Coverage Enforcement:** Add `jacoco-maven-plugin` with minimum 80% instruction and branch coverage rule.
