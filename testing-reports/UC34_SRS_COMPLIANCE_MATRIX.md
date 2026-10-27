# MA TRẬN ĐÁNH GIÁ MỨC ĐỘ TUÂN THỦ SRS & QUY TẮC NGHIỆP VỤ (UC-34)
## CINEORA Cinema Management System — SRS Compliance & Traceability Matrix

---

## 1. MA TRẬN TRUY XUẤT YÊU CẦU NGHIỆP VỤ (REQUIREMENTS TRACEABILITY MATRIX)

| Mã yêu cầu SRS | Mô tả yêu cầu theo đặc tả nghiệp vụ | Thành phần hiện thực trong Source Code | Test Case tương ứng | Trạng thái thực tế | Khoảng trống / Ghi chú (Gaps & Notes) |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **UC-34.1** | Xem báo cáo doanh thu theo các mốc thời gian: Hôm nay, 7 ngày qua, Tháng này, Quý này, Tất cả | `RevenueReportController.java`<br>`RevenueReportService.java`<br>`reports.html`<br>`manager-reports.js` | TC-REPORT-01<br>TC-REPORT-02 | **PASS** | Hoạt động trơn tru, tự động cập nhật ngày và số liệu chính xác khi nhấn từng nút preset. |
| **UC-34.2** | Xem báo cáo doanh thu theo khoảng ngày tùy chọn (Custom Date Range) | `RevenueReportController.java`<br>`manager-reports.js` | TC-REPORT-03 | **PASS** | Đã có validate 2 lớp: Frontend pop-up và Backend HTTP 400 khi ngày bắt đầu lớn hơn ngày kết thúc. |
| **UC-34.3** | Phân loại báo cáo theo 3 góc nhìn: Theo ngày (DAILY), theo phim (MOVIE), chi tiết giao dịch (TRANSACTIONS) | `RevenueReportDao.java`<br>`RevenueReportService.java`<br>`reports.html` | TC-REPORT-04 | **PASS** | Đổi cấu trúc bảng tương ứng, hiển thị đúng dữ liệu phim và danh sách từng đơn hàng. |
| **UC-34.4** | Hiển thị 5 thẻ chỉ số tài chính KPI: Tổng doanh thu, Doanh thu vé, Doanh thu F&B, Số vé bán, Số đơn thành công | `RevenueReportDao.getSummary`<br>`reports.html` | TC-REPORT-05<br>TC-REPORT-06<br>TC-REPORT-07<br>TC-REPORT-08<br>TC-REPORT-09 | **PASS** | Số liệu đối soát khớp 100% với CSDL SQL Server thực tế (816k, 340k, 476k, 4 vé, 2 đơn). |
| **UC-34.5** | Hiển thị bảng tổng hợp doanh thu theo ngày với dòng cộng tổng (TOTAL) | `RevenueReportDao.getDailyRevenue`<br>`reports.html` (tfoot) | TC-REPORT-10 | **PASS** | Lỗi hiển thị 0 ở dòng TOTAL trước đây đã được khắc phục hoàn toàn. |
| **UC-34.6** | Xuất báo cáo tài chính ra file Excel định dạng CSV tương thích Microsoft Excel | `RevenueReportApiController.java`<br>`RevenueReportService.generateCsvReport` | TC-REPORT-11<br>TC-REPORT-12 | **PARTIAL** | Tải file thành công, có UTF-8 BOM hiển thị tiếng Việt chuẩn. Tuy nhiên còn tồn tại lỗi BUG-UC34-02 (số tiền có dấu phẩy làm ngắt cột sai). |
| **UC-34.7** | In ấn báo cáo doanh thu trực tiếp từ trình duyệt (Print Report) | `reports.html` (inline onclick & CSS `@media print`) | TC-REPORT-13 | **PASS** | CSS `@media print` đã ẩn sidebar/header, trải rộng báo cáo 100% trang in và giữ nguyên bảng số liệu. |
| **BR-22** | Quy tắc công nhận doanh thu: Chỉ tính các đơn hàng có trạng thái PAID, CONFIRMED, COMPLETED | `RevenueReportDao.java`<br>(Mệnh đề `WHERE UPPER(b.[Status]) IN (...)`) | TC-REPORT-05<br>TC-REPORT-09 | **PASS** | Đã loại trừ chính xác đơn hàng `EXPIRED` (BookingID 5, 85,000 VND), chỉ tính 2 đơn `CONFIRMED`. |
| **BR-24** | Phân tách doanh thu vé xem phim độc lập | `RevenueReportDao.ticketSql`<br>`dbo.BookingSeat.UnitPrice` | TC-REPORT-06 | **PASS** | Tính chính xác 340,000 VND từ 4 ghế A2, A3, A4, A5. |
| **BR-25** | Phân tách doanh thu bắp nước / F&B độc lập | `RevenueReportDao.fbSql`<br>`dbo.BookingFoodBeverage` | TC-REPORT-07 | **PASS** | Tính chính xác 476,000 VND từ 4 phần bắp rang bơ phô mai. |
| **BR-37** | Khấu trừ giá trị voucher giảm giá vào doanh thu thực nhận | `dbo.Booking.DiscountAmount` | TC-REPORT-05<br>TC-REPORT-10 | **PASS** | Không bị trừ trùng lặp, khớp đúng công thức Total = Ticket + FB - Discount. |
| **SEC-01** | Kiểm soát quyền truy cập theo vai trò (Role-Based Access Control) | `SecurityConfig.java`<br>`ManagerSecurityUtils.java` | TC-REPORT-14 | **PASS** | Chỉ ADMIN và MANAGER được truy cập; ANONYMOUS, STAFF và CUSTOMER bị chặn 100%. |

---

## 2. TỔNG HỢP TRẠNG THÁI TUÂN THỦ (COMPLIANCE SUMMARY)

```
Tổng số tiêu chí kiểm định: 12
- PASS (Đạt yêu cầu):         11 / 12 (91.7%)
- PARTIAL (Đạt một phần):      1 / 12 (8.3%)  [Lỗi định dạng ngắt cột CSV - BUG-UC34-02]
- FAIL (Không đạt):            0 / 12 (0%)
- NOT IMPLEMENTED (Chưa làm):  0 / 12 (0%)
- BLOCKED (Bị chặn):           0 / 12 (0%)
```

---

## 3. LỘ TRÌNH VÀ KHUYẾN NGHỊ ƯU TIÊN (PRIORITY ACTION PLAN)

1. **Ưu tiên 1 (P1 - Cần sửa ngay):**
   - Khắc phục lỗi `BUG-UC34-02` trong `RevenueReportService.java`: Bọc các trường tiền tệ được format có dấu phẩy trong dấu ngoặc kép `"..."` hoặc xuất số nguyên thuần tuý để bảo đảm file CSV mở trong Microsoft Excel không bị lệch cột.
2. **Ưu tiên 2 (P2 - Tối ưu trải nghiệm):**
   - Bổ sung dòng chân bảng `TOTAL` cho chế độ xem báo cáo phim (`Movie performance`) tương tự như bảng báo cáo theo ngày (`Daily revenue summary`).
3. **Ưu tiên 3 (P3 - Giữ vững chất lượng):**
   - Tiếp tục duy trì 443 bài kiểm tra automated unit & integration tests trong quy trình CI/CD để ngăn ngừa tái phát lỗi trượt con trỏ `rs.next()`.
