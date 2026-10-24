# DANH SÁCH ƯU TIÊN SỬA LỖI VÀ HOÀN THIỆN HỆ THỐNG (FIX PRIORITY CHECKLIST)
**Dự án:** Cinema Management System (SWP_RapPhim)  
**Tài liệu tham chiếu:** `docs/SRS_CinemaManagementSystem_SWP391_G5.docx.md`  
**Ngày kiểm toán:** 23/10/2026  

---

## BẢNG TỔNG HỢP MỨC ĐỘ ƯU TIÊN

| Mức độ ưu tiên | Ý nghĩa | Số lượng hạng mục |
| :--- | :--- | :---: |
| **P0 (Critical / Blocker)** | Lỗ hổng tắc nghẽn luồng giao dịch cốt lõi, sai lệch tính tiền, vi phạm quy tắc dữ liệu | **4** |
| **P1 (High)** | Thiếu Use Case bắt buộc trong phạm vi nghiệp vụ khách hàng và quản lý | **6** |
| **P2 (Medium)** | Thiếu Background Jobs, tính năng mở rộng, validation logic phụ trợ | **5** |
| **P3 (Low)** | Giao diện in ấn, chuẩn hóa thông báo lỗi song ngữ (MSG01-19) | **3** |

---

## 1. MỨC ĐỘ P0 – CRITICAL (ƯU TIÊN HÀNG ĐẦU / KHẮC PHỤC NGAY)

### [P0-01] Hoàn thiện tích hợp Cổng thanh toán trực tuyến VNPay (UC12 / API-01, API-02 / BR-22, BR-23, BR-24, BR-25, BR-41)
- **Vấn đề:** Hiện tại khách đặt vé online đến bước Checkout bị chặn lại với thông báo: *"Online payment is not connected yet"*. Khách không thể thanh toán để chuyển đơn vé sang trạng thái `CONFIRMED`, dẫn đến không sinh vé điện tử `dbo.Ticket` và ghế bị hủy sau 10 phút.
- **Tập tin bị ảnh hưởng:**
  - `src/main/java/com/cinema/controller/CustomerBookingController.java` (dòng 233-239)
  - `src/main/java/com/cinema/service/CustomerBookingService.java`
  - Tạo mới: `src/main/java/com/cinema/service/PaymentService.java` hoặc `VNPayService.java`
  - Tạo mới: `src/main/java/com/cinema/controller/PaymentCallbackController.java`
- **Giải pháp khuyến nghị:**
  1. Xây dựng `VNPayService` tạo URL redirect sang Sandbox VNPay kèm chữ ký số SHA512.
  2. Xây dựng Controller xử lý Return URL (khách chuyển về trình duyệt) và IPN Webhook (thông báo ngầm server-to-server).
  3. Khi nhận kết quả thanh toán thành công: Lưu `dbo.Payment`, đổi `dbo.Booking.Status = 'CONFIRMED'`, sinh bản ghi `dbo.Ticket` (kèm QRCode duy nhất), cộng điểm tích lũy thành viên (`Customer.AvailablePoints`, `TierPoints`).

---

### [P0-02] Triển khai Nghiệp vụ Kiểm tra và Tính giảm giá Voucher khi Đặt vé (UC11 / BR-16, BR-17, BR-18, BR-19, BR-20, BR-21, BR-38)
- **Vấn đề:** Mã voucher khách nhập ở màn hình `promotions.html` chỉ lưu vào HTTP Session mà không hề được validate với database, không kiểm tra điều kiện áp dụng và không trừ tiền vào tổng đơn hàng.
- **Tập tin bị ảnh hưởng:**
  - `src/main/java/com/cinema/controller/CustomerBookingController.java`
  - `src/main/java/com/cinema/service/CustomerBookingService.java`
  - `src/main/java/com/cinema/dao/PromotionDAO.java`
- **Giải pháp khuyến nghị:**
  1. Trong `PromotionDAO`, bổ sung hàm tìm kiếm voucher chi tiết: trạng thái `ACTIVE`, ngày `ValidFrom <= now <= ValidTo`.
  2. Trong `CustomerBookingService`, kiểm tra:
     - Giá trị đơn hàng tối thiểu (`MinBookingAmount`).
     - Hạng thành viên của khách (`RequiredRankID`).
     - Lượt dùng tổng hệ thống (`TotalUsageLimit`).
     - Lượt dùng của tài khoản hiện tại (`PerAccountLimit`).
  3. Tính toán số tiền giảm (`DiscountAmount`) theo % hoặc số tiền cố định (không vượt quá `MaxDiscountAmount`) và cập nhật lại `Booking.TotalAmount = Subtotal + FBTotal - DiscountAmount`.

---

### [P0-03] Chặn đánh giá phim trái phép khi chưa mua vé xem phim (UC17 / BR-30 / GB-09)
- **Vấn đề:** Phương thức `MovieCatalogService.addReview` không kiểm tra điều kiện khách hàng đã có đơn đặt vé thanh toán thành công cho bộ phim đó, dẫn đến khách hàng bất kỳ có thể spam review.
- **Tập tin bị ảnh hưởng:**
  - `src/main/java/com/cinema/service/MovieCatalogService.java` (dòng 66-75)
  - `src/main/java/com/cinema/dao/MovieReviewDAO.java`
- **Giải pháp khuyến nghị:**
  1. Trong `MovieReviewDAO` (hoặc `CustomerBookingDAO`), thêm method:
     ```sql
     SELECT COUNT(*) FROM dbo.Booking b
     JOIN dbo.BookingSeat bs ON bs.BookingID = b.BookingID
     JOIN dbo.Showtime st ON st.ShowtimeID = b.ShowtimeID
     WHERE b.CustomerID = ? AND st.MovieID = ? AND UPPER(b.[Status]) = 'CONFIRMED'
     ```
  2. Trong `MovieCatalogService.addReview`, gọi hàm trên. Nếu kết quả `= 0`, từ chối lưu review và ném ngoại lệ hoặc trả về thông báo lỗi: *"Bạn chỉ có thể đánh giá những bộ phim mà bạn đã từng mua vé xem."*

---

### [P0-04] Xây dựng Giao diện & Luồng Hiển thị Vé điện tử kèm mã QR (UC13 / Screen 24 / BR-26, BR-28)
- **Vấn đề:** Sau khi đặt vé, khách hàng không có cách nào xem lại vé điện tử và mã QR Code để xuất trình tại quầy check-in.
- **Tập tin bị ảnh hưởng:**
  - `src/main/java/com/cinema/controller/CustomerBookingController.java`
  - Tạo mới template: `src/main/resources/templates/customer/booking/ticket.html`
- **Giải pháp khuyến nghị:**
  1. Tạo endpoint `GET /customer/bookings/{bookingId}/ticket`.
  2. Tạo template hiển thị vé điện tử trực quan: poster phim, suất chiếu, phòng, số ghế, mã QR code (render inline SVG hoặc thư viện JS QRCode/ZXing).

---

## 2. MỨC ĐỘ P1 – HIGH (ƯU TIÊN CAO / CẦN HOÀN THIỆN SỚM)

### [P1-01] Xây dựng Giao diện & Endpoint Xem lịch sử đơn hàng của Khách hàng (UC14 / Screen 26 / BR-29, BR-37)
- **Vấn đề:** Khách hàng không thể tra cứu lại các đơn đặt vé trong quá khứ của mình.
- **Tập tin cần bổ sung:**
  - Bổ sung vào `CustomerProfileController` hoặc tạo `CustomerHistoryController.java`.
  - Bổ sung DAO query trong `CustomerBookingDAO` tìm danh sách booking theo `CustomerID`.
  - Tạo template: `src/main/resources/templates/customer/history.html`.

---

### [P1-02] Xây dựng Phân hệ Quản lý Khuyến mãi & Voucher cho Manager (UC33 / Screen 62 / BR-17..21, BR-38)
- **Vấn đề:** Manager hoàn toàn không có công cụ tạo mới, sửa hoặc tắt/bật voucher khuyến mãi.
- **Tập tin cần bổ sung:**
  - `src/main/java/com/cinema/controller/PromotionController.java` và `api/PromotionApiController.java`
  - `src/main/java/com/cinema/service/PromotionManagementService.java`
  - Bổ sung các phương thức `insert`, `update`, `delete`, `toggleStatus` vào `PromotionDAO.java`.
  - Tạo template: `src/main/resources/templates/manager/promotions.html`.

---

### [P1-03] Xây dựng Chức năng Duyệt tin tức cho Manager (UC35 / Screen 63 / BR-45, BR-46, BR-48)
- **Vấn đề:** Staff tạo bài viết tin tức ở trạng thái `PENDING` nhưng Manager không có giao diện và API để phê duyệt (`PUBLISHED`) hoặc từ chối (`REJECTED`).
- **Tập tin cần bổ sung:**
  - Bổ sung endpoint trong `StaffNewsApiController` hoặc tạo `ManagerNewsController.java`.
  - Bổ sung các method `approveNews`, `rejectNews` trong `StaffNewsService.java`.
  - Cập nhật menu `fragments/manager-layout.html` và tạo giao diện duyệt tin bài.

---

### [P1-04] Bổ sung Tính năng Đổi mật khẩu cho Người dùng đã đăng nhập (UC10 / Screen 28 / BR-05)
- **Vấn đề:** Service `AuthService.changePassword` đã có nhưng chưa có Controller endpoint, chưa có Form DTO và chưa có Modal trên giao diện người dùng.
- **Tập tin cần bổ sung:**
  - Tạo DTO: `src/main/java/com/cinema/dto/auth/ChangePasswordForm.java` (oldPassword, newPassword, confirmPassword).
  - Thêm endpoint `POST /customer/profile/change-password` trong `CustomerProfileController.java` (hoặc `AuthController`).
  - Thêm Modal hoặc tab "Đổi mật khẩu" trong `customer/profile.html`.

---

### [P1-05] Nâng cấp Check-in Soát vé hỗ trợ Quét từng vé qua QR Code (UC21 / BR-26, BR-27, BR-36)
- **Vấn đề:** Check-in hiện chỉ nhận `bookingCode` và duyệt toàn bộ vé của booking một lúc, không hỗ trợ quét mã QR của từng chiếc vé (`Ticket.QRCode`) và thiếu camera scanner.
- **Tập tin bị ảnh hưởng:**
  - `src/main/java/com/cinema/service/StaffOperationsService.java`
  - `src/main/java/com/cinema/dao/StaffOperationsDAO.java`
  - `src/main/resources/templates/staff/check-in.html`
- **Giải pháp khuyến nghị:**
  1. Cho phép check-in bằng `QRCode` hoặc `BookingCode`.
  2. Tích hợp thư viện quét mã vạch bằng camera (HTML5 QR Code Scanner) trên giao diện `check-in.html`.

---

### [P1-06] Xây dựng Modal Xác minh Độ tuổi cho Nhân viên soát vé (UC22 / Screen 5.7 / BR-40)
- **Vấn đề:** Thiếu màn hình Modal để Staff đối chiếu ngày sinh khách với giới hạn độ tuổi phim (`P`, `K`, `T13`, `T16`, `T18`) trước khi cho vào rạp.
- **Tập tin bị ảnh hưởng:**
  - `src/main/resources/templates/staff/check-in.html`
  - Bổ sung hiển thị thông tin giới hạn tuổi của phim và nút xác nhận đã kiểm tra CCCD khách hàng.

---

## 3. MỨC ĐỘ P2 – MEDIUM (TRUNG BÌNH / CẢI TIẾN HỆ THỐNG)

### [P2-01] Xây dựng Background Job JOB-03: Tự động hết hạn Khuyến mãi & Voucher
- **Mục đích:** Quét các bản ghi `dbo.Promotion` và `dbo.Voucher` có `EndDate < SYSDATETIME()` hoặc `ValidTo < SYSDATETIME()` và chuyển `Status = 'EXPIRED'`.
- **Tạo mới:** `src/main/java/com/cinema/job/ExpirePromotionsAndVouchersJob.java`

### [P2-02] Xây dựng Background Job JOB-04: Dọn dẹp OTP và Token reset hết hạn
- **Mục đích:** Xóa hoặc đánh dấu vô hiệu hóa các mã OTP trong `dbo.OtpToken` đã hết hạn trên 24 giờ.
- **Tạo mới:** `src/main/java/com/cinema/job/CleanExpiredOtpTokensJob.java`

### [P2-03] Xây dựng Background Job JOB-06: Gửi Email Nhắc lịch chiếu trước 2 giờ
- **Mục đích:** Quét các đơn đặt vé đã `CONFIRMED` có giờ chiếu cách hiện tại 2 giờ và gửi email nhắc lịch cho khách hàng.
- **Tạo mới:** `src/main/java/com/cinema/job/BookingReminderNotificationJob.java`

### [P2-04] Bổ sung Validation custom kiểm tra `endDate >= releaseDate` trong MovieForm
- **Mục đích:** Ngăn chặn ngày kết thúc chiếu sớm hơn ngày phát hành ngay tại tầng DTO/Controller thay vì đợi DB ném ngoại lệ SQL Server.

### [P2-05] Hoàn thiện Trang Thông tin Hội viên & Tiến độ thăng hạng (UC18)
- **Mục đích:** Hiển thị thanh tiến độ điểm thăng hạng (`TierPoints`), các quyền lợi thẻ và danh sách voucher khả dụng theo hạng thành viên.

---

## 4. MỨC ĐỘ P3 – LOW (THẤP / GIAO DIỆN & HOÀN THIỆN TRẢI NGHIỆM)

### [P3-01] Tích hợp Tính năng In Vé & Hóa đơn thanh toán tại quầy (UC24 / Screen 5.5)
- **Mô tả:** Thêm nút "In Vé" và "In Hóa Đơn" trong trang `staff/booking-detail.html` sử dụng CSS `@media print` hoặc mở trang in phiếu nhiệt.

### [P3-02] Chuẩn hóa Mã thông báo Hệ thống song ngữ theo Bảng System Messages (MSG01 – MSG19)
- **Mô tả:** Đưa các thông báo lỗi và thông báo thành công vào file tài nguyên `messages.properties` và `messages_vi.properties`, đảm bảo đồng nhất ngôn ngữ trong toàn bộ ứng dụng.

### [P3-03] Bổ sung cảnh báo độ tuổi khách hàng khi đặt vé trực tuyến
- **Mô tả:** Trên giao diện chọn suất chiếu và chọn ghế, nếu phim có độ tuổi `T16`/`T18`, hiển thị hộp thoại cảnh báo: *"Phim dành cho khán giả từ 18 tuổi trở lên. Vui lòng mang CCCD khi đến rạp."*
