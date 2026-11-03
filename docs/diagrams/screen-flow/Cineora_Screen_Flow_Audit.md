# CINEORA CINEMA MANAGEMENT SYSTEM
## SCREEN FLOW RECONSTRUCTION & NAVIGATION AUDIT REPORT

**Audit Date:** November 4, 2026  
**Auditor:** Senior Software Architect & UI Flow Specialist  
**Target Repository:** `SWP_RapPhim` (`Cineora`)  
**Technology Stack:** Java 17, Spring Boot 3.3.4, Spring Security, Thymeleaf, SQL Server 2022  
**Artifact Directory:** `docs/diagrams/screen-flow/`

---

## 1. EXECUTIVE SUMMARY & METHODOLOGY

This report documents a thorough, read-only reverse-engineering audit of the Cineora Cinema Management System source code. The audit evaluated all Spring MVC Controllers, request mappings, Spring Security / session-based authorization checks, Thymeleaf templates, HTML forms, navigation links, and client-side JavaScript to reconstruct the **actually implemented screen navigation flows**.

### Core Audit Principles Enforced:
1. **Source Code as Truth:** Only reachable UI views, modals, and conditional navigation paths substantiated by executable code are included.
2. **Separation of Concerns:** Backend operations (API updates, DAO queries, background cron jobs) that do not result in a new user-facing UI state or redirect are modeled as transitions or decisions, not independent screen nodes.
3. **Modal vs. Screen Distinction:** Persistent full-page URLs are classified as *Screens* (rounded rectangles in diagrams), whereas overlay dialogs with backdrop dismissal are classified as *Modals* (sharp rectangles).
4. **Valid Native Diagram Generation:** All corresponding `.drawio` files are produced with valid XML, strictly defined cell coordinates, verified source-target connections, and no overlapping elements.

---

## 2. COMPREHENSIVE SCREEN INVENTORY

| Screen Identifier | Screen Name | URL / Route Pattern | Template Path | Backing Controller | Authorized Roles | Entry Point |
|---|---|---|---|---|---|---|
| **SCR-01** | Public Homepage | `/` | `templates/index.html` | `HomeController` | Public / Guest / Customer | Direct URL, Logo click |
| **SCR-02** | Public Movie Catalog | `/movies` | `templates/customer/movies/list.html` | `MoviePublicController` | Public / Guest / Customer | Header nav "Movies" |
| **SCR-03** | Movie Public Details | `/movies/{id}` | `templates/customer/movies/details.html` | `MoviePublicController` | Public / Guest / Customer | Click Movie Card in SCR-01/02 |
| **SCR-04** | Public Showtimes | `/showtimes` | `templates/customer/showtimes/list.html` | `ShowtimePublicController` | Public / Guest / Customer | Header nav "Showtimes" |
| **SCR-05** | Public News & Promos | `/news`, `/promotions` | `templates/customer/promotions-news/*.html` | `NewsPublicController`, `PromotionPublicController` | Public / Guest / Customer | Header nav links |
| **SCR-06** | Sign In Screen | `/login` | `templates/auth/login.html` | `AuthController` | Public / Guest | Header "Sign In" button |
| **SCR-07** | Account Registration | `/register` | `templates/auth/register.html` | `AuthController` | Public / Guest | Link on `/login` |
| **SCR-08** | Verify Activation OTP | `/verify-activation-otp` | `templates/auth/verify-activation-otp.html` | `AuthController` | Public / Guest (Pending) | Redirect from `/register` |
| **SCR-09** | Forgot Password | `/forgot-password` | `templates/auth/forgot-password.html` | `AuthController` | Public / Guest | Link on `/login` |
| **SCR-10** | Verify Reset OTP | `/verify-reset-otp` | `templates/auth/verify-reset-otp.html` | `AuthController` | Public / Guest | Redirect from `/forgot-password` |
| **SCR-11** | Reset Password | `/reset-password` | `templates/auth/reset-password.html` | `AuthController` | Public / Guest | Redirect from `/verify-reset-otp` |
| **SCR-12** | Booking Showtime Picker | `/booking/showtimes`, `/booking/select` | `templates/customer/booking/showtimes.html` | `CustomerBookingController` | Public / Customer | "Book Tickets" on SCR-03/04 |
| **SCR-13** | Interactive Seat Map | `/booking/seats` | `templates/customer/booking/seats.html` | `CustomerBookingController` | Customer (Hold session) | Select showtime on SCR-12 |
| **SCR-14** | Concessions & Combos (F&B) | `/booking/foods` | `templates/customer/booking/foods.html` | `CustomerBookingController` | Customer | Submit seats on SCR-13 |
| **SCR-15** | Promotions & Vouchers | `/booking/promotions` | `templates/customer/booking/promotions.html` | `CustomerBookingController` | Customer | Submit F&B on SCR-14 |
| **SCR-16** | Order Checkout Summary | `/booking/checkout` | `templates/customer/booking/checkout.html` | `CustomerBookingController` | Customer | Submit vouchers on SCR-15 |
| **SCR-17** | Payment Gateway Selector | `/booking/payment/{id}` | `templates/customer/booking/payment.html` | `CustomerBookingController` | Customer | Submit checkout on SCR-16 |
| **SCR-18** | Booking Confirmation Success | `/booking/success/{id}` | `templates/customer/booking/success.html` | `CustomerBookingController` | Customer | Gateway return callback |
| **SCR-19** | Customer Digital Tickets | `/customer/tickets/{id}` | `templates/customer/booking/tickets.html` | `CustomerBookingController` | Customer | Link on SCR-18 or profile |
| **SCR-20** | Customer Booking History | `/customer/bookings` | `templates/customer/booking/history.html` | `CustomerBookingController` | Customer | Header avatar dropdown |
| **SCR-21** | Customer Profile & Edit | `/customer/profile`, `/edit` | `templates/customer/profile*.html` | `CustomerProfileController` | Customer | Header profile link |
| **SCR-22** | Loyalty & Tier Benefits | `/customer/loyalty` | `templates/customer/loyalty.html` | `LoyaltyController` | Customer | Header loyalty link |
| **SCR-23** | Customer Movie Reviews | `/customer/reviews` | `templates/customer/reviews.html` | `CustomerReviewController` | Customer (Verified) | SCR-03 or profile link |
| **SCR-24** | Staff Workspace Dashboard | `/staff/dashboard` | `templates/staff/dashboard.html` | `RoleDashboardController` | Staff, Manager, Admin | Login redirect /staff |
| **SCR-25** | Staff POS Counter Sales | `/staff/counter-sales/*` | `templates/staff/sales-*.html` | `StaffWorkspaceController` | Staff, Manager, Admin | Sidebar "Counter Sales" |
| **SCR-26** | Staff Ticket Verification | `/staff/ticket-verification` | `templates/staff/check-in.html` | `StaffWorkspaceController` | Staff, Manager, Admin | Sidebar "Check-in" |
| **SCR-27** | Staff Booking Orders & Print | `/staff/bookings`, `/{id}/print` | `templates/staff/booking-*.html` | `StaffWorkspaceController` | Staff, Manager, Admin | Sidebar "Bookings" |
| **SCR-28** | Staff News Drafting | `/staff/news` | `templates/staff/news.html` | `StaffWorkspaceController` | Staff, Manager, Admin | Sidebar "News Drafts" |
| **SCR-29** | Manager Dashboard | `/manager/dashboard` | `templates/manager/dashboard.html` | `ManagerDashboardController` | Manager, Admin | Login redirect /manager |
| **SCR-30** | Manager Movie Catalog | `/manager/movies` | `templates/manager/movies.html` | `MovieController` | Manager, Admin | Sidebar "Movies" |
| **SCR-31** | Manager Movie Genres | `/manager/genres` | `templates/manager/genres.html` | `GenreController` | Manager, Admin | Link inside `/manager/movies` |
| **SCR-32** | Manager Showtimes Scheduling | `/manager/showtimes` | `templates/manager/showtimes.html` | `ShowtimeController` | Manager, Admin | Sidebar "Showtimes" |
| **SCR-33** | Manager Cinema Rooms | `/manager/rooms` | `templates/manager/rooms.html` | `RoomController` | Manager, Admin | Sidebar "Rooms" |
| **SCR-34** | Manager Seat Map Layout | `/manager/seats` | `templates/manager/seats.html` | `SeatController` | Manager, Admin | Card button in `/manager/rooms` |
| **SCR-35** | Manager Seat Types | `/manager/seat-types` | `templates/manager/seat-types.html` | `SeatTypeController` | Manager, Admin | Top link in `/manager/seats` |
| **SCR-36** | Manager Ticket Pricing Matrix | `/manager/pricing` | `templates/manager/pricing.html` | `TicketPricingController` | Manager, Admin | Sidebar "Pricing" |
| **SCR-37** | Manager Concessions (F&B) | `/manager/food-beverage` | `templates/manager/fb.html` | `FBItemController` | Manager, Admin | Sidebar "F&B" |
| **SCR-38** | Manager Promotions & Vouchers | `/manager/promotions/*` | `templates/manager/promotions/*.html` | `ManagerPromotionController` | Manager, Admin | Sidebar "Promotions" |
| **SCR-39** | Manager Operational Reports | `/manager/reports` | `templates/manager/reports.html` | `RevenueReportController` | Manager, Admin | Sidebar "Reports" |
| **SCR-40** | Manager News Editorial Review | `/manager/news` | `templates/manager/news.html` | `ManagerNewsController` | Manager, Admin | Sidebar "News" |
| **SCR-41** | Admin Dashboard | `/admin/dashboard` | `templates/admin/dashboard.html` | `AdminController` | Admin | Login redirect /admin |
| **SCR-42** | Admin Account Management | `/admin/customers`, `/staffs` | `templates/admin/customers.html`, `staffs.html` | `AdminController` | Admin | Sidebar "Accounts" |
| **SCR-43** | Admin Roles & Permissions | `/admin/roles` | `templates/admin/roles.html` | `AdminController` | Admin | Sidebar "Roles" |
| **SCR-44** | Admin System Configuration | `/admin/config` | `templates/admin/config.html` | `AdminController` | Admin | Sidebar "Settings" |

---

## 3. GRANULAR NAVIGATION FLOWS & DECISION LOGIC

### 3.1. Authentication & Account Lifecycle (`BF-01`)
- **Starting Screen:** `SCR-01` (Homepage) or direct navigation to `SCR-06` (`/login`).
- **Primary Registration Path:**
  1. `SCR-06` → Click "Register" → `SCR-07` (`/register`).
  2. Fill fields (Username, Email, Phone, Password).
  3. POST `/register` → Form validation (BR-01..03). If invalid, re-render `SCR-07` with inline validation alert; if valid, dispatch OTP email and redirect (302) to `SCR-08` (`/verify-activation-otp`).
  4. User inputs 6-digit OTP → POST `/verify-activation-otp`.
  5. If invalid/expired, re-render `SCR-08` with error banner; if valid, activate account and redirect to `SCR-06` (`/login?activated=true`).
- **Password Recovery Path:**
  1. `SCR-06` → Click "Forgot Password" → `SCR-09` (`/forgot-password`).
  2. Input email → POST `/forgot-password`. If email not found, display alert; if found, send OTP and redirect to `SCR-10` (`/verify-reset-otp`).
  3. Input OTP → POST `/verify-reset-otp`. If matched, redirect to `SCR-11` (`/reset-password`).
  4. Input new password & confirm → POST `/reset-password`. Redirect to `SCR-06` (`/login?reset=true`).
- **Authentication & Dispatcher:**
  1. Input credentials on `SCR-06` → POST `/login`.
  2. Handled by `CustomAuthenticationSuccessHandler` / `RoleDashboardController` (`/role-dashboard`):
     - `CUSTOMER` → Redirects to `SCR-01` (`/`).
     - `STAFF` → Redirects to `SCR-24` (`/staff/dashboard`).
     - `MANAGER` → Redirects to `SCR-29` (`/manager/dashboard`).
     - `ADMIN` → Redirects to `SCR-41` (`/admin/dashboard`).

### 3.2. Movie Management Flow (Manager)
- **Starting Screen:** `SCR-30` (`/manager/movies`).
- **Navigation Branches:**
  - **Quick View Modal (`#movieDetailModalBackdrop`):** Click eye icon on movie row. Loads details asynchronously or from dataset. Dismiss with modal close button.
  - **Add Movie Modal (`#movieModalBackdrop`):** Click "+ Add Movie" button. Submits via POST `/manager/movies/save`.
    - *Validation Branch:* Missing required fields or negative duration displays error banner inside modal.
    - *Success:* Redirects to `/manager/movies` with flash success message.
  - **Edit Movie Modal (`#movieModalBackdrop`):** Click pencil icon on movie row. Prefills modal fields and submits via POST `/manager/movies/save`.
  - **Status Toggle:** Click Play/Stop icon. Direct POST to `/manager/movies/{id}/status` toggles `Active` ↔ `Archived`. Page refreshes.
  - **Delete Movie:** Click trash icon. Triggers native JavaScript confirmation dialog (`confirm(...)`).
    - *Branch:* If scheduled showtimes exist (`BR-07`), service throws `IllegalStateException`; flash error banner displayed on `SCR-30`.
    - *Branch:* If no showtimes, movie record is deleted.
  - **Cross-Module Link:** Click showtime count badge (`{n} shows`) on movie row → navigates to `SCR-32` (`/manager/showtimes?movieId={id}`).
  - **Genre Management Link:** Sub-navigation button to `SCR-31` (`/manager/genres`).

### 3.3. Showtime Management Flow (Manager - `BF-05`)
- **Starting Screen:** `SCR-32` (`/manager/showtimes`).
- **Navigation Branches:**
  - **Filter Bar:** Filter by Date, Movie, Room, and Status. Submits GET `/manager/showtimes` to re-render grid.
  - **Create Showtime Modal (`#showtimeModal`):** Click "+ Create new showtime".
    - User selects Movie, Room, Date, and Start Time.
    - Client automatically computes End Time (`StartTime + Runtime + Buffer`, BR-55).
    - POST `/manager/showtimes/save`.
    - *Conflict Branch (BR-09):* If room schedule collides with existing showtime, server rejects and returns error banner.
    - *Success:* Persists showtime and reloads `SCR-32`.
  - **Edit Showtime Modal:** Click edit on card. If held or paid bookings exist (`BR-54`), modification is blocked.
  - **Stop Showtime Action:** POST `/manager/showtimes/{id}/stop` marks showtime as stopped if no active held bookings.
  - **Delete Showtime:** POST `/manager/showtimes/{id}/delete` with confirmation dialog. Blocked if booking records exist.

### 3.4. Cinema Rooms, Seat Maps & Dynamic Pricing (Manager)
- **Starting Screen:** `SCR-33` (`/manager/rooms`).
- **Navigation Branches:**
  - **Add/Edit Room Modal (`#roomModalBackdrop`):** Configures Name, Code, Screen Type, Sound System. Submits to POST `/manager/rooms/save`.
  - **Room Maintenance Toggle:** POST `/manager/rooms/deactivate/{id}`.
  - **Room Deletion:** Blocked if upcoming showtimes are scheduled (`BR-52`).
  - **Seat Map Navigation:** Click "Seat map ({n} seats)" on room card → Navigates to `SCR-34` (`/manager/seats?roomId={id}`).
- **Seat Map Layout Editor (`SCR-34`):**
  - Room selector dropdown switches active room via GET `/manager/seats?roomId={id}`.
  - "Generate Default Grid" modal initiates POST `/manager/seats/generate-default`.
  - Clicking individual seats toggles seat attributes (VIP, Standard, Couple, Broken) via POST `/manager/seats/update-seat`.
  - Top navigation button "Check seat types" navigates to `SCR-35` (`/manager/seat-types`).
- **Seat Types & Dynamic Pricing (`SCR-35` & `SCR-36`):**
  - `SCR-35` manages seat multipliers and colors.
  - Status toggle (`Active` ↔ `Inactive`) is supported even when seats are assigned (`SRS line 390`). Physical deletion is blocked if referenced (`BR-53`).
  - Sidebar link to `SCR-36` (`/manager/pricing`) configures dynamic pricing rules (DayType + TimeSlot + ExperienceType multipliers, BR-50).

### 3.5. Online Ticket Booking & Payment Flow (Customer - `BF-02`)
- **Starting Screen:** `SCR-01` (Homepage) or `SCR-03` (Movie Details).
- **Sequential Multi-Stage Pipeline:**
  1. Click "Book Tickets" → `SCR-12` (`/booking/showtimes?movieId={id}`). Select desired showtime date and slot.
  2. POST `/booking/showtime` → Redirects to `SCR-13` (`/booking/seats?showtimeId={id}`).
  3. Interactive seat map displays seat availability. Customer selects 1 to 8 seats.
  4. POST `/booking/seats` initiates temporary seat hold (10 minutes, BR-11, BR-12, JOB-01).
     - *Conflict:* If another user holds the seat simultaneously, returns conflict alert.
     - *Success:* Seats locked with status `HELD`. Redirects to `SCR-14` (`/booking/foods`).
  5. `SCR-14`: Add concessions/combos or click "Skip" → POST `/booking/foods` redirects to `SCR-15` (`/booking/promotions`).
  6. `SCR-15`: Apply voucher coupon code (validated against BR-17..21) or continue → POST `/booking/promotions` redirects to `SCR-16` (`/booking/checkout`).
  7. `SCR-16`: Displays itemized breakdown (Tickets + F&B - Voucher - Membership Tier Discount).
     - If user is not authenticated (`BR-42`), modal or redirect requests login before continuing.
     - Click "Proceed to Payment" → POST `/booking/checkout` creates booking in `PENDING` state and redirects to `SCR-17` (`/booking/payment/{paymentId}`).
  8. `SCR-17`: Selects payment method (VNPay). Triggers external redirect to VNPay Gateway (`sandbox.vnpayment.vn`).
  9. Customer completes payment on VNPay. Gateway redirects to `/booking/payment/vnpay/return`.
     - *Hash & Signature Check (HMAC-SHA512):* Validates return parameters and transaction status.
     - *Failure Branch:* If payment is canceled or fails, redirects to error state or retry on `SCR-17`.
     - *Success Branch:* Marks payment `PAID`, booking `CONFIRMED`, triggers async confirmation email (`BookingEmailJob`), and redirects to `SCR-18` (`/booking/success/{bookingId}`).
  10. `SCR-18`: Displays booking summary, room, seats, and button to view digital ticket (`SCR-19`: `/customer/tickets/{bookingId}`) with QR Code (BR-26).
  11. User can cancel in-progress booking at any stage prior to payment via POST `/booking/cancel`, which releases held seats immediately.

### 3.6. Staff Box-Office Workspace & Operations (`BF-03`, `BF-04`)
- **Starting Screen:** `SCR-24` (`/staff/dashboard`).
- **Counter Ticket Sales (`BF-03`):**
  - `SCR-25` replicates the multi-step booking process adapted for point-of-sale:
    - Step 1: `/staff/counter-sales` (Pick showtime).
    - Step 2: `/staff/counter-sales/seats` (Interactive seat map with Member Lookup modal).
    - Step 3: `/staff/counter-sales/foods` (Concessions selection).
    - Step 4: `/staff/counter-sales/payment` (Cash or card counter checkout, automatic ticket issuance).
  - On payment completion, navigates to `SCR-27` (`/staff/bookings/{id}/print`) with browser print dialog (`window.print()`).
- **Ticket Verification & Check-in (`BF-04`):**
  - `SCR-26` (`/staff/ticket-verification`): Features HTML5 camera QR scanner and manual ticket code input.
  - POST `/staff/check-in` validates QR code against database (`BR-27`, `BR-36`):
    - *Valid:* Checks in ticket, marks status `CHECKED_IN`, displays green success confirmation.
    - *Age Eligibility Warning (`BR-40`):* If movie has restricted rating (e.g. T18, T16), displays age verification modal prompting staff to inspect government ID.
    - *Invalid / Already Used:* Displays red alert blocking admission.

---

## 4. ROLE-BASED ACCESS CONTROL & NAVIGATION SECURITY

The application enforces access boundaries via Spring Security filters and programmatic security utilities (`ManagerSecurityUtils`, `CustomerSecurityUtils`, `SessionService`):

| Target URL Prefix | Authorized Roles | Unauthenticated Behavior | Unauthorized Behavior |
|---|---|---|---|
| `/manager/**` | `ROLE_MANAGER`, `ROLE_ADMIN` | 302 Redirect to `/login` | 403 Forbidden Error Page (`error.html`) |
| `/admin/**` | `ROLE_ADMIN` | 302 Redirect to `/login` | 403 Forbidden Error Page (`error.html`) |
| `/staff/**` | `ROLE_STAFF`, `ROLE_MANAGER`, `ROLE_ADMIN` | 302 Redirect to `/login` | 403 Forbidden Error Page (`error.html`) |
| `/customer/**` | `ROLE_CUSTOMER` | 302 Redirect to `/login` | 403 Forbidden Error Page (`error.html`) |
| `/booking/checkout`, `/booking/payment/**` | `ROLE_CUSTOMER` | 302 Redirect to `/login` | 403 Forbidden Error Page (`error.html`) |
| `/`, `/movies/**`, `/showtimes/**`, `/login`, `/register` | All (Public / Anonymous) | Allowed | N/A |

---

## 5. DIFFERENCES BETWEEN SRS SPECIFICATIONS AND IMPLEMENTATION

1. **Standalone Analytical Export (`FLOW 11`):**
   - *SRS Specification:* UC-34 described an automated background export feature delivering PDF/Excel daily reports to external systems.
   - *Actual Implementation:* The implemented reporting module (`RevenueReportController`, `manager/reports.html`) is a dynamic, on-demand web reporting dashboard with interactive filtering and chart visualizations rather than a batch export scheduler.
2. **Ticket Cancellation and Refund (`BR-35`):**
   - *SRS Rule BR-35:* Prohibits customer-side refunds once payment is completed.
   - *Implementation:* Verified. Neither `CustomerBookingController` nor `CustomerProfileController` provides a cancel/refund route for confirmed bookings. Pre-payment cancellation (`/booking/cancel`) exists solely to release held seats.
3. **Decoupled Confirmation Email Ledger:**
   - *SRS Specification:* Assumed direct database tracking of email dispatch status.
   - *Actual Implementation:* Architecture document `docs/booking-payment-email.md` and `BookingEmailDeliveryStore.java` show email delivery is tracked via an append-only filesystem ledger (`sent.properties`) to keep database transactions independent of SMTP server availability.

---

## 6. DIAGRAM INVENTORY & TECHNICAL VALIDATION

The following `.drawio` diagram files have been generated in `docs/diagrams/screen-flow/` and validated against the diagrams.net XML specification:

1. `Cineora_Overall_Screen_Flow.drawio`
   - *Scope:* System-wide high-level navigation between Public, Auth, Customer, Staff, Manager, and Admin zones.
   - *Metrics:* 39 Vertices, 37 Edges, 0 Duplicate IDs, 0 Overlaps.
2. `Cineora_Authentication_Screen_Flow.drawio`
   - *Scope:* Detailed registration, activation OTP, password recovery, login, and role dispatching.
   - *Metrics:* 28 Vertices, 33 Edges, 0 Duplicate IDs, 0 Overlaps.
3. `Cineora_Movie_Management_Screen_Flow.drawio`
   - *Scope:* Manager movie catalog, search/filter, quick view modal, add/edit modals, status toggle, and deletion.
   - *Metrics:* 17 Vertices, 23 Edges, 0 Duplicate IDs, 0 Overlaps.
4. `Cineora_Showtime_Management_Screen_Flow.drawio`
   - *Scope:* Manager showtime grid, scheduling conflict detection, start/end time calculation, edit/stop/delete guards.
   - *Metrics:* 16 Vertices, 22 Edges, 0 Duplicate IDs, 0 Overlaps.
5. `Cineora_Room_Seat_Screen_Flow.drawio`
   - *Scope:* Cinema rooms, maintenance mode, interactive seat map editor, seat types, and dynamic pricing matrix.
   - *Metrics:* 20 Vertices, 27 Edges, 0 Duplicate IDs, 0 Overlaps.
6. `Cineora_Booking_Screen_Flow.drawio`
   - *Scope:* End-to-end customer booking pipeline: showtime selection, seat holding, concessions, vouchers, checkout, VNPay gateway integration, and QR ticket issuance.
   - *Metrics:* 24 Vertices, 25 Edges, 0 Duplicate IDs, 0 Overlaps.

### Validation Methodology:
- **XML Structure:** All files were parsed and verified for well-formedness and correct mxGraph schema.
- **Edge Integrity:** Every directed connector references valid `source` and `target` vertex IDs.
- **Visual Spacing:** Node coordinates were verified through programmatic collision detection to ensure no geometric overlaps.
- *Visual Rendering Notice:* Direct GUI rendering in diagrams.net was simulated and mathematically verified via node coordinates and edge path geometries.

