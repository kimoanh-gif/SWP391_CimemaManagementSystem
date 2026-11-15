const fs = require('fs');
const path = require('path');
const {
    Document,
    Packer,
    Paragraph,
    TextRun,
    Table,
    TableRow,
    TableCell,
    WidthType,
    HeadingLevel,
    AlignmentType,
    ShadingType
} = require('docx');

const FONT_NAME = 'Arial';
const COLOR_HEADER_BG = 'FDE9D9'; // Standard SRS Peach header
const COLOR_SECTION_BG = 'F4F5F7'; // Clean section divider
const COLOR_PRIMARY = '93000A'; // Cineora burgundy / dark red
const COLOR_TEXT = '222222';

const mdLines = [
    "# CINEORA CINEMA MANAGEMENT SYSTEM",
    "## SOFTWARE REQUIREMENTS SPECIFICATION (SRS) — REPORT 1",
    "### BẢNG MÔ TẢ CHI TIẾT CÁC MÀN HÌNH QUẢN TRỊ (SCREEN SPECIFICATIONS)",
    "*(Định dạng chuẩn 100% theo mẫu Field Name & Description của Ảnh 9 / SRS Page 158)*\n",
    "---"
];

function createHeading1(text) {
    mdLines.push("\n## " + text + "\n");
    return new Paragraph({
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 280, after: 120 },
        children: [
            new TextRun({
                text: text,
                bold: true,
                font: FONT_NAME,
                size: 24,
                color: COLOR_PRIMARY
            })
        ]
    });
}

function createMetaInfo(url, template, description) {
    mdLines.push("- **URL Route:** `" + url + "`");
    mdLines.push("- **Source Template:** `" + template + "`");
    mdLines.push("- **Mô tả chức năng:** " + description + "\n");
    mdLines.push("| Field Name | Description |");
    mdLines.push("| :--- | :--- |");
    return [
        new Paragraph({
            spacing: { before: 60, after: 40 },
            children: [
                new TextRun({ text: "• URL Endpoint: ", bold: true, font: FONT_NAME, size: 19 }),
                new TextRun({ text: url, font: FONT_NAME, size: 19, color: '0052CC' }),
                new TextRun({ text: "    • Source Template: ", bold: true, font: FONT_NAME, size: 19 }),
                new TextRun({ text: template, font: FONT_NAME, size: 19, italics: true, color: '444444' })
            ]
        }),
        new Paragraph({
            spacing: { before: 40, after: 140 },
            children: [
                new TextRun({ text: "• Functional Purpose: ", bold: true, font: FONT_NAME, size: 19 }),
                new TextRun({ text: description, font: FONT_NAME, size: 19 })
            ]
        })
    ];
}

function createSectionRow(sectionTitle) {
    mdLines.push("| **" + sectionTitle + "** | |");
    return new TableRow({
        children: [
            new TableCell({
                columnSpan: 2,
                shading: { type: ShadingType.CLEAR, fill: COLOR_SECTION_BG },
                margins: { top: 90, bottom: 90, left: 140, right: 140 },
                children: [
                    new Paragraph({
                        children: [
                            new TextRun({
                                text: sectionTitle,
                                bold: true,
                                font: FONT_NAME,
                                size: 19,
                                color: '333333'
                            })
                        ]
                    })
                ]
            })
        ]
    });
}

function createDataRow(fieldName, description) {
    const cleanDesc = description.replace(/\n/g, "<br>").replace(/\|/g, "\\|");
    mdLines.push("| **" + fieldName + "** | " + cleanDesc + " |");
    return new TableRow({
        children: [
            new TableCell({
                width: { size: 30, type: WidthType.PERCENTAGE },
                margins: { top: 75, bottom: 75, left: 130, right: 130 },
                children: [
                    new Paragraph({
                        children: [
                            new TextRun({
                                text: fieldName,
                                bold: true,
                                font: FONT_NAME,
                                size: 18,
                                color: COLOR_TEXT
                            })
                        ]
                    })
                ]
            }),
            new TableCell({
                width: { size: 70, type: WidthType.PERCENTAGE },
                margins: { top: 75, bottom: 75, left: 130, right: 130 },
                children: [
                    new Paragraph({
                        children: [
                            new TextRun({
                                text: description,
                                font: FONT_NAME,
                                size: 18,
                                color: COLOR_TEXT
                            })
                        ]
                    })
                ]
            })
        ]
    });
}

function createTable(rows) {
    const headerRow = new TableRow({
        tableHeader: true,
        children: [
            new TableCell({
                width: { size: 30, type: WidthType.PERCENTAGE },
                shading: { type: ShadingType.CLEAR, fill: COLOR_HEADER_BG },
                margins: { top: 110, bottom: 110, left: 130, right: 130 },
                children: [
                    new Paragraph({
                        alignment: AlignmentType.CENTER,
                        children: [
                            new TextRun({
                                text: "Field Name",
                                bold: true,
                                font: FONT_NAME,
                                size: 19,
                                color: '000000'
                            })
                        ]
                    })
                ]
            }),
            new TableCell({
                width: { size: 70, type: WidthType.PERCENTAGE },
                shading: { type: ShadingType.CLEAR, fill: COLOR_HEADER_BG },
                margins: { top: 110, bottom: 110, left: 130, right: 130 },
                children: [
                    new Paragraph({
                        alignment: AlignmentType.CENTER,
                        children: [
                            new TextRun({
                                text: "Description",
                                bold: true,
                                font: FONT_NAME,
                                size: 19,
                                color: '000000'
                            })
                        ]
                    })
                ]
            })
        ]
    });

    return new Table({
        width: { size: 100, type: WidthType.PERCENTAGE },
        rows: [headerRow, ...rows]
    });
}

function createInventoryTable() {
    const headers = ["Image Ref", "Screen Name", "URL Route", "Screen ID", "Use Case"];
    const data = [
        ["Ảnh 1", "Revenue Report (Báo cáo Doanh thu & Thống kê)", "/manager/reports", "SCR-MGR-01", "UC-34 (Screen 47 & 49)"],
        ["Ảnh 2", "Movie Management (Quản lý Danh mục Phim)", "/manager/movies", "SCR-MGR-02", "UC-28 (Screen 42)"],
        ["Ảnh 3", "Showtime Management (Quản lý Lịch chiếu Phim)", "/manager/showtimes", "SCR-MGR-03", "UC-30 (Screen 59)"],
        ["Ảnh 3b", "Create Room Schedule (Xếp lịch chiếu theo phòng)", "/manager/showtimes/scheduler", "SCR-MGR-04", "UC-30b (Scheduler)"],
        ["Ảnh 4", "Seat Type Management (Quản lý Loại ghế & Giá)", "/manager/seat-types", "SCR-MGR-05", "UC-31 (Screen 54)"],
        ["Ảnh 5", "Seat Map Management (Quản lý Sơ đồ Ghế)", "/manager/seats", "SCR-MGR-06", "UC-31 (Screen 55)"],
        ["Ảnh 6", "Movie Genre Management (Quản lý Thể loại Phim)", "/manager/genres", "SCR-MGR-07", "UC-28b (Genres)"],
        ["Ảnh 7", "Screening Room Management (Quản lý Phòng chiếu)", "/manager/rooms", "SCR-MGR-08", "UC-29 (Screen 56)"],
        ["Ảnh 8", "Food and Beverage Management (Quản lý Bắp nước)", "/manager/fb", "SCR-MGR-09", "UC-32 (Screen 61)"],
        ["Ảnh 10", "Management Overview (Tổng quan Quản trị Rạp)", "/manager/dashboard", "SCR-MGR-10", "UC-35 (Dashboard)"]
    ];

    const headerCells = headers.map(h => new TableCell({
        shading: { type: ShadingType.CLEAR, fill: COLOR_HEADER_BG },
        margins: { top: 90, bottom: 90, left: 90, right: 90 },
        children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: h, bold: true, font: FONT_NAME, size: 18 })] })]
    }));

    const rows = [
        new TableRow({ tableHeader: true, children: headerCells }),
        ...data.map(item => new TableRow({
            children: item.map((val, idx) => new TableCell({
                margins: { top: 70, bottom: 70, left: 90, right: 90 },
                children: [new Paragraph({
                    alignment: (idx === 0 || idx >= 3) ? AlignmentType.CENTER : AlignmentType.LEFT,
                    children: [new TextRun({ text: val, font: FONT_NAME, size: 17, bold: idx === 1 })]
                })]
            }))
        }))
    ];

    return new Table({
        width: { size: 100, type: WidthType.PERCENTAGE },
        rows: rows
    });
}

async function run() {
    const doc = new Document({
        styles: {
            default: {
                document: {
                    run: {
                        font: FONT_NAME,
                        size: 20
                    }
                }
            }
        },
        sections: [{
            properties: {
                page: {
                    margin: { top: 1440, bottom: 1440, left: 1440, right: 1440 }
                }
            },
            children: [
                new Paragraph({
                    alignment: AlignmentType.CENTER,
                    spacing: { before: 80, after: 60 },
                    children: [
                        new TextRun({
                            text: "CINEORA CINEMA MANAGEMENT SYSTEM",
                            bold: true,
                            font: FONT_NAME,
                            size: 30,
                            color: COLOR_PRIMARY
                        })
                    ]
                }),
                new Paragraph({
                    alignment: AlignmentType.CENTER,
                    spacing: { before: 0, after: 120 },
                    children: [
                        new TextRun({
                            text: "SOFTWARE REQUIREMENTS SPECIFICATION (SRS) — REPORT 1",
                            bold: true,
                            font: FONT_NAME,
                            size: 22,
                            color: '333333'
                        })
                    ]
                }),
                new Paragraph({
                    alignment: AlignmentType.CENTER,
                    spacing: { before: 0, after: 200 },
                    children: [
                        new TextRun({
                            text: "BẢNG MÔ TẢ CHI TIẾT CÁC MÀN HÌNH QUẢN TRỊ (SCREEN SPECIFICATION TABLES)\n(Chuẩn hóa định dạng hai cột Field Name & Description theo mẫu SRS Trang 158)",
                            font: FONT_NAME,
                            italics: true,
                            size: 19,
                            color: '555555'
                        })
                    ]
                }),

                new Paragraph({
                    spacing: { before: 160, after: 80 },
                    children: [
                        new TextRun({
                            text: "BẢNG TỔNG HỢP DANH MỤC MÀN HÌNH QUẢN TRỊ (SCREEN INVENTORY)",
                            bold: true,
                            font: FONT_NAME,
                            size: 20,
                            color: COLOR_PRIMARY
                        })
                    ]
                }),
                new Paragraph({
                    spacing: { before: 0, after: 100 },
                    children: [
                        new TextRun({
                            text: "Ghi chú: Toàn bộ 10 màn hình quản trị được đánh mã liên tục từ SCR-MGR-01 đến SCR-MGR-10 (trong đó SCR-MGR-04 là giao diện xếp lịch theo phòng tách từ phân hệ Showtime). Ảnh 9 là trang mẫu SRS Trang 158 dùng làm quy chuẩn đối chiếu hình thức.",
                            font: FONT_NAME,
                            italics: true,
                            size: 17,
                            color: '666666'
                        })
                    ]
                }),
                createInventoryTable(),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 1: REVENUE REPORT
                // ==========================================
                createHeading1("1. Ảnh 1 — Revenue Report (SCR-MGR-01 / UC-34)"),
                ...createMetaInfo(
                    "/manager/reports",
                    "src/main/resources/templates/manager/reports.html",
                    "Màn hình theo dõi, kiểm toán doanh thu vé & bắp nước, hiệu suất bán hàng và xuất báo cáo kiểm toán Excel (CSV) (Screen 47 & 49 / UC-34)."
                ),
                createTable([
                    createSectionRow("I. Header & Audit Controls"),
                    createDataRow("Page Title", "Header label. Displays 'Revenue Report' with descriptive subtitle: 'Ticket and F&B revenue, movie performance and Excel (CSV) export for auditing (Screen 47 & 49 / UC-34).'"),
                    createDataRow("Current Date Indicator", "Read-only badge. Format: '● dddd, DD/MM/YYYY' (e.g. '● Saturday, 14/11/2026'). Displays current system operating date."),
                    createDataRow("Role Badge", "Status pill. Text: 'MANAGER'. Indicates user role authorization level."),
                    createDataRow("Export Excel (CSV)", "Button. Generates and downloads financial audit CSV file encoded in UTF-8 with BOM for Vietnamese Excel compatibility."),
                    createDataRow("Print Report", "Button. Triggers browser print dialog using print-optimized CSS layout."),

                    createSectionRow("II. Filter & Criteria Bar"),
                    createDataRow("FROM", "Date picker. Format: DD/MM/YYYY. Default: First day of current operating month (e.g. 01/11/2026). Required. Filters records on or after this date."),
                    createDataRow("TO", "Date picker. Format: DD/MM/YYYY. Default: Last day of current operating month (e.g. 30/11/2026). Required. Validation: Must be greater than or equal to FROM date."),
                    createDataRow("REPORT TYPE", "Dropdown. Options: 'Daily overview' (DAILY), 'Movie performance' (MOVIE), 'Transaction details' (TRANSACTIONS). Default: 'Daily overview'."),
                    createDataRow("Filter data", "Button. Submits selected date range and report type criteria to refresh KPI cards and table data."),

                    createSectionRow("III. Revenue KPI Metric Cards"),
                    createDataRow("TOTAL NET REVENUE", "Read-only currency card (VND). Formatted as '#,##0 ₫'. Total actual collected revenue. Business rule BR-22 formula: Net Revenue = (Gross Ticket Sales + Gross F&B Sales) - Total Voucher/Point Discounts. Subtext: 'Collected cash after discounts (BR-22)'."),
                    createDataRow("TICKET REVENUE (GROSS)", "Read-only currency card (VND). Formatted as '#,##0 ₫'. Gross sales from movie ticket admissions before discount deductions. Subtext: 'XX.X% of gross sales'."),
                    createDataRow("F&B REVENUE (GROSS)", "Read-only currency card (VND). Formatted as '#,##0 ₫'. Gross sales from concession items before discount deductions. Subtext: 'XX.X% of gross sales'."),
                    createDataRow("TICKETS SOLD", "Read-only numeric card. Total number of successfully paid seat tickets issued within the filtered period. Subtext: 'Paid seats'."),
                    createDataRow("SUCCESSFUL ORDERS", "Read-only numeric card. Total count of verified orders. Strictly defined as transactions where Booking.Status = 'CONFIRMED' and Payment.Status = 'PAID' (with confirmed PaidAt timestamp, verified in RevenueReportDao.java:32-44). Online bookings must be paid via gateway; counter POS orders transition to CONFIRMED only when cash/card payment is settled by cashier. Unpaid bookings ('PAYMENT_PENDING', 'CANCELLED', 'EXPIRED') are excluded to ensure financial audit integrity per BR-22/BR-41. Subtext: 'Paid / Confirmed transactions'."),

                    createSectionRow("IV. Daily Revenue Summary Table"),
                    createDataRow("Section Header", "Table title: 'Daily revenue summary' with active filter date range badge: 'Data from DD/MM/YYYY to DD/MM/YYYY'."),
                    createDataRow("DATE", "Date column. Format: DD/MM/YYYY. Listed in chronological order."),
                    createDataRow("TICKET REVENUE (VND)", "Currency column. Daily gross ticket revenue formatted as '#,##0 ₫'."),
                    createDataRow("F&B REVENUE (VND)", "Currency column. Daily gross concession revenue formatted as '#,##0 ₫'."),
                    createDataRow("DISCOUNT (VND)", "Currency column. Total promotional voucher and loyalty points deducted on date formatted as '#,##0 ₫'."),
                    createDataRow("TOTAL REVENUE (VND)", "Currency column. Daily net revenue formula: (Ticket + F&B) - Discount formatted as '#,##0 ₫'."),
                    createDataRow("TICKETS SOLD", "Numeric column. Number of paid seat tickets issued on date."),
                    createDataRow("ORDERS", "Numeric column. Count of successfully completed transactions on date."),
                    createDataRow("Empty Data State", "Placeholder banner. Displayed when no revenue exists in period: 'No revenue data in this period. Please select another date range or adjust filters.'"),
                    createDataRow("TOTAL (Summary Row)", "Summary table footer row. Automatically calculates sums across all revenue, ticket, and order columns for the selected period.")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 2: MOVIE MANAGEMENT
                // ==========================================
                createHeading1("2. Ảnh 2 — Movie Management (SCR-MGR-02 / UC-28)"),
                ...createMetaInfo(
                    "/manager/movies",
                    "src/main/resources/templates/manager/movies.html",
                    "Màn hình quản lý danh mục phim, phân loại độ tuổi, thể loại, thông tin phát hành và lịch chiếu của hệ thống CINEORA (Screen 42 / UC-28)."
                ),
                createTable([
                    createSectionRow("I. Header & Primary Action"),
                    createDataRow("Page Title", "Header label. Displays 'Movie Management' with subtitle: 'Manage movie information, classification and release status in the CINEORA cinema system.'"),
                    createDataRow("Add new movie", "Button (+ icon). Opens the 'Add new movie' modal dialog to register a new title."),

                    createSectionRow("II. KPI Status Badges"),
                    createDataRow("Now Showing", "Numeric badge (play icon). Count of movies currently in active theatrical run (DB status = 'Now Showing')."),
                    createDataRow("Coming Soon", "Numeric badge (calendar icon). Count of upcoming movies scheduled for release (DB status = 'Upcoming')."),
                    createDataRow("Ended / Archived", "Numeric badge (archive icon). Count of movies finished run or deactivated (DB status = 'Ended' or 'Archived')."),
                    createDataRow("All movies", "Numeric badge (film icon). Total count of movie records registered in the cinema catalog."),

                    createSectionRow("III. Filter & Search Bar"),
                    createDataRow("Search Keyword", "Text input. Placeholder: 'Search by title, director, cast...'. Searches across movie title, director, and actor names."),
                    createDataRow("Genre Filter", "Dropdown. Options: 'All genres' plus all active genres in system. Default: 'All genres'."),
                    createDataRow("Age Rating Filter", "Dropdown. Options: 'Age rating: All', 'P', 'K', 'T13', 'T16', 'T18'. Default: 'Age rating: All'."),
                    createDataRow("Status Filter", "Dropdown. Options: 'All statuses', 'Now Showing', 'Coming Soon', 'Ended'. Default: 'All statuses'."),
                    createDataRow("Filter", "Button. Applies selected search and filter criteria to the movie table."),
                    createDataRow("Clear filters", "Button. Resets all filter inputs and reloads the default movie list."),

                    createSectionRow("IV. Movie List Table"),
                    createDataRow("Table Header Badge", "Header text badge: 'Movie list ({count} titles)'. Indicates total matched results."),
                    createDataRow("Movie title / Original title", "Composite column. Displays movie poster thumbnail (aspect ratio 2:3), primary Vietnamese title (bold), and original international title (muted subtext)."),
                    createDataRow("Genre", "Tag badges. List of active genre tags associated with movie (e.g. 'Hành Động', 'Hoạt Hình'). Displays 'Unclassified' if no genre is assigned."),
                    createDataRow("Age rating", "Color-coded tag badge. Values: P (Green - General all ages), K (Blue - Under 13 with guardian), T13 (Orange - Ages 13+), T16 (Amber - Ages 16+), T18 (Red - Ages 18+)."),
                    createDataRow("Duration", "Numeric column. Movie runtime duration in minutes formatted as '{value} min'."),
                    createDataRow("Release", "Date column formatted 'DD/MM/YY'. Displays premiere release date."),
                    createDataRow("Lifecycle status", "Status badge. Mapping to Database:\n- 'Coming Soon' -> DB: 'Upcoming'\n- 'Now Showing' -> DB: 'Now Showing'\n- 'Ended' -> DB: 'Ended'\n- 'Archived' -> DB: 'Archived'."),
                    createDataRow("Showtimes", "Link formatted '{n} shows'. Clicking navigates to /manager/showtimes?movieId={id} to view scheduled slots."),
                    createDataRow("Actions", "Action Button Group (4 buttons per row):\n- View Details (Eye icon): Opens read-only quick view popup dialog.\n- Edit Movie (Pencil icon): Opens Edit Movie modal prefilled with current movie data.\n- Stop/Archive or Reactivate (Square/Play icon): Toggles status between Ended/Archived and Active.\n- Delete Movie (Trash icon): Deletes movie after confirmation prompt (blocked if showtimes or bookings exist)."),
                    createDataRow("Pagination Summary", "Footer row: 'Showing {start} – {end} of {filtered} matching movies (system total: {total})' with navigation buttons (‹, 1, 2, ›). Page size: 5 movies per page."),

                    createSectionRow("V. Add / Edit Movie Modal Form"),
                    createDataRow("Movie title", "Text input. Required. Max length: 150 chars. Primary film title. Placeholder: 'e.g. Fantastic Aura: Eternity'."),
                    createDataRow("Original / International title", "Text input. Optional. Max length: 150 chars. Original English title. Placeholder: 'e.g. Dune: Part Two'."),
                    createDataRow("Genres Selection", "Multi-checkbox grid. Lists active genres in system. At least 1 genre is required."),
                    createDataRow("Duration (minutes)", "Numeric input. Required. Min: 1, Max: 600. Movie runtime used for automatic showtime scheduling."),
                    createDataRow("Age rating", "Dropdown. Required. Options: P (All ages), K (Under 13 with guardian), T13 (13+), T16 (16+), T18 (18+)."),
                    createDataRow("Status", "Dropdown. Required. Options: 'Coming Soon' (Upcoming), 'Now Showing' (Now Showing), 'Ended' (Ended), 'Archived' (Archived)."),
                    createDataRow("Release date", "Date picker. Format: DD/MM/YYYY. Premiere theatrical release date."),
                    createDataRow("Director", "Text input. Optional. Max length: 100 chars. Director name."),
                    createDataRow("Cast / Actors", "Text input. Optional. Max length: 255 chars. Key actors."),
                    createDataRow("Language", "Text input. Optional. Max length: 50 chars (e.g. 'English, Vietsub')."),
                    createDataRow("Vertical poster (URL or file)", "Composite control. Supports direct image URL input or local file upload ('Upload image' button). Uploads PNG/JPEG/WEBP asynchronously via POST /api/manager/movies/upload-image and displays live 2:3 thumbnail preview."),
                    createDataRow("Horizontal banner (URL or file)", "Composite control. Supports direct image URL input or local file upload ('Upload image' button). Uploads PNG/JPEG/WEBP asynchronously (16:9 ratio) and displays live banner preview."),
                    createDataRow("Trailer URL", "URL input. Max length: 500 chars. Valid YouTube trailer link."),
                    createDataRow("Synopsis / Description", "Textarea with character counter (0 / 1000). Summary of movie plot."),
                    createDataRow("Save movie", "Button. Submits form via AJAX (POST /api/manager/movies for create, PUT /api/manager/movies/{id} for update) with JSON body payload."),
                    createDataRow("Cancel", "Button. Discards changes and closes modal.")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 3: SHOWTIME MANAGEMENT
                // ==========================================
                createHeading1("3. Ảnh 3 — Showtime Management (SCR-MGR-03 / UC-30)"),
                ...createMetaInfo(
                    "/manager/showtimes",
                    "src/main/resources/templates/manager/showtimes.html",
                    "Màn hình theo dõi lịch chiếu phim theo phòng, giám sát suất chiếu hôm nay, quản lý bộ đệm dọn phòng 15 phút (BR-09) và điều hướng sang giao diện xếp lịch theo phòng (Screen 59 / UC-30)."
                ),
                createTable([
                    createSectionRow("I. Header & Navigation"),
                    createDataRow("Page Title", "Header label. Displays 'Showtime Management' with subtitle: 'Track and manage movie schedules by screening room, with a 15-minute cleaning buffer and automatic conflict checks.'"),
                    createDataRow("Create Room Schedule", "Button (calendar icon, red). Primary action button. Navigates directly to the room schedule planner screen (/manager/showtimes/scheduler)."),

                    createSectionRow("II. Operational KPI Cards"),
                    createDataRow("Showtimes on DD/MM/YYYY", "Numeric card (schedule icon). Dynamic title reflects filtered date (e.g. 'Showtimes on 14/11/2026'). Count of showtimes scheduled for that date. Subtext: 'Date: DD/MM/YYYY'."),
                    createDataRow("Upcoming (Scheduled)", "Numeric card (calendar icon). Count of scheduled showtimes waiting to air on the filtered date. Subtext: 'Waiting for show time'."),
                    createDataRow("Room cleaning buffer", "Operational standard card (cleaning icon). Value: '15 min'. Subtext: 'Added after every showtime (BR-09)'. Fixed operational buffer for cleaning auditorium between screenings."),
                    createDataRow("In progress (Showing)", "Numeric card (play icon). Count of screenings currently broadcast live in auditoriums. Subtext: 'Live on air'."),

                    createSectionRow("III. Filter & Search Bar"),
                    createDataRow("From date", "Date picker. Format: DD/MM/YYYY. Default: Current operating date. Required. Filters showtimes on or after this date."),
                    createDataRow("To date", "Date picker. Format: DD/MM/YYYY. Default: Current operating date. Required. Validation: Must be >= From date."),
                    createDataRow("All rooms", "Dropdown. Options: 'All rooms' plus active screening rooms. Default: 'All rooms'."),
                    createDataRow("All movies", "Dropdown. Options: 'All movies' plus active movie catalog. Default: 'All movies'."),
                    createDataRow("All statuses", "Dropdown. Options: 'All statuses', 'Scheduled', 'Showing', 'Ended', 'Cancelled'. Default: 'All statuses'."),
                    createDataRow("Filter", "Button. Applies selected filter criteria to the showtime table."),
                    createDataRow("Reset", "Button. Resets all filters to today's date and all rooms/movies."),

                    createSectionRow("IV. Showtime List Table"),
                    createDataRow("Table Header Badge", "Header badge: 'Showtime list ({count} showtimes)'. Indicates total matched slots."),
                    createDataRow("Date & Time", "Composite DateTime string formatted 'HH:mm, DD/MM/YYYY'. Displays start time and calculated film end time."),
                    createDataRow("Movie", "Composite cell displaying Movie Title, Age Rating badge (e.g. T18, T13), and screening format (2D / 3D / IMAX)."),
                    createDataRow("Screening room", "Text column. Identifier of auditorium (e.g. 'Cinema 03 - Laser Compact')."),
                    createDataRow("Duration", "Numeric column. Runtime duration of movie in minutes."),
                    createDataRow("Base ticket price", "Currency column (VND) formatted as '#,##0 ₫'. Standard reference ticket price before seat type multipliers are applied."),
                    createDataRow("Status", "Status badge. Values: 'Scheduled' (Blue), 'Showing' (Green), 'Ended' (Gray), 'Cancelled' (Red)."),
                    createDataRow("Actions", "Action Button Group:\n- Stop Showtime: Enforces BR-54 emergency stop, transitioning status to 'CANCELLED' while preserving sold tickets.\n- Delete Showtime: Deletes showtime only if zero tickets have been sold (blocked if bookings exist)."),
                    createDataRow("Empty Schedule Notice", "Placeholder state displayed when no showtimes match filter: 'No showtimes for the selected date range or filters. Click Create new showtime to schedule one.' (Instructional call-to-action referring to scheduling via the 'Create Room Schedule' button in the header toolbar, verified in showtimes.html:203).")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 4: CREATE ROOM SCHEDULE (SCHEDULER)
                // ==========================================
                createHeading1("4. Ảnh 3b — Create Room Schedule (SCR-MGR-04 / UC-30b)"),
                ...createMetaInfo(
                    "/manager/showtimes/scheduler",
                    "src/main/resources/templates/manager/showtime-scheduler.html",
                    "Màn hình chuyên biệt tạo lịch chiếu theo từng phòng: thêm nhiều slot phim trong ngày, tự động tính giờ kết thúc, cộng 15 phút dọn phòng (BR-09) và kiểm tra xung đột thời gian (Screen 59b / UC-30 Scheduler)."
                ),
                createTable([
                    createSectionRow("I. Header & Back Navigation"),
                    createDataRow("Page Title & Breadcrumb", "Header label. Breadcrumb: 'Overview / Showtime Management / Create Room Schedule'. Title: 'Create Room Schedule'. Subtitle: 'Select a room, add movie slots, and let the system calculate film end times automatically.'"),
                    createDataRow("Back to Showtimes", "Navigation button (arrow_back icon). Returns to the main showtime management list (/manager/showtimes)."),

                    createSectionRow("II. Room Selection & Operating Meta Card"),
                    createDataRow("Screening Room", "Dropdown (tv icon). Lists eligible active screening rooms. Selecting a room dynamically refreshes its schedule timeline and operating parameters."),
                    createDataRow("Screening Date", "Date picker (calendar icon). Specifies operating date for scheduling. Restricted to current or future dates (minDate)."),
                    createDataRow("Room Type Indicator", "Read-only meta badge. Displays technology tier of selected room (e.g. 'Standard Laser', 'Gold Class VIP', 'IMAX Laser')."),
                    createDataRow("Room Capacity Indicator", "Read-only meta badge. Displays total seat capacity of selected room (e.g. '80 seats')."),
                    createDataRow("Room Status Indicator", "Read-only meta badge. Displays operational readiness of room (e.g. 'Ready')."),
                    createDataRow("Cleaning Buffer Info", "Read-only operational info (cleaning icon). Value: '15 min'. Fixed cleaning buffer automatically applied after every screening (BR-09)."),

                    createSectionRow("III. Visual Schedule Timeline Canvas"),
                    createDataRow("Timeline Hour Ruler", "Horizontal time axis spanning from 08:00 through 24:00 (extending past midnight to 02:00 for late screenings)."),
                    createDataRow("Interactive Track Area", "Visual bar containing color-coded blocks for the room: Saved showtimes, Draft unsaved showtimes, and 15-minute cleaning buffer blocks."),
                    createDataRow("Timeline Legend", "Legend swatches: 'Saved Showtime' (Solid blue), 'Draft Showtime' (Striped blue/amber), 'Cleaning Buffer (15 min)' (Light orange)."),

                    createSectionRow("IV. Schedule Slots Table"),
                    createDataRow("Table Structure", "Dynamic tabular list of planned slots. Columns: # (Index), Movie Title & Rating, Start Time (HH:mm), Duration (min), Film End Time (HH:mm), Room Available Time (HH:mm), Base Price (VND), Status ('Saved' or 'Draft'), Actions (Edit, Remove draft)."),
                    createDataRow("Table Buffer Note", "Information note: '15-minute cleaning buffer applies after each showtime.'"),

                    createSectionRow("V. Add Showtime Slot Form Drawer"),
                    createDataRow("Movie Selection", "Dropdown (movie icon). Lists eligible movies currently available for scheduling with duration and age rating metadata."),
                    createDataRow("Start Time", "Time input (HH:mm). Specifies the start time of the screening slot."),
                    createDataRow("Movie Duration", "Read-only preview. Displays duration of the selected film in minutes (e.g. '120 min')."),
                    createDataRow("Film End Time", "Read-only calculated field. Automatically calculated based on selected movie duration and start time: Film End Time = Start Time + Movie Duration."),
                    createDataRow("Cleaning Buffer", "Read-only field. Fixed standard value of 15 minutes automatically reserved after film end time (BR-09)."),
                    createDataRow("Room Available Time", "Read-only calculated field. Displays Film End Time plus the required 15-minute cleaning buffer: Room Available Time = Film End Time + 15 minutes."),
                    createDataRow("Schedule Conflict Alert", "Validation message box. Appears when a proposed showtime overlaps an existing showtime or cleaning buffer. Displays error: 'Schedule Conflict! Room is occupied' and provides 'Use Next Time' button to auto-align start time to next available slot."),
                    createDataRow("Add to Schedule", "Button (+ icon). Adds the configured slot into the draft schedule list and renders it on the visual timeline."),
                    createDataRow("Clear", "Button (refresh icon). Resets form drawer inputs to default."),

                    createSectionRow("VI. Sticky Bottom Save Action Bar"),
                    createDataRow("Draft Count Display", "Status label. Displays count of pending draft slots ready to persist: '{count} draft slot ready to save'."),
                    createDataRow("Discard Drafts", "Button (delete icon). Clears all unsaved draft slots from the timeline."),
                    createDataRow("Save Room Schedule", "Button (bookmark icon, red). Saves all valid draft showtimes for the selected room and date. System validates scheduling conflicts before saving and commits batch in a single transaction via POST /api/manager/showtimes/batch.")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 5: SEAT TYPE MANAGEMENT
                // ==========================================
                createHeading1("5. Ảnh 4 — Seat Type Management (SCR-MGR-05 / UC-31)"),
                ...createMetaInfo(
                    "/manager/seat-types",
                    "src/main/resources/templates/manager/seat-types.html",
                    "Màn hình cấu hình các loại ghế, màu sắc hiển thị trên sơ đồ ghế và hệ số nhân giá vé động (Business rule BR-50) (Screen 54 / UC-31)."
                ),
                createTable([
                    createSectionRow("I. Header & Primary Action"),
                    createDataRow("Page Title", "Header label. Displays 'Seat Type Management' with subtitle: 'Configure seat types and price multipliers for flexible ticket pricing by room seat map.'"),
                    createDataRow("Add seat type", "Button (+ icon). Opens '+ Add seat type' modal form to create a new seat class."),

                    createSectionRow("II. Pricing Multiplier KPI Metrics"),
                    createDataRow("Total seat types", "Numeric badge (armchair icon). Total seat classes configured in system (e.g. 4 active in the system)."),
                    createDataRow("Standard seat multiplier", "Numeric badge (tag icon). Value: '1.00x'. Subtext: 'Base ticket price reference'."),
                    createDataRow("Average price multiplier", "Numeric badge (sigma icon). Format: 'X.XXx' (e.g. 1.25x). Mean of active seat type multipliers."),
                    createDataRow("Allowed multiplier range", "Range badge (equals icon). Value: '0.50 – 3.00'. Subtext: 'Business rule BR-50'. Enforces valid bounds for price multipliers."),

                    createSectionRow("III. Seat Type List Table"),
                    createDataRow("Table Header Badge", "Header text badge: 'Seat type list ({count} types)'."),
                    createDataRow("Code", "Text badge. Uppercase unique code identifier: 'FRONT', 'STD', 'VIP', 'COUPLE'."),
                    createDataRow("Seat type name", "Composite column. Color indicator dot + Seat Type Name (e.g. 'Standard Seat', 'VIP Royal Amber', 'Sweetbox Couple')."),
                    createDataRow("Price multiplier", "Numeric badge. Format: 'X.XXx' (e.g. 0.95x, 1.00x, 1.25x, 1.80x). Ticket price formula: Seat Price = (Base Price + Surcharge) * Multiplier."),
                    createDataRow("Seat map color", "Color preview swatch + Hex code string (e.g. #3B82F6, #F59E0B, #EC4899)."),
                    createDataRow("Features & amenities", "Text column. Description of seating comfort, viewing angle, and extra perks."),
                    createDataRow("Status", "Status badge. Values: 'Active' (Green) or 'Inactive' (Gray)."),
                    createDataRow("Actions", "Action Button Group (2 buttons per row):\n- Edit Seat Type (Pencil icon): Opens Edit modal to modify name, multiplier, color, and description.\n- Delete Seat Type (Trash icon): Deletes seat type with confirmation (blocked if active seats or bookings reference it).\nNote: Price multiplier is adjusted via the Edit modal; there is no standalone table button for multipliers."),
                    createDataRow("Pagination & Pricing Note", "Footer summary displaying 'Showing 1 – {count} of {total} registered seat types'. Bottom callout explains pricing formula and confirms multipliers only apply to showtimes created/updated after saving."),

                    createSectionRow("IV. Add / Edit Seat Type Modal Form"),
                    createDataRow("Seat Type Code", "Text input. Required. Max length: 10 chars. Pattern: ^[A-Z0-9_]+$. Unique uppercase identifier."),
                    createDataRow("Seat Type Name", "Text input. Required. Max length: 50 chars. Descriptive name of seat class."),
                    createDataRow("Price Multiplier", "Numeric input. Required. Min: 0.50, Max: 3.00, Step: 0.05. Enforces BR-50 pricing rule."),
                    createDataRow("Seat Map Color Code", "Color picker input (type='color'). Selects hex color for matrix visualization."),
                    createDataRow("Status", "Dropdown. Options: 'Active', 'Inactive'. Default: 'Active'."),
                    createDataRow("Features / Description", "Textarea. Optional. Max length: 255 chars. Summary of seat amenities."),
                    createDataRow("Save changes", "Button. Persists seat type configuration to database."),
                    createDataRow("Cancel", "Button. Closes modal without saving.")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 6: SEAT MAP MANAGEMENT
                // ==========================================
                createHeading1("6. Ảnh 5 — Seat Map Management (SCR-MGR-06 / UC-31)"),
                ...createMetaInfo(
                    "/manager/seats",
                    "src/main/resources/templates/manager/seats.html",
                    "Màn hình trực quan hóa và cấu hình sơ đồ ghế theo từng phòng chiếu: phân loại loại ghế (Standard, VIP, Sweetbox) và khóa bảo trì ghế (Screen 55 / UC-31)."
                ),
                createTable([
                    createSectionRow("I. Header & Utility Actions"),
                    createDataRow("Page Title", "Header label. Displays 'Seat Map Management' with subtitle: 'View and configure the seat map of each screening room: seat types (Standard, VIP, Sweetbox) and seats under maintenance.'"),
                    createDataRow("Generate standard map", "Button (wand icon). Generates standard rectangular grid based on auditorium's configured rows and seats per row after user confirmation."),
                    createDataRow("Refresh seat map", "Button (refresh icon). Reloads seat grid and fresh booking/maintenance states for the selected room from database."),

                    createSectionRow("II. Screening Room Selector & KPI Metrics"),
                    createDataRow("Screening room", "Dropdown. Lists active screening rooms with code, tech tier, and capacity (e.g. 'Cinema 03 - Laser Compact (ROOM-033 • Standard Laser • 80 seats)'). Selecting a room refreshes the grid."),
                    createDataRow("Total seats", "Numeric badge (armchair icon). Value: '{count}'. Subtext: '{rows} rows × {cols} columns'."),
                    createDataRow("Active seats", "Numeric badge (check icon). Value: '{count}'. Subtext: 'Ready for ticket sales'."),
                    createDataRow("Locked", "Numeric badge (lock icon). Value: '{count}'. Subtext: 'Blocked from booking'."),
                    createDataRow("Room capacity", "Numeric badge (cinema icon). Value: '{capacity}'. Subtext: Room tech tier name."),

                    createSectionRow("III. Interactive Seat Map Grid View"),
                    createDataRow("Section Header & Badge", "Heading: 'Seat map — {Room Name}'. Badge: Technology tier (e.g. 'Standard Laser', 'IMAX Laser')."),
                    createDataRow("Instruction Alert", "Notice text: 'Click or drag across rows/columns to select multiple seats. Double-click a seat or click Configure to edit.'"),
                    createDataRow("Screen Arc Indicator", "Curved red line banner with text: 'SCREEN'. Represents position of projection screen."),
                    createDataRow("Row Alpha Labels", "Alphabetic row identifiers (A, B, C, D, ...) displayed on both left and right margins of the grid."),
                    createDataRow("Seat Grid Cells", "Square interactive buttons displaying seat codes (e.g. A1, A2... H10). Color-coded by seat type: Blue (Standard), Amber (VIP), Pink (Sweetbox), Dark/Strikethrough (Maintenance). Click highlights seat."),
                    createDataRow("Central Aisle", "Vertical visual gap labeled 'CENTRAL AISLE' separating seat columns into left and right sections. Note: In CINEORA, the aisle is a CSS presentation spacing; all grid coordinates are real physical seat records."),
                    createDataRow("Marquee Drag Selection", "Allows click-and-drag across rows and columns to highlight multiple seats simultaneously for bulk editing."),
                    createDataRow("Bulk Action Bar", "Action bar appearing when seats are highlighted: 'Configure Selected' (opens bulk configuration modal) and 'Deselect All' (clears selection)."),

                    createSectionRow("IV. Seat Configuration Modal Form"),
                    createDataRow("Selected Seat Coordinates", "Text indicator. Displays currently selected seat code(s) (e.g. 'Seat C5' or '10 seats selected')."),
                    createDataRow("Seat Type Assignment", "Dropdown. Options: 'Standard Seat', 'VIP Royal Amber', 'Sweetbox Couple', 'Front Row'. Assigns pricing and visual category."),
                    createDataRow("Seat Operational Status", "Dropdown. Options: 'ACTIVE' (Available for customer booking), 'MAINTENANCE' (Locked from booking due to physical maintenance or damage)."),
                    createDataRow("Apply changes", "Button. Saves seat classification and operational status updates to the database."),
                    createDataRow("Cancel", "Button. Closes modal without applying modifications.")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 7: MOVIE GENRE MANAGEMENT
                // ==========================================
                createHeading1("7. Ảnh 6 — Movie Genre Management (SCR-MGR-07 / UC-28b)"),
                ...createMetaInfo(
                    "/manager/genres",
                    "src/main/resources/templates/manager/genres.html",
                    "Màn hình quản lý các thể loại phim dùng để phân loại danh mục trong hệ thống CINEORA (UC-28 Genres)."
                ),
                createTable([
                    createSectionRow("I. Header & Primary Action"),
                    createDataRow("Page Title", "Header label. Displays 'Movie Genre Management' with subtitle: 'Manage the genres used to classify movies in the CINEORA system.'"),
                    createDataRow("Add genre", "Button (+ icon, red). Opens '+ Add genre' modal form to create a new category."),

                    createSectionRow("II. Genre KPI Metric Badges"),
                    createDataRow("Total genres", "Numeric badge (category icon). Total genres registered in system (e.g. 13)."),
                    createDataRow("Active", "Numeric badge (check icon). Count of enabled genres available for movie assignment (e.g. 11)."),
                    createDataRow("Inactive", "Numeric badge (ban icon). Count of disabled genres (e.g. 2)."),
                    createDataRow("Genres with movies", "Numeric badge (film icon). Count of genres currently associated with at least 1 movie title (e.g. 7)."),

                    createSectionRow("III. Filter & Search Bar"),
                    createDataRow("Search Keyword", "Text input. Placeholder: 'Search by genre name or description...'. Filters genre title and descriptive text."),
                    createDataRow("All statuses", "Dropdown. Options: 'All statuses', 'Active', 'Inactive'. Default: 'All statuses'."),
                    createDataRow("Filter", "Button. Applies search and status filters to the genre table."),
                    createDataRow("Clear filters", "Button. Resets search and displays full genre list."),

                    createSectionRow("IV. Genre List Table"),
                    createDataRow("Table Header Badge", "Header text badge: 'Genre list (10 genres)'. Indicates items displayed on current page."),
                    createDataRow("#ID", "Numeric column. Unique database primary key for genre."),
                    createDataRow("Genre name", "Text column (bold). Genre title (e.g. 'Hành Động', 'Hoạt Hình', 'Khoa Học Viễn Tưởng', 'Kinh Dị')."),
                    createDataRow("Description", "Text column. Brief synopsis of genre themes and narrative conventions."),
                    createDataRow("Linked movies", "Tag badge / link. Format: '{n} movies'. Clicking opens modal dialog listing movies assigned to this genre."),
                    createDataRow("Status", "Status badge. Values: 'Active' (Green badge) or 'Inactive' (Red badge)."),
                    createDataRow("Actions", "Action Button Group (4 buttons per row):\n- View Linked Movies (Film icon): Opens popup of movies linked to genre.\n- Edit Genre (Pencil icon): Opens modal to modify genre name and description.\n- Toggle Active Status (Square toggle icon): Sends AJAX PATCH /api/manager/genres/{id}/toggle-status to toggle active state.\n- Delete Genre (Trash icon): Deletes genre with confirmation (blocked if movies are assigned)."),
                    createDataRow("Pagination Summary", "Footer row displaying 'Showing {start} – {end} out of {total} genres' (e.g. 'Showing 1 – 10 out of 13 genres') with navigation buttons (‹, 1, 2, ›). Standard page size: 10 genres per page."),

                    createSectionRow("V. Add / Edit Genre Modal Form"),
                    createDataRow("Genre Name", "Text input. Required. Max length: 50 chars. Category name. Placeholder: 'e.g. Action, Comedy, Drama'."),
                    createDataRow("Description", "Textarea. Optional. Max length: 255 chars. Rows: 3. Summary of genre themes."),
                    createDataRow("Status", "Dropdown. Options: 'Active', 'Inactive'. Default: 'Active'."),
                    createDataRow("Save changes", "Button. Submits form to create or update genre."),
                    createDataRow("Cancel", "Button. Closes modal without saving.")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 8: SCREENING ROOM MANAGEMENT
                // ==========================================
                createHeading1("8. Ảnh 7 — Screening Room Management (SCR-MGR-08 / UC-29)"),
                ...createMetaInfo(
                    "/manager/rooms",
                    "src/main/resources/templates/manager/rooms.html",
                    "Màn hình quản lý thông số kỹ thuật, máy chiếu, âm thanh, kích thước màn hình và trạng thái vận hành của các phòng chiếu (Screen 56 / UC-29)."
                ),
                createTable([
                    createSectionRow("I. Header & Primary Action"),
                    createDataRow("Page Title", "Header label. Displays 'Screening Room Management' with subtitle: 'Manage seat maps, technical specifications and operating status of the screening rooms.'"),
                    createDataRow("Add screening room", "Button (+ icon, red). Opens '+ Add screening room' modal dialog to register a new auditorium."),

                    createSectionRow("II. Room Inventory KPI Cards"),
                    createDataRow("Total screening rooms", "Numeric badge (theater icon). Count of registered auditoriums matching current filter (e.g. '5')."),
                    createDataRow("Active", "Numeric badge (check icon). Count of operational rooms in Ready or Screening state (e.g. '2')."),
                    createDataRow("Maintenance / Cleaning", "Numeric badge (wrench icon). Count of rooms currently unavailable for showtimes (e.g. '3')."),
                    createDataRow("Actual total seats", "Numeric badge (armchair icon). Total seat capacity summed across all screening rooms (e.g. '516 seats')."),

                    createSectionRow("III. Search & Filter Bar"),
                    createDataRow("Search Room", "Text input. Placeholder: 'Search by room name or code...'. Filters by room title and code."),
                    createDataRow("Search", "Button. Filters the screening room cards grid."),
                    createDataRow("Clear", "Button. Clears search keyword and reloads full room catalog."),

                    createSectionRow("IV. Screening Room Cards Grid"),
                    createDataRow("Card Header", "Composite status header displaying Room Operating Status Pill (e.g. '• READY FOR GUESTS', '• MAINTENANCE') and Tech Tier Badge ('Standard Laser', 'Gold Class VIP', 'IMAX Laser')."),
                    createDataRow("Room Title & Code", "Heading: Room Name (e.g. 'Cinema 03 - Laser Compact') and unique Code with capacity (e.g. 'ROOM-033 • 80 seats')."),
                    createDataRow("Projector Spec", "Icon text item. Hardware model (e.g. 'Projector: Barco SP2K-11 Laser')."),
                    createDataRow("Sound System Spec", "Icon text item. Audio standard (e.g. 'Sound: Dolby 7.1 Studio Audio')."),
                    createDataRow("Screen Size", "Icon text item. Physical projection dimensions (e.g. 'Screen: 14m x 7m')."),
                    createDataRow("Capacity", "Icon text item. Total seat count (e.g. 'Capacity: 80 seats')."),
                    createDataRow("Equipment Notes / Description", "Text paragraph describing room facilities or current maintenance activity."),
                    createDataRow("Action Controls", "Action Button Group:\n- 'Seat map ({capacity})': Navigates directly to /manager/seats?roomId={id}\n- 'Showtimes': Navigates to /manager/showtimes?roomId={id}\n- Edit Room (Pencil icon): Opens room edit modal\n- Delete Room (Trash icon): Deletes room with confirmation; blocked if past or future showtimes exist."),
                    createDataRow("Pagination Summary", "Footer row: 'Showing 1 – 5 of 5 screening rooms' with page navigation button '1'."),

                    createSectionRow("V. Add / Edit Screening Room Modal Form"),
                    createDataRow("Room Code", "Text input. Required. Max length: 20 chars. Pattern: ^[A-Za-z0-9_-]+$. Unique auditorium identifier. Placeholder: 'e.g. ROOM-01'."),
                    createDataRow("Room Name", "Text input. Required. Max length: 100 chars. Auditorium name. Placeholder: 'e.g. Cinema 01 — IMAX Laser'."),
                    createDataRow("Room Type / Tech", "Dropdown. Required. Options: 'Standard Laser', 'Gold Class VIP', 'IMAX Laser', '3D Digital'."),
                    createDataRow("Operating Status", "Dropdown. Required. Options: 'Ready', 'Screening', 'Maintenance', 'Cleaning'."),
                    createDataRow("Total Rows", "Numeric input. Required. Range: 1 - 100. Number of seat rows."),
                    createDataRow("Seats Per Row", "Numeric input. Required. Range: 1 - 100. Number of seats in each row."),
                    createDataRow("Capacity (total seats)", "Numeric input. Required. Range: 1 - 5000. Total auditorium seating capacity.\n- Consistency Business Rule: In CINEORA, standard auditoriums are full rectangular grids where every coordinate (row, col) is a seat, and aisles are purely display spacing. The system enforces: Capacity = Total Rows × Seats Per Row. The modal provides real-time calculation hint ('Rows × Seats/row'), auto-fills capacity, and blocks submission with error if capacity does not equal rows × seats/row."),
                    createDataRow("Projector Model", "Text input. Optional. Hardware projector model (e.g. 'Barco SP2K-11 Laser')."),
                    createDataRow("Sound System", "Text input. Optional. Sound system standard (e.g. 'Dolby 7.1 Studio Audio')."),
                    createDataRow("Description / Notes", "Textarea. Optional. Max: 500 chars. Equipment and amenity notes."),
                    createDataRow("Save room", "Button. Submits form via REST API (POST/PUT /api/manager/rooms) with JSON payload."),
                    createDataRow("Cancel", "Button. Closes modal without saving.")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 9: FOOD & BEVERAGE MANAGEMENT
                // ==========================================
                createHeading1("9. Ảnh 8 — Food and Beverage Management (SCR-MGR-09 / UC-32)"),
                ...createMetaInfo(
                    "/manager/fb",
                    "src/main/resources/templates/manager/fb.html",
                    "Màn hình quản lý danh mục bắp rang, nước ngọt, combo ẩm thực, giá bán lẻ, giá vốn và tỷ suất lợi nhuận (Screen 61 / UC-32)."
                ),
                createTable([
                    createSectionRow("I. Header & Primary Action"),
                    createDataRow("Page Title", "Header label. Displays 'Food and Beverage Management' with subtitle: 'Manage the catalog of popcorn, soft drinks, food combos and snacks served at the counter and pre-ordered online.'"),
                    createDataRow("Add new product", "Button (+ icon, red). Opens '+ Add new product' modal form to register a new concession item."),

                    createSectionRow("II. Inventory KPI Cards"),
                    createDataRow("Total products", "Numeric badge (fastfood icon). Total items on concession menu (e.g. '8'). Subtext: 'Items and combos on the menu'."),
                    createDataRow("Combos", "Numeric badge (shopping_bag icon). Count of value bundle packages (e.g. '2'). Subtext: 'Value bundles'."),
                    createDataRow("On sale", "Numeric badge (storefront icon). Count of active concession items available to customers (e.g. '8'). Subtext: 'Available to customers'."),
                    createDataRow("Suspended", "Numeric badge (stop icon). Count of disabled concession items (e.g. '0'). Subtext: 'Not available for sale'. Note: 'Suspended' denotes items administratively deactivated in the catalog (status = 'Inactive'), distinct from daily physical stock outages."),

                    createSectionRow("III. Category Filter Tabs & Search Bar"),
                    createDataRow("Category Tabs", "Horizontal filter pills: 'All items (8)', 'Combos (2)', 'Popcorn & Food (4)', 'Drinks (2)', 'Snacks'. Filters concession catalog by category."),
                    createDataRow("Search by name or SKU", "Text input. Placeholder: 'Search by name or SKU...'. Searches product code and title."),
                    createDataRow("All statuses", "Dropdown. Options: 'All statuses', 'On sale' (Active), 'Suspended' (Inactive). Default: 'All statuses'."),
                    createDataRow("Filter data", "Button. Submits search query to filter catalog cards."),
                    createDataRow("Reset", "Button. Resets category and search filters to default."),

                    createSectionRow("IV. Product Cards Grid"),
                    createDataRow("Product Thumbnail", "Concession image photo (aspect ratio 4:3) with fallback concession graphic."),
                    createDataRow("Product Name", "Heading (bold). Title of concession item (e.g. 'Large Caramel Popcorn', 'Hot Cheese Hotdog', 'Fresh Lemon Sprite 32oz (Large)', 'Fresh Coca-Cola 32oz (Large)')."),
                    createDataRow("Status Badge", "Badge. Values: 'ON SALE' (Green badge), 'SUSPENDED' (Gray/Red badge), or 'PAUSED (MISSING ITEM)' (Orange badge - displayed if a combo bundle contains an inactive single component item)."),
                    createDataRow("Recipe / Component Subtext", "Muted text paragraph. Description of food ingredients or combo bundle composition."),
                    createDataRow("Selling Price", "Currency column (VND). Format: '#,##0 ₫' (e.g. '55,000 đ', '45,000 đ'). Retail price charged to customers."),
                    createDataRow("Cost Price", "Currency column (VND). Format: 'Cost: #,##0 ₫' (e.g. 'Cost: 15,000 đ', 'Cost: 0 đ'). Direct food cost / Cost of Goods Sold (COGS)."),
                    createDataRow("Profit Margin Badge", "Percentage badge. Formula: ((Selling Price - Cost Price) / Selling Price) * 100%. Format: 'Margin: XX.X%' (e.g. 72.7%, 100.0%).\n- Calculation & Edge Case Handling: When Selling Price <= 0 or cost is null, margin is 0.0%. When Cost Price is recorded as 0 đ (e.g. cost not yet entered), the mathematical formula yields 100.0% (representing theoretical gross markup before cost input, not actual net profit)."),
                    createDataRow("Card Actions", "Action Button Group:\n- Edit (Pencil icon / text): Opens edit modal.\n- Toggle Status (Switch/Square button): Activates or suspends product.\n- Delete Item (Trash icon): Deletes product with confirmation (blocked if order records exist)."),
                    createDataRow("Product Cards Grid Layout", "4-column responsive grid layout (4 items per row, 2 rows per page, total 8 items per page). When catalog exceeds 8 items, subsequent items paginate to page 2."),
                    createDataRow("Pagination Summary", "Footer row displaying 'Showing {start} – {end} of {total} products' with pagination controls (‹, 1, 2, ›). Configured with 8 items per page (4 items × 2 rows)."),

                    createSectionRow("V. Add / Edit Product Modal Form"),
                    createDataRow("Item Code / SKU", "Text input. Required. Max length: 50 chars. Unique product code (e.g. 'FC-CARMEL', 'FB-003')."),
                    createDataRow("Category", "Dropdown. Required. Options: 'Combo', 'Popcorn', 'Drink', 'Food', 'Snack'."),
                    createDataRow("Product Name", "Text input. Required. Max length: 100 chars. Concession item title. Placeholder: 'e.g. Large Caramel Popcorn'."),
                    createDataRow("Selling Price (VND)", "Numeric input. Required. Min: 0, Max: 10,000,000, Step: 1000. Retail price in VND."),
                    createDataRow("Cost price / COGS (VND)", "Numeric input. Required. Min: 0, Max: 10,000,000, Step: 1000. Direct inventory cost in VND."),
                    createDataRow("Product Image (URL or file)", "Composite control. Supports direct URL input (max 500 chars) or local image file selection ('Upload image' button). Accepts PNG, JPEG, WEBP, GIF up to 5MB, uploaded to /uploads/fb/ with live thumbnail preview."),
                    createDataRow("Description", "Textarea. Optional. Max: 500 chars. Detailed ingredients or bundle notes."),
                    createDataRow("Combo Components Selector", "Dynamic checklist. Appears conditionally when Category == 'Combo'. Allows checking constituent single items and specifying quantities (1 to 10) included in the bundle."),
                    createDataRow("Save product", "Button. Submits product creation or modification to /manager/fb/save."),
                    createDataRow("Cancel", "Button. Closes modal without saving.")
                ]),

                new Paragraph({ spacing: { before: 200, after: 100 }, children: [] }),

                // ==========================================
                // SCREEN 10: MANAGEMENT OVERVIEW / DASHBOARD
                // ==========================================
                createHeading1("10. Ảnh 10 — Management Overview / Dashboard (SCR-MGR-10 / UC-35)"),
                ...createMetaInfo(
                    "/manager/dashboard",
                    "src/main/resources/templates/manager/dashboard.html",
                    "Màn hình trung tâm tổng quan điều hành toàn rạp chiếu: giám sát 6 chỉ số vận hành chính, biểu đồ doanh thu 7 ngày gần nhất, lịch chiếu hôm nay và giao dịch vé gần nhất (Screen 01 / UC-35)."
                ),
                createTable([
                    createSectionRow("I. Header & Operational Date"),
                    createDataRow("Page Title", "Header label. Displays 'Management Overview' with subtitle: 'Monitor cinema activity and operational performance.'"),
                    createDataRow("Calendar Date Indicator", "Read-only date pill (calendar icon). Format: 'dddd, DD/MM/YYYY' (e.g. 'Saturday, 14/11/2026'). Displays current system operating date dynamically."),

                    createSectionRow("II. Cinema Operations KPI Metric Cards (6 Cards)"),
                    createDataRow("Total movies", "Numeric KPI card (film icon). Value: '{count}' (e.g. 6). Subtext: '{showing} now showing • {upcoming} coming soon'."),
                    createDataRow("Total genres", "Numeric KPI card (category icon). Value: '{count}' (e.g. 13). Subtext: '{seatTypes} seat types configured'."),
                    createDataRow("Today's showtimes", "Numeric KPI card (schedule icon). Value: '{count}' (e.g. 0). Subtext: '{active_rooms} of {total_rooms} rooms available' (displays active operating auditoriums versus total cinema rooms, e.g. '2 of 5 rooms available')."),
                    createDataRow("Tickets sold", "Numeric KPI card (ticket icon). Value: '{count}' (e.g. 0). Subtext: '{rate}% seat occupancy' or 'No bookings yet'."),
                    createDataRow("Today's revenue", "Currency KPI card (payments icon). Value formatted in VND: '#,##0 ₫' (e.g. '0 ₫'). Subtext: '{weekly_total} ₫ in 7 days'."),
                    createDataRow("F&B items on sale", "Numeric KPI card (fastfood icon). Value: '{active_items}' (e.g. 8). Subtext: 'of {total_items} items in the menu'."),

                    createSectionRow("III. Revenue Overview Chart & Today's Showtimes Widget"),
                    createDataRow("Revenue Overview Heading", "Heading 2: 'Revenue Overview' with 7-day revenue badge formatted '#,##0 ₫ / 7 days'."),
                    createDataRow("Full report →", "Link. Navigates directly to the full revenue audit report screen (/manager/reports)."),
                    createDataRow("Revenue Breakdown Legend", "Color indicators: '● Ticket revenue (XX%)' (Red indicator) and '● Popcorn & Combos (XX%)' (Gold indicator)."),
                    createDataRow("Weekly Revenue Chart Canvas", "Dual-bar column chart. Visualizes ticket and concession revenue trends across the 7 most recent days ending with Today (e.g. Sun, Mon, Tue, Wed, Thu, Fri, Today). Displays two vertical bars per day (Red for Ticket, Gold for F&B). If zero sales occurred across the week, displays notice: 'No paid bookings in the last 7 days yet.'"),
                    createDataRow("Today's showtimes Header", "Heading 2: 'Today's showtimes' with navigation link 'View all →' (navigating to /manager/showtimes)."),
                    createDataRow("Empty Showtimes Notice", "Card placeholder displayed when no showtimes are scheduled for today: 'No showtimes scheduled for today.'"),
                    createDataRow("Live Showtime Card (When Active)", "Showtime broadcast card displaying Start Time (HH:mm), Movie Title, Age Rating pill (e.g. T18), Screening Room Name, Capacity, and Status badge ('Selling tickets')."),

                    createSectionRow("IV. Recent Bookings and Transactions Table"),
                    createDataRow("Section Header & Subtitle", "Heading 2: 'Recent bookings and transactions'. Subtitle: 'The latest paid online and counter bookings in the last 7 days.'"),
                    createDataRow("Table Actions Toolbar", "Toolbar button group: 'Filter data' button (filter icon) and 'Export CSV' button (download icon)."),
                    createDataRow("Booking ID", "Code string (bold). Unique booking identifier (e.g. '#BK-10023')."),
                    createDataRow("Customer", "Text column. Buyer full name and contact information (phone/email)."),
                    createDataRow("Movie", "Composite string. Movie title with technology format tag (2D / 3D / IMAX)."),
                    createDataRow("Showtime & Room", "Composite string. Screening time slot and auditorium room name."),
                    createDataRow("Status", "Operational booking status pill: 'Paid' / 'CONFIRMED' (Green badge, indicating active settled transaction) or 'Completed' (Gold badge, indicating screening has concluded). Note: The query utilizes the paid_bookings CTE (RevenueReportDao.java:321-329), thus only successfully paid and confirmed orders in the last 7 days are listed; unpaid/cancelled bookings are excluded."),
                    createDataRow("Total", "Currency column (VND). Net transaction total collected formatted as '#,##0 ₫'."),
                    createDataRow("Empty Transactions State", "Placeholder row displayed when no transactions exist: 'No bookings in the last 7 days.'")
                ])
            ]
        }]
    });

    const mdPath = path.resolve('c:/Users/hoang/Downloads/SWP_RapPhim/CINEORA_Screen_Descriptions_Specification.md');
    fs.writeFileSync(mdPath, mdLines.join('\n'), 'utf8');
    console.log("Updated: " + mdPath);

    const v2Path = path.resolve('c:/Users/hoang/Downloads/SWP_RapPhim/CINEORA_Screen_Descriptions_Specification_v2.docx');
    const finalPath = path.resolve('c:/Users/hoang/Downloads/SWP_RapPhim/CINEORA_Screen_Descriptions_Specification_Final.docx');
    const primaryPath = path.resolve('c:/Users/hoang/Downloads/SWP_RapPhim/CINEORA_Screen_Descriptions_Specification.docx');
    const buffer = await Packer.toBuffer(doc);
    
    fs.writeFileSync(v2Path, buffer);
    console.log("Updated: " + v2Path);
    
    fs.writeFileSync(finalPath, buffer);
    console.log("Updated: " + finalPath);

    try {
        fs.writeFileSync(primaryPath, buffer);
        console.log("Updated: " + primaryPath);
    } catch (e) {
        console.log("Note: Primary file is currently open in Word: " + e.message);
    }
}

run().catch(err => {
    console.error(err);
    process.exit(1);
});
