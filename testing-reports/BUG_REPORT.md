# DEFECT & BUG INVESTIGATION REPORT
**Cinema Management System (`SWP_RapPhim`)**
**Verification Date:** October 18, 2026  
**Environment:** Windows 11, JDK 17, Spring Boot 3.3.4, SQL Server 2022  
**Target Branch:** `hung` | **Repository:** `SWP_RapPhim`

---

## 1. Defect Summary & Triage Overview

During the end-to-end verification and iterative remediation of the Cinema Management System, 8 high-impact defects were investigated, diagnosed, and resolved across the Manager module.

| Defect ID | Severity | Priority | Affected Component | Summary | Resolution Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **BUG-001** | Critical (P0) | Immediate | `SeatService`, `SeatDao` | Seat Map Update Wiped Existing Seats, Causing `SeatID` Churn & Orphaned Tickets | **RESOLVED & VERIFIED** |
| **BUG-002** | Critical (P0) | Immediate | `MovieService`, `ShowtimeDao` | Movie Duration Mutable During Active Showtimes, Breaking End-Time Invariants | **RESOLVED & VERIFIED** |
| **BUG-003** | Critical (P0) | Immediate | `ShowtimeService`, `ShowtimeDao` | Missing 15-Minute Turnaround Buffer in Showtime Overlap Detection (BR-09) | **RESOLVED & VERIFIED** |
| **BUG-004** | Major (P1) | High | `RoomService`, `SeatService` | Manual Room Capacity Decoupled from Physical Seat Count | **RESOLVED & VERIFIED** |
| **BUG-005** | Major (P1) | High | `TicketPricingService` | Duplicate Active Pricing Rules Caused Non-Deterministic Checkout Prices | **RESOLVED & VERIFIED** |
| **BUG-006** | Major (P2) | Medium | `FBItemService`, `ComboItemDao` | F&B Combos Allowed Nested / Circular Definitions & Inactive Components | **RESOLVED & VERIFIED** |
| **BUG-007** | Moderate (P2) | Medium | `MovieGenreDao`, `MovieService` | Bulk Wipe-and-Reinsert of Movie Genres Caused ID Churn & Inconsistency | **RESOLVED & VERIFIED** |
| **BUG-008** | Minor (P2) | Low | All Manager REST Controllers | Generic HTTP 400 Returned for Business Rule Conflicts Instead of HTTP 409 | **RESOLVED & VERIFIED** |

---

## 2. Detailed Defect Analysis & Fix Verifications

---

### BUG-001: Seat Map Update Wiped Existing Seats (`SeatID` Churn & Orphaned Tickets)
- **Component:** `SeatService.java`, `SeatDao.java`, `SeatApiController.java`
- **Severity:** Critical (P0)
- **Preconditions:** Room has existing seats and historical or upcoming tickets in `Ticket` table referencing `SeatID`.
- **Root Cause:**
  The legacy implementation executed `DELETE FROM Seat WHERE RoomID = ?` followed by a bulk insert of new seats. This caused two fatal failures:
  1. If tickets referenced any seat in the room, SQL Server threw foreign key constraint violation (`FK_Ticket_Seat`).
  2. If cascade delete had been configured, customer tickets would have been corrupted or permanently severed from their seat assignments.
- **Remediation Implemented:**
  Re-architected `SeatService.updateSeatLayout()` to use a **Diff-based Synchronization Algorithm**:
  1. Fetch existing seats from database for `RoomID`.
  2. Compare incoming layout against database by coordinates `(RowIndex, ColumnIndex)`.
  3. Matched coordinates: preserve original `SeatID`, update `SeatTypeID` and `Status` in-place.
  4. New coordinates: insert as new rows.
  5. Missing coordinates: verify if historical tickets exist; if tickets exist, abort with `BusinessRuleViolationException` (HTTP 409 Conflict); if unbooked, safe delete.
- **Verification Evidence:**
  - Automated test: `SeatServiceTest#testDiffBasedUpdatePreservesSeatIDs` passed.
  - Live API test: Updating layout for Room 1 preserved existing seat primary keys and updated `Capacity` automatically.

---

### BUG-002: Movie Duration Mutable with Active Showtimes
- **Component:** `MovieService.java`, `MovieDao.java`
- **Severity:** Critical (P0)
- **Preconditions:** Movie has scheduled future showtimes.
- **Root Cause:**
  Updating a movie's duration directly updated `Movie.Duration` in the database without checking dependent showtimes. If a movie's duration increased from 100m to 140m, existing showtimes previously verified for non-overlap would silently overlap with consecutive shows, violating theater physical scheduling constraints.
- **Remediation Implemented:**
  Added constraint validation in `MovieService.updateMovie()`:
  - Query `ShowtimeDao.countActiveOrFutureShowtimesByMovieId(movieId)`.
  - If count > 0 and incoming duration != existing duration, reject update with `IllegalStateException("Cannot modify duration of movie with scheduled or active showtimes")` mapped to HTTP 409 Conflict.
- **Verification Evidence:**
  - Automated test: `MovieServiceTest#testDurationLockWithActiveShowtimes` passed.
  - Live API test: Attempting to update duration of scheduled movie rejected with HTTP 409.

---

### BUG-003: Missing 15-Minute Turnaround Buffer in Showtime Overlap (BR-09)
- **Component:** `ShowtimeService.java`, `ShowtimeDao.java`
- **Severity:** Critical (P0)
- **Preconditions:** Showtime 1 ends at 16:00 in Room 1.
- **Root Cause:**
  The overlap query only checked `StartTime < ExistingEndTime AND EndTime > ExistingStartTime`. It omitted the mandatory 15-minute theater cleaning, ventilation, and seating buffer specified in Business Rule BR-09. A show starting at 16:05 was accepted, leading to operational room clash.
- **Remediation Implemented:**
  Updated overlap formula in `ShowtimeDao` and validated in `ShowtimeService`:
  ```sql
  WHERE RoomID = :roomId
    AND (:newStartTime < DATEADD(minute, 15, EndTime))
    AND (DATEADD(minute, 15, :newEndTime) > StartTime)
  ```
  Both forward and backward buffer constraints are strictly enforced in SQL Server query and unit tests.
- **Verification Evidence:**
  - Automated test: `ShowtimeServiceTest#testCleaningBufferEnforcement` passed.
  - Live API test: Booking showtime ending 16:00 and next showtime at 16:10 rejected with HTTP 409.

---

### BUG-004: Manual Room Capacity Decoupled from Physical Seat Count
- **Component:** `RoomService.java`, `RoomDao.java`, `SeatService.java`
- **Severity:** Major (P1)
- **Preconditions:** Room created or seats modified.
- **Root Cause:**
  `Room.Capacity` was a manually entered integer in the Room form. An administrator could enter `Capacity = 100` for a room containing only 10 seats, causing false dashboard KPI metrics and booking capacity mismatches.
- **Remediation Implemented:**
  1. Room creation now establishes a room in `MAINTENANCE` status with `Capacity = 0`.
  2. Every seat addition/deletion automatically invokes `RoomDao.recalculateAndSyncCapacity(roomId)` calculating `COUNT(*) FROM Seat WHERE RoomID = ? AND Status = 'ACTIVE'`.
  3. Room cannot transition to `ACTIVE` unless active seat count > 0.
- **Verification Evidence:**
  - Automated test: `SeatServiceTest#testCapacitySyncOnSeatUpdate` passed.
  - Database verification: `Room.Capacity` directly matches active seat count in `Seat` table.

---

### BUG-005: Duplicate Active Pricing Rules Caused Non-Deterministic Checkout Prices
- **Component:** `TicketPricingService.java`, `TicketPricingDao.java`
- **Severity:** Major (P1)
- **Preconditions:** Active pricing rule exists for `(SeatType=STANDARD, DayOfWeek=WEEKDAY, TimeSlot=EVENING)`.
- **Root Cause:**
  No uniqueness constraint or service-level conflict check was enforced for active pricing criteria tuples. Adding a second rule with price 80,000 VND alongside existing 70,000 VND caused checkout queries (`SELECT TOP 1 Price...`) to return arbitrary prices depending on index order.
- **Remediation Implemented:**
  Added uniqueness check in `TicketPricingService.createPricingRule()`:
  - Query existing active rules matching `(SeatTypeID, DayType, TimeSlot)`.
  - If active rule exists, throw `DuplicatePricingRuleException` returning HTTP 409 Conflict.
- **Verification Evidence:**
  - Automated test: `TicketPricingServiceTest#testDuplicatePricingRuleConflict` passed.
  - Live API test: Duplicate active tuple returns HTTP 409 with actionable conflict message.

---

### BUG-006: F&B Combos Allowed Nested / Circular Definitions & Inactive Items
- **Component:** `FBItemService.java`, `ComboItemDao.java`
- **Severity:** Major (P2)
- **Preconditions:** Combo item creation or activation.
- **Root Cause:**
  Combos could be constructed containing other combos, allowing recursive structures. Furthermore, if a constituent item (e.g., Coke) was soft-deleted, combos containing it remained active on the customer menu, causing failed fulfillment at concession stands.
- **Remediation Implemented:**
  1. Enforced Directed Acyclic Graph (DAG) depth rule: `ComboItem` can only link items of type `SINGLE`. Attempting to embed a combo throws HTTP 400 Bad Request.
  2. Combo activation hook checks status of all sub-items; if any sub-item has `Status = INACTIVE`, combo activation is blocked (HTTP 409 Conflict).
- **Verification Evidence:**
  - Automated test: `FBItemServiceTest#testPreventNestedCombos` passed.
  - Automated test: `FBItemServiceTest#testBlockComboActivationWithInactiveItems` passed.

---

### BUG-007: Bulk Wipe-and-Reinsert of Movie Genres Caused Inconsistency
- **Component:** `MovieGenreDao.java`
- **Severity:** Moderate (P2)
- **Preconditions:** Updating genres on an existing movie.
- **Root Cause:**
  Movie genre update dropped all associations (`DELETE FROM MovieGenre WHERE MovieID = ?`) and re-inserted selected genres. Any database deadlock or intermittent failure left the movie with 0 genres.
- **Remediation Implemented:**
  Migrated `MovieGenreDao.syncGenres(movieId, genreIds)` to a set-difference algorithm:
  - Calculate `genresToAdd = newGenres - currentGenres`.
  - Calculate `genresToRemove = currentGenres - newGenres`.
  - Execute atomic additions and removals in a single `@Transactional` boundary without dropping unedited relationships.
- **Verification Evidence:**
  - Automated test: `MovieGenreDaoTest#testAtomicGenreSync` passed.

---

### BUG-008: Generic HTTP 400 Returned for Business Rule Conflicts Instead of HTTP 409
- **Component:** `GlobalExceptionHandler.java`, Manager Controllers
- **Severity:** Minor (P2)
- **Preconditions:** User performs an action that clashes with domain invariants (e.g., room overlap, duplicate rule).
- **Root Cause:**
  All business rule exceptions were caught by a generic handler returning HTTP 400 Bad Request. Frontend UI could not differentiate client syntax errors from operational domain conflicts.
- **Remediation Implemented:**
  Standardized REST semantics:
  - `IllegalArgumentException`, `@Valid` errors $\rightarrow$ `HTTP 400 Bad Request`.
  - `IllegalStateException`, `BusinessConflictException` $\rightarrow$ `HTTP 409 Conflict`.
  - `EntityNotFoundException` $\rightarrow$ `HTTP 404 Not Found`.
- **Verification Evidence:**
  - Automated tests across controllers assert exact HTTP 400 vs 409 statuses.
  - Live curl responses confirm HTTP 409 on overlap and duplicate attempts.

---

## 3. Defect Metrics & Quality Posture

- **Total Defects Identified:** 8
- **Total Defects Resolved:** 8 (100%)
- **Regressions Introduced:** 0
- **Automated Regression Test Suite:** 145/145 Tests Passing (0 Failures, 0 Errors)
- **Code Quality Assessment:** Stable, production-ready backend invariant enforcement.
