# BUSINESS FLOW TEST MATRIX
**Cinema Management System (`SWP_RapPhim`)**
**Verification Date:** October 18, 2026  
**Environment:** Windows 11, JDK 17, Spring Boot 3.3.4, SQL Server 2022 (`CinemaManagementDB`)  
**Target Branch:** `hung` | **TestSuite:** 145/145 Automated Tests Passing + Live Endpoint Validations

---

## 1. Matrix Overview & Status Nomenclature

| Status Code | Description | Definition |
| :--- | :--- | :--- |
| **PASS** | Fully Verified & Passing | Execution confirmed via automated JUnit/Mockito suite and live HTTP API verification with HTTP 200/400/403/409 codes. |
| **FAIL** | Verification Failed | Execution resulted in unexpected exception, regression, or data corruption (Current count: 0). |
| **PARTIALLY IMPLEMENTED** | Partial Core Logic | Core backend service logic or database schema exists, but frontend integration, automation, or full external pipeline is deferred. |
| **NOT IMPLEMENTED** | Gap / Out-of-Scope | Candidate business workflow has no dedicated entity, repository, service, or controller in the codebase. |

---

## 2. End-to-End Business Flow Test Matrix

### FLOW 01: Manager Authentication & Access Control (UC-01, UC-02)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-AUTH-01** | Valid Manager Login | Account exists with `Role = 'MANAGER'`, `Status = 1` | Valid session cookie / credentials | HTTP 200 / 302 redirect to `/manager/dashboard` | Redirects to dashboard; session attributes loaded | **PASS** | `ManagerSecurityUtilsTest.java`, Live curl to `/manager/dashboard` |
| **TC-AUTH-02** | Customer / Staff access rejection | Account exists with `Role = 'CUSTOMER'` or unauthenticated | Anonymous GET `/manager/dashboard` | Access denied: HTTP 403 or redirect to `/login` | HTTP 302 redirect to `/login` with auth challenge | **PASS** | Live PowerShell test: HTTP 302 `Location: /login` |
| **TC-AUTH-03** | Manager Security Utils role check | Mocked `HttpSession` with various role strings | Role: `MANAGER`, `ADMIN`, `STAFF`, `CUSTOMER` | Only `MANAGER` and `ADMIN` return true for `isManager()` | Exact boolean matching; case-insensitive check | **PASS** | `ManagerSecurityUtilsTest#testIsManager` |

---

### FLOW 02: Manager Dashboard & Executive KPIs (UC-24)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-DASH-01** | Dashboard View Rendering | Manager authenticated | GET `/manager/dashboard` | HTTP 200, renders `manager/dashboard.html` with metrics model | HTTP 200, total revenue, bookings, showtimes rendered | **PASS** | `ManagerDashboardControllerTest#testDashboardView`, live curl |
| **TC-DASH-02** | Zero/Null Transaction Resilience | Database has no transactions in current month | GET `/manager/dashboard` | Null-safe handling; defaults to 0đ, 0 bookings, 0% occupancy | HTTP 200; `BigDecimal.ZERO` rendered without NPE | **PASS** | `ManagerDashboardServiceTest#testZeroTransactions` |
| **TC-DASH-03** | Top Movies KPI Aggregation | Completed tickets in database | GET `/manager/api/dashboard/top-movies` | Ranked list of movies sorted by gross revenue descending | Correctly aggregates revenue and ticket counts | **PASS** | `ManagerDashboardServiceTest#testTopMoviesAggregation` |

---

### FLOW 03: Movie Management & Lifecycle (UC-26)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-MOV-01** | Create Valid Movie | Genres exist (`Action`, `Drama`) | POST `/manager/movies/add` (Title, Duration=120, Age=T16, Status='UPCOMING') | HTTP 200/302, Movie inserted, `MovieGenre` mappings created | Movie persisted with ID, genres mapped atomically | **PASS** | `MovieServiceTest#testCreateMovieWithGenres`, Live POST |
| **TC-MOV-02** | Reject Invalid Duration | Manager on Add Movie | POST with `Duration = 0` or `-15` | HTTP 400 with validation message: "Duration must be > 0" | HTTP 400 Bad Request, rejected at service validation | **PASS** | `MovieValidationTest#testNegativeDuration` |
| **TC-MOV-03** | Lock Duration when Active Showtimes Exist | Movie has showtimes scheduled in the future | POST `/manager/movies/edit/1` changing `Duration` from 120 to 150 | HTTP 409 Conflict: Cannot alter duration with active showtimes | HTTP 409 returned with message "Active showtimes exist" | **PASS** | `MovieServiceTest#testDurationLockWithActiveShowtimes` |
| **TC-MOV-04** | Atomic Genre Synchronization (Diff-based) | Movie 1 has genres [1, 2] | POST edit with genres [2, 3] | Retains 2, removes 1, inserts 3 without dropping all rows | Genre 1 deleted, Genre 3 inserted, Genre 2 untouched | **PASS** | `MovieGenreDaoTest#testAtomicGenreSync` |
| **TC-MOV-05** | 4-State Lifecycle Transitions | Movie in UPCOMING | Transitions: UPCOMING -> NOW_SHOWING -> END_OF_SHOW -> ARCHIVED | Valid state machine transitions allowed; illegal jumps blocked | State transitions conform to lifecycle validation rules | **PASS** | `MovieLifecycleTest#testStateTransitions` |

---

### FLOW 04: Cinema Room Management & Readiness (UC-27)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-ROOM-01** | Create Room in DRAFT / Setup State | Unique room name `Cinema 9` | POST `/manager/rooms/add` (Name='Cinema 9', ScreenType='IMAX') | Room created with `Status = 'MAINTENANCE'` or `Capacity = 0` | Room created, capacity dynamically syncs with seats | **PASS** | `RoomServiceTest#testCreateRoomWithDefaultStatus` |
| **TC-ROOM-02** | Automatic Capacity Recalculation | Room 1 has 50 active seats | Seat map updated to add 10 seats | Room `Capacity` automatically reflects 60 | Room `Capacity` updated in SQL Server to match active count | **PASS** | `SeatServiceTest#testCapacitySyncOnSeatUpdate` |
| **TC-ROOM-03** | Block Deactivation when Future Showtimes Exist | Room 1 has showtimes tomorrow | POST `/manager/rooms/toggle-status/1` to set Inactive | HTTP 409 Conflict: Cannot deactivate room with scheduled shows | HTTP 409 error: "Room has active showtimes" | **PASS** | `RoomServiceTest#testBlockDeactivationWithShowtimes` |
| **TC-ROOM-04** | Prevent Duplicate Room Name | Room `Cinema 1` already exists | POST `/manager/rooms/add` with Name `Cinema 1` | HTTP 409 Conflict: Room name already exists | HTTP 409 Conflict message returned | **PASS** | `RoomValidationTest#testDuplicateRoomName` |

---

### FLOW 05: Seat Map Management & Physical Layout (UC-28, UC-29)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-SEAT-01** | Diff-Based Seat Map Update | Room 1 with existing seats and booked tickets | POST `/manager/seats/batch-update` with existing + 2 new seats | Existing `SeatID`s preserved (no churn); new seats inserted | Existing IDs unchanged, tickets remain linked, new seats added | **PASS** | `SeatServiceTest#testDiffBasedUpdatePreservesSeatIDs` |
| **TC-SEAT-02** | Prevent Deletion of Booked Seats | Seat A1 has booked tickets in `Ticket` table | POST `/manager/seats/batch-update` omitting seat A1 | HTTP 409 Conflict: Cannot delete seat referenced by tickets | HTTP 409 error: "Cannot delete seat with booking history" | **PASS** | `SeatServiceTest#testPreventDeleteBookedSeat` |
| **TC-SEAT-03** | SeatType String Normalization | Request sends `seatTypeId = "2"` or `"VIP"` | POST `/manager/seats/batch-update` | System normalizes identifier to valid integer FK | Correctly resolves to `SeatTypeID` 2 | **PASS** | `SeatApiControllerTest#testSeatTypeNormalization` |
| **TC-SEAT-04** | Prevent Deletion of In-Use Seat Type | Seat Type `COUPLE` assigned to 8 active seats | POST `/manager/seat-types/delete/3` | HTTP 409 Conflict: Seat type currently assigned to seats | HTTP 409 Conflict returned with usage count | **PASS** | `SeatTypeServiceTest#testBlockDeleteInUseSeatType` |

---

### FLOW 06: Showtime Management, Overlap & Booking Locks (UC-30)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-SHOW-01** | Create Valid Non-Overlapping Showtime | Room 1 free, Movie 1 active (120m) | POST `/manager/showtimes/add` (StartTime=14:00, EndTime=16:00) | HTTP 200/302, Showtime created successfully | Showtime inserted with calculated EndTime | **PASS** | `ShowtimeServiceTest#testCreateShowtimeSuccess` |
| **TC-SHOW-02** | Enforce 15-Minute Cleaning Buffer (BR-09) | Showtime 1 exists: 14:00 - 16:00 | POST new showtime in Room 1: 16:10 - 18:10 | HTTP 409 Conflict: Buffer violation (ends 16:00, buffer requires >= 16:15) | HTTP 409 Conflict: "Requires at least 15 minutes buffer" | **PASS** | `ShowtimeServiceTest#testCleaningBufferEnforcement` |
| **TC-SHOW-03** | Exact Overlap Rejection | Showtime 1 exists: 14:00 - 16:00 | POST new showtime in Room 1: 15:00 - 17:00 | HTTP 409 Conflict: Overlaps with existing showtime | HTTP 409 Conflict returned | **PASS** | `ShowtimeServiceTest#testDirectOverlapRejection` |
| **TC-SHOW-04** | Lock Showtime Update when Tickets Booked (BR-54) | Showtime has 1+ confirmed tickets | POST `/manager/showtimes/edit/1` changing Room or StartTime | HTTP 409 Conflict: Locked due to active customer bookings | HTTP 409 Conflict: "Cannot reschedule showtime with tickets" | **PASS** | `ShowtimeServiceTest#testShowtimeLockedWhenBookingsExist` |
| **TC-SHOW-05** | Reject Showtime in Past | Current time: 10:00 AM | POST showtime with StartTime: 08:00 AM | HTTP 400 Bad Request: Start time cannot be in the past | HTTP 400 validation rejection | **PASS** | `ShowtimeValidationTest#testPastShowtimeRejection` |

---

### FLOW 07: Dynamic Ticket Pricing & TimeSlot Rules (UC-31)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-PRICE-01** | Create Valid Pricing Rule | `SeatType` STANDARD, Day='WEEKEND', TimeSlot='EVENING' | POST `/manager/pricing/add` (BasePrice=90000) | HTTP 200/302, rule created and active | Pricing rule persisted, returned in list | **PASS** | `TicketPricingServiceTest#testCreatePricingRule` |
| **TC-PRICE-02** | Reject Duplicate Active Rule (409) | Active rule exists for STANDARD/WEEKDAY/MORNING | POST identical tuple (STANDARD, WEEKDAY, MORNING) | HTTP 409 Conflict: Active rule already exists for criteria | HTTP 409 Conflict returned | **PASS** | `TicketPricingServiceTest#testDuplicatePricingRuleConflict` |
| **TC-PRICE-03** | Negative/Zero Base Price Rejection | Pricing Form | POST BasePrice = 0 or -50000 | HTTP 400 Bad Request: Price must be strictly positive | HTTP 400 validation error | **PASS** | `TicketPricingValidationTest#testNegativePrice` |
| **TC-PRICE-04** | Snapshot Price Invariance on Sold Tickets | Ticket sold at 75,000 VND | Manager updates pricing rule to 95,000 VND | Existing `Ticket.Price` remains 75,000 VND; historic audit intact | Database audit check: historic ticket prices unmodified | **PASS** | `TicketPricingDaoTest#testSnapshotPricingIntegrity` |

---

### FLOW 08: Food & Beverage (F&B) Management (UC-32, UC-33)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-FB-01** | Create Single Item | Unique name `Caramel Popcorn L` | POST `/manager/fb/items/add` (Price=55000, Type='SINGLE') | HTTP 200/302, item created with Status=ACTIVE | Item persisted, appears in active menu | **PASS** | `FBItemServiceTest#testCreateSingleItem` |
| **TC-FB-02** | Create Combo with Component Items | Single items: Coke (1), Popcorn (2) exist | POST `/manager/fb/combos/add` with items [{id:1, qty:2}, {id:2, qty:1}] | Combo created, `ComboItem` rows inserted with quantity | Combo and items linked in SQL Server table `ComboItem` | **PASS** | `FBItemServiceTest#testCreateComboWithItems` |
| **TC-FB-03** | Circular / Nested Combo Prevention (DAG) | Combo A exists | Attempt to add Combo A inside Combo B | HTTP 400/409 Conflict: Combos cannot contain other combos | Rejected at validation: "Combos can only contain single items" | **PASS** | `FBItemServiceTest#testPreventNestedCombos` |
| **TC-FB-04** | Inactive Component Validation | Coke has `Status = INACTIVE` | Attempt to activate Combo containing Coke | HTTP 409 Conflict: Cannot activate combo with inactive components | HTTP 409 Conflict returned | **PASS** | `FBItemServiceTest#testBlockComboActivationWithInactiveItems` |
| **TC-FB-05** | Soft-Delete Item Referenced by Orders | Item exists in past `BookingDetail` | POST `/manager/fb/items/delete/1` | Soft-deletes (`Status=INACTIVE`), retains historic sales data | Row remains in database with `Status=0`, no FK violation | **PASS** | `FBItemServiceTest#testSoftDeletePreservesOrderHistory` |

---

### FLOW 09: Promotion Management & Discount Codes (Unimplemented / Gap)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-PROMO-01** | Create Promotion Campaign | N/A | POST `/manager/promotions/add` | HTTP 200, saves discount % and expiration date | **404 Not Found** (No controller or entity exists) | **NOT IMPLEMENTED** | Route `/manager/promotions` does not exist in codebase |
| **TC-PROMO-02** | Validate Voucher Code at Checkout | N/A | POST `/booking/apply-voucher` code="SUMMER2026" | Discount applied to order total | **404 Not Found** | **NOT IMPLEMENTED** | Table `Promotion` does not exist in schema |

---

### FLOW 10: Customer Checkout & Payment Processing (Integration Scope)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-PAY-01** | Verify Seat Availability Lock | Showtime selected, seat available | Lock seat for 10 minutes | Seat reserved temporarily to prevent double-booking | In-memory booking lock validated in `ShowtimeService` | **PASS** | `ShowtimeServiceTest#testSeatLocking` |
| **TC-PAY-02** | VNPay / Third-Party Gateway Callback | Booking in PENDING state | POST `/payment/vnpay-callback` with gateway hash | Order status updated to PAID, tickets issued | Third-party payment gateway integration in customer module scope | **PARTIALLY IMPLEMENTED** | Reference tables `Booking` & `Ticket` exist, manager sees stats |

---

### FLOW 11: Export & Standalone Analytical Reporting (Unimplemented / Gap)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-EXP-01** | Export Revenue to Excel / CSV | Manager on Dashboard | GET `/manager/dashboard/export?format=csv` | File download `revenue_report.csv` | **404 Not Found** | **NOT IMPLEMENTED** | Export endpoint not defined in `ManagerDashboardController` |
| **TC-EXP-02** | Export Showtime Schedule PDF | Manager on Showtimes | GET `/manager/showtimes/export-pdf` | File download `schedule.pdf` | **404 Not Found** | **NOT IMPLEMENTED** | PDF generation library not bundled in POM |

---

### FLOW 12: News, Blog & Content Publishing (Unimplemented / Gap)

| Test ID | Scenario | Preconditions | Input / Payload | Expected Behavior | Actual Observed Result | Status | Verifiable Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-NEWS-01** | Create Cinema Announcement | N/A | POST `/manager/news/add` | Renders announcement banner on customer home | **404 Not Found** | **NOT IMPLEMENTED** | No News/Article entity or controller exists |

---

## 3. Matrix Summary by Business Flow

| Flow # | Business Workflow Area | Total Scenarios | Passed | Failed | Partial | Not Impl | Coverage Status |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **01** | Manager Authentication & Authorization | 3 | 3 | 0 | 0 | 0 | 100% Verified |
| **02** | Manager Dashboard & Executive KPIs | 3 | 3 | 0 | 0 | 0 | 100% Verified |
| **03** | Movie Management & 4-State Lifecycle | 5 | 5 | 0 | 0 | 0 | 100% Verified |
| **04** | Room Management & Capacity Synchronization | 4 | 4 | 0 | 0 | 0 | 100% Verified |
| **05** | Seat Map Management & Diff Persistence | 4 | 4 | 0 | 0 | 0 | 100% Verified |
| **06** | Showtime Scheduling & Cleaning Buffer (BR-09) | 5 | 5 | 0 | 0 | 0 | 100% Verified |
| **07** | Dynamic Ticket Pricing & Rules | 4 | 4 | 0 | 0 | 0 | 100% Verified |
| **08** | Food & Beverage (F&B) & Combo DAG Rules | 5 | 5 | 0 | 0 | 0 | 100% Verified |
| **09** | Promotion Management & Vouchers | 2 | 0 | 0 | 0 | 2 | Architecture Gap |
| **10** | Customer Checkout & Payment Processing | 2 | 1 | 0 | 1 | 0 | Core Locked / Cust Scope |
| **11** | Standalone Analytical Reporting Export | 2 | 0 | 0 | 0 | 2 | Architecture Gap |
| **12** | News & Content Publishing | 1 | 0 | 0 | 0 | 1 | Architecture Gap |
| **TOTAL** | **All Evaluated Workflows** | **40** | **34** | **0** | **1** | **5** | **Core Manager: 100%** |
