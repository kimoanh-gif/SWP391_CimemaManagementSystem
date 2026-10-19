# 03 — Business Flow Extraction

This document reconstructs the complete business flows extracted directly from the executable codebase (Controllers, Services, DAOs, and UI scripts).

---

## FLOW-01: Manager Dashboard Monitoring & KPI Aggregation
- **Flow ID:** `BF-MGR-01`
- **Actor:** Cinema Manager (`MANAGER` or `ADMIN`)
- **Preconditions:** System database is running with operational tables (`Movie`, `Room`, `Showtime`, `TicketPricing`, `FBItem`). Request passes `ManagerSecurityUtils`.
- **Trigger:** Manager accesses `GET /manager` or `GET /manager/dashboard`.
- **Main Success Scenario:**
  1. Controller validates manager authorization.
  2. `ManagerDashboardService` aggregates counts and averages from all active domain services.
  3. Controller injects `stats`, `todayShowtimes`, `activeMovies`, `rooms`, `userRole`, and localized `currentDateStr` into Thymeleaf Model.
  4. View renders KPI cards, quick action tiles, live showtimes table, and room readiness list.
- **Alternative Scenarios:**
  - REST client calls `GET /api/manager/dashboard/stats`: Returns JSON `ApiResponse` containing the `ManagerDashboardStats` payload.
- **Exception Scenarios:**
  - Non-manager role (e.g. `role=CUSTOMER`): Throws `UnauthorizedManagerException` (HTTP 403 Forbidden).
- **Validation Rules:**
  - Metrics must reflect real database records.
- **Expected Result:** Manager views accurate operational overview without hardcoded fake figures.
- **Database Changes:** None (Read-only query).
- **Related Endpoints:** `GET /manager`, `GET /manager/dashboard`, `GET /api/manager/dashboard/stats`.
- **Implementation Gaps:** If a table has no records, fallback defaults (e.g., base ticket price default `85000`) are used.

---

## FLOW-02: Movie Creation with Multi-Genre Association
- **Flow ID:** `BF-MOV-01`
- **Actor:** Cinema Manager
- **Preconditions:** At least one active Genre exists in the database.
- **Trigger:** Manager submits Movie creation form from `/manager/movies` or sends `POST /api/manager/movies`.
- **Main Success Scenario:**
  1. Form is submitted with Title, Duration (>0), AgeRating, ReleaseDate, Status (`Upcoming` or `Now Showing`), and array of `genreIds`.
  2. `MovieService.validateMovieForm()` ensures Title is not empty, Duration is positive, Status is valid.
  3. `MovieDao.insert()` inserts record into `Movie` table, obtaining generated `MovieID`.
  4. `MovieDao.replaceMovieGenres()` creates composite entries in `MovieGenre` table, ensuring no duplicate mappings (BR-44).
  5. UI redirects to `/manager/movies` with success flash message.
- **Exception Scenarios:**
  - Empty title or duration <= 0: Field validation error or `IllegalArgumentException`.
  - Invalid status: `InvalidMovieStatusException`.
- **Validation Rules:**
  - Status must be in `['Upcoming', 'Now Showing', 'Ended', 'Archived']`.
  - Genre IDs must correspond to existing records.
- **Database Changes:** `INSERT INTO Movie`, `INSERT INTO MovieGenre`.
- **Related Endpoints:** `POST /manager/movies/save`, `POST /api/manager/movies`.
- **Implementation Gaps:** Poster/trailer upload uses URL strings rather than physical file multipart upload.

---

## FLOW-03: Movie Deletion / Deactivation Guarded by Showtimes (BR-37)
- **Flow ID:** `BF-MOV-02`
- **Actor:** Cinema Manager
- **Preconditions:** Movie exists in system.
- **Trigger:** Manager clicks delete on a movie or sends `DELETE /api/manager/movies/{id}`.
- **Main Success Scenario (Safe Movie):**
  1. System checks `movieDao.hasActiveShowtimes(movieId)`.
  2. If count is 0, associated `MovieGenre` mappings are cleared and `Movie` record is deleted.
- **Exception Scenario (Movie with Active Showtimes):**
  1. System detects active showtimes (`StartTime >= NOW`).
  2. `MovieService` throws `MovieHasActiveShowtimesException` (BR-37).
  3. Deletion is rejected; error message displayed to user.
- **Database Changes:** `DELETE FROM MovieGenre`, `DELETE FROM Movie` (when safe).
- **Related Endpoints:** `POST /manager/movies/delete/{id}`, `DELETE /api/manager/movies/{id}`.
- **Implementation Gaps:** None.

---

## FLOW-04: Cinema Room Maintenance & Deactivation Guard (BR-52)
- **Flow ID:** `BF-ROOM-01`
- **Actor:** Cinema Manager
- **Preconditions:** Room exists.
- **Trigger:** Manager attempts to deactivate a room via `POST /manager/rooms/deactivate/{id}`.
- **Main Success Scenario:**
  1. System queries `roomDao.hasUpcomingShowtimes(roomId)`.
  2. If no future showtimes exist, room status is transitioned to `Maintenance`.
- **Exception Scenario:**
  1. Room is scheduled for one or more upcoming showtimes (`StartTime > GETDATE()`).
  2. System throws `RoomHasActiveShowtimesException` (BR-52).
  3. Operation is aborted, status remains unchanged.
- **Database Changes:** `UPDATE Room SET Status = 'Maintenance' WHERE RoomID = ?`.
- **Related Endpoints:** `POST /manager/rooms/deactivate/{id}`, `PATCH /api/manager/rooms/{id}/status`.
- **Implementation Gaps:** None.

---

## FLOW-05: Seat Layout Visual Customization & Default Generation
- **Flow ID:** `BF-SEAT-01`
- **Actor:** Cinema Manager
- **Preconditions:** Room exists with configured `TotalRows` and `SeatsPerRow`.
- **Trigger:** Manager selects a Room on `/manager/seats` and clicks "Khởi Tạo Sơ Đồ Chuẩn" or modifies individual seats.
- **Main Success Scenario (Auto-Generate):**
  1. Manager triggers `POST /manager/seats/generate-default?roomId={id}`.
  2. `SeatService.generateDefaultLayout()` creates grid:
     - Row A: `Front`
     - Central rows: `VIP`
     - Last row: `Couple`
     - Other rows: `Standard`
  3. System validates capacity constraints (`seats.size() <= room.getCapacity()`).
  4. System clears old room seats and executes batch insert with `(RoomID, RowLabel, SeatNumber, GridRow, GridColumn, Status)`.
- **Alternative Scenario (Update Individual Seat):**
  - Manager clicks seat in UI modal, changes seat type or toggles status to `Maintenance` via `POST /manager/seats/update-seat`.
- **Exception Scenarios:**
  - Duplicate seat position `(RoomID, RowLabel, SeatNumber)`: Throws `InvalidSeatLayoutException`.
  - Exceeds room capacity: Throws `InvalidSeatLayoutException`.
- **Database Changes:** `DELETE FROM Seat WHERE RoomID = ?`, `INSERT INTO Seat`.
- **Related Endpoints:** `POST /manager/seats/generate-default`, `POST /manager/seats/update-seat`, `PUT /api/manager/seats/room/{roomId}/layout`.
- **Implementation Gaps:** Currently replacing layout deletes existing seats; if historical tickets reference `SeatID` directly via foreign key, deletion would fail unless CASCADE/RESTRICT is properly handled.

---

## FLOW-06: Showtime Scheduling with Cleaning Buffer & Overlap Validation (BR-07, BR-08, BR-09, BR-55)
- **Flow ID:** `BF-SHW-01`
- **Actor:** Cinema Manager
- **Preconditions:** Movie and Room are active; movie status is `Upcoming` or `Now Showing` (BR-07); room status is `Ready` or `Screening` (BR-08).
- **Trigger:** Manager fills showtime form and submits `POST /manager/showtimes/save`.
- **Main Success Scenario:**
  1. Frontend / backend computes `EndTime = StartTime + Movie.Duration` (BR-55).
  2. `StartTime` verified to be in the future (`StartTime > NOW`).
  3. Buffer time added: `BufferEnd = EndTime + 15 minutes` (BR-09).
  4. Backend runs overlap query on target room:
     `newStart < existingEnd AND bufferEnd > existingStart`
  5. If no overlapping records exist, record inserted into `Showtime` table with status `Scheduled`.
- **Exception Scenarios:**
  - Movie is `Ended` or `Archived`: Throws `InvalidShowtimeException` (BR-07).
  - Room is in `Maintenance` or `Cleaning`: Throws `InvalidShowtimeException` (BR-08).
  - Scheduling conflict detected: Throws `ShowtimeConflictException` with conflicting movie/time details (BR-09).
- **Database Changes:** `INSERT INTO Showtime`.
- **Related Endpoints:** `POST /manager/showtimes/save`, `POST /api/manager/showtimes`.
- **Implementation Gaps:** None.

---

## FLOW-07: Dynamic Ticket Pricing & Matrix Evaluation (BR-50, BR-51)
- **Flow ID:** `BF-PRC-01`
- **Actor:** Cinema Manager
- **Preconditions:** Base ticket pricing rules and seat types configured.
- **Trigger:** Manager accesses `/manager/pricing` or updates multipliers/base prices.
- **Main Success Scenario:**
  1. `TicketPricingService.generatePricingMatrix()` retrieves all active pricing rules and active seat types.
  2. For every rule and seat type, evaluates formula:
     $$\text{Calculated Price} = \text{round}\Big((\text{BasePrice} + \text{FormatSurcharge}) \times \text{PriceMultiplier}, -3\Big)$$
     using `RoundingMode.HALF_UP` to nearest 1,000 VND.
  3. Manager can update SeatType multiplier (`0.50` to `3.00`) via `POST /manager/seat-types/multiplier`.
  4. Matrix updates immediately on screen.
- **Exception Scenarios:**
  - Multiplier outside `[0.50, 3.00]`: Throws `InvalidPricingConfigurationException`.
  - Negative base price or format surcharge: Throws `InvalidPricingConfigurationException`.
- **Database Changes:** `UPDATE SeatType SET PriceMultiplier = ?`, `INSERT / UPDATE TicketPricing`.
- **Related Endpoints:** `POST /manager/pricing/save`, `POST /manager/seat-types/multiplier`, `GET /api/manager/pricing/matrix`.
- **Implementation Gaps:** Snapshot preservation (BR-51) is enforced by design; tickets store paid price rather than dynamically re-evaluating historical pricing.

---

## FLOW-08: Food & Beverage Catalog & Combo Assembly (BR-15, BR-51)
- **Flow ID:** `BF-FB-01`
- **Actor:** Cinema Manager
- **Preconditions:** None.
- **Trigger:** Manager adds food/drink or creates a Combo item with child items.
- **Main Success Scenario:**
  1. Manager enters `ItemCode`, `ItemName`, `Category` (`Combo`, `Food`, `Drink`, `Snack`), `SellingPrice`, `CostPrice`.
  2. For `Combo`, manager selects component item IDs and quantities.
  3. `FBItemService.createItem()` inserts into `FBItem`, then saves component lines into `ComboItem`.
- **Exception Scenarios:**
  - Duplicate ItemCode: Throws `InvalidFBItemException`.
  - Negative price: Throws `InvalidFBItemException`.
- **Database Changes:** `INSERT INTO FBItem`, `INSERT INTO ComboItem`.
- **Related Endpoints:** `POST /manager/fb/save`, `POST /api/manager/fb`.
- **Implementation Gaps:** None.

---

## FLOW-09: Customer Profile View & Update (Screen 08)
- **Flow ID:** `BF-CUST-01`
- **Actor:** Customer
- **Preconditions:** `Account` and `Customer` rows exist in database with matching `AccountID`.
- **Trigger:** User accesses `GET /customer/profile?accountId={id}`.
- **Main Success Scenario:**
  1. System joins `Account`, `Customer`, and `MembershipRank`.
  2. User views full name, email, phone, date of birth, address, membership tier, and loyalty points.
  3. User edits address or phone via `POST /customer/profile/edit?accountId={id}`.
- **Security & Architectural Vulnerabilities:**
  - **Critical Flaw:** No authentication or ownership verification. Anyone can supply any `accountId` parameter to read and alter other users' private information.
  - **Dependency Gap:** Requires tables `Account`, `Customer`, and `MembershipRank` which are not created in the migration scripts in `database/`.
