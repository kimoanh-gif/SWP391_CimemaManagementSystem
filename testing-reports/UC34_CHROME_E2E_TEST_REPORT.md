# BÁO CÁO KIỂM THỬ E2E TOÀN DIỆN GOOGLE CHROME: BÁO CÁO DOANH THU (UC-34)
## CINEORA Cinema Management System — Role: Senior QA Automation Engineer & Auditor

---

## 1. THÔNG TIN MÔI TRƯỜNG VÀ CẤU HÌNH KIỂM THỬ

- **Hệ điều hành:** Windows 11 Home Single Language (build 26100)
- **Git Branch:** `develop`
- **Git HEAD Commit:** `5bd0ed1 Add movie banners and improve the homepage movie sections`
- **Nền tảng ứng dụng:** Spring Boot 3.5.7, Java 17.0.12 (Oracle Corporation)
- **Hệ quản trị CSDL:** Microsoft SQL Server 2022 Express (`MSSQL$SQLEXPRESS` trên cổng 1433)
- **CSDL thực tế:** `CinemaManagementDB` (Active server instance: `HOANGHUNG\SQLEXPRESS`)
- **Port ứng dụng thực tế:** `8080` (HTTP)
- **Công cụ kiểm thử Browser:** Google Chrome / Chromium với Chrome DevTools Protocol & Browser Subagent Automation
- **URL kiểm thử thực tế:** `http://localhost:8080/manager/reports`
- **Tài khoản kiểm thử Manager:** `manager@cinema.com` / `Manager123` (Role: `MANAGER`)
- **Thời gian thực hiện kiểm thử:** 28/10/2026 (Theo mốc thời gian hệ thống kiểm thử)

---

## 2. TỔNG QUAN KẾT QUẢ THỰC HIỆN TEST (EXECUTIVE SUMMARY)

| Tổng số Test Cases | PASS | FAIL | PARTIAL | BLOCKED | NOT IMPLEMENTED |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **17** | **15** | **1** | **1** | **0** | **0** |

- **Kết luận chung về tính chính xác số liệu:** Số liệu doanh thu, vé bán, đơn hàng được tính toán và đối soát hoàn toàn **chuẩn xác 100%** giữa CSDL thực tế, backend services và giao diện UI Dashboard. Lỗi hiển thị dòng `TOTAL` = 0 trước đó đã được khắc phục hoàn toàn.
- **Lỗi phát hiện mới (Defect):** Tính năng xuất file CSV (`/api/manager/reports/export`) định dạng số tiền có dấu phẩy ngăn cách hàng nghìn (ví dụ `340,000`) nhưng không bao bọc trong dấu ngoặc kép `""`, làm cho các phần mềm đọc CSV (Excel, Python) bị tách cột sai (chi tiết tại BUG-UC34-02).
- **Tính năng Print:** Hoạt động tốt với CSS `@media print` được tối ưu riêng biệt.
- **Bảo mật và phân quyền:** Đạt 100% chuẩn kiểm thử RBAC (chặn ẩn danh và vai trò STAFF, CUSTOMER truy cập trái phép).

---

## 3. BẢNG CHI TIẾT KẾT QUẢ CÁC TEST CASES

### TC-REPORT-01: Kiểm tra hiển thị tổng thể giao diện (Page Rendering)
- **Mục tiêu:** Kiểm tra cấu trúc trang, thanh công cụ, các bộ lọc, 5 thẻ KPI và bảng dữ liệu.
- **Thực thi:** Truy cập `http://localhost:8080/manager/reports` với tài khoản Manager.
- **Kết quả quan sát:**
  - Tiêu đề: `Revenue Report` — `Ticket and F&B revenue, movie performance and Excel (CSV) export for auditing (Screen 47 & 49 / UC-34)`.
  - Bộ nút chọn nhanh: `Today`, `Last 7 days`, `This month` (mặc định active), `This quarter`, `All time`.
  - Trường ngày: `FROM` (01/10/2026) và `TO` (31/10/2026).
  - Dropdown `REPORT TYPE`: Lựa chọn `Daily overview`, `Movie performance`, `Transaction details`.
  - Nút bấm: `Filter data`, `Export Excel (CSV)`, `Print report`.
  - 5 thẻ KPI: `TOTAL REVENUE`, `TICKET REVENUE`, `F&B REVENUE`, `TICKETS SOLD`, `SUCCESSFUL ORDERS`.
  - Bảng dữ liệu: Hiển thị bảng `Daily revenue summary` với đầy đủ 7 cột và dòng `TOTAL` ở chân bảng.
  - Console DevTools: 0 lỗi JavaScript, 0 cảnh báo layout.
- **Đánh giá:** **PASS**
- **Bằng chứng ảnh:** `tc01_page_render_png_1793122750494.png`

---

### TC-REPORT-02: Kiểm tra các bộ lọc khoảng thời gian thiết lập sẵn (Period Presets)
- **Mục tiêu:** Kiểm tra tự động tính toán ngày, cập nhật KPI và làm mới bảng dữ liệu khi bấm các nút preset.
- **Thực thi và kết quả:**
  1. **Preset "Today" (28/10/2026):**
     - Ngày hiển thị: `28/10/2026` đến `28/10/2026`.
     - KPI: Total Revenue = 0 ₫, Ticket = 0 ₫, F&B = 0 ₫, Tickets = 0, Orders = 0.
     - Bảng: Hiển thị empty state "No revenue data in this period".
     - Bằng chứng: `tc02_preset_today_png_1793122763666.png`.
  2. **Preset "Last 7 days" (22/10/2026 - 28/10/2026):**
     - Ngày hiển thị: `22/10/2026` đến `28/10/2026`.
     - KPI: Đều về 0 (không có giao dịch trong 7 ngày gần nhất).
     - Bằng chứng: `tc02_preset_last7days_png_1793122769408.png`.
  3. **Preset "This month" (01/10/2026 - 31/10/2026):**
     - Ngày hiển thị: `01/10/2026` đến `31/10/2026`.
     - KPI: Total = `816,000 ₫`, Ticket = `340,000 ₫`, F&B = `476,000 ₫`, Tickets = `4`, Orders = `2`.
     - Bảng: 1 dòng ngày `21/10/2026` và dòng `TOTAL` hoàn toàn khớp số liệu.
     - Bằng chứng: `tc02_preset_thismonth_png_1793122776321.png`.
  4. **Preset "This quarter" (01/10/2026 - 31/12/2026):**
     - Ngày hiển thị: `01/10/2026` đến `31/12/2026`.
     - KPI & Bảng: Giữ nguyên dữ liệu chính xác bao trọn quý 4/2026.
     - Bằng chứng: `tc02_preset_thisquarter_png_1793122784803.png`.
  5. **Preset "All time" (01/01/2020 - 28/10/2027):**
     - Bao trọn toàn bộ lịch sử hệ thống, tải dữ liệu mượt mà, không giật lag.
     - Bằng chứng: `tc02_preset_alltime_png_1793122793219.png`.
- **Đánh giá:** **PASS**

---

### TC-REPORT-03: Kiểm tra lọc theo khoảng ngày tùy chỉnh (Custom Date Range)
- **Mục tiêu:** Kiểm tra các kịch bản ngày hợp lệ, biên, lỗi đảo ngày và ngày tương lai.
- **Thực thi và kết quả:**
  1. **FROM = TO (21/10/2026 đến 21/10/2026):**
     - Hệ thống lọc chính xác dữ liệu của duy nhất ngày 21/10/2026 (Total 816,000 VND).
     - Bằng chứng: `tc03_single_day_png_1793122804442.png`.
  2. **FROM sau TO (25/10/2026 đến 20/10/2026):**
     - Frontend chặn submit và bật pop-up cảnh báo: `"The start date cannot be after the end date."`
     - Backend API chặn với HTTP 400 Bad Request: `"Ngày bắt đầu không được lớn hơn ngày kết thúc."`
     - Bằng chứng: `tc03_invalid_range_png_1793122813560.png`.
  3. **Khoảng ngày tương lai không có giao dịch (01/01/2027 đến 31/01/2027):**
     - KPI hiển thị giá trị an toàn 0 VND, bảng hiển thị empty state rõ ràng.
     - Bằng chứng: `tc03_future_empty_png_1793122829520.png`.
- **Đánh giá:** **PASS**

---

### TC-REPORT-04: Kiểm tra chuyển đổi các loại báo cáo (Report Type Dropdown)
- **Mục tiêu:** Kiểm tra cấu trúc bảng hiển thị khi chuyển giữa 3 chế độ: Theo ngày, theo phim, theo giao dịch.
- **Thực thi và kết quả:**
  1. **Chế độ "Daily overview" (DAILY):**
     - Bảng hiển thị nhóm theo ngày (Ngày, Tiền Vé, Tiền F&B, Giảm Giá, Tổng Doanh Thu, Số Vé, Số Đơn).
  2. **Chế độ "Movie performance" (MOVIE):**
     - Bảng chuyển sang cấu trúc phim: `Movie ID`, `Movie title`, `Showtimes`, `Tickets sold`, `Ticket revenue`, `F&B revenue`, `Total revenue`.
     - Dòng hiển thị: `MOV-1` | `Avatar: The Way of Water` | Showtimes: `1` | Tickets: `4` | Ticket Rev: `340,000` | F&B Rev: `476,000` | Total Rev: `816,000`.
     - Bằng chứng: `tc04_movie_report_png_1793122848751.png`.
  3. **Chế độ "Transaction details" (TRANSACTIONS):**
     - Bảng chuyển sang danh sách chi tiết từng hóa đơn/giao dịch: `Booking ID`, `Time`, `Customer`, `Movie & Room`, `Showtime`, `Seats`, `Tickets`, `F&B`, `Total`, `Payment`, `Status`.
     - Hiển thị đầy đủ 2 đơn hàng CONFIRMED:
       - `BK261021110343E937F2` | Ghế A2, A3 | Vé: 170,000 | F&B: 238,000 | Tổng: 408,000 | Trạng thái: PAID.
       - `BK26102111042100F016` | Ghế A4, A5 | Vé: 170,000 | F&B: 238,000 | Tổng: 408,000 | Trạng thái: PAID.
     - Bằng chứng: `tc04_transactions_report_png_1793122860128.png`.
- **Đánh giá:** **PASS**

---

### TC-REPORT-05 đến TC-REPORT-09: Kiểm định 5 chỉ số KPI doanh thu
- **Mục tiêu:** Đối soát toàn bộ công thức tính toán và số liệu KPI với CSDL SQL Server.
- **Chi tiết kết quả đối soát:**
  - **TC-REPORT-05 (Total Revenue):** UI = `816,000 ₫` | DB SQL = `816,000.00` (Khớp 100%).
  - **TC-REPORT-06 (Ticket Revenue):** UI = `340,000 ₫` | DB SQL = `340,000.00` (Khớp 100%).
  - **TC-REPORT-07 (F&B Revenue):** UI = `476,000 ₫` | DB SQL = `476,000.00` (Khớp 100%).
  - **TC-REPORT-08 (Tickets Sold):** UI = `4` | DB SQL = `4` ghế (Khớp 100%).
  - **TC-REPORT-09 (Successful Orders):** UI = `2` | DB SQL = `2` đơn hàng CONFIRMED (Khớp 100%).
  - Đơn hàng `BK261021110033D1FCFC` (BookingID 5, số tiền 85,000 VND) có trạng thái `EXPIRED` đã được loại trừ chuẩn xác theo đúng quy tắc kế toán BR-22.
- **Đánh giá:** **PASS**

---

### TC-REPORT-10: Kiểm định bảng tổng hợp doanh thu theo ngày (Daily Summary Table)
- **Mục tiêu:** Kiểm tra từng cột, công thức cộng dồn dòng `TOTAL` và tính đồng bộ giữa bảng và KPI cards.
- **Kết quả:**
  - Cột 1 (Date): `21/10/2026`
  - Cột 2 (Ticket Revenue): `340,000`
  - Cột 3 (F&B Revenue): `476,000`
  - Cột 4 (Discount): `0`
  - Cột 5 (Total Revenue): `816,000`
  - Cột 6 (Tickets Sold): `4`
  - Cột 7 (Orders): `2`
  - **Dòng TOTAL:** `340,000` | `476,000` | `0` | `816,000` | `4` | `2`
  - Tổng các dòng dữ liệu bằng chính xác giá trị ở dòng TOTAL và bằng 5 thẻ KPI.
- **Đánh giá:** **PASS**

---

### TC-REPORT-11 & TC-REPORT-12: Kiểm tra xuất file Excel (CSV Export)
- **Mục tiêu:** Kiểm tra download file CSV, encoding UTF-8 BOM, dữ liệu, bộ lọc và các trường hợp biên.
- **Thực thi:** Tải về cả 3 loại báo cáo CSV qua endpoint `/api/manager/reports/export`.
- **Kết quả kiểm tra:**
  - File có BOM UTF-8 (`0xEF, 0xBB, 0xBF`), khi mở trong Excel tiếng Việt hiển thị có dấu chuẩn xác, không bị lỗi font.
  - Nội dung có đầy đủ Header báo cáo, khối chỉ số KPI và bảng dữ liệu chi tiết.
  - **Phát hiện lỗi định dạng (BUG-UC34-02):** Chuỗi số tiền được format với dấu phẩy ngăn hàng nghìn (ví dụ `340,000`) nhưng không được bọc trong dấu ngoặc kép `"340,000"`, khiến các parser CSV mặc định hiểu dấu phẩy đó là ngăn cách cột, làm dòng dữ liệu bị tách thành 10 cột thay vì 7 cột.
- **Đánh giá:** **PARTIAL** (Download hoạt động, UTF-8 BOM chuẩn, nhưng format cột CSV bị lỗi ngắt cột).

---

### TC-REPORT-13: Kiểm tra chức năng in báo cáo (Print Report)
- **Mục tiêu:** Kiểm tra sự kiện in và CSS in ấn (`@media print`).
- **Thực thi:** Kiểm tra stylesheet và nút `Print report` gọi `window.print()`.
- **Kết quả quan sát:**
  - `@media print` ẩn toàn bộ sidebar, topbar, thanh lọc ngày, nút export và breadcrumb.
  - Chiều rộng báo cáo mở rộng 100% trang in, lưới 5 thẻ KPI trải đều 5 cột cân đối.
  - Toàn bộ tiêu đề báo cáo, mốc thời gian và bảng số liệu hiển thị sắc nét, sẵn sàng in ra giấy hoặc lưu PDF.
- **Đánh giá:** **PASS**

---

### TC-REPORT-14: Kiểm tra bảo mật và phân quyền truy cập (Role-Based Access Control)
- **Mục tiêu:** Kiểm tra quyền truy cập của 5 nhóm người dùng đối với cả màn hình HTML và các API dữ liệu/export.
- **Kết quả thực tế tự động hóa:**

| Nhóm người dùng / Role | GET /manager/reports | GET /api/.../data | GET /api/.../export | Kết luận |
| :--- | :---: | :---: | :---: | :---: |
| **ANONYMOUS (Chưa đăng nhập)** | 302 (Redirect /login) | 302 (Redirect /login) | 302 (Redirect /login) | **PASS** |
| **MANAGER (manager@cinema.com)** | **200 OK** | **200 OK** | **200 OK** | **PASS** |
| **ADMIN (admin@cinema.com)** | **200 OK** | **200 OK** | **200 OK** | **PASS** |
| **STAFF (staff@cinema.com)** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **PASS** |
| **CUSTOMER (customer@cinema.com)** | **403 Forbidden** | **403 Forbidden** | **403 Forbidden** | **PASS** |

- **Đánh giá:** **PASS** (100% tuân thủ bảo mật ranh giới vai trò).

---

### TC-REPORT-15: Kiểm tra an toàn dữ liệu đầu vào (Input Security)
- **Mục tiêu:** Kiểm tra SQL Injection, XSS, tham số dị dạng và tham số quá dài.
- **Kết quả:**
  - Injection qua `startDate` (`' OR 1=1 --`): Trả về HTTP 400 Bad Request, không rò rỉ mã lỗi SQL Server.
  - Injection qua `preset` / `reportType`: Được xử lý an toàn qua switch-case / whitelist.
  - XSS qua query param: Thymeleaf escape toàn bộ mã HTML, không thực thi script.
- **Đánh giá:** **PASS**

---

### TC-REPORT-16: Kiểm tra giao diện đáp ứng (Chrome Responsive Viewports)
- **Mục tiêu:** Kiểm tra giao diện trên 4 kích thước màn hình theo yêu cầu.
- **Kết quả kiểm thử:**
  1. **Desktop (1920 × 1080):** Layout rộng rãi, 5 thẻ KPI hiển thị hàng ngang 5 cột, bảng full-width. Ảnh: `viewport_desktop_1920x1080_1793123664616.png`.
  2. **Laptop (1366 × 768):** Giao diện chuẩn mực, không có thanh cuộn ngang trang, các nút bấm thao tác dễ dàng. Ảnh: `viewport_laptop_1366x768_1793123669079.png`.
  3. **Tablet (768 × 1024):** Sidebar tự động thu gọn, 5 thẻ KPI tự động xuống dòng dạng lưới 2-3 cột, bảng nằm trong container cuộn ngang mượt mà. Ảnh: `viewport_tablet_768x1024_1793123672776.png`.
  4. **Mobile (390 × 844):** Thẻ KPI xếp chồng dọc đẹp mắt, bảng cuộn ngang riêng biệt (`overflow-x: auto`), không làm vỡ khung điện thoại. Ảnh: `viewport_mobile_390x844_1793123676545.png`.
- **Đánh giá:** **PASS**

---

### TC-REPORT-17: Kiểm tra nhật ký và lỗi trình duyệt (Browser Error Inspection)
- **Mục tiêu:** Kiểm tra Console errors, Network failures và vòng lặp request.
- **Kết quả:**
  - Console: 0 lỗi Uncaught TypeError hay SyntaxError.
  - Network: Mọi tài nguyên static (`manager-common.css`, `manager-reports.js`, fonts) tải về HTTP 200 OK.
- **Đánh giá:** **PASS**

---

## 4. KẾT QUẢ AUTOMATED TEST SUITE TOÀN DỰ ÁN

- **Lệnh chạy:** `.\mvnw.cmd test`
- **Kết quả kiểm thử:**
  - **Tổng số tests chạy:** **443 tests**
  - **Passed:** **443 tests**
  - **Failures:** **0**
  - **Errors:** **0**
  - **Skipped:** **0**
  - **Trạng thái Build:** **BUILD SUCCESS** (Thời gian chạy: 42.09 giây)
- Bao gồm 14 test cases trực tiếp cho module Báo cáo Doanh thu:
  - `RevenueReportServiceTest`: 6/6 tests pass
  - `RevenueReportControllerTest`: 3/3 tests pass
  - `RevenueReportApiControllerTest`: 5/5 tests pass
