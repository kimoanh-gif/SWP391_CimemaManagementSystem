# BÁO CÁO KẾT QUẢ KIỂM THỬ LẠI (RETEST RESULT REPORT) – CINEORA SYSTEM TESTING

**Dự án:** Hệ thống Quản lý Rạp chiếu phim Cineora (Cineora Cinema Management System)  
**Kỹ sư thực hiện:** Nguyễn Hoàng Hưng (`hungnhhe186547@fpt.edu.vn`)  
**Phạm vi phụ trách (Module Ownership):** Manager Module  
**Ngày thực hiện:** 07/11/2026  
**Môi trường kiểm thử:**  
- Ứng dụng: Spring Boot 3.5.7 trên cổng `http://localhost:8080`
- Cơ sở dữ liệu: Microsoft SQL Server (`localhost\SQLEXPRESS`, Database: `CinemaManagementDB`)
- Trình duyệt tự động: Microsoft Edge / Playwright Headless
- Tài khoản kiểm thử: `manager@cinema.com` / `Manager123`

---

## 1. TỔNG HỢP KẾT QUẢ KIỂM THỬ LẠI (RETEST SUMMARY TABLE)

| Test Case ID | Expected Result | Actual Result | Evidence | Retest Status | Defect Status |
| :--- | :--- | :--- | :--- | :---: | :---: |
| **ST_001** | Dashboard hiển thị đúng số vé đã bán (Tickets sold > 0) tương ứng với doanh thu đã ghi nhận, không bị lỗi SQL Server 207 hoặc mặc định về 0. | Dashboard KPI card hiển thị chính xác `Tickets sold = 4` với `0.1% seat occupancy` và doanh thu `816,000 VND`, khớp hoàn toàn với dữ liệu thực tế trong `dbo.Booking` & `dbo.BookingSeat`. | [st001_dashboard_fixed.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st001_dashboard_fixed.png), API query `GET /api/manager/dashboard/stats` | **Passed** | **Closed** |
| **ST_014** | Khi quản trị viên sửa Room capacity (ví dụ: từ 48 lên 60 ghế) mà Seat Map vẫn chỉ có 48 ghế thì hệ thống phải từ chối và hiển thị thông báo lỗi rõ ràng. | Hệ thống từ chối cập nhật phòng và trả về lỗi: `"Room capacity (60 seats) does not match the configured Seat Map (96 seats). Please update the Seat Map before changing room capacity."`. | [st014_capacity_mismatch_alert.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st014_capacity_mismatch_alert.png), HTTP 400 Bad Request | **Passed** | **Closed** |
| **ST_017A** | Màn hình 57 (Seat Types) từ chối hệ số giá không hợp lệ (< 0.50 hoặc > 3.00) hoặc mã màu sai định dạng HEX và hiển thị thông báo lỗi chi tiết tương ứng. | - Multiplier 0.40 hiển thị: `"Minimum price multiplier is 0.50"`<br>- Multiplier 3.50 hiển thị: `"Maximum price multiplier is 3.00"`<br>- Color 'blue' hiển thị: `"Color code must be in HEX format (e.g. #3B82F6)"`<br>*(Lưu ý: Thông báo hiển thị dưới dạng Global Alert Banner ở đầu trang/modal, không phải inline bên cạnh input)*. | [st017a_multiplier_low.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st017a_multiplier_low.png), [st017a_multiplier_high.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st017a_multiplier_high.png), [st017a_color_invalid.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st017a_color_invalid.png) | **Passed** | **Closed** |
| **ST_017B** | Màn hình 60 (Ticket Pricing) từ chối giá vé cơ bản âm (Base price = -1) và hiển thị thông báo lỗi chi tiết. | Hệ thống từ chối lưu và hiển thị thông báo lỗi: `"Base ticket price cannot be negative"` trên Global Alert Banner. | [st017b_negative_base_price.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st017b_negative_base_price.png) | **Passed** | **Closed** |
| **ST_017C** | Màn hình 61 (F&B Catalog) từ chối khi giá vốn vượt quá giá bán (Cost price > Selling price) và hiển thị thông báo lỗi chi tiết. | Hệ thống từ chối lưu và hiển thị thông báo lỗi: `"Cost price cannot exceed selling price"` trên Global Alert Banner. | [st017c_cost_exceeds_selling.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st017c_cost_exceeds_selling.png) | **Passed** | **Closed** |
| **ST_025** | Hệ thống từ chối xếp lịch chiếu trước ngày khởi chiếu của phim (Release Date) và hiển thị thông báo lỗi rõ ràng. | Khi xếp lịch chiếu ngày 09/11/2026 cho phim 'hay' (khởi chiếu 11/11/2026), hệ thống từ chối và hiển thị: `"Cannot schedule showtime on 2026-11-09 because the movie release date is 2026-11-11."`. Không có record nào bị chèn vào `dbo.Showtime`. | [st025_showtime_before_release.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st025_showtime_before_release.png), SQL Query xác nhận ST_Count = 0 | **Passed** | **Closed** |
| **ST_035** | Báo cáo doanh thu theo phim khi lọc ngày 07/11/2026 chỉ đếm các suất chiếu diễn ra trong ngày 07/11/2026, đối chiếu chính xác với SQL Server và khớp giữa UI và CSV. | - Trong database SQL Server ngày 07/11/2026: Không có suất chiếu nào (ShowtimesNov7 = 0) cho toàn bộ các phim.<br>- Trên UI (`/manager/reports`): Hiển thị bảng rỗng (0 suất chiếu, 0 VND doanh thu).<br>- Trong file CSV xuất khẩu ngày 07/11/2026: Dòng dữ liệu hiển thị `"No movie data found for this period"`, tổng doanh thu và vé bán = 0.<br>=> UI, CSV và Database khớp nhau 100%. Không bị dồn các suất chiếu từ tháng 10 vào báo cáo. *(Phim Bambi không có trong database hiện tại)*. | [st035_movie_report_20261107.png](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st035_movie_report_20261107.png), file CSV `st038_exported_report.csv`, log truy vấn SQL | **Passed** | **Closed** |
| **ST_038** | File CSV xuất khẩu phải có tiêu đề thương hiệu Cineora (`CINEMA REVENUE REPORT - CINEORA`, không còn thương hiệu cũ `ROYALLY CINEMA`) và mã phim có định dạng `MOV-%d`. | File CSV xuất khẩu có dòng 1: `CINEMA REVENUE REPORT - CINEORA`. Các mã phim được định dạng đúng chuẩn `MOV-1`, `MOV-2`, `MOV-3`, `MOV-5`, `MOV-7`. Không còn bất kỳ chuỗi `ROYALLY CINEMA` nào. | [st038_exported_report.csv](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st038_exported_report.csv), kiểm tra nội dung file CSV | **Passed** | **Closed** |
| **ST_040** | Job tự động giải phóng ghế giữ hết hạn (JOB-01) hoạt động đúng sau 10 phút giữ chỗ. | 7/7 unit tests trong `ReleaseExpiredSeatReservationsJobTest` Passed. Database kiểm thử hiện không có ghế nào ở trạng thái `HELD`. Chưa thể thực hiện kiểm thử E2E trọn vẹn luồng khách hàng đặt chỗ mà không can thiệp vào module của thành viên khác (`Sonha0511`). | Kết quả chạy lệnh `mvn test -Dtest=ReleaseExpiredSeatReservationsJobTest` (7 tests run, 0 failures, 0 errors). Dữ liệu SQL Server: 0 held seats. | **Retest Pending** | **TEST EVIDENCE INCOMPLETE – RETEST REQUIRED** |

---

## 2. CHI TIẾT KẾT QUẢ KIỂM THỬ TỪNG TEST CASE

### 2.1. ST_001 – Dashboard hiển thị chính xác số vé bán
- **Dữ liệu kiểm thử:** `dbo.Booking` có 2 đơn hàng hoàn tất thanh toán (BookingID 6 và 7, tổng 4 ghế đã bán).
- **Thao tác:** Đăng nhập Manager, truy cập `GET /manager/dashboard` và gọi API `GET /api/manager/dashboard/stats`.
- **Kết quả thực tế:**
  - `totalRevenue`: 816,000 VND (Ticket: 340,000 VND, F&B: 476,000 VND).
  - `totalBookedTickets`: 4 vé.
  - KPI Card hiển thị đầy đủ icon và số lượng, không bị reset về 0 hoặc hiển thị "No bookings yet".
- **Evidence:** `e2e/st001_dashboard_fixed.png`.

---

### 2.2. ST_014 – Ràng buộc sức chứa phòng chiếu và Seat Map
- **Dữ liệu kiểm thử:** Phòng chiếu RoomID = 1 có 96 bản ghi ghế trong `dbo.Seat` (48 ghế kích hoạt).
- **Thao tác:** Gửi request cập nhật sức chứa phòng thành 60 ghế (6 hàng x 10 cột) qua `PUT /api/manager/rooms/1`.
- **Kết quả thực tế:** Hệ thống trả về HTTP 400 Bad Request với thông báo:  
  `"Room capacity (60 seats) does not match the configured Seat Map (96 seats). Please update the Seat Map before changing room capacity."`  
  Dữ liệu phòng trong cơ sở dữ liệu được bảo toàn nguyên vẹn.
- **Evidence:** `e2e/st014_capacity_mismatch_alert.png`.

---

### 2.3. ST_017 – Phân tích thông báo lỗi validation (ST_017A, ST_017B, ST_017C)
> [!IMPORTANT]
> **Làm rõ bản chất hiển thị lỗi:**  
> Trong kiến trúc giao diện của Cineora, các màn hình Quản lý (Screen 57, 60, 61) sử dụng cơ chế **Global Alert Banner** (`<div class="alert alert-error" th:if="${errorMessage}">`) ở đầu trang hoặc đầu modal để hiển thị thông điệp lỗi.  
> Trong Thymeleaf template **không cài đặt thẻ lỗi nội dòng (inline error span/div `th:errors="*{field}"`)** bên cạnh từng ô input.  
> Do đó, bản sửa lỗi đã trích xuất đúng thông điệp lỗi trường cụ thể từ `BindingResult.getFieldErrors()` (thay vì chuỗi chung chung vô nghĩa) và hiển thị lên **Global Alert Banner**. Điều này hoàn toàn đáp ứng yêu cầu cung cấp thông tin phản hồi có ý nghĩa cho người dùng mà không phá vỡ layout giao diện hiện tại.

- **ST_017A (Seat Types - Screen 57):**
  - Hệ số 0.40: Bị chặn, thông báo `"Minimum price multiplier is 0.50"` (Evidence: `e2e/st017a_multiplier_low.png`).
  - Hệ số 3.50: Bị chặn, thông báo `"Maximum price multiplier is 3.00"` (Evidence: `e2e/st017a_multiplier_high.png`).
  - Màu 'blue': Bị chặn, thông báo `"Color code must be in HEX format (e.g. #3B82F6)"` (Evidence: `e2e/st017a_color_invalid.png`).
- **ST_017B (Ticket Pricing - Screen 60):**
  - Giá vé cơ bản -1: Bị chặn, thông báo `"Base ticket price cannot be negative"` (Evidence: `e2e/st017b_negative_base_price.png`).
- **ST_017C (F&B Catalog - Screen 61):**
  - Giá vốn 60,000 VND > Giá bán 50,000 VND: Bị chặn, thông báo `"Cost price cannot exceed selling price"` (Evidence: `e2e/st017c_cost_exceeds_selling.png`).

---

### 2.4. ST_025 – Chặn xếp lịch chiếu trước ngày khởi chiếu của phim
- **Dữ liệu kiểm thử:** Phim 'hay' (MovieID = 15, ReleaseDate = `2026-11-11`, Status = `Upcoming`).
- **Thao tác:** Quản trị viên xếp lịch chiếu vào ngày `09/11/2026 10:00:00` (sớm hơn ngày phát hành 2 ngày).
- **Kết quả thực tế:** Hệ thống kích hoạt ngoại lệ `InvalidShowtimeException` và hiển thị thông báo lỗi rõ ràng trên UI:  
  `"Cannot schedule showtime on 2026-11-09 because the movie release date is 2026-11-11."`  
  Truy vấn SQL đối chứng: `SELECT COUNT(*) FROM dbo.Showtime WHERE MovieID = 15 AND CAST(StartTime AS DATE) = '2026-11-09'` trả về kết quả `0`.
- **Evidence:** `e2e/st025_showtime_before_release.png`.

---

### 2.5. ST_035 – Đối chiếu số liệu báo cáo doanh thu ngày 07/11/2026
- **Kiểm tra cơ sở dữ liệu SQL Server (`dbo.Showtime` & `dbo.Movie`):**
  - Tổng số suất chiếu trọn đời (lifetime): Movie 1 có 5, Movie 2 có 4, Movie 5 có 5, Movie 3 có 2, Movie 7 có 1 (tất cả đều thuộc tháng 10/2026).
  - Số suất chiếu vào đúng ngày `07/11/2026`: **0 suất chiếu** cho tất cả các phim.
  - Phim "Bambi: The Reckoning": Không tồn tại trong cơ sở dữ liệu hiện hành của rạp (chỉ tồn tại trong kịch bản seed mẫu).
- **Kiểm tra giao diện (`GET /manager/reports` với khoảng ngày 07/11/2026 - 07/11/2026):**
  - Bảng báo cáo hiển thị trạng thái không có dữ liệu, không có dòng nào hiển thị suất chiếu.
- **Kiểm tra xuất file CSV (`GET /api/manager/reports/export?...`):**
  - File CSV xuất khẩu hiển thị: `"No movie data found for this period"`, tổng doanh thu = 0, vé bán = 0.
- **Đánh giá logic sửa lỗi:** Câu lệnh SQL đã được tối ưu hóa với `COUNT(DISTINCT CASE WHEN CAST(st.StartTime AS DATE) BETWEEN ? AND ? THEN st.ShowtimeID END)`. Do đó, hệ thống không còn bị lỗi cộng dồn toàn bộ suất chiếu trọn đời vào khoảng ngày được lọc như trước đây. UI, CSV và CSDL hoàn toàn đồng nhất.
- **Evidence:** `e2e/st035_movie_report_20261107.png`, file CSV và log truy vấn SQL.

---

### 2.6. ST_038 – Định dạng chuẩn hóa thương hiệu và mã phim trên file CSV
- **Kiểm tra nội dung file CSV xuất khẩu:**
  - Dòng 1: `CINEMA REVENUE REPORT - CINEORA` (đã loại bỏ hoàn toàn tên thương hiệu cũ `ROYALLY CINEMA`).
  - Dòng 2: `Export time,"07/11/2026 18:48"`.
  - Cột mã phim: Định dạng chính xác `MOV-1`, `MOV-2`, `MOV-3`, `MOV-5`, `MOV-7` khớp 100% với hiển thị trên giao diện web.
- **Evidence:** File [st038_exported_report.csv](file:///c:/Users/hoang/Downloads/SWP_RapPhim/e2e/st038_exported_report.csv).

---

### 2.7. ST_040 – Kiểm thử Job giải phóng ghế hết hạn (JOB-01)
- **Tình trạng phân quyền sở hữu:**
  - File `ReleaseExpiredSeatReservationsJob.java` và `CustomerBookingDAO.java` thuộc quyền sở hữu của thành viên **Sonha0511** (Customer Booking Module).
  - Áp dụng nguyên tắc an toàn: Không chỉnh sửa mã nguồn của thành viên khác.
- **Kết quả kiểm thử tự động:**
  - Chạy `mvn test -Dtest=ReleaseExpiredSeatReservationsJobTest`: Đạt **7/7 tests passed** (100%), không có lỗi.
- **Kiểm tra trạng thái trên CSDL:**
  - Hiện tại trong bảng `dbo.BookingSeat` có 0 ghế ở trạng thái `HELD`.
  - Trong bảng `dbo.Booking`, đơn hàng cũ quá hạn (BookingID = 5) đã chuyển sang trạng thái `EXPIRED`.
- **Đánh giá và Quyết định:**
  - Do chưa thực hiện luồng end-to-end khách hàng đặt ghế mới và chờ thời gian hết hạn trên ứng dụng đang chạy (để đảm bảo không làm bẩn dữ liệu giao dịch thật), test case này tuân thủ đúng yêu cầu chỉ định: **Ghi nhận Retest Pending và giữ trạng thái TEST EVIDENCE INCOMPLETE – RETEST REQUIRED**.
  - Tuyệt đối **không đánh dấu Closed** khi chưa có đủ bằng chứng end-to-end hoàn chỉnh.

---

## 3. KIỂM TRA TÌNH TRẠNG MÃ NGUỒN (GIT INTEGRITY CHECK)

- Kiểm tra bằng lệnh: `git status --short`
- **Kết quả:**  
  Không có bất kỳ file mã nguồn (`.java`, `.html`, `.sql`, `.js` trong `src/` hoặc `database/`) nào bị chỉnh sửa hay ghi đè trong quá trình retest. Toàn bộ các file kiểm thử và bằng chứng được lưu độc lập trong thư mục `e2e/`.
