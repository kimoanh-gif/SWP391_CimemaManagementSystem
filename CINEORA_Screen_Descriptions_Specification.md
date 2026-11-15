# CINEORA CINEMA MANAGEMENT SYSTEM
## SOFTWARE REQUIREMENTS SPECIFICATION (SRS) — REPORT 1
### BẢNG MÔ TẢ CHI TIẾT CÁC MÀN HÌNH QUẢN TRỊ (SCREEN SPECIFICATIONS)
*(Định dạng chuẩn 100% theo mẫu Field Name & Description của Ảnh 9 / SRS Page 158)*

---

## 1. Ảnh 1 — Revenue Report (SCR-MGR-01 / UC-34)

- **URL Route:** `/manager/reports`
- **Source Template:** `src/main/resources/templates/manager/reports.html`
- **Mô tả chức năng:** Màn hình theo dõi, kiểm toán doanh thu vé & bắp nước, hiệu suất bán hàng và xuất báo cáo kiểm toán Excel (CSV) (Screen 47 & 49 / UC-34).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Audit Controls** | |
| **Page Title** | Header label. Displays 'Revenue Report' with descriptive subtitle: 'Ticket and F&B revenue, movie performance and Excel (CSV) export for auditing (Screen 47 & 49 / UC-34).' |
| **Current Date Indicator** | Read-only badge. Format: '● dddd, DD/MM/YYYY' (e.g. '● Saturday, 14/11/2026'). Displays current system operating date. |
| **Role Badge** | Status pill. Text: 'MANAGER'. Indicates user role authorization level. |
| **Export Excel (CSV)** | Button. Generates and downloads financial audit CSV file encoded in UTF-8 with BOM for Vietnamese Excel compatibility. |
| **Print Report** | Button. Triggers browser print dialog using print-optimized CSS layout. |
| **II. Filter & Criteria Bar** | |
| **FROM** | Date picker. Format: DD/MM/YYYY. Default: First day of current operating month (e.g. 01/11/2026). Required. Filters records on or after this date. |
| **TO** | Date picker. Format: DD/MM/YYYY. Default: Last day of current operating month (e.g. 30/11/2026). Required. Validation: Must be greater than or equal to FROM date. |
| **REPORT TYPE** | Dropdown. Options: 'Daily overview' (DAILY), 'Movie performance' (MOVIE), 'Transaction details' (TRANSACTIONS). Default: 'Daily overview'. |
| **Filter data** | Button. Submits selected date range and report type criteria to refresh KPI cards and table data. |
| **III. Revenue KPI Metric Cards** | |
| **TOTAL NET REVENUE** | Read-only currency card (VND). Formatted as '#,##0 ₫'. Total actual collected revenue. Business rule BR-22 formula: Net Revenue = (Gross Ticket Sales + Gross F&B Sales) - Total Voucher/Point Discounts. Subtext: 'Collected cash after discounts (BR-22)'. |
| **TICKET REVENUE (GROSS)** | Read-only currency card (VND). Formatted as '#,##0 ₫'. Gross sales from movie ticket admissions before discount deductions. Subtext: 'XX.X% of gross sales'. |
| **F&B REVENUE (GROSS)** | Read-only currency card (VND). Formatted as '#,##0 ₫'. Gross sales from concession items before discount deductions. Subtext: 'XX.X% of gross sales'. |
| **TICKETS SOLD** | Read-only numeric card. Total number of successfully paid seat tickets issued within the filtered period. Subtext: 'Paid seats'. |
| **SUCCESSFUL ORDERS** | Read-only numeric card. Total count of verified orders. Strictly defined as transactions where Booking.Status = 'CONFIRMED' and Payment.Status = 'PAID' (with confirmed PaidAt timestamp, verified in RevenueReportDao.java:32-44). Online bookings must be paid via gateway; counter POS orders transition to CONFIRMED only when cash/card payment is settled by cashier. Unpaid bookings ('PAYMENT_PENDING', 'CANCELLED', 'EXPIRED') are excluded to ensure financial audit integrity per BR-22/BR-41. Subtext: 'Paid / Confirmed transactions'. |
| **IV. Daily Revenue Summary Table** | |
| **Section Header** | Table title: 'Daily revenue summary' with active filter date range badge: 'Data from DD/MM/YYYY to DD/MM/YYYY'. |
| **DATE** | Date column. Format: DD/MM/YYYY. Listed in chronological order. |
| **TICKET REVENUE (VND)** | Currency column. Daily gross ticket revenue formatted as '#,##0 ₫'. |
| **F&B REVENUE (VND)** | Currency column. Daily gross concession revenue formatted as '#,##0 ₫'. |
| **DISCOUNT (VND)** | Currency column. Total promotional voucher and loyalty points deducted on date formatted as '#,##0 ₫'. |
| **TOTAL REVENUE (VND)** | Currency column. Daily net revenue formula: (Ticket + F&B) - Discount formatted as '#,##0 ₫'. |
| **TICKETS SOLD** | Numeric column. Number of paid seat tickets issued on date. |
| **ORDERS** | Numeric column. Count of successfully completed transactions on date. |
| **Empty Data State** | Placeholder banner. Displayed when no revenue exists in period: 'No revenue data in this period. Please select another date range or adjust filters.' |
| **TOTAL (Summary Row)** | Summary table footer row. Automatically calculates sums across all revenue, ticket, and order columns for the selected period. |

## 2. Ảnh 2 — Movie Management (SCR-MGR-02 / UC-28)

- **URL Route:** `/manager/movies`
- **Source Template:** `src/main/resources/templates/manager/movies.html`
- **Mô tả chức năng:** Màn hình quản lý danh mục phim, phân loại độ tuổi, thể loại, thông tin phát hành và lịch chiếu của hệ thống CINEORA (Screen 42 / UC-28).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Primary Action** | |
| **Page Title** | Header label. Displays 'Movie Management' with subtitle: 'Manage movie information, classification and release status in the CINEORA cinema system.' |
| **Add new movie** | Button (+ icon). Opens the 'Add new movie' modal dialog to register a new title. |
| **II. KPI Status Badges** | |
| **Now Showing** | Numeric badge (play icon). Count of movies currently in active theatrical run (DB status = 'Now Showing'). |
| **Coming Soon** | Numeric badge (calendar icon). Count of upcoming movies scheduled for release (DB status = 'Upcoming'). |
| **Ended / Archived** | Numeric badge (archive icon). Count of movies finished run or deactivated (DB status = 'Ended' or 'Archived'). |
| **All movies** | Numeric badge (film icon). Total count of movie records registered in the cinema catalog. |
| **III. Filter & Search Bar** | |
| **Search Keyword** | Text input. Placeholder: 'Search by title, director, cast...'. Searches across movie title, director, and actor names. |
| **Genre Filter** | Dropdown. Options: 'All genres' plus all active genres in system. Default: 'All genres'. |
| **Age Rating Filter** | Dropdown. Options: 'Age rating: All', 'P', 'K', 'T13', 'T16', 'T18'. Default: 'Age rating: All'. |
| **Status Filter** | Dropdown. Options: 'All statuses', 'Now Showing', 'Coming Soon', 'Ended'. Default: 'All statuses'. |
| **Filter** | Button. Applies selected search and filter criteria to the movie table. |
| **Clear filters** | Button. Resets all filter inputs and reloads the default movie list. |
| **IV. Movie List Table** | |
| **Table Header Badge** | Header text badge: 'Movie list ({count} titles)'. Indicates total matched results. |
| **Movie title / Original title** | Composite column. Displays movie poster thumbnail (aspect ratio 2:3), primary Vietnamese title (bold), and original international title (muted subtext). |
| **Genre** | Tag badges. List of active genre tags associated with movie (e.g. 'Hành Động', 'Hoạt Hình'). Displays 'Unclassified' if no genre is assigned. |
| **Age rating** | Color-coded tag badge. Values: P (Green - General all ages), K (Blue - Under 13 with guardian), T13 (Orange - Ages 13+), T16 (Amber - Ages 16+), T18 (Red - Ages 18+). |
| **Duration** | Numeric column. Movie runtime duration in minutes formatted as '{value} min'. |
| **Release** | Date column formatted 'DD/MM/YY'. Displays premiere release date. |
| **Lifecycle status** | Status badge. Mapping to Database:<br>- 'Coming Soon' -> DB: 'Upcoming'<br>- 'Now Showing' -> DB: 'Now Showing'<br>- 'Ended' -> DB: 'Ended'<br>- 'Archived' -> DB: 'Archived'. |
| **Showtimes** | Link formatted '{n} shows'. Clicking navigates to /manager/showtimes?movieId={id} to view scheduled slots. |
| **Actions** | Action Button Group (4 buttons per row):<br>- View Details (Eye icon): Opens read-only quick view popup dialog.<br>- Edit Movie (Pencil icon): Opens Edit Movie modal prefilled with current movie data.<br>- Stop/Archive or Reactivate (Square/Play icon): Toggles status between Ended/Archived and Active.<br>- Delete Movie (Trash icon): Deletes movie after confirmation prompt (blocked if showtimes or bookings exist). |
| **Pagination Summary** | Footer row: 'Showing {start} – {end} of {filtered} matching movies (system total: {total})' with navigation buttons (‹, 1, 2, ›). Page size: 5 movies per page. |
| **V. Add / Edit Movie Modal Form** | |
| **Movie title** | Text input. Required. Max length: 150 chars. Primary film title. Placeholder: 'e.g. Fantastic Aura: Eternity'. |
| **Original / International title** | Text input. Optional. Max length: 150 chars. Original English title. Placeholder: 'e.g. Dune: Part Two'. |
| **Genres Selection** | Multi-checkbox grid. Lists active genres in system. At least 1 genre is required. |
| **Duration (minutes)** | Numeric input. Required. Min: 1, Max: 600. Movie runtime used for automatic showtime scheduling. |
| **Age rating** | Dropdown. Required. Options: P (All ages), K (Under 13 with guardian), T13 (13+), T16 (16+), T18 (18+). |
| **Status** | Dropdown. Required. Options: 'Coming Soon' (Upcoming), 'Now Showing' (Now Showing), 'Ended' (Ended), 'Archived' (Archived). |
| **Release date** | Date picker. Format: DD/MM/YYYY. Premiere theatrical release date. |
| **Director** | Text input. Optional. Max length: 100 chars. Director name. |
| **Cast / Actors** | Text input. Optional. Max length: 255 chars. Key actors. |
| **Language** | Text input. Optional. Max length: 50 chars (e.g. 'English, Vietsub'). |
| **Vertical poster (URL or file)** | Composite control. Supports direct image URL input or local file upload ('Upload image' button). Uploads PNG/JPEG/WEBP asynchronously via POST /api/manager/movies/upload-image and displays live 2:3 thumbnail preview. |
| **Horizontal banner (URL or file)** | Composite control. Supports direct image URL input or local file upload ('Upload image' button). Uploads PNG/JPEG/WEBP asynchronously (16:9 ratio) and displays live banner preview. |
| **Trailer URL** | URL input. Max length: 500 chars. Valid YouTube trailer link. |
| **Synopsis / Description** | Textarea with character counter (0 / 1000). Summary of movie plot. |
| **Save movie** | Button. Submits form via AJAX (POST /api/manager/movies for create, PUT /api/manager/movies/{id} for update) with JSON body payload. |
| **Cancel** | Button. Discards changes and closes modal. |

## 3. Ảnh 3 — Showtime Management (SCR-MGR-03 / UC-30)

- **URL Route:** `/manager/showtimes`
- **Source Template:** `src/main/resources/templates/manager/showtimes.html`
- **Mô tả chức năng:** Màn hình theo dõi lịch chiếu phim theo phòng, giám sát suất chiếu hôm nay, quản lý bộ đệm dọn phòng 15 phút (BR-09) và điều hướng sang giao diện xếp lịch theo phòng (Screen 59 / UC-30).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Navigation** | |
| **Page Title** | Header label. Displays 'Showtime Management' with subtitle: 'Track and manage movie schedules by screening room, with a 15-minute cleaning buffer and automatic conflict checks.' |
| **Create Room Schedule** | Button (calendar icon, red). Primary action button. Navigates directly to the room schedule planner screen (/manager/showtimes/scheduler). |
| **II. Operational KPI Cards** | |
| **Showtimes on DD/MM/YYYY** | Numeric card (schedule icon). Dynamic title reflects filtered date (e.g. 'Showtimes on 14/11/2026'). Count of showtimes scheduled for that date. Subtext: 'Date: DD/MM/YYYY'. |
| **Upcoming (Scheduled)** | Numeric card (calendar icon). Count of scheduled showtimes waiting to air on the filtered date. Subtext: 'Waiting for show time'. |
| **Room cleaning buffer** | Operational standard card (cleaning icon). Value: '15 min'. Subtext: 'Added after every showtime (BR-09)'. Fixed operational buffer for cleaning auditorium between screenings. |
| **In progress (Showing)** | Numeric card (play icon). Count of screenings currently broadcast live in auditoriums. Subtext: 'Live on air'. |
| **III. Filter & Search Bar** | |
| **From date** | Date picker. Format: DD/MM/YYYY. Default: Current operating date. Required. Filters showtimes on or after this date. |
| **To date** | Date picker. Format: DD/MM/YYYY. Default: Current operating date. Required. Validation: Must be >= From date. |
| **All rooms** | Dropdown. Options: 'All rooms' plus active screening rooms. Default: 'All rooms'. |
| **All movies** | Dropdown. Options: 'All movies' plus active movie catalog. Default: 'All movies'. |
| **All statuses** | Dropdown. Options: 'All statuses', 'Scheduled', 'Showing', 'Ended', 'Cancelled'. Default: 'All statuses'. |
| **Filter** | Button. Applies selected filter criteria to the showtime table. |
| **Reset** | Button. Resets all filters to today's date and all rooms/movies. |
| **IV. Showtime List Table** | |
| **Table Header Badge** | Header badge: 'Showtime list ({count} showtimes)'. Indicates total matched slots. |
| **Date & Time** | Composite DateTime string formatted 'HH:mm, DD/MM/YYYY'. Displays start time and calculated film end time. |
| **Movie** | Composite cell displaying Movie Title, Age Rating badge (e.g. T18, T13), and screening format (2D / 3D / IMAX). |
| **Screening room** | Text column. Identifier of auditorium (e.g. 'Cinema 03 - Laser Compact'). |
| **Duration** | Numeric column. Runtime duration of movie in minutes. |
| **Base ticket price** | Currency column (VND) formatted as '#,##0 ₫'. Standard reference ticket price before seat type multipliers are applied. |
| **Status** | Status badge. Values: 'Scheduled' (Blue), 'Showing' (Green), 'Ended' (Gray), 'Cancelled' (Red). |
| **Actions** | Action Button Group:<br>- Stop Showtime: Enforces BR-54 emergency stop, transitioning status to 'CANCELLED' while preserving sold tickets.<br>- Delete Showtime: Deletes showtime only if zero tickets have been sold (blocked if bookings exist). |
| **Empty Schedule Notice** | Placeholder state displayed when no showtimes match filter: 'No showtimes for the selected date range or filters. Click Create new showtime to schedule one.' (Instructional call-to-action referring to scheduling via the 'Create Room Schedule' button in the header toolbar, verified in showtimes.html:203). |

## 4. Ảnh 3b — Create Room Schedule (SCR-MGR-04 / UC-30b)

- **URL Route:** `/manager/showtimes/scheduler`
- **Source Template:** `src/main/resources/templates/manager/showtime-scheduler.html`
- **Mô tả chức năng:** Màn hình chuyên biệt tạo lịch chiếu theo từng phòng: thêm nhiều slot phim trong ngày, tự động tính giờ kết thúc, cộng 15 phút dọn phòng (BR-09) và kiểm tra xung đột thời gian (Screen 59b / UC-30 Scheduler).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Back Navigation** | |
| **Page Title & Breadcrumb** | Header label. Breadcrumb: 'Overview / Showtime Management / Create Room Schedule'. Title: 'Create Room Schedule'. Subtitle: 'Select a room, add movie slots, and let the system calculate film end times automatically.' |
| **Back to Showtimes** | Navigation button (arrow_back icon). Returns to the main showtime management list (/manager/showtimes). |
| **II. Room Selection & Operating Meta Card** | |
| **Screening Room** | Dropdown (tv icon). Lists eligible active screening rooms. Selecting a room dynamically refreshes its schedule timeline and operating parameters. |
| **Screening Date** | Date picker (calendar icon). Specifies operating date for scheduling. Restricted to current or future dates (minDate). |
| **Room Type Indicator** | Read-only meta badge. Displays technology tier of selected room (e.g. 'Standard Laser', 'Gold Class VIP', 'IMAX Laser'). |
| **Room Capacity Indicator** | Read-only meta badge. Displays total seat capacity of selected room (e.g. '80 seats'). |
| **Room Status Indicator** | Read-only meta badge. Displays operational readiness of room (e.g. 'Ready'). |
| **Cleaning Buffer Info** | Read-only operational info (cleaning icon). Value: '15 min'. Fixed cleaning buffer automatically applied after every screening (BR-09). |
| **III. Visual Schedule Timeline Canvas** | |
| **Timeline Hour Ruler** | Horizontal time axis spanning from 08:00 through 24:00 (extending past midnight to 02:00 for late screenings). |
| **Interactive Track Area** | Visual bar containing color-coded blocks for the room: Saved showtimes, Draft unsaved showtimes, and 15-minute cleaning buffer blocks. |
| **Timeline Legend** | Legend swatches: 'Saved Showtime' (Solid blue), 'Draft Showtime' (Striped blue/amber), 'Cleaning Buffer (15 min)' (Light orange). |
| **IV. Schedule Slots Table** | |
| **Table Structure** | Dynamic tabular list of planned slots. Columns: # (Index), Movie Title & Rating, Start Time (HH:mm), Duration (min), Film End Time (HH:mm), Room Available Time (HH:mm), Base Price (VND), Status ('Saved' or 'Draft'), Actions (Edit, Remove draft). |
| **Table Buffer Note** | Information note: '15-minute cleaning buffer applies after each showtime.' |
| **V. Add Showtime Slot Form Drawer** | |
| **Movie Selection** | Dropdown (movie icon). Lists eligible movies currently available for scheduling with duration and age rating metadata. |
| **Start Time** | Time input (HH:mm). Specifies the start time of the screening slot. |
| **Movie Duration** | Read-only preview. Displays duration of the selected film in minutes (e.g. '120 min'). |
| **Film End Time** | Read-only calculated field. Automatically calculated based on selected movie duration and start time: Film End Time = Start Time + Movie Duration. |
| **Cleaning Buffer** | Read-only field. Fixed standard value of 15 minutes automatically reserved after film end time (BR-09). |
| **Room Available Time** | Read-only calculated field. Displays Film End Time plus the required 15-minute cleaning buffer: Room Available Time = Film End Time + 15 minutes. |
| **Schedule Conflict Alert** | Validation message box. Appears when a proposed showtime overlaps an existing showtime or cleaning buffer. Displays error: 'Schedule Conflict! Room is occupied' and provides 'Use Next Time' button to auto-align start time to next available slot. |
| **Add to Schedule** | Button (+ icon). Adds the configured slot into the draft schedule list and renders it on the visual timeline. |
| **Clear** | Button (refresh icon). Resets form drawer inputs to default. |
| **VI. Sticky Bottom Save Action Bar** | |
| **Draft Count Display** | Status label. Displays count of pending draft slots ready to persist: '{count} draft slot ready to save'. |
| **Discard Drafts** | Button (delete icon). Clears all unsaved draft slots from the timeline. |
| **Save Room Schedule** | Button (bookmark icon, red). Saves all valid draft showtimes for the selected room and date. System validates scheduling conflicts before saving and commits batch in a single transaction via POST /api/manager/showtimes/batch. |

## 5. Ảnh 4 — Seat Type Management (SCR-MGR-05 / UC-31)

- **URL Route:** `/manager/seat-types`
- **Source Template:** `src/main/resources/templates/manager/seat-types.html`
- **Mô tả chức năng:** Màn hình cấu hình các loại ghế, màu sắc hiển thị trên sơ đồ ghế và hệ số nhân giá vé động (Business rule BR-50) (Screen 54 / UC-31).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Primary Action** | |
| **Page Title** | Header label. Displays 'Seat Type Management' with subtitle: 'Configure seat types and price multipliers for flexible ticket pricing by room seat map.' |
| **Add seat type** | Button (+ icon). Opens '+ Add seat type' modal form to create a new seat class. |
| **II. Pricing Multiplier KPI Metrics** | |
| **Total seat types** | Numeric badge (armchair icon). Total seat classes configured in system (e.g. 4 active in the system). |
| **Standard seat multiplier** | Numeric badge (tag icon). Value: '1.00x'. Subtext: 'Base ticket price reference'. |
| **Average price multiplier** | Numeric badge (sigma icon). Format: 'X.XXx' (e.g. 1.25x). Mean of active seat type multipliers. |
| **Allowed multiplier range** | Range badge (equals icon). Value: '0.50 – 3.00'. Subtext: 'Business rule BR-50'. Enforces valid bounds for price multipliers. |
| **III. Seat Type List Table** | |
| **Table Header Badge** | Header text badge: 'Seat type list ({count} types)'. |
| **Code** | Text badge. Uppercase unique code identifier: 'FRONT', 'STD', 'VIP', 'COUPLE'. |
| **Seat type name** | Composite column. Color indicator dot + Seat Type Name (e.g. 'Standard Seat', 'VIP Royal Amber', 'Sweetbox Couple'). |
| **Price multiplier** | Numeric badge. Format: 'X.XXx' (e.g. 0.95x, 1.00x, 1.25x, 1.80x). Ticket price formula: Seat Price = (Base Price + Surcharge) * Multiplier. |
| **Seat map color** | Color preview swatch + Hex code string (e.g. #3B82F6, #F59E0B, #EC4899). |
| **Features & amenities** | Text column. Description of seating comfort, viewing angle, and extra perks. |
| **Status** | Status badge. Values: 'Active' (Green) or 'Inactive' (Gray). |
| **Actions** | Action Button Group (2 buttons per row):<br>- Edit Seat Type (Pencil icon): Opens Edit modal to modify name, multiplier, color, and description.<br>- Delete Seat Type (Trash icon): Deletes seat type with confirmation (blocked if active seats or bookings reference it).<br>Note: Price multiplier is adjusted via the Edit modal; there is no standalone table button for multipliers. |
| **Pagination & Pricing Note** | Footer summary displaying 'Showing 1 – {count} of {total} registered seat types'. Bottom callout explains pricing formula and confirms multipliers only apply to showtimes created/updated after saving. |
| **IV. Add / Edit Seat Type Modal Form** | |
| **Seat Type Code** | Text input. Required. Max length: 10 chars. Pattern: ^[A-Z0-9_]+$. Unique uppercase identifier. |
| **Seat Type Name** | Text input. Required. Max length: 50 chars. Descriptive name of seat class. |
| **Price Multiplier** | Numeric input. Required. Min: 0.50, Max: 3.00, Step: 0.05. Enforces BR-50 pricing rule. |
| **Seat Map Color Code** | Color picker input (type='color'). Selects hex color for matrix visualization. |
| **Status** | Dropdown. Options: 'Active', 'Inactive'. Default: 'Active'. |
| **Features / Description** | Textarea. Optional. Max length: 255 chars. Summary of seat amenities. |
| **Save changes** | Button. Persists seat type configuration to database. |
| **Cancel** | Button. Closes modal without saving. |

## 6. Ảnh 5 — Seat Map Management (SCR-MGR-06 / UC-31)

- **URL Route:** `/manager/seats`
- **Source Template:** `src/main/resources/templates/manager/seats.html`
- **Mô tả chức năng:** Màn hình trực quan hóa và cấu hình sơ đồ ghế theo từng phòng chiếu: phân loại loại ghế (Standard, VIP, Sweetbox) và khóa bảo trì ghế (Screen 55 / UC-31).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Utility Actions** | |
| **Page Title** | Header label. Displays 'Seat Map Management' with subtitle: 'View and configure the seat map of each screening room: seat types (Standard, VIP, Sweetbox) and seats under maintenance.' |
| **Generate standard map** | Button (wand icon). Generates standard rectangular grid based on auditorium's configured rows and seats per row after user confirmation. |
| **Refresh seat map** | Button (refresh icon). Reloads seat grid and fresh booking/maintenance states for the selected room from database. |
| **II. Screening Room Selector & KPI Metrics** | |
| **Screening room** | Dropdown. Lists active screening rooms with code, tech tier, and capacity (e.g. 'Cinema 03 - Laser Compact (ROOM-033 • Standard Laser • 80 seats)'). Selecting a room refreshes the grid. |
| **Total seats** | Numeric badge (armchair icon). Value: '{count}'. Subtext: '{rows} rows × {cols} columns'. |
| **Active seats** | Numeric badge (check icon). Value: '{count}'. Subtext: 'Ready for ticket sales'. |
| **Locked** | Numeric badge (lock icon). Value: '{count}'. Subtext: 'Blocked from booking'. |
| **Room capacity** | Numeric badge (cinema icon). Value: '{capacity}'. Subtext: Room tech tier name. |
| **III. Interactive Seat Map Grid View** | |
| **Section Header & Badge** | Heading: 'Seat map — {Room Name}'. Badge: Technology tier (e.g. 'Standard Laser', 'IMAX Laser'). |
| **Instruction Alert** | Notice text: 'Click or drag across rows/columns to select multiple seats. Double-click a seat or click Configure to edit.' |
| **Screen Arc Indicator** | Curved red line banner with text: 'SCREEN'. Represents position of projection screen. |
| **Row Alpha Labels** | Alphabetic row identifiers (A, B, C, D, ...) displayed on both left and right margins of the grid. |
| **Seat Grid Cells** | Square interactive buttons displaying seat codes (e.g. A1, A2... H10). Color-coded by seat type: Blue (Standard), Amber (VIP), Pink (Sweetbox), Dark/Strikethrough (Maintenance). Click highlights seat. |
| **Central Aisle** | Vertical visual gap labeled 'CENTRAL AISLE' separating seat columns into left and right sections. Note: In CINEORA, the aisle is a CSS presentation spacing; all grid coordinates are real physical seat records. |
| **Marquee Drag Selection** | Allows click-and-drag across rows and columns to highlight multiple seats simultaneously for bulk editing. |
| **Bulk Action Bar** | Action bar appearing when seats are highlighted: 'Configure Selected' (opens bulk configuration modal) and 'Deselect All' (clears selection). |
| **IV. Seat Configuration Modal Form** | |
| **Selected Seat Coordinates** | Text indicator. Displays currently selected seat code(s) (e.g. 'Seat C5' or '10 seats selected'). |
| **Seat Type Assignment** | Dropdown. Options: 'Standard Seat', 'VIP Royal Amber', 'Sweetbox Couple', 'Front Row'. Assigns pricing and visual category. |
| **Seat Operational Status** | Dropdown. Options: 'ACTIVE' (Available for customer booking), 'MAINTENANCE' (Locked from booking due to physical maintenance or damage). |
| **Apply changes** | Button. Saves seat classification and operational status updates to the database. |
| **Cancel** | Button. Closes modal without applying modifications. |

## 7. Ảnh 6 — Movie Genre Management (SCR-MGR-07 / UC-28b)

- **URL Route:** `/manager/genres`
- **Source Template:** `src/main/resources/templates/manager/genres.html`
- **Mô tả chức năng:** Màn hình quản lý các thể loại phim dùng để phân loại danh mục trong hệ thống CINEORA (UC-28 Genres).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Primary Action** | |
| **Page Title** | Header label. Displays 'Movie Genre Management' with subtitle: 'Manage the genres used to classify movies in the CINEORA system.' |
| **Add genre** | Button (+ icon, red). Opens '+ Add genre' modal form to create a new category. |
| **II. Genre KPI Metric Badges** | |
| **Total genres** | Numeric badge (category icon). Total genres registered in system (e.g. 13). |
| **Active** | Numeric badge (check icon). Count of enabled genres available for movie assignment (e.g. 11). |
| **Inactive** | Numeric badge (ban icon). Count of disabled genres (e.g. 2). |
| **Genres with movies** | Numeric badge (film icon). Count of genres currently associated with at least 1 movie title (e.g. 7). |
| **III. Filter & Search Bar** | |
| **Search Keyword** | Text input. Placeholder: 'Search by genre name or description...'. Filters genre title and descriptive text. |
| **All statuses** | Dropdown. Options: 'All statuses', 'Active', 'Inactive'. Default: 'All statuses'. |
| **Filter** | Button. Applies search and status filters to the genre table. |
| **Clear filters** | Button. Resets search and displays full genre list. |
| **IV. Genre List Table** | |
| **Table Header Badge** | Header text badge: 'Genre list (10 genres)'. Indicates items displayed on current page. |
| **#ID** | Numeric column. Unique database primary key for genre. |
| **Genre name** | Text column (bold). Genre title (e.g. 'Hành Động', 'Hoạt Hình', 'Khoa Học Viễn Tưởng', 'Kinh Dị'). |
| **Description** | Text column. Brief synopsis of genre themes and narrative conventions. |
| **Linked movies** | Tag badge / link. Format: '{n} movies'. Clicking opens modal dialog listing movies assigned to this genre. |
| **Status** | Status badge. Values: 'Active' (Green badge) or 'Inactive' (Red badge). |
| **Actions** | Action Button Group (4 buttons per row):<br>- View Linked Movies (Film icon): Opens popup of movies linked to genre.<br>- Edit Genre (Pencil icon): Opens modal to modify genre name and description.<br>- Toggle Active Status (Square toggle icon): Sends AJAX PATCH /api/manager/genres/{id}/toggle-status to toggle active state.<br>- Delete Genre (Trash icon): Deletes genre with confirmation (blocked if movies are assigned). |
| **Pagination Summary** | Footer row displaying 'Showing {start} – {end} out of {total} genres' (e.g. 'Showing 1 – 10 out of 13 genres') with navigation buttons (‹, 1, 2, ›). Standard page size: 10 genres per page. |
| **V. Add / Edit Genre Modal Form** | |
| **Genre Name** | Text input. Required. Max length: 50 chars. Category name. Placeholder: 'e.g. Action, Comedy, Drama'. |
| **Description** | Textarea. Optional. Max length: 255 chars. Rows: 3. Summary of genre themes. |
| **Status** | Dropdown. Options: 'Active', 'Inactive'. Default: 'Active'. |
| **Save changes** | Button. Submits form to create or update genre. |
| **Cancel** | Button. Closes modal without saving. |

## 8. Ảnh 7 — Screening Room Management (SCR-MGR-08 / UC-29)

- **URL Route:** `/manager/rooms`
- **Source Template:** `src/main/resources/templates/manager/rooms.html`
- **Mô tả chức năng:** Màn hình quản lý thông số kỹ thuật, máy chiếu, âm thanh, kích thước màn hình và trạng thái vận hành của các phòng chiếu (Screen 56 / UC-29).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Primary Action** | |
| **Page Title** | Header label. Displays 'Screening Room Management' with subtitle: 'Manage seat maps, technical specifications and operating status of the screening rooms.' |
| **Add screening room** | Button (+ icon, red). Opens '+ Add screening room' modal dialog to register a new auditorium. |
| **II. Room Inventory KPI Cards** | |
| **Total screening rooms** | Numeric badge (theater icon). Count of registered auditoriums matching current filter (e.g. '5'). |
| **Active** | Numeric badge (check icon). Count of operational rooms in Ready or Screening state (e.g. '2'). |
| **Maintenance / Cleaning** | Numeric badge (wrench icon). Count of rooms currently unavailable for showtimes (e.g. '3'). |
| **Actual total seats** | Numeric badge (armchair icon). Total seat capacity summed across all screening rooms (e.g. '516 seats'). |
| **III. Search & Filter Bar** | |
| **Search Room** | Text input. Placeholder: 'Search by room name or code...'. Filters by room title and code. |
| **Search** | Button. Filters the screening room cards grid. |
| **Clear** | Button. Clears search keyword and reloads full room catalog. |
| **IV. Screening Room Cards Grid** | |
| **Card Header** | Composite status header displaying Room Operating Status Pill (e.g. '• READY FOR GUESTS', '• MAINTENANCE') and Tech Tier Badge ('Standard Laser', 'Gold Class VIP', 'IMAX Laser'). |
| **Room Title & Code** | Heading: Room Name (e.g. 'Cinema 03 - Laser Compact') and unique Code with capacity (e.g. 'ROOM-033 • 80 seats'). |
| **Projector Spec** | Icon text item. Hardware model (e.g. 'Projector: Barco SP2K-11 Laser'). |
| **Sound System Spec** | Icon text item. Audio standard (e.g. 'Sound: Dolby 7.1 Studio Audio'). |
| **Screen Size** | Icon text item. Physical projection dimensions (e.g. 'Screen: 14m x 7m'). |
| **Capacity** | Icon text item. Total seat count (e.g. 'Capacity: 80 seats'). |
| **Equipment Notes / Description** | Text paragraph describing room facilities or current maintenance activity. |
| **Action Controls** | Action Button Group:<br>- 'Seat map ({capacity})': Navigates directly to /manager/seats?roomId={id}<br>- 'Showtimes': Navigates to /manager/showtimes?roomId={id}<br>- Edit Room (Pencil icon): Opens room edit modal<br>- Delete Room (Trash icon): Deletes room with confirmation; blocked if past or future showtimes exist. |
| **Pagination Summary** | Footer row: 'Showing 1 – 5 of 5 screening rooms' with page navigation button '1'. |
| **V. Add / Edit Screening Room Modal Form** | |
| **Room Code** | Text input. Required. Max length: 20 chars. Pattern: ^[A-Za-z0-9_-]+$. Unique auditorium identifier. Placeholder: 'e.g. ROOM-01'. |
| **Room Name** | Text input. Required. Max length: 100 chars. Auditorium name. Placeholder: 'e.g. Cinema 01 — IMAX Laser'. |
| **Room Type / Tech** | Dropdown. Required. Options: 'Standard Laser', 'Gold Class VIP', 'IMAX Laser', '3D Digital'. |
| **Operating Status** | Dropdown. Required. Options: 'Ready', 'Screening', 'Maintenance', 'Cleaning'. |
| **Total Rows** | Numeric input. Required. Range: 1 - 100. Number of seat rows. |
| **Seats Per Row** | Numeric input. Required. Range: 1 - 100. Number of seats in each row. |
| **Capacity (total seats)** | Numeric input. Required. Range: 1 - 5000. Total auditorium seating capacity.<br>- Consistency Business Rule: In CINEORA, standard auditoriums are full rectangular grids where every coordinate (row, col) is a seat, and aisles are purely display spacing. The system enforces: Capacity = Total Rows × Seats Per Row. The modal provides real-time calculation hint ('Rows × Seats/row'), auto-fills capacity, and blocks submission with error if capacity does not equal rows × seats/row. |
| **Projector Model** | Text input. Optional. Hardware projector model (e.g. 'Barco SP2K-11 Laser'). |
| **Sound System** | Text input. Optional. Sound system standard (e.g. 'Dolby 7.1 Studio Audio'). |
| **Description / Notes** | Textarea. Optional. Max: 500 chars. Equipment and amenity notes. |
| **Save room** | Button. Submits form via REST API (POST/PUT /api/manager/rooms) with JSON payload. |
| **Cancel** | Button. Closes modal without saving. |

## 9. Ảnh 8 — Food and Beverage Management (SCR-MGR-09 / UC-32)

- **URL Route:** `/manager/fb`
- **Source Template:** `src/main/resources/templates/manager/fb.html`
- **Mô tả chức năng:** Màn hình quản lý danh mục bắp rang, nước ngọt, combo ẩm thực, giá bán lẻ, giá vốn và tỷ suất lợi nhuận (Screen 61 / UC-32).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Primary Action** | |
| **Page Title** | Header label. Displays 'Food and Beverage Management' with subtitle: 'Manage the catalog of popcorn, soft drinks, food combos and snacks served at the counter and pre-ordered online.' |
| **Add new product** | Button (+ icon, red). Opens '+ Add new product' modal form to register a new concession item. |
| **II. Inventory KPI Cards** | |
| **Total products** | Numeric badge (fastfood icon). Total items on concession menu (e.g. '8'). Subtext: 'Items and combos on the menu'. |
| **Combos** | Numeric badge (shopping_bag icon). Count of value bundle packages (e.g. '2'). Subtext: 'Value bundles'. |
| **On sale** | Numeric badge (storefront icon). Count of active concession items available to customers (e.g. '8'). Subtext: 'Available to customers'. |
| **Suspended** | Numeric badge (stop icon). Count of disabled concession items (e.g. '0'). Subtext: 'Not available for sale'. Note: 'Suspended' denotes items administratively deactivated in the catalog (status = 'Inactive'), distinct from daily physical stock outages. |
| **III. Category Filter Tabs & Search Bar** | |
| **Category Tabs** | Horizontal filter pills: 'All items (8)', 'Combos (2)', 'Popcorn & Food (4)', 'Drinks (2)', 'Snacks'. Filters concession catalog by category. |
| **Search by name or SKU** | Text input. Placeholder: 'Search by name or SKU...'. Searches product code and title. |
| **All statuses** | Dropdown. Options: 'All statuses', 'On sale' (Active), 'Suspended' (Inactive). Default: 'All statuses'. |
| **Filter data** | Button. Submits search query to filter catalog cards. |
| **Reset** | Button. Resets category and search filters to default. |
| **IV. Product Cards Grid** | |
| **Product Thumbnail** | Concession image photo (aspect ratio 4:3) with fallback concession graphic. |
| **Product Name** | Heading (bold). Title of concession item (e.g. 'Large Caramel Popcorn', 'Hot Cheese Hotdog', 'Fresh Lemon Sprite 32oz (Large)', 'Fresh Coca-Cola 32oz (Large)'). |
| **Status Badge** | Badge. Values: 'ON SALE' (Green badge), 'SUSPENDED' (Gray/Red badge), or 'PAUSED (MISSING ITEM)' (Orange badge - displayed if a combo bundle contains an inactive single component item). |
| **Recipe / Component Subtext** | Muted text paragraph. Description of food ingredients or combo bundle composition. |
| **Selling Price** | Currency column (VND). Format: '#,##0 ₫' (e.g. '55,000 đ', '45,000 đ'). Retail price charged to customers. |
| **Cost Price** | Currency column (VND). Format: 'Cost: #,##0 ₫' (e.g. 'Cost: 15,000 đ', 'Cost: 0 đ'). Direct food cost / Cost of Goods Sold (COGS). |
| **Profit Margin Badge** | Percentage badge. Formula: ((Selling Price - Cost Price) / Selling Price) * 100%. Format: 'Margin: XX.X%' (e.g. 72.7%, 100.0%).<br>- Calculation & Edge Case Handling: When Selling Price <= 0 or cost is null, margin is 0.0%. When Cost Price is recorded as 0 đ (e.g. cost not yet entered), the mathematical formula yields 100.0% (representing theoretical gross markup before cost input, not actual net profit). |
| **Card Actions** | Action Button Group:<br>- Edit (Pencil icon / text): Opens edit modal.<br>- Toggle Status (Switch/Square button): Activates or suspends product.<br>- Delete Item (Trash icon): Deletes product with confirmation (blocked if order records exist). |
| **Product Cards Grid Layout** | 4-column responsive grid layout (4 items per row, 2 rows per page, total 8 items per page). When catalog exceeds 8 items, subsequent items paginate to page 2. |
| **Pagination Summary** | Footer row displaying 'Showing {start} – {end} of {total} products' with pagination controls (‹, 1, 2, ›). Configured with 8 items per page (4 items × 2 rows). |
| **V. Add / Edit Product Modal Form** | |
| **Item Code / SKU** | Text input. Required. Max length: 50 chars. Unique product code (e.g. 'FC-CARMEL', 'FB-003'). |
| **Category** | Dropdown. Required. Options: 'Combo', 'Popcorn', 'Drink', 'Food', 'Snack'. |
| **Product Name** | Text input. Required. Max length: 100 chars. Concession item title. Placeholder: 'e.g. Large Caramel Popcorn'. |
| **Selling Price (VND)** | Numeric input. Required. Min: 0, Max: 10,000,000, Step: 1000. Retail price in VND. |
| **Cost price / COGS (VND)** | Numeric input. Required. Min: 0, Max: 10,000,000, Step: 1000. Direct inventory cost in VND. |
| **Product Image (URL or file)** | Composite control. Supports direct URL input (max 500 chars) or local image file selection ('Upload image' button). Accepts PNG, JPEG, WEBP, GIF up to 5MB, uploaded to /uploads/fb/ with live thumbnail preview. |
| **Description** | Textarea. Optional. Max: 500 chars. Detailed ingredients or bundle notes. |
| **Combo Components Selector** | Dynamic checklist. Appears conditionally when Category == 'Combo'. Allows checking constituent single items and specifying quantities (1 to 10) included in the bundle. |
| **Save product** | Button. Submits product creation or modification to /manager/fb/save. |
| **Cancel** | Button. Closes modal without saving. |

## 10. Ảnh 10 — Management Overview / Dashboard (SCR-MGR-10 / UC-35)

- **URL Route:** `/manager/dashboard`
- **Source Template:** `src/main/resources/templates/manager/dashboard.html`
- **Mô tả chức năng:** Màn hình trung tâm tổng quan điều hành toàn rạp chiếu: giám sát 6 chỉ số vận hành chính, biểu đồ doanh thu 7 ngày gần nhất, lịch chiếu hôm nay và giao dịch vé gần nhất (Screen 01 / UC-35).

| Field Name | Description |
| :--- | :--- |
| **I. Header & Operational Date** | |
| **Page Title** | Header label. Displays 'Management Overview' with subtitle: 'Monitor cinema activity and operational performance.' |
| **Calendar Date Indicator** | Read-only date pill (calendar icon). Format: 'dddd, DD/MM/YYYY' (e.g. 'Saturday, 14/11/2026'). Displays current system operating date dynamically. |
| **II. Cinema Operations KPI Metric Cards (6 Cards)** | |
| **Total movies** | Numeric KPI card (film icon). Value: '{count}' (e.g. 6). Subtext: '{showing} now showing • {upcoming} coming soon'. |
| **Total genres** | Numeric KPI card (category icon). Value: '{count}' (e.g. 13). Subtext: '{seatTypes} seat types configured'. |
| **Today's showtimes** | Numeric KPI card (schedule icon). Value: '{count}' (e.g. 0). Subtext: '{active_rooms} of {total_rooms} rooms available' (displays active operating auditoriums versus total cinema rooms, e.g. '2 of 5 rooms available'). |
| **Tickets sold** | Numeric KPI card (ticket icon). Value: '{count}' (e.g. 0). Subtext: '{rate}% seat occupancy' or 'No bookings yet'. |
| **Today's revenue** | Currency KPI card (payments icon). Value formatted in VND: '#,##0 ₫' (e.g. '0 ₫'). Subtext: '{weekly_total} ₫ in 7 days'. |
| **F&B items on sale** | Numeric KPI card (fastfood icon). Value: '{active_items}' (e.g. 8). Subtext: 'of {total_items} items in the menu'. |
| **III. Revenue Overview Chart & Today's Showtimes Widget** | |
| **Revenue Overview Heading** | Heading 2: 'Revenue Overview' with 7-day revenue badge formatted '#,##0 ₫ / 7 days'. |
| **Full report →** | Link. Navigates directly to the full revenue audit report screen (/manager/reports). |
| **Revenue Breakdown Legend** | Color indicators: '● Ticket revenue (XX%)' (Red indicator) and '● Popcorn & Combos (XX%)' (Gold indicator). |
| **Weekly Revenue Chart Canvas** | Dual-bar column chart. Visualizes ticket and concession revenue trends across the 7 most recent days ending with Today (e.g. Sun, Mon, Tue, Wed, Thu, Fri, Today). Displays two vertical bars per day (Red for Ticket, Gold for F&B). If zero sales occurred across the week, displays notice: 'No paid bookings in the last 7 days yet.' |
| **Today's showtimes Header** | Heading 2: 'Today's showtimes' with navigation link 'View all →' (navigating to /manager/showtimes). |
| **Empty Showtimes Notice** | Card placeholder displayed when no showtimes are scheduled for today: 'No showtimes scheduled for today.' |
| **Live Showtime Card (When Active)** | Showtime broadcast card displaying Start Time (HH:mm), Movie Title, Age Rating pill (e.g. T18), Screening Room Name, Capacity, and Status badge ('Selling tickets'). |
| **IV. Recent Bookings and Transactions Table** | |
| **Section Header & Subtitle** | Heading 2: 'Recent bookings and transactions'. Subtitle: 'The latest paid online and counter bookings in the last 7 days.' |
| **Table Actions Toolbar** | Toolbar button group: 'Filter data' button (filter icon) and 'Export CSV' button (download icon). |
| **Booking ID** | Code string (bold). Unique booking identifier (e.g. '#BK-10023'). |
| **Customer** | Text column. Buyer full name and contact information (phone/email). |
| **Movie** | Composite string. Movie title with technology format tag (2D / 3D / IMAX). |
| **Showtime & Room** | Composite string. Screening time slot and auditorium room name. |
| **Status** | Operational booking status pill: 'Paid' / 'CONFIRMED' (Green badge, indicating active settled transaction) or 'Completed' (Gold badge, indicating screening has concluded). Note: The query utilizes the paid_bookings CTE (RevenueReportDao.java:321-329), thus only successfully paid and confirmed orders in the last 7 days are listed; unpaid/cancelled bookings are excluded. |
| **Total** | Currency column (VND). Net transaction total collected formatted as '#,##0 ₫'. |
| **Empty Transactions State** | Placeholder row displayed when no transactions exist: 'No bookings in the last 7 days.' |