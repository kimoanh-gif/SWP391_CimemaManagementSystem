# DEFECT FIX SUMMARY – CINEORA

**Author:** Nguyễn Hoàng Hưng (`hungnhhe186547@fpt.edu.vn`)  
**Assigned Module Ownership:** Manager Module  
**Date:** 07/11/2026  

---

### Defect 1: ST_001
- **Defect ID:** DEF-ST-001
- **Test Case ID:** ST_001
- **Description:** Dashboard displays `Tickets sold = 0` and `No bookings yet` even when tickets and revenue have been recorded.
- **Root Cause:** In `ManagerDashboardService`, the ticket count query joined `dbo.Ticket t` on `t.BookingID = b.BookingID`. In the Cineora schema, `dbo.Ticket` has no `BookingID` column (only `BookingSeatID`). SQL Server threw Error 207 `Invalid column name 'BookingID'`, which was caught by the fallback catch block, wiping all stats to 0.
- **Fix Description:** Replaced the query to count from `dbo.BookingSeat` joined with `dbo.Booking` on `BookingID` (with fallback to `Ticket JOIN BookingSeat`), added proper SLF4J logging, and computed ticket and F&B revenue accurately.
- **Fixed Files:**
  - `src/main/java/com/cinema/service/ManagerDashboardService.java`
- **Retest Result:** Passed (`ManagerDashboardServiceTest`, Integration test, live dashboard verification).
- **Final Status:** **FIXED**

---

### Defect 2: ST_014
- **Defect ID:** DEF-ST-014
- **Test Case ID:** ST_014
- **Description:** Manager can change room capacity from 20 to 30 while the Seat Map still contains only 20 seats.
- **Root Cause:** `RoomService.update()` validated rows x seats per row against the form capacity, but failed to cross-check whether seats already exist in `dbo.Seat` via `SeatDao`, allowing desynchronization between `Room.Capacity` and actual physical seats.
- **Fix Description:** Injected and invoked `seatDao.findByRoomId()` in `RoomService.update()`. If configured seats exist, enforced that `form.getCapacity()` must match the configured seat count. Additionally guarded against altering capacity or dimensions when upcoming showtimes exist.
- **Fixed Files:**
  - `src/main/java/com/cinema/service/RoomService.java`
  - `src/test/java/com/cinema/service/RoomServiceTest.java`
- **Retest Result:** Passed (16/16 tests in `RoomServiceTest` pass, including new boundary and mismatch test cases).
- **Final Status:** **FIXED**

---

### Defect 3: ST_017A
- **Defect ID:** DEF-ST-017A
- **Test Case ID:** ST_017 (Sub-case A: Seat Type Validation)
- **Description:** Screen 57 displays generic message `"Invalid seat type data."` when invalid multipliers or hex colors are submitted.
- **Root Cause:** `SeatTypeController.saveSeatType` and `updateSeatType` discarded field-level errors in `BindingResult` and flashed a hardcoded string.
- **Fix Description:** Extracted the first specific field error from `bindingResult.getFieldErrors()` to display actionable messages (e.g. `"Minimum price multiplier is 0.50"`, `"Maximum price multiplier is 3.00"`, `"Color code must be in HEX format (e.g. #3B82F6)"`).
- **Fixed Files:**
  - `src/main/java/com/cinema/controller/SeatTypeController.java`
- **Retest Result:** Passed (`SeatTypeControllerTest`).
- **Final Status:** **FIXED**

---

### Defect 4: ST_017B
- **Defect ID:** DEF-ST-017B
- **Test Case ID:** ST_017 (Sub-case B: Ticket Pricing Validation)
- **Description:** Screen 60 displays generic message `"Invalid pricing rule information. Please check again."` when negative base price is submitted.
- **Root Cause:** `TicketPricingController.savePricingRule` discarded field-level error messages in `BindingResult`.
- **Fix Description:** Extracted the specific field error from `bindingResult.getFieldErrors()` (e.g., displaying `"Base ticket price cannot be negative"`).
- **Fixed Files:**
  - `src/main/java/com/cinema/controller/TicketPricingController.java`
- **Retest Result:** Passed (`TicketPricingServiceTest`).
- **Final Status:** **FIXED**

---

### Defect 5: ST_017C
- **Defect ID:** DEF-ST-017C
- **Test Case ID:** ST_017 (Sub-case C: F&B Validation)
- **Description:** Screen 61 displays generic message `"Invalid F&B product information. Please check again."` when cost price exceeds selling price or negative values are submitted.
- **Root Cause:** `FBItemController.saveFBItem` discarded errors from `BindingResult` and returned a hardcoded generic string.
- **Fix Description:** Extracted specific validation messages from `bindingResult.getAllErrors()` (e.g., displaying `"Cost price cannot exceed selling price"`, `"Selling price cannot be empty or negative"`).
- **Fixed Files:**
  - `src/main/java/com/cinema/controller/FBItemController.java`
- **Retest Result:** Passed (`FBItemServiceTest`).
- **Final Status:** **FIXED**

---

### Defect 6: ST_025
- **Defect ID:** DEF-ST-025
- **Test Case ID:** ST_025
- **Description:** Showtime can be created before movie release date (e.g., showtime on 15/11/2026 for a movie releasing on 20/11/2026 was accepted).
- **Root Cause:** `ShowtimeService.validateShowtime()` checked movie status (`Now Showing` / `Upcoming`) and tomorrow rule (BR-55), but failed to compare `startTime.toLocalDate()` with `movie.getReleaseDate()` and `movie.getEndDate()`.
- **Fix Description:** Added validation in `validateShowtime()` throwing `InvalidShowtimeException` if showtime date is before `movie.getReleaseDate()` or after `movie.getEndDate()`.
- **Fixed Files:**
  - `src/main/java/com/cinema/service/ShowtimeService.java`
  - `src/test/java/com/cinema/service/ShowtimeServiceTest.java`
- **Retest Result:** Passed (13/13 tests in `ShowtimeServiceTest` pass, including `testCreateShowtime_RejectBeforeMovieReleaseDate_ST025`).
- **Final Status:** **FIXED**

---

### Defect 7: ST_035
- **Defect ID:** DEF-ST-035
- **Test Case ID:** ST_035
- **Description:** Movie Performance report for 07/11/2026 displays 20 showtimes for Bambi instead of the expected 3 showtimes.
- **Root Cause:** In `RevenueReportDao.getMovieRevenue()`, `dbo.Showtime st` was joined unconditionally on `st.MovieID = m.MovieID` without filtering by the user-selected date range. As a result, all historical and future showtimes were aggregated into `ShowtimesCount`.
- **Fix Description:** Changed `ShowtimesCount` aggregation to conditionally count showtimes within the specified date range: `COUNT(DISTINCT CASE WHEN CAST(st.StartTime AS DATE) BETWEEN ? AND ? THEN st.ShowtimeID END)`, keeping all revenue calculations unaltered.
- **Fixed Files:**
  - `src/main/java/com/cinema/dao/RevenueReportDao.java`
- **Retest Result:** Passed (`RevenueReportServiceTest`, verified query in SQL Server).
- **Final Status:** **FIXED**

---

### Defect 8: ST_038
- **Defect ID:** DEF-ST-038
- **Test Case ID:** ST_038
- **Description:** Exported CSV header displays legacy brand `CINEMA REVENUE REPORT - ROYALLY CINEMA` instead of `CINEMA REVENUE REPORT - CINEORA`, and movie code is exported as raw integer (`20`) instead of matching UI format (`MOV-20`).
- **Root Cause:** In `RevenueReportService.generateCsvReport()`, the header was hardcoded with `"ROYALLY CINEMA"` and movie rows were formatted with `%d` instead of `"MOV-%d"`.
- **Fix Description:** Updated CSV header to `"CINEMA REVENUE REPORT - CINEORA"` and movie code formatting to `"MOV-%d"`.
- **Fixed Files:**
  - `src/main/java/com/cinema/service/RevenueReportService.java`
  - `src/test/java/com/cinema/service/RevenueReportServiceTest.java`
- **Retest Result:** Passed (7/7 tests in `RevenueReportServiceTest` pass with updated header and movie code assertions).
- **Final Status:** **FIXED**

---

### Defect 9: ST_040
- **Defect ID:** DEF-ST-040
- **Test Case ID:** ST_040
- **Description:** Test case marked Passed, but test execution was aborted before full seat-hold expiration period elapsed.
- **Root Cause:** The code in `ReleaseExpiredSeatReservationsJob.java` and `CustomerBookingDAO.java` correctly implements SRS 6.1 (JOB-01: 10-minute hold limit, 30-second scheduler). The defect was a QA execution defect: the tester did not wait for the 10-minute hold window before inspecting the database. Furthermore, this component belongs to the Customer Booking module (`Sonha0511`).
- **Fix Description:** No source code modifications authorized or required. Retest procedure provided in `RETEST_GUIDE.md`.
- **Fixed Files:** None (Ownership boundary respected).
- **Retest Result:** Unit tests pass (`ReleaseExpiredSeatReservationsJobTest`). Manual retest guide provided.
- **Final Status:** **TEST EVIDENCE INCOMPLETE – RETEST REQUIRED**
