# 02 — Implemented Functionality Inventory

This document provides a feature-by-feature technical audit of all modules identified across the codebase. Each feature is assessed based on actual source code evidence.

Status classification strictly follows:
- **Fully Implemented:** Complete UI, MVC and/or REST controllers, domain services, DAO persistence, validation rules, and error handling exist and are operational.
- **Partially Implemented:** Some layers exist (e.g., UI exists but lacks full auth or persistence, or backend exists without complete frontend integration).
- **Not Implemented:** Mentioned in SRS or navigation menus, but no operational controllers or persistent entities exist.
- **Cannot Determine:** Incomplete or ambiguous implementation whose end-to-end viability cannot be verified without external dependencies.

---

## Module 1: Manager Dashboard (Screen 53 / UC-25)
- **Feature Name:** Operational Overview & Cinema Navigation Hub
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/dashboard.html`, `static/css/manager-dashboard.css`, `static/js/manager-dashboard.js`
- **Backend Controllers & Services:**
  - `ManagerDashboardController.java` (`/manager`, `/manager/dashboard`)
  - `ManagerDashboardApiController.java` (`/api/manager/dashboard/stats`)
  - `ManagerDashboardService.java`
- **API Endpoints:**
  - `GET /manager`, `GET /manager/dashboard`
  - `GET /api/manager/dashboard/stats`
- **Database Entities:** Aggregates live data from `Movie`, `Genre`, `Room`, `Showtime`, `SeatType`, `TicketPricing`, `FBItem`.
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Modules 2, 3, 4, 5, 6, 7, 8.
- **Source Code Evidence:**
  - Real calculations via `ManagerDashboardService.getDashboardStats()` querying actual DAOs.
  - Role enforcement using `ManagerSecurityUtils.verifyManagerAccess(request)`.
  - Tested in `ManagerDashboardServiceTest.java`, `ManagerDashboardControllerTest.java`, `ManagerDashboardApiControllerTest.java`.

---

## Module 2: Movie Management (Screen 54 / UC-26)
- **Feature Name:** Cinema Movie Catalog Management
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/movies.html`, `static/css/manager-movies.css`, `static/js/manager-movies.js`
- **Backend Controllers & Services:**
  - `MovieController.java` (`/manager/movies`)
  - `MovieApiController.java` (`/api/manager/movies`)
  - `MovieService.java`
  - `MovieDao.java`
- **API Endpoints:**
  - `GET /manager/movies`
  - `POST /manager/movies/save`
  - `POST /manager/movies/status/{id}`
  - `POST /manager/movies/delete/{id}`
  - `GET /api/manager/movies`
  - `GET /api/manager/movies/{id}`
  - `POST /api/manager/movies`
  - `PUT /api/manager/movies/{id}`
  - `PATCH /api/manager/movies/{id}/status`
  - `DELETE /api/manager/movies/{id}`
- **Database Entities:** `Movie` (table `Movie`), `MovieGenre` (table `MovieGenre`), `Genre`.
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Genre Management (Module 3) for genre assignment, Showtime Management (Module 7) for foreign-key constraints.
- **Source Code Evidence:**
  - Enforces BR-07 (Movie status validation: Upcoming, Now Showing, Ended, Archived).
  - Enforces BR-37 (Cannot delete or deactivate a movie currently scheduled in active showtimes via `MovieHasActiveShowtimesException`).
  - Enforces BR-44 (Unique genre mapping per movie in `MovieDao.replaceMovieGenres`).
  - Unit tests in `MovieServiceTest.java`, `MovieControllerTest.java`, `MovieApiControllerTest.java`.

---

## Module 3: Genre Management (Screen 55 / UC-26)
- **Feature Name:** Movie Classification & Genre Catalog
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/genres.html`, `static/css/manager-genres.css`, `static/js/manager-genres.js`
- **Backend Controllers & Services:**
  - `GenreController.java` (`/manager/genres`)
  - `GenreApiController.java` (`/api/manager/genres`)
  - `GenreService.java`
  - `GenreDao.java`
- **API Endpoints:**
  - `GET /manager/genres`
  - `POST /manager/genres/save`
  - `POST /manager/genres/status/{id}`
  - `POST /manager/genres/delete/{id}`
  - `GET /api/manager/genres`
  - `GET /api/manager/genres/{id}`
  - `POST /api/manager/genres`
  - `PUT /api/manager/genres/{id}`
  - `PATCH /api/manager/genres/{id}/status`
  - `DELETE /api/manager/genres/{id}`
- **Database Entities:** `Genre` (table `Genre`), `MovieGenre`.
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Movie Management (Module 2).
- **Source Code Evidence:**
  - Enforces case-insensitive Genre name uniqueness in `GenreDao.isNameDuplicate()`.
  - Prevents physical deletion if genre is associated with active movies (`GenreAssociationException`).
  - Tested in `GenreServiceTest.java`, `GenreControllerTest.java`, `GenreApiControllerTest.java`.

---

## Module 4: Cinema Room Management (Screen 56 / UC-27)
- **Feature Name:** Screening Hall Infrastructure & Capacity Configuration
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/rooms.html`, `static/css/manager-rooms.css`, `static/js/manager-rooms.js`
- **Backend Controllers & Services:**
  - `RoomController.java` (`/manager/rooms`)
  - `RoomApiController.java` (`/api/manager/rooms`)
  - `RoomService.java`
  - `RoomDao.java`
- **API Endpoints:**
  - `GET /manager/rooms`
  - `POST /manager/rooms/save`
  - `POST /manager/rooms/deactivate/{id}`
  - `POST /manager/rooms/delete/{id}`
  - `GET /api/manager/rooms`
  - `GET /api/manager/rooms/{id}`
  - `POST /api/manager/rooms`
  - `PUT /api/manager/rooms/{id}`
  - `PATCH /api/manager/rooms/{id}/status`
  - `DELETE /api/manager/rooms/{id}`
- **Database Entities:** `Room` (table `Room`), `Seat` (table `Seat`).
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Showtime Management (Module 7), Seat Management (Module 6).
- **Source Code Evidence:**
  - Enforces BR-08: Only rooms in Ready/Screening status are eligible for showtimes.
  - Enforces BR-52: Cannot deactivate or put a room into maintenance if upcoming future showtimes exist (`RoomHasActiveShowtimesException`).
  - Tested in `RoomServiceTest.java`.

---

## Module 5: Seat Type Management (Screen 57 / UC-28)
- **Feature Name:** Seat Classification & Price Multiplier Configuration
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/seat-types.html`, `static/css/manager-seat-types.css`, `static/js/manager-seat-types.js`
- **Backend Controllers & Services:**
  - `SeatTypeController.java` (`/manager/seat-types`)
  - `SeatTypeApiController.java` (`/api/manager/seat-types`)
  - `TicketPricingService.java`
  - `SeatTypeDao.java`
- **API Endpoints:**
  - `GET /manager/seat-types`
  - `POST /manager/seat-types/save`
  - `POST /manager/seat-types/multiplier`
  - `POST /manager/seat-types/update/{id}`
  - `POST /manager/seat-types/status/{id}`
  - `GET /api/manager/seat-types`
  - `GET /api/manager/seat-types/{id}`
  - `POST /api/manager/seat-types`
  - `PUT /api/manager/seat-types/{id}/multiplier`
  - `PUT /api/manager/seat-types/{id}`
  - `PUT /api/manager/seat-types/{id}/status`
- **Database Entities:** `SeatType` (table `SeatType`).
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Ticket Pricing Management (Module 8), Seat Layout Management (Module 6).
- **Source Code Evidence:**
  - Multiplier validation: Must be between `0.50` and `3.00` per SRS rules (`TicketPricingService.updateSeatTypeMultiplier`).
  - Uniqueness of `TypeCode` and `TypeName`.
  - Tested in `SeatTypeControllerTest.java`, `SeatTypeApiControllerTest.java`.

---

## Module 6: Seat Layout Management (Screen 58 / UC-29)
- **Feature Name:** Room Grid & Physical Seat Layout Visualizer
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/seats.html`, `static/css/manager-seats.css`, `static/js/manager-seats.js`
- **Backend Controllers & Services:**
  - `SeatController.java` (`/manager/seats`)
  - `SeatApiController.java` (`/api/manager/seats`)
  - `SeatService.java`
  - `SeatDao.java`
- **API Endpoints:**
  - `GET /manager/seats`
  - `POST /manager/seats/generate-default`
  - `POST /manager/seats/update-seat`
  - `GET /api/manager/seats/room/{roomId}`
  - `GET /api/manager/seats/{id}`
  - `PUT /api/manager/seats/room/{roomId}/layout`
  - `POST /api/manager/seats/room/{roomId}/generate-default`
  - `PATCH /api/manager/seats/{id}`
- **Database Entities:** `Seat` (table `Seat`), `Room`, `SeatType`.
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Cinema Room Management (Module 4), Seat Type Management (Module 5).
- **Source Code Evidence:**
  - Capacity boundary enforcement (`seats.size() <= room.getCapacity()`).
  - Duplicate seat position prevention (`UQ_Seat_Room_Position`: `RoomID + RowLabel + SeatNumber`).
  - Visual seat status configuration (Active, Maintenance, Unavailable).
  - Tested in `SeatServiceTest.java`, `SeatControllerTest.java`, `SeatApiControllerTest.java`.

---

## Module 7: Showtime Management (Screen 59 / UC-30)
- **Feature Name:** Movie Screening Schedule & Room Conflict Prevention
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/showtimes.html`, `static/css/manager-showtimes.css`, `static/js/manager-showtimes.js`
- **Backend Controllers & Services:**
  - `ShowtimeController.java` (`/manager/showtimes`)
  - `ShowtimeApiController.java` (`/api/manager/showtimes`)
  - `ShowtimeService.java`
  - `ShowtimeDao.java`
- **API Endpoints:**
  - `GET /manager/showtimes`
  - `POST /manager/showtimes/save`
  - `POST /manager/showtimes/status/{id}`
  - `GET /api/manager/showtimes`
  - `GET /api/manager/showtimes/{id}`
  - `POST /api/manager/showtimes`
  - `PUT /api/manager/showtimes/{id}`
  - `PATCH /api/manager/showtimes/{id}/status`
- **Database Entities:** `Showtime` (table `Showtime`), `Movie`, `Room`.
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Movie Management (Module 2), Room Management (Module 4).
- **Source Code Evidence:**
  - BR-07: Only movies with status `Upcoming` or `Now Showing` can be scheduled (`ShowtimeService.validateShowtime`).
  - BR-08: Only rooms with status `Ready` or `Screening` can host showtimes.
  - BR-09: Overlap validation formula (`newStart < existingEnd AND newEnd > existingStart`) including 15-minute room cleaning buffer (`CLEANING_BUFFER_MINUTES = 15`).
  - BR-54: Protection of showtimes with existing bookings from modifications (`ShowtimeDao.hasActiveBookings`).
  - BR-55: Automatically derives `EndTime` from `StartTime + Movie.Duration`.
  - Tested in `ShowtimeServiceTest.java`, `ShowtimeApiControllerTest.java`.

---

## Module 8: Ticket Pricing Management (Screen 60 / UC-31)
- **Feature Name:** Ticket Pricing Matrix & Time-slot Base Pricing
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/pricing.html`, `static/css/manager-pricing.css`, `static/js/manager-pricing.js`
- **Backend Controllers & Services:**
  - `TicketPricingController.java` (`/manager/pricing`)
  - `TicketPricingApiController.java` (`/api/manager/pricing`)
  - `TicketPricingService.java`
  - `TicketPricingDao.java`, `SeatTypeDao.java`
- **API Endpoints:**
  - `GET /manager/pricing`
  - `POST /manager/pricing/save`
  - `POST /manager/pricing/status/{id}`
  - `POST /manager/pricing/seat-multiplier`
  - `GET /api/manager/pricing`
  - `GET /api/manager/pricing/{id}`
  - `POST /api/manager/pricing`
  - `PUT /api/manager/pricing/{id}`
  - `PATCH /api/manager/pricing/{id}/base-price`
  - `PATCH /api/manager/pricing/{id}/status`
  - `GET /api/manager/pricing/matrix`
- **Database Entities:** `TicketPricing` (table `TicketPricing`), `SeatType` (table `SeatType`).
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Seat Type Management (Module 5).
- **Source Code Evidence:**
  - BR-50 pricing formula: `Unit Price = (BaseTicketPrice + FormatSurcharge) × SeatTypeMultiplier`, rounded to the nearest 1,000 VND using `RoundingMode.HALF_UP`.
  - Matrix calculation generator: `TicketPricingService.generatePricingMatrix()`.
  - Average ticket price computation (`ATP`).
  - Tested in `TicketPricingServiceTest.java`, `TicketPricingApiControllerTest.java`.

---

## Module 9: Food & Beverage Management (Screen 61 / UC-32)
- **Feature Name:** Concessions Inventory & Combo Recipes
- **Actor / Role:** `MANAGER`, `ADMIN`
- **Frontend Pages:** `src/main/resources/templates/manager/fb.html`, `static/css/manager-fb.css`, `static/js/manager-fb.js`
- **Backend Controllers & Services:**
  - `FBItemController.java` (`/manager/fb`)
  - `FBItemApiController.java` (`/api/manager/fb`)
  - `FBItemService.java`
  - `FBItemDao.java`
- **API Endpoints:**
  - `GET /manager/fb`
  - `POST /manager/fb/save`
  - `POST /manager/fb/status/{id}`
  - `GET /api/manager/fb`
  - `GET /api/manager/fb/{id}`
  - `POST /api/manager/fb`
  - `PUT /api/manager/fb/{id}`
  - `PATCH /api/manager/fb/{id}/status`
  - `DELETE /api/manager/fb/{id}`
- **Database Entities:** `FBItem` (table `FBItem`), `ComboItem` (table `ComboItem`).
- **Implementation Status:** **Fully Implemented**
- **Dependencies:** Independent catalog; referenced by booking orders (when implemented).
- **Source Code Evidence:**
  - Enforces BR-15: Concessions are optional add-ons.
  - Enforces BR-51: Historical booking prices preserved.
  - SKU / ItemCode uniqueness and non-negative pricing validation.
  - Prevent physical deletion if referenced by historical orders (`FBItemDao.isReferencedInOrders`).
  - Tested in `FBItemServiceTest.java`, `FBItemApiControllerTest.java`.

---

## Module 10: Customer Profile Management (Screen 08 / UC-08)
- **Feature Name:** Customer Account View and Profile Update
- **Actor / Role:** `CUSTOMER`
- **Frontend Pages:** `src/main/resources/templates/customer/profile.html`, `profile-edit.html`, `static/css/customer-profile.css`
- **Backend Controllers & Services:**
  - `CustomerProfileController.java` (`/customer/profile`)
  - `CustomerProfileService.java`
  - `CustomerProfileDao.java`
- **API Endpoints:**
  - `GET /customer/profile?accountId={id}`
  - `GET /customer/profile/edit?accountId={id}`
  - `POST /customer/profile/edit?accountId={id}`
- **Database Entities:** `Account`, `Customer`, `MembershipRank`.
- **Implementation Status:** **Partially Implemented**
- **Deficiencies & Gaps:**
  - **No Authentication Check:** Anyone can view/edit any account by supplying `?accountId=X`.
  - **No automated tests:** No test class exists in `src/test/java/com/cinema/controller/` or `service/` for `CustomerProfile`.
  - Requires pre-seeded `Account`, `Customer`, and `MembershipRank` tables not defined in the current migration scripts in `database/`.

---

## Module 11: Authentication & Authorization (Login, Registration, Logout)
- **Feature Name:** Identity Verification & Session Management
- **Actor / Role:** `GUEST`, `CUSTOMER`, `MANAGER`, `STAFF`, `ADMIN`
- **Frontend Pages:** Empty placeholder `.gitkeep` files in `templates/auth/`.
- **Backend Controllers & Services:** None.
- **API Endpoints:** None.
- **Implementation Status:** **Not Implemented**
- **Dependencies:** Security foundation for all modules.

---

## Module 12: Customer Ticket Booking & Online Payment
- **Feature Name:** Movie Ticket Selection, Seat Booking & Payment Gateway
- **Actor / Role:** `CUSTOMER`
- **Frontend Pages:** Empty or placeholder links in `index.html` and `profile.html`.
- **Backend Controllers & Services:** None.
- **API Endpoints:** None.
- **Implementation Status:** **Not Implemented** (Planned for later iterations per SRS).

---

## Modules 13, 14, 15: Promotions, Reports & News Approval
- **Feature Names:**
  - Screen 62: Promotion & Voucher Management
  - Screen 63: Financial Reports & Sales Statistics
  - Screen 64: Content Review & News Publication
- **Actor / Role:** `MANAGER`, `MARKETING`
- **Frontend Pages:** Rendered as disabled tiles (`tile-disabled`) with `"Sắp Ra Mắt"` badges on `dashboard.html`.
- **Backend Controllers & Services:** None.
- **Implementation Status:** **Not Implemented** (Placeholders present in Manager navigation).
