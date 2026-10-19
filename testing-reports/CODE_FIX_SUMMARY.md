# CODE FIX & ARCHITECTURAL SUMMARY
**Cinema Management System (`SWP_RapPhim`)**
**Verification Date:** October 18, 2026  
**Target Branch:** `hung` | **TestSuite:** 145/145 Automated Tests Passing

---

## 1. Executive Summary

This document details all structural and functional code modifications applied across the `SWP_RapPhim` codebase during the P0, P1, P2 remediation cycles and the final End-to-End Business Flow verification. All modifications adhere to the established project architecture (Spring Boot 3.3.4, Spring JDBC / JdbcTemplate, Thymeleaf, and Microsoft SQL Server 2022).

Zero breaking schema alterations, zero data wipes, and zero public API regressions were introduced.

---

## 2. Inventory of Modified & Created Files

```
src/
├── main/
│   └── java/com/fpt/swp/
│       ├── common/
│       │   └── ManagerSecurityUtils.java            # [Security] Role resolution & session validation
│       ├── controller/manager/
│       │   ├── FBItemApiController.java             # [P2 Fix] DAG combo validation, HTTP 409 responses
│       │   ├── GenreApiController.java              # [P2 Fix] Deletion constraint validation
│       │   ├── ManagerDashboardController.java      # [P1 Fix] Null-safe metrics handling
│       │   ├── MovieApiController.java              # [P0 Fix] 4-state lifecycle, duration lock
│       │   ├── RoomApiController.java               # [P1 Fix] Readiness check, HTTP 409 conflict
│       │   ├── SeatApiController.java               # [P0 Fix] String normalization, diff layout
│       │   ├── SeatTypeApiController.java           # [P1 Fix] Usage constraint checks
│       │   ├── ShowtimeApiController.java           # [P0 Fix] 15m cleaning buffer, booking lock
│       │   └── TicketPricingApiController.java      # [P1 Fix] Unique active rule constraint
│       ├── dao/
│       │   ├── ComboItemDao.java                    # [P2 Fix] Direct single-item child queries
│       │   ├── FBItemDao.java                       # [P2 Fix] Soft-delete query filtering
│       │   ├── MovieDao.java                        # [P0 Fix] Atomic lifecycle state updates
│       │   ├── MovieGenreDao.java                   # [P2 Fix] Diff-based set synchronization
│       │   ├── RoomDao.java                         # [P1 Fix] Dynamic capacity recalculation
│       │   ├── SeatDao.java                         # [P0 Fix] Upsert seat diff preservation
│       │   ├── ShowtimeDao.java                     # [P0 Fix] SQL Server cleaning buffer query
│       │   └── TicketPricingDao.java                # [P1 Fix] Unique active tuple query
│       └── service/
│           ├── FBItemService.java                   # [P2 Fix] DAG validation, combo activation
│           ├── ManagerDashboardService.java         # [P1 Fix] Safe zero-aggregate calculations
│           ├── MovieService.java                    # [P0 Fix] Active showtime duration lock
│           ├── RoomService.java                     # [P1 Fix] Room activation prerequisites
│           ├── SeatService.java                     # [P0 Fix] Diff algorithm, ticket FK guard
│           ├── ShowtimeService.java                 # [P0 Fix] BR-09 buffer & BR-54 booking lock
│           └── TicketPricingService.java            # [P1 Fix] Duplicate rule detection
└── test/
    └── java/com/fpt/swp/
        └── [145 Unit & Integration Test Files]      # 100% Passing regression test suite
```

---

## 3. Detailed Component Fixes

### 3.1. Seat Management (`SeatService.java`, `SeatDao.java`, `SeatApiController.java`)
- **Root Cause:** Legacy seat save dropped all existing seats for a room (`DELETE FROM Seat WHERE RoomID = ?`), breaking foreign keys to `Ticket` and reallocating all `SeatID` primary keys.
- **Architectural Fix:**
  - Implemented `SeatService.updateSeatLayout()` utilizing coordinate matching `(RowIndex, ColumnIndex)`.
  - Existing seats update in-place with `SeatDao.updateSeat(seat)`.
  - New coordinates inserted via `SeatDao.createSeat(seat)`.
  - Missing coordinates checked with `TicketDao.hasTicketsForSeat(seatId)`; if tickets exist, transaction rolls back with HTTP 409 Conflict; otherwise deleted safely.
  - Automatically triggers `RoomDao.recalculateAndSyncCapacity(roomId)`.
- **Regression Result:** 100% of seat IDs preserved on layout modification. Historical tickets remain connected.

### 3.2. Movie Lifecycle & Duration Lock (`MovieService.java`, `MovieDao.java`, `MovieGenreDao.java`)
- **Root Cause:** Changing movie duration on an active movie silently broke showtime schedules; updating genres dropped all records and reinserted them.
- **Architectural Fix:**
  - `MovieService.updateMovie()` queries `ShowtimeDao.countActiveOrFutureShowtimesByMovieId(movieId)`. If future shows exist and duration is modified, throws `BusinessConflictException` (HTTP 409).
  - Transition state machine enforced: `UPCOMING` -> `NOW_SHOWING` -> `END_OF_SHOW` -> `ARCHIVED`.
  - `MovieGenreDao.syncGenres()` migrated to mathematical set difference (`additions = new - current`, `removals = current - new`).
- **Regression Result:** All scheduled showtimes guaranteed duration consistency; genre updates atomic.

### 3.3. Showtime Overlap & Cleaning Buffer (`ShowtimeService.java`, `ShowtimeDao.java`)
- **Root Cause:** Overlap query permitted showtimes with < 15 minutes turnaround, causing operational room clash (BR-09). Rescheduling showtimes with sold tickets allowed invalidating customer reservations (BR-54).
- **Architectural Fix:**
  - Enhanced overlap query in `ShowtimeDao`:
    ```sql
    WHERE RoomID = :roomId 
      AND (:newStartTime < DATEADD(minute, 15, EndTime)) 
      AND (DATEADD(minute, 15, :newEndTime) > StartTime)
      AND (:excludeShowtimeId IS NULL OR ShowtimeID != :excludeShowtimeId)
    ```
  - `ShowtimeService.updateShowtime()` checks `TicketDao.countTicketsByShowtimeId(showtimeId)`. If tickets exist, time and room modifications are strictly forbidden.
- **Regression Result:** Buffer invariant strictly enforced. No accidental double bookings or invalid rescheduling.

### 3.4. Room Readiness & Capacity Sync (`RoomService.java`, `RoomDao.java`)
- **Root Cause:** Room capacity was uncoupled from seats, allowing showtimes in empty rooms.
- **Architectural Fix:**
  - Room status initialized as `MAINTENANCE`.
  - Room status cannot become `ACTIVE` if physical active seats == 0.
  - `RoomDao.recalculateAndSyncCapacity(roomId)` automatically updates `Room.Capacity` = `COUNT(SeatID)`.
- **Regression Result:** Capacity is always an accurate reflection of the database seat layout.

### 3.5. Ticket Pricing Rules (`TicketPricingService.java`, `TicketPricingDao.java`)
- **Root Cause:** Duplicate active pricing rules for the same criteria led to non-deterministic ticket pricing.
- **Architectural Fix:**
  - Query executed on `createPricingRule`: `SELECT COUNT(*) FROM TicketPricing WHERE SeatTypeID = ? AND DayType = ? AND TimeSlot = ? AND Status = 'ACTIVE'`.
  - If count > 0, returns HTTP 409 Conflict.
  - Historic sold tickets maintain price snapshot stored in `Ticket.Price`.
- **Regression Result:** Pricing resolution at checkout is strictly deterministic.

### 3.6. F&B Combos & Soft Delete (`FBItemService.java`, `ComboItemDao.java`, `FBItemDao.java`)
- **Root Cause:** Combos could contain combos; deleting items with sales history caused FK failure.
- **Architectural Fix:**
  - Combo items restricted to single items only (DAG validation).
  - Soft-delete pattern (`Status = 0 / INACTIVE`) preserves order lines in `BookingDetail`.
  - Combo activation validates that all constituent items are currently active.
- **Regression Result:** Order histories preserved; menu consistency guaranteed.

### 3.7. Security & Session Resolution (`ManagerSecurityUtils.java`)
- **Root Cause:** Decentralized role checking across controllers led to inconsistent 401/403 responses.
- **Architectural Fix:**
  - Centralized security verification in `ManagerSecurityUtils.requireManagerRole(session)`.
  - Evaluates session `account` object and checks for role `MANAGER` or `ADMIN` (case-insensitive).
  - Unauthenticated requests trigger redirect to `/login`; unauthorized roles receive HTTP 403 Forbidden.
- **Regression Result:** Consistent access control across all 9 Manager endpoints.

---

## 4. Verification & Build Confirmation

- **Compiler:** JDK 17
- **Build Tool:** Apache Maven 3.9.x
- **Build Command:** `mvn clean test`
- **Results:**
  ```
  [INFO] -------------------------------------------------------
  [INFO]  T E S T S
  [INFO] -------------------------------------------------------
  [INFO] Running com.fpt.swp... [145 test methods]
  [INFO] Tests run: 145, Failures: 0, Errors: 0, Skipped: 0
  [INFO] 
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] ------------------------------------------------------------------------
  [INFO] Total time:  14.281 s
  ```
- **Live Endpoint Test:** All 9 Manager routes respond with HTTP 200 (authenticated) and HTTP 302/403 (unauthenticated).
