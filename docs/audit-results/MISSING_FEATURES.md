# BÁO CÁO CÁC CHỨC NĂNG VÀ THÀNH PHẦN CÒN THIẾU (MISSING FEATURES REPORT)
**Dự án:** Cinema Management System (SWP_RapPhim)  
**Tài liệu tham chiếu:** `docs/SRS_CinemaManagementSystem_SWP391_G5.docx.md`  
**Ngày kiểm toán:** 23/10/2026  

---

## 1. DANH SÁCH USE CASES CHƯA TRIỂN KHAI (NOT IMPLEMENTED)

Dưới đây là các Use Case có trong tài liệu SRS nhưng **hoàn toàn chưa được xây dựng** trong source code:

### 1. UC12: Make Payment (Online Payment Integration)
- **Mô tả tài liệu:** Khách hàng thanh toán đơn đặt vé trực tuyến thông qua cổng thanh toán VNPay (API-01, API-02). Khi thanh toán thành công, hệ thống cập nhật trạng thái đơn vé sang `CONFIRMED`/`PAID`, ghi nhận giao dịch `dbo.Payment`, sinh bản ghi `dbo.Ticket` với mã QR code và cộng điểm tích lũy thành viên (BR-22, BR-23, BR-24, BR-31).
- **Hiện trạng code:** 
  - Tại [`CustomerBookingController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/CustomerBookingController.java#L233-L239), endpoint `@PostMapping("/checkout")` chỉ trả về thông báo lỗi dạng placeholder:
    ```
    "Online payment is not connected yet. Your seats remain held until the timer expires."
    ```
  - Chưa có Service thanh toán VNPay (tạo URL redirect, checksum HMAC-SHA512, xử lý IPN Webhook).

---

### 2. UC13: View Digital Ticket (Customer View)
- **Mô tả tài liệu:** Sau khi thanh toán thành công, khách hàng có thể xem vé điện tử chứa:
  - Mã vé và mã QRCode duy nhất (BR-26).
  - Tên phim, thời lượng, độ tuổi.
  - Phòng chiếu, ngày giờ chiếu, danh sách số ghế, loại ghế.
  - Bắp nước đã mua kèm.
  - Trạng thái vé (`VALID`, `CHECKED_IN`, `EXPIRED`).
- **Hiện trạng code:**
  - Không có Controller mapping (ví dụ: `/customer/tickets` hoặc `/customer/tickets/{id}`).
  - Không có view template HTML hiển thị vé điện tử kèm QR code cho Customer.

---

### 3. UC14: View Booking & Transaction History (Customer View)
- **Mô tả tài liệu:** Khách hàng đã đăng nhập có thể xem lịch sử toàn bộ các đơn đặt vé của chính mình (BR-29, BR-37), xem chi tiết số tiền, phương thức thanh toán, thời gian giao dịch và trạng thái đơn hàng.
- **Hiện trạng code:**
  - Customer hoàn toàn không có trang lịch sử giao dịch (Staff có trang `/staff/bookings`, nhưng Customer thì không có).
  - Thiếu Controller endpoint `/customer/history` hoặc `/customer/bookings`.
  - Thiếu template HTML danh sách và chi tiết lịch sử đơn hàng của Customer.

---

### 4. UC33: Promotion & Voucher Management (Manager View & CRUD)
- **Mô tả tài liệu:** Cho phép Manager tạo mới, cập nhật thông tin, kích hoạt, vô hiệu hóa các chương trình khuyến mãi (`Promotion`) và mã giảm giá (`Voucher`). Cấu hình:
  - Mã code, loại giảm giá (phần trăm % hoặc số tiền cố định), giá trị giảm tối đa.
  - Giá trị đơn hàng tối thiểu (`MinBookingAmount`).
  - Hạn sử dụng (`ValidFrom`, `ValidTo`).
  - Giới hạn lượt dùng tổng thể (`TotalUsageLimit`) và giới hạn lượt dùng mỗi tài khoản (`PerAccountLimit`).
  - Điều kiện cấp bậc thành viên (`RequiredRankID`).
- **Hiện trạng code:**
  - [`PromotionDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/PromotionDAO.java) chỉ có các câu lệnh `SELECT` để đọc dữ liệu công khai. Không có bất kỳ hàm `INSERT`, `UPDATE`, hay `DELETE` nào.
  - Không có `PromotionController`, `PromotionApiController`, `PromotionService` phục vụ Manager.
  - Không có giao diện `manager/promotions.html` hay menu quản lý voucher.

---

### 5. UC35: Review & Approve News (Manager Approval Workflow)
- **Mô tả tài liệu:** Manager xem danh sách bài viết tin tức do Staff gửi lên (`PENDING`), kiểm tra nội dung, thực hiện Phê duyệt (`PUBLISHED`) hoặc Từ chối kèm lý do (`REJECTED`) hoặc Lưu trữ (`ARCHIVED`) (BR-45, BR-46, BR-48).
- **Hiện trạng code:**
  - Staff đã có giao diện nộp bài viết với trạng thái `PENDING`.
  - Tuy nhiên, Manager hoàn toàn không có giao diện, không có Controller endpoint, và Service không có hàm xử lý duyệt / từ chối tin tức.

---

## 2. DANH SÁCH MÀN HÌNH VÀ MODAL CÒN THIẾU (SCREEN INVENTORY GAPS)

Đối chiếu với **Mục 5.1 Screen Inventory** trong tài liệu SRS:

| Mã màn hình | Tên màn hình theo SRS | Phân quyền | Hiện trạng trong Code |
| :--- | :--- | :--- | :--- |
| **Screen 5.5** | **Ticket & Receipt Printing** | Staff | **THIẾU GIAO DIỆN IN**. Trang `staff/booking-detail.html` hiển thị chi tiết nhưng không có nút in (Print) hoặc trigger `window.print()` / phiếu in nhiệt. |
| **Screen 5.7** | **Age Eligibility Verification Modal** | Staff | **HOÀN TOÀN THIẾU**. Không có Modal kiểm tra CCCD/ngày sinh đối chiếu giới hạn tuổi phim trước khi soát vé hoặc bán vé (BR-40). |
| **Screen 24** | **Digital Ticket Display** | Customer | **HOÀN TOÀN THIẾU**. Không có màn hình xem vé điện tử và hiển thị QR code sau khi đặt vé online thành công. |
| **Screen 26** | **Booking History & Order Details** | Customer | **HOÀN TOÀN THIẾU**. Không có màn hình lịch sử đặt vé cho khách hàng. |
| **Screen 28** | **Change Password Modal / Page** | All Authenticated | **THIẾU GIAO DIỆN**. Service có hàm `changePassword` nhưng không có màn hình hoặc modal cho người dùng tự đổi mật khẩu. |
| **Screen 62** | **Promotion & Voucher Management** | Manager | **HOÀN TOÀN THIẾU**. Không có màn hình danh sách, tạo mới, chỉnh sửa voucher. |
| **Screen 63** | **News Approval Management** | Manager | **HOÀN TOÀN THIẾU**. Không có màn hình duyệt tin tức cho Manager. |

---

## 3. DANH SÁCH BACKGROUND JOBS CHƯA TRIỂN KHAI (BACKGROUND JOB GAPS)

Trong tài liệu **Mục 6. Background Job Inventory**, hệ thống quy định 6 Background Jobs định kỳ. Hiện tại trong package `com.cinema.job` mới chỉ có duy nhất [`ReleaseExpiredSeatReservationsJob.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/job/ReleaseExpiredSeatReservationsJob.java):

| Mã Job | Tên Job | Tần suất quy định | Hiện trạng | Tác động nếu thiếu |
| :--- | :--- | :--- | :--- | :--- |
| **JOB-01** | Release Expired Seat Reservations | Định kỳ (30s - 1 phút) | **ĐÃ TRIỂN KHAI** | Hoạt động tốt, giải phóng ghế giữ quá 10 phút. |
| **JOB-02** | Cancel Expired Unpaid Bookings | Định kỳ (30s - 1 phút) | **ĐÃ TÍCH HỢP** | Được gộp chung trong hàm giải phóng ghế của JOB-01. |
| **JOB-03** | Update Expired Promotions and Vouchers | Hàng ngày hoặc định kỳ | **CHƯA CÓ** | Khuyến mãi và voucher đã hết hạn (`EndDate`/`ValidTo < now`) không tự động đổi sang `EXPIRED`. |
| **JOB-04** | Clean Up Expired Password Reset Tokens | Định kỳ (mỗi giờ) | **CHƯA CÓ** | Bảng `dbo.OtpToken` và token reset password lưu dồn vô hạn mà không được dọn dẹp định kỳ. |
| **JOB-05** | Clean Up Old Temporary Booking Data | Hàng ngày (lúc nửa đêm) | **CHƯA CÓ** | Các bản ghi đơn hàng tạm rác, ghế giữ hết hạn lâu ngày không được lưu trữ hoặc xóa sạch. |
| **JOB-06** | Booking Reminder Notification | Định kỳ (mỗi 15-30 phút) | **CHƯA CÓ** | Khách hàng đã đặt vé không nhận được email nhắc nhở trước giờ chiếu 2 tiếng theo SRS 6.6. |

---

## 4. DANH SÁCH EXTERNAL APIS CHƯA KẾT NỐI (EXTERNAL API GAPS)

Theo **Mục 7. External API Inventory**:

1. **API-01: Payment Gateway API (`POST /api/v1/payments/create`)**:
   - **Mục đích:** Tạo yêu cầu thanh toán trực tuyến và sinh URL chuyển hướng khách sang VNPay.
   - **Hiện trạng:** Chưa xây dựng controller, service và helper tính toán checksum bảo mật HMAC-SHA512.
2. **API-02: VNPay Webhook (`POST /api/v1/payments/webhook/vnpay`)**:
   - **Mục đích:** Tiếp nhận thông báo kết quả giao dịch ngầm (IPN) từ VNPay để cập nhật trạng thái đơn hàng sang `CONFIRMED`.
   - **Hiện trạng:** Chưa có endpoint tiếp nhận và thẩm định chữ ký số từ cổng thanh toán.

---

## 5. CÁC QUY TẮC NGHIỆP VỤ BỊ THIẾU HOẶC CHƯA BẢO ĐẢM TRONG CODE (BUSINESS RULE GAPS)

1. **BR-16 đến BR-21, BR-38 (Áp dụng Voucher):**
   - Trong luồng đặt vé trực tuyến, mã voucher chỉ lưu vào session, hoàn toàn không tính toán giảm trừ tiền vào tổng hóa đơn (`TotalAmount = Subtotal + FBTotal - DiscountAmount`), không kiểm tra hạn dùng, điều kiện hạng thẻ hay giới hạn số lần dùng.
2. **BR-30 & GB-09 (Điều kiện viết Review phim):**
   - Không kiểm tra khách hàng đã từng mua vé xem phim đó hay chưa trước khi cho phép lưu nhận xét.
3. **BR-26 & BR-36 (Kiểm soát tính duy nhất và một lần dùng của Vé):**
   - Việc soát vé check-in hiện chỉ dựa trên `BookingCode` gộp chung, chưa xử lý kiểm tra và cập nhật trạng thái độc lập từng vé qua mã QR `Ticket.QRCode`.
4. **BR-40 (Kiểm tra điều kiện độ tuổi):**
   - Chưa tích hợp kiểm tra chéo độ tuổi của khách với phân loại phim trên giao diện Staff soát vé và bán vé.
