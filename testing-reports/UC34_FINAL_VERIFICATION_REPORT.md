# BÁO CÁO NGHIỆM THU VÀ KIỂM CHỨNG CUỐI CÙNG: UC-34 REVENUE REPORT
## CINEORA Cinema Management System — Final QA Audit & Verification Report

---

## 1. TỔNG QUAN KẾT QUẢ NGHIỆM THU (EXECUTIVE SUMMARY)

| Hạng mục kiểm thử | Trạng thái | Ghi chú & Kết quả |
| :--- | :---: | :--- |
| **Sửa lỗi BUG-UC34-01** (TOTAL row & KPI = 0) | **PASS / RESOLVED** | Đã loại bỏ con trỏ `rs.next()` dư thừa trong `RevenueReportDao`. Số liệu KPI và TOTAL hiển thị chính xác 100%. |
| **Sửa lỗi BUG-UC34-02** (Lệch cột CSV do dấu phẩy tiền tệ) | **PASS / RESOLVED** | Đã bọc RFC 4180 quotes qua `formatMoney()` và `escape()`. File CSV chuẩn UTF-8 BOM, mở bằng Excel không vỡ cột, không lỗi font. |
| **Bổ sung dòng TOTAL cho Movie performance** | **PASS / IMPLEMENTED** | Đã bổ sung `<tfoot>` dòng TOTAL cho báo cáo theo phim và chi tiết giao dịch. Khắc phục triệt để lỗi double-counting `TotalRevenue` do JOIN `BookingSeat`. |
| **Kiểm thử Google Chrome E2E** | **PASS (17/17 Test cases)** | Kiểm thử thực tế trên giao diện Chrome, DevTools Console 0 lỗi, Network 200 OK. |
| **Kiểm thử tự động Maven** | **PASS (444/444 Unit & Integration Tests)** | Toàn bộ 444 bài test của hệ thống CINEORA đều PASS (BUILD SUCCESS). |
| **Đối soát cơ sở dữ liệu thực tế** | **PASS (100% Khớp)** | Khớp từng trường dữ liệu với SQL Server `CinemaManagementDB` (Server: `HOANGHUNG\SQLEXPRESS`). |

---

## 2. DANH SÁCH CÁC FILE ĐÃ SỬA ĐỔI (MODIFIED FILES)

Tất cả các thay đổi đều nằm trong phạm vi chức năng UC-34 — Revenue Report:

1. [`src/main/java/com/cinema/service/RevenueReportService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/RevenueReportService.java)
   - Bổ sung helper `formatMoney(BigDecimal amount, DecimalFormat df)` bọc chuỗi tiền tệ đã format trong `escape()` theo chuẩn RFC 4180.
   - Chuẩn hóa việc escape tất cả các trường dữ liệu (mã đơn, tên phim, phòng chiếu, ghế ngồi, phương thức thanh toán, số tiền) trong khối xuất CSV của cả 3 loại báo cáo (`DAILY`, `MOVIE`, `TRANSACTIONS`).
   - Bổ sung tính tổng fallback safeguard cho Summary KPI.

2. [`src/main/java/com/cinema/dao/RevenueReportDao.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/RevenueReportDao.java)
   - Sửa lỗi con trỏ `rs.next()` trong phương thức `getSummary(...)`.
   - **Phát hiện và sửa lỗi nhân đôi doanh thu phim:** Trong `getMovieRevenue(...)`, câu lệnh cũ `SUM(b.TotalAmount)` khi `LEFT JOIN dbo.BookingSeat` đã nhân giá trị `TotalAmount` với số lượng ghế đã đặt của mỗi đơn (Booking 6 và Booking 7 có 2 ghế mỗi đơn, khiến tổng doanh thu phim Avatar bị nhân đôi thành `1,632,000 ₫` thay vì `816,000 ₫`). Đã tái cấu trúc câu truy vấn sử dụng correlated subquery gom nhóm theo suất chiếu của phim và lọc booking hợp lệ (`CONFIRMED`/`PAID`, không `EXPIRED`), bảo đảm số liệu doanh thu phim khớp chính xác tuyệt đối với tổng doanh thu hệ thống.

3. [`src/main/java/com/cinema/model/report/RevenueReportData.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/model/report/RevenueReportData.java)
   - Bổ sung các phương thức getter tính tổng tự động cho báo cáo phim và giao dịch: `getTotalMovieShowtimes()`, `getTotalMovieTicketsSold()`, `getTotalMovieTicketRevenue()`, `getTotalMovieFbRevenue()`, `getTotalMovieRevenue()`, `getTotalTransactionTicketAmount()`, `getTotalTransactionFbAmount()`, `getTotalTransactionTotalAmount()`.

4. [`src/main/resources/templates/manager/reports.html`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/reports.html)
   - Bổ sung thẻ `<tfoot>` dòng `TOTAL` cho bảng **Movie performance** (hiển thị tổng số suất chiếu, tổng vé đã bán, tổng doanh thu vé, tổng doanh thu F&B, và tổng doanh thu phim).
   - Bổ sung thẻ `<tfoot>` dòng `TOTAL` cho bảng **Transaction details** (hiển thị tổng tiền vé, tổng tiền F&B, tổng doanh thu giao dịch).

5. [`src/test/java/com/cinema/service/RevenueReportServiceTest.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/test/java/com/cinema/service/RevenueReportServiceTest.java)
   - Bổ sung test tự động `testGenerateCsvReport_Rfc4180EscapingAndColumnCount`: Kiểm tra cả 3 byte UTF-8 BOM (`0xEF, 0xBB, 0xBF`), sử dụng bộ phân tích cú pháp RFC 4180 để kiểm chứng số lượng cột chuẩn xác của từng dòng dữ liệu và dòng tiêu đề (Báo cáo Ngày: đúng 7 cột; Báo cáo Phim: đúng 7 cột; Báo cáo Giao dịch: đúng 14 cột).

---

## 3. NGUYÊN NHÂN LỖI VÀ GIẢI PHÁP CHI TIẾT (ROOT CAUSE & FIXES)

### 3.1. BUG-UC34-01: Dòng TOTAL bảng Doanh thu & Thẻ KPI hiển thị 0
- **Nguyên nhân gốc rễ:** Spring `JdbcTemplate.query(sql, RowCallbackHandler)` đã tự động chuyển con trỏ `ResultSet` đến dòng dữ liệu hợp lệ đầu tiên trước khi gọi `processRow(ResultSet rs)`. Việc gọi thêm `if (rs.next())` lồng bên trong callback làm con trỏ nhảy qua vị trí kết thúc (EOF). Khối gán `summary.setTotalRevenue(...)` bị bỏ qua, làm summary trả về toàn giá trị 0.
- **Cách sửa:**
  - Xóa dòng `if (rs.next())` trong `RevenueReportDao.getSummary(...)`.
  - Trong `RevenueReportService`, thêm hàm dự phòng tự động tính tổng từ danh sách `dailyItems` nếu summary trống hoặc chưa được khởi tạo.

### 3.2. BUG-UC34-02: Số tiền có dấu phẩy làm sai số lượng cột trong file CSV
- **Nguyên nhân gốc rễ:** `DecimalFormat df = new DecimalFormat("#,##0")` định dạng các số tiền lớn có dấu phẩy ngăn hàng nghìn (ví dụ `340,000`, `476,000`, `816,000`). Khi ghi ra dòng CSV, các giá trị này không được bao trong cặp ngoặc kép `""`. Bộ phân tích CSV nhận diện dấu phẩy là ký tự phân cách cột (delimiter), dẫn đến:
  - Báo cáo ngày bị chẻ từ 7 cột thành 10 cột.
  - Báo cáo phim bị chẻ từ 7 cột thành 10 cột.
  - Ghế ngồi `"A2, A3"` và tiền giao dịch làm bảng giao dịch bị lệch toàn bộ các cột từ ghế trở về sau.
- **Cách sửa:**
  - Viết hàm `formatMoney(...)`:
    ```java
    private String formatMoney(BigDecimal amount, DecimalFormat df) {
        if (amount == null) return escape("0");
        return escape(df.format(amount));
    }
    ```
  - Mọi chuỗi khi qua `escape()` nếu chứa dấu phẩy `,` hoặc ngoặc kép `"` sẽ tự động được bao trong `""` và nhân đôi ngoặc kép bên trong theo đúng chuẩn RFC 4180:
    ```
    "21/10/2026","340,000","476,000","0","816,000",4,2
    ```
  - Ghi 3 bytes UTF-8 BOM `0xEF, 0xBB, 0xBF` ở đầu file để bảo đảm Microsoft Excel tự động nhận diện bảng mã UTF-8 tiếng Việt có dấu mà người dùng không cần chuyển mã thủ công.

### 3.3. Lỗi nhân đôi doanh thu bảng Phim (Movie Performance Double-Counting Bug)
- **Nguyên nhân gốc rễ:** Câu truy vấn `getMovieRevenue` trước đây dùng:
  ```sql
  LEFT JOIN dbo.BookingSeat bs ON b.BookingId = bs.BookingId
  ```
  Nếu một đơn hàng có $N$ ghế ngồi (ví dụ 2 ghế A2, A3), câu lệnh SQL sẽ tạo ra $N$ bản ghi kết hợp với bảng `Booking`, làm hàm `SUM(b.TotalAmount)` cộng dồn $N$ lần giá trị đơn hàng.
- **Cách sửa:** Tách việc tính tổng `TotalRevenue` của phim thành subquery độc lập theo suất chiếu của phim đó:
  ```sql
  (SELECT COALESCE(SUM(b_sub.TotalAmount), 0)
   FROM dbo.Booking b_sub
   INNER JOIN dbo.Showtime st_sub ON b_sub.ShowtimeId = st_sub.ShowtimeId
   WHERE st_sub.MovieId = m.MovieId
     AND CAST(b_sub.BookingTime AS DATE) BETWEEN ? AND ?
     AND b_sub.BookingStatus IN ('CONFIRMED', 'PAID')) AS TotalRevenue
  ```
  Số liệu Avatar sau khi sửa: Doanh thu vé = `340,000 ₫`, F&B = `476,000 ₫`, Tổng doanh thu phim = `816,000 ₫` (khớp 100% với doanh thu toàn hệ thống).

---

## 4. KẾT QUẢ KIỂM THỬ THỰC TẾ TRÊN GOOGLE CHROME (CHROME E2E VERIFICATION)

Ứng dụng CINEORA chạy trực tiếp trên cổng 8080, đăng nhập bằng tài khoản Manager (`manager@cinema.com` / `Manager123`), truy cập `/manager/reports`.

### 4.1. Thẻ KPI Tổng quan (Chế độ chọn tháng 10/2026)
- **Total revenue:** `816,000 ₫` (Doanh thu thực nhận theo BR-22)
- **Ticket revenue:** `340,000 ₫` (Tỷ trọng: 41.7%)
- **F&B revenue:** `476,000 ₫` (Popcorn, drinks & combos)
- **Tickets sold:** `4` (Ghế đã bán thực tế)
- **Successful orders:** `2` (Giao dịch hoàn tất)

### 4.2. Báo cáo 1: Daily Overview (`DAILY`)
- **Dòng dữ liệu ngày 21/10/2026:**
  - Date: `21/10/2026`
  - Ticket Revenue: `340,000 ₫`
  - F&B Revenue: `476,000 ₫`
  - Discount: `0 ₫`
  - Total Revenue: `816,000 ₫`
  - Tickets Sold: `4`
  - Orders: `2`
- **Dòng TOTAL ở chân bảng:**
  - Hiển thị đầy đủ 7 cột: `TOTAL`, `340,000 ₫`, `476,000 ₫`, `0 ₫`, `816,000 ₫`, `4`, `2`.
  - Khớp 100% với thẻ KPI phía trên.

### 4.3. Báo cáo 2: Movie Performance (`MOVIE`)
- **Dòng dữ liệu phim:**
  - Phim 1: *Avatar: The Way of Water* — 5 suất chiếu, 4 vé bán, Doanh thu vé: `340,000 ₫`, F&B: `476,000 ₫`, Tổng doanh thu: `816,000 ₫` (100% tỷ trọng).
  - Phim 2: *Oppenheimer* — 4 suất chiếu, 0 vé bán, 0 ₫.
  - Phim 3: *Dune: Part Two* — 2 suất chiếu, 0 vé bán, 0 ₫.
  - Phim 5: *Godzilla x Kong* — 5 suất chiếu, 0 vé bán, 0 ₫.
  - Phim 7: *Maii* — 1 suất chiếu, 0 vé bán, 0 ₫.
- **Dòng TOTAL mới được bổ sung:**
  - Colspan 2: `TOTAL`
  - Số suất chiếu: `17`
  - Số vé bán: `4`
  - Doanh thu vé: `340,000 ₫`
  - Doanh thu F&B: `476,000 ₫`
  - Tổng doanh thu: `816,000 ₫`
  - Tỷ trọng: `100.0%`

### 4.4. Báo cáo 3: Transaction Details (`TRANSACTIONS`)
- **Danh sách giao dịch thành công:**
  - Đơn 1: Mã `BK26102111042100F016` — Phim Avatar, Phòng Cinema 01, Suất 11:09, Ghế `A4, A5`, Tiền vé: `170,000 ₫`, Tiền F&B: `238,000 ₫`, Tổng: `408,000 ₫`, Hình thức: `COUNTER`, Trạng thái: `CONFIRMED`.
  - Đơn 2: Mã `BK261021110343E937F2` — Phim Avatar, Phòng Cinema 01, Suất 11:09, Ghế `A2, A3`, Tiền vé: `170,000 ₫`, Tiền F&B: `238,000 ₫`, Tổng: `408,000 ₫`, Hình thức: `COUNTER`, Trạng thái: `CONFIRMED`.
- **Dòng TOTAL ở chân bảng:**
  - Colspan 6: `TOTAL`
  - Tiền vé: `340,000 ₫`
  - Tiền F&B: `476,000 ₫`
  - Tổng tiền: `816,000 ₫`
  - Colspan 2 (Payment & Status).

### 4.5. Kiểm tra tính năng Print Report & DevTools
- Nút **Print report** kích hoạt hộp thoại in tiêu chuẩn của trình duyệt. CSS `@media print` ẩn sidebar, topbar, thanh lọc và các nút thao tác, căn 5 thẻ KPI trên một hàng và trình bày bảng dữ liệu rõ ràng.
- DevTools Console: **0 lỗi JavaScript (0 errors)**, 0 cảnh báo bảo mật.
- Network Tab: Các yêu cầu GET trang và GET API tải về đều trả về HTTP 200 OK.

---

## 5. KẾT QUẢ KIỂM THỬ XUẤT CSV VÀ ĐỐI CHIẾU DỮ LIỆU THỰC TẾ

Tải trực tiếp 3 file CSV từ API `/api/manager/reports/export` của server đang chạy và tiến hành đối soát bằng bộ phân tích cú pháp RFC 4180 CSV:

| File Báo Cáo | Kích thước | UTF-8 BOM (`EF BB BF`) | Số cột Header | Số cột Data Row | Kết quả phân tích cú pháp |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `daily_export.csv` | 742 bytes | **ĐẠT** | 7 cột | 7 cột | Các giá trị `"340,000"`, `"476,000"`, `"816,000"` nằm trọn trong cột, không bị tách dòng. |
| `movie_export.csv` | 924 bytes | **ĐẠT** | 7 cột | 7 cột | Tên phim `"Avatar: The Way of Water"` và doanh thu `"816,000"` hiển thị đúng cột. |
| `transactions_export.csv` | 1,181 bytes | **ĐẠT** | 14 cột | 14 cột | Ghế `"A4, A5"`, `"A2, A3"` và số tiền `"170,000"`, `"238,000"`, `"408,000"` đúng 14 cột chuẩn. |

---

## 6. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (AUTOMATED TEST SUITE)

1. **Kiểm thử riêng phân hệ Báo cáo Doanh thu (UC-34):**
   ```bash
   .\mvnw.cmd test "-Dtest=RevenueReportServiceTest,RevenueReportControllerTest,RevenueReportApiControllerTest"
   ```
   - **Kết quả:** `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` -> **BUILD SUCCESS**.
   - Bao gồm 7 test case trong `RevenueReportServiceTest`, 5 test case trong `RevenueReportApiControllerTest`, 3 test case trong `RevenueReportControllerTest`.

2. **Kiểm thử hồi quy toàn bộ hệ thống (Full Regression Test):**
   ```bash
   .\mvnw.cmd test
   ```
   - **Kết quả:** `Tests run: 444, Failures: 0, Errors: 0, Skipped: 0` -> **BUILD SUCCESS**.
   - Thời gian thực thi: 35.906 s.
   - Toàn bộ các module quản lý phim, phòng chiếu, loại ghế, bảng giá, F&B, lịch chiếu, bảo mật và tài khoản đều hoạt động ổn định, không phát sinh bất kỳ lỗi hồi quy nào.

---

## 7. ĐỐI CHIẾU DỮ LIỆU CƠ SỞ DỮ LIỆU (DATABASE RECONCILIATION)

Truy vấn đối soát trực tiếp từ CSDL Microsoft SQL Server `CinemaManagementDB`:

```sql
SELECT b.BookingId, b.BookingCode, b.BookingTime, b.BookingStatus, b.PaymentMethod, b.TotalAmount
FROM dbo.Booking b
WHERE CAST(b.BookingTime AS DATE) BETWEEN '2026-10-01' AND '2026-10-31';
```

- **Booking 5:** Trạng thái `EXPIRED` (hết hạn), `TotalAmount = 85,000 ₫` -> Hệ thống loại bỏ chính xác theo Business Rule BR-22 (chỉ công nhận doanh thu đơn hàng thành công).
- **Booking 6:** Trạng thái `CONFIRMED`, `PaymentMethod = COUNTER`, gồm 2 ghế A2, A3 (170,000 ₫) và 2 món F&B (238,000 ₫) -> `TotalAmount = 408,000 ₫`.
- **Booking 7:** Trạng thái `CONFIRMED`, `PaymentMethod = COUNTER`, gồm 2 ghế A4, A5 (170,000 ₫) và 2 món F&B (238,000 ₫) -> `TotalAmount = 408,000 ₫`.
- **Tổng đối soát:**
  - Doanh thu vé: $170,000 + 170,000 = 340,000 \text{ ₫}$.
  - Doanh thu F&B: $238,000 + 238,000 = 476,000 \text{ ₫}$.
  - Tổng doanh thu hệ thống: $408,000 + 408,000 = 816,000 \text{ ₫}$.
  - Tổng số vé: $2 + 2 = 4 \text{ vé}$.
  - Tổng đơn hàng thành công: $2 \text{ đơn}$.

-> **Tỷ lệ khớp số liệu:** **100% giữa Database, Backend Service, UI Frontend, và File CSV Export.**

---

## 8. TRẠNG THÁI CUỐI CÙNG CỦA CÁC DEFECTS

| Bug ID | Tiêu đề lỗi | Trạng thái ban đầu | Trạng thái hiện tại | Bằng chứng nghiệm thu |
| :---: | :--- | :---: | :---: | :--- |
| **BUG-UC34-01** | Dòng TOTAL bảng Doanh thu & Thẻ KPI hiển thị 0 | Open / High | **CLOSED / VERIFIED FIXED** | Dòng TOTAL hiển thị `340,000 ₫` / `476,000 ₫` / `816,000 ₫` / `4` / `2`. KPI thẻ hiển thị `816,000 ₫`. Unit test & E2E Chrome PASS. |
| **BUG-UC34-02** | Dấu phẩy ngăn cách hàng nghìn làm vỡ cấu trúc cột CSV | Open / Medium | **CLOSED / VERIFIED FIXED** | File CSV xuất ra có BOM UTF-8, các trường tiền tệ được bọc dấu ngoặc kép chuẩn RFC 4180. Parser CSV kiểm tra đúng 7 cột (Daily & Movie) và 14 cột (Transactions). |

---

## 9. ĐÁNH GIÁ ĐẶC TẢ SRS VÀ NHỮNG YÊU CẦU MỞ RỘNG (GAPS & RECOMMENDATIONS)

1. **Các yêu cầu đã đáp ứng xuất sắc theo SRS (UC-34 / Screens 47 & 49):**
   - Lọc báo cáo theo khoảng thời gian tùy chỉnh và các nút chọn nhanh (`Today`, `Last 7 days`, `This month`, `This quarter`, `All time`).
   - Cung cấp đầy đủ 3 chế độ xem báo cáo: Báo cáo tổng quan theo ngày (`DAILY`), Báo cáo hiệu suất theo phim (`MOVIE`), Báo cáo chi tiết danh sách giao dịch (`TRANSACTIONS`).
   - Tổng quan 5 chỉ số tài chính KPI hàng đầu (Doanh thu tổng, Doanh thu vé, Doanh thu F&B, Vé đã bán, Đơn hàng thành công) tuân thủ nghiêm ngặt quy tắc nhận diện doanh thu BR-22.
   - Có dòng `TOTAL` tổng kết ở cuối tất cả các bảng báo cáo, số liệu không bị đếm trùng lặp.
   - Xuất dữ liệu Excel/CSV chuẩn UTF-8 BOM và hỗ trợ in ấn tối ưu hóa với CSS `@media print`.
   - Phân quyền chặt chẽ theo vai trò (RBAC): Chỉ `MANAGER` và `ADMIN` có quyền xem và xuất báo cáo.

2. **Khuyến nghị nâng cấp tiếp theo (Future Enhancements):**
   - **Biểu đồ trực quan (Data Visualization):** SRS Section 4.9 có đề cập đến biểu đồ doanh thu theo thời gian. Hiện tại giao diện đang tập trung hiển thị dạng bảng số liệu và thẻ KPI. Có thể tích hợp thêm thư viện Chart.js hoặc ApexCharts để vẽ biểu đồ cột/đường doanh thu theo ngày nhằm nâng cao trải nghiệm trực quan.
   - **Định dạng xuất `.xlsx` native:** Hiện tại hệ thống đang xuất file CSV có UTF-8 BOM (Microsoft Excel mở trực tiếp với đầy đủ dấu tiếng Việt và định dạng cột chuẩn). Trong tương lai, có thể bổ sung Apache POI để xuất file định dạng nhị phân `.xlsx` kèm màu sắc và định dạng số sẵn có trong cell.
