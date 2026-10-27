# BÁO CÁO TỔNG HỢP LỖI PHẦN MỀM: BÁO CÁO DOANH THU (UC-34)
## CINEORA Cinema Management System — Defect Tracking & Bug Report

---

## DANH SÁCH LỖI (DEFECT SUMMARY TABLE)

| Bug ID | Mức độ nghiêm trọng (Severity) | Trạng thái (Status) | Tính năng bị ảnh hưởng | Tóm tắt lỗi |
| :---: | :---: | :---: | :--- | :--- |
| **BUG-UC34-01** | **High** | **RESOLVED (Đã khắc phục & verify)** | Dòng TOTAL bảng Doanh thu & Thẻ KPI | Con trỏ `rs.next()` trong `RowCallbackHandler` làm cho dòng TOTAL và KPI hiển thị 0 |
| **BUG-UC34-02** | **Medium** | **OPEN (Phát hiện mới)** | Xuất file Excel (CSV Export) | Số tiền có dấu phẩy hàng nghìn không có dấu ngoặc kép làm tách cột sai khi mở file CSV |

---

## CHI TIẾT TỪNG DEFECT

### 1. BUG-UC34-01: Dòng TOTAL bảng Doanh thu và thẻ KPI hiển thị 0
- **Bug ID:** `BUG-UC34-01`
- **Mức độ (Severity):** **High** (Sai lệch số liệu báo cáo tài chính hiển thị cho Manager)
- **Tính năng ảnh hưởng:** Màn hình Báo cáo Doanh thu (`/manager/reports`), thẻ KPI `Total revenue`, `Ticket revenue`, `Tickets sold`, `Orders`, và dòng `TOTAL` của bảng `Daily revenue summary`.
- **Trạng thái:** **RESOLVED** (Đã sửa mã nguồn và kiểm chứng thực tế trên trình duyệt Chrome)

#### Các bước tái hiện lỗi (Steps to Reproduce):
1. Đăng nhập hệ thống với tài khoản Quản lý (`manager@cinema.com` / `Manager123`).
2. Điều hướng vào menu **Reports & Statistics** (`/manager/reports`).
3. Chọn khoảng thời gian tháng 10/2026 (Preset: "This month").
4. Quan sát dòng ngày `21/10/2026` và dòng `TOTAL` ở chân bảng.

#### Kết quả kỳ vọng (Expected Behavior):
- Dòng `TOTAL` phải bằng tổng các cột của các dòng phía trên:
  - Ticket Revenue = `340,000`
  - F&B Revenue = `476,000`
  - Discount = `0`
  - Total Revenue = `816,000`
  - Tickets Sold = `4`
  - Orders = `2`

#### Kết quả thực tế trước khi sửa (Actual Behavior):
- Dòng ngày `21/10/2026` có số liệu đầy đủ: `340,000`, `476,000`, `0`, `816,000`, `4`, `2`.
- Nhưng dòng `TOTAL` lại hiển thị:
  - Ticket Revenue = `0`
  - F&B Revenue = `476,000` (duy nhất trường này có số liệu vì dùng `queryForObject`)
  - Discount = `0`
  - Total Revenue = `0`
  - Tickets Sold = `0`
  - Orders = `0`

#### Nguyên nhân gốc rễ (Root Cause):
- Trong [`RevenueReportDao.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/RevenueReportDao.java#L49-L85), phương thức `getSummary(...)` sử dụng `RowCallbackHandler` của Spring JdbcTemplate.
- Quy ước của Spring JdbcTemplate là `rs.next()` đã được gọi trước khi chuyển vào callback `processRow(ResultSet rs)`.
- Đoạn code cũ lại viết lồng `if (rs.next()) { ... }` bên trong lambda callback, khiến con trỏ duyệt qua dòng duy nhất của câu lệnh `SELECT SUM(...)` sang trạng thái kết thúc (EOF). Vì vậy, toàn bộ khối lệnh gán dữ liệu `summary.setTotalRevenue(...)`, `summary.setTicketRevenue(...)` không bao giờ được thực thi, dẫn đến giá trị mặc định là 0.

#### Bằng chứng (Evidence):
- Ảnh lỗi ban đầu từ người dùng: Ảnh chụp `media_1793121529226.png`.
- Ảnh kết quả sau khi khắc phục thành công: `reports_summary_1793122034675.png`.

#### Cách khắc phục đã thực hiện (Fix Applied):
1. Sửa `RevenueReportDao.java`: Bỏ lệnh `if (rs.next())` bên trong lambda của `jdbcTemplate.query(...)`.
2. Bổ sung Consistency Safeguard trong `RevenueReportService.java`: Tự động cộng tổng từ `dailyItems` nếu summary trống để bảo đảm 100% dòng `TOTAL` luôn luôn khớp với các dòng hiển thị phía trên.

---

### 2. BUG-UC34-02: Số tiền có dấu phẩy ngăn hàng nghìn trong file CSV làm vỡ cấu trúc cột
- **Bug ID:** `BUG-UC34-02`
- **Mức độ (Severity):** **Medium** (Ảnh hưởng đến độ toàn vẹn của file dữ liệu khi người dùng mở bằng Excel hoặc phần mềm kế toán)
- **Tính năng ảnh hưởng:** API xuất báo cáo Excel/CSV (`/api/manager/reports/export`) và nút bấm `Export Excel (CSV)`.
- **Trạng thái:** **OPEN** (Đã phát hiện, phân tích nguyên nhân và có phương án sửa đổi chi tiết)

#### Các bước tái hiện lỗi (Steps to Reproduce):
1. Đăng nhập với tài khoản Quản lý.
2. Truy cập `/manager/reports`.
3. Bấm nút **Export Excel (CSV)** để tải file `report_export_DAILY.csv`.
4. Mở file CSV bằng công cụ phân tích hoặc phần mềm bảng tính.

#### Kết quả kỳ vọng (Expected Behavior):
- Dòng tiêu đề có 7 cột: `Ngày,Doanh Thu Vé (VNĐ),Doanh Thu F&B (VNĐ),Giảm Giá (VNĐ),Tổng Doanh Thu (VNĐ),Số Vé Bán,Số Đơn Hàng`.
- Dòng dữ liệu tương ứng phải có đúng 7 cột. Số tiền nếu có dấu phẩy phải được bọc trong ngoặc kép:
  `"21/10/2026","340,000","476,000","0","816,000",4,2` hoặc dạng số thuần `21/10/2026,340000,476000,0,816000,4,2`.

#### Kết quả thực tế (Actual Behavior):
- Dòng dữ liệu xuất ra thực tế trong file CSV:
  `"21/10/2026",340,000,476,000,0,816,000,4,2`
- Do dấu phẩy `,` trong `340,000`, `476,000`, `816,000` không nằm trong ngoặc kép, parser CSV hiểu đây là các dấu phân tách cột (delimiter), khiến dòng bị ngắt thành **10 cột**:
  - Cột 1: `"21/10/2026"`
  - Cột 2: `340`
  - Cột 3: `000`
  - Cột 4: `476`
  - Cột 5: `000`
  - Cột 6: `0`
  - Cột 7: `816`
  - Cột 8: `000`
  - Cột 9: `4`
  - Cột 10: `2`
- Điều tương tự cũng xảy ra đối với báo cáo phim (`report_export_MOVIE.csv`) và báo cáo giao dịch (`report_export_TRANSACTIONS.csv`).

#### Nguyên nhân gốc rễ (Root Cause):
- Trong [`RevenueReportService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/RevenueReportService.java#L168-L255):
  - Biến `DecimalFormat df = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.US));` định dạng số thành chuỗi có dấu phẩy.
  - Khi in ra dòng CSV: `df.format(d.getTicketRevenue())` được ghép trực tiếp mà không bọc trong hàm `escape(...)` hoặc không bọc dấu ngoặc kép.

#### Bằng chứng kiểm thử (Evidence):
- File kiểm tra: `C:\Users\hoang\.gemini\antigravity-ide\brain\39b1f65f-19db-4665-9f8a-a481a7479171\scratch\report_export_DAILY.csv`, dòng 16:
  `"21/10/2026",340,000,476,000,0,816,000,4,2`

#### Giải pháp khắc phục khuyến nghị (Recommended Solution):
Trong file `RevenueReportService.java`:
1. **Phương án A (Khuyến nghị chuẩn Excel):** Bọc các trường tiền tệ qua hàm `escape(...)`:
   ```java
   escape(df.format(d.getTicketRevenue()))
   ```
   Hàm `escape(val)` sẽ bọc dấu ngoặc kép thành `"340,000"`, đảm bảo đúng chuẩn RFC 4180.
2. **Phương án B (Khuyến nghị chuẩn Database):** In trực tiếp số thô không có dấu phân cách hàng nghìn (ví dụ `d.getTicketRevenue().toPlainString()`), giúp Excel tự động nhận diện kiểu Number để kế toán viên có thể dùng hàm `SUM()` trực tiếp trên Excel.
