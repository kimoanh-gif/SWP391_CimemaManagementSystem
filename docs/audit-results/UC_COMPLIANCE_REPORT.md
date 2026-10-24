# BÁO CÁO ĐỐI SOÁT USE CASE SPECIFICATIONS VS SOURCE CODE
**Dự án:** Cinema Management System (SWP_RapPhim)  
**Tài liệu tham chiếu:** `docs/SRS_CinemaManagementSystem_SWP391_G5.docx.md`  
**Ngày kiểm toán:** 23/10/2026  
**Chuyên môn:** Senior Software Engineer / Business Analyst / QA Engineer  

---

## 1. TỔNG QUAN VÀ BẢNG THỐNG KÊ TRẠNG THÁI (39 USE CASES)

| Phân nhóm Actor | Tổng số UC | PASS | PARTIAL | NOT IMPLEMENTED | FAIL |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Guest / Public** (UC01 - UC04, UC08) | 5 | 5 | 0 | 0 | 0 |
| **Authentication** (UC05 - UC07, UC09 - UC10) | 5 | 4 | 1 | 0 | 0 |
| **Customer Functions** (UC11 - UC18) | 8 | 2 | 2 | 4 | 0 |
| **Staff Workspace** (UC19 - UC25) | 7 | 4 | 3 | 0 | 0 |
| **Manager Management** (UC26 - UC35) | 10 | 8 | 0 | 2 | 0 |
| **Admin Operations** (UC36 - UC39) | 4 | 4 | 0 | 0 | 0 |
| **TỔNG CỘNG** | **39** | **27 (69.2%)** | **6 (15.4%)** | **6 (15.4%)** | **0 (0.0%)** |

> **Quy chuẩn đánh giá trạng thái:**
> - **PASS:** Chức năng đã được triển khai đầy đủ từ UI, Controller, Service, DAO, Database; đáp ứng đúng Main Flow, Pre/Post-conditions và Business Rules cơ bản.
> - **PARTIAL:** Đã có code một phần (thường có Service/DAO hoặc UI cơ bản) nhưng thiếu Controller/UI kết nối, thiếu Alternative/Exception Flow quan trọng, hoặc vi phạm Business Rule nghiêm trọng.
> - **NOT IMPLEMENTED:** Hoàn toàn chưa có Controller, Service nghiệp vụ và View tương ứng trong source code thực tế.
> - **FAIL:** Chức năng chạy sai lệch hoàn toàn với yêu cầu hoặc gây lỗi hệ thống (Không có UC nào bị FAIL hoàn toàn).

---

## 2. MA TRẬN ĐỐI SOÁT CHI TIẾT TỪNG USE CASE (UC01 – UC39)

### 2.1. Phân nhóm Guest & Public (UC01 – UC04, UC08)

#### **UC01: View Movie List**
- **Actor:** All (Guest, Customer, Staff, Manager, Admin)
- **Mục tiêu:** Cho phép người dùng duyệt danh sách phim, tìm kiếm theo từ khóa, lọc theo thể loại (Genre), độ tuổi (AgeRating), trạng thái (Now Showing, Coming Soon) và sắp xếp.
- **Hiện trạng Code:**
  - Controller: [`MoviePublicController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/MoviePublicController.java#L56-L81) (`GET /movies`)
  - Service: [`MovieCatalogService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/MovieCatalogService.java#L44-L46)
  - DAO: [`MovieDao.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/MovieDao.java) (`searchMovies`)
  - View: `customer/movies/list.html`
- **Preconditions / Postconditions:** Đạt.
- **Flow Compliance:** Hỗ trợ tìm kiếm từ khóa `q`, lọc `genreId`, `type`, sắp xếp `sort` (newest, rating, title).
- **Kết luận:** **PASS**

#### **UC02: View Movie Information**
- **Actor:** All
- **Mục tiêu:** Xem chi tiết phim gồm tiêu đề, mô tả, thời lượng, thể loại, đạo diễn, diễn viên, giới hạn độ tuổi, poster, trailer, lịch chiếu khả dụng và đánh giá.
- **Hiện trạng Code:**
  - Controller: [`MoviePublicController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/MoviePublicController.java#L86-L130) (`GET /movies/{id}`)
  - View: `customer/movies/details.html`
- **Preconditions / Postconditions:** Đạt. Tự động kiểm tra movie tồn tại, nếu không tìm thấy redirect và báo lỗi.
- **Flow Compliance:** Đạt. Hiển thị danh sách ngày có suất chiếu, danh sách suất chiếu theo ngày, phim liên quan và danh sách reviews.
- **Kết luận:** **PASS**

#### **UC03: View Showtimes**
- **Actor:** All
- **Mục tiêu:** Xem lịch chiếu theo ngày (trong vòng 14 ngày tới), lọc theo phim, định dạng phòng chiếu.
- **Hiện trạng Code:**
  - Controller: [`ShowtimePublicController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/ShowtimePublicController.java) (`GET /showtimes`)
  - Service: [`PublicShowtimeService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/PublicShowtimeService.java)
  - DAO: [`PublicShowtimeDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/PublicShowtimeDAO.java)
  - View: `customer/showtimes/list.html`
- **Preconditions / Postconditions:** Đạt.
- **Flow Compliance:** Cho phép chọn ngày trong vòng 14 ngày, hiển thị suất chiếu theo từng phim, phòng chiếu và định dạng (2D/3D).
- **Kết luận:** **PASS**

#### **UC04: View Promotions & News**
- **Actor:** All
- **Mục tiêu:** Xem danh sách khuyến mãi đang kích hoạt (`ACTIVE`) và tin tức điện ảnh đã xuất bản (`PUBLISHED`).
- **Hiện trạng Code:**
  - Controller: [`PromotionNewsPublicController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/PromotionNewsPublicController.java#L41-L100) (`GET /promotions-news`, `GET /promotions/{id}`, `GET /news/{id}`)
  - Service: [`PromotionNewsService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/PromotionNewsService.java)
  - DAO: [`PromotionDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/PromotionDAO.java), [`NewsDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/NewsDAO.java)
  - View: `customer/promotions-news/list.html`, `promotion-details.html`, `news-details.html`
- **Business Rules:** Tuân thủ BR-47 (Chỉ tin tức trạng thái `PUBLISHED` mới hiển thị cho Guest/Customer).
- **Kết luận:** **PASS**

#### **UC08: Verify Email (OTP)**
- **Actor:** Guest, Email Service
- **Mục tiêu:** Xác minh email đăng ký qua mã OTP 6 chữ số trong vòng thời gian quy định trước khi kích hoạt tài khoản.
- **Hiện trạng Code:**
  - Controller: [`AuthController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AuthController.java#L119-L193) (`GET/POST /verify-activation-otp`, `POST /verify-activation-otp/resend`)
  - Service: [`OtpService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/OtpService.java), [`AuthService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/AuthService.java)
  - DAO: [`OtpTokenDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/OtpTokenDAO.java)
  - View: `auth/verify-activation-otp.html`
- **Business Rules:** Tuân thủ BR-04 (Tài khoản kích hoạt sau khi xác thực OTP thành công), có cooldown chống spam gửi lại OTP.
- **Kết luận:** **PASS**

---

### 2.2. Phân nhóm Authentication (UC05 – UC07, UC09 – UC10)

#### **UC05: Register Account**
- **Actor:** Guest
- **Mục tiêu:** Khách nhập Username, Email, Phone, FullName, Password, ConfirmPassword để tạo tài khoản mới ở trạng thái `PENDING_VERIFICATION`.
- **Hiện trạng Code:**
  - Controller: [`AuthController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AuthController.java#L61-L116) (`GET/POST /register`)
  - Form DTO: [`RegisterForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/auth/RegisterForm.java)
  - Service: [`AuthService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/AuthService.java#L100-L160)
  - View: `auth/register.html`
- **Business Rules:** Tuân thủ BR-01 (Unique username), BR-02 (Unique email), BR-03 (Unique phone number), BR-04 (Xác thực OTP).
- **Kết luận:** **PASS**

#### **UC06: Login**
- **Actor:** Customer, Staff, Manager, Admin
- **Mục tiêu:** Xác thực tài khoản bằng Email/Username và Password (hoặc Google OAuth2), phân quyền điều hướng về Dashboard theo Role (`CUSTOMER`, `STAFF`, `MANAGER`, `ADMIN`).
- **Hiện trạng Code:**
  - Controller: [`AuthController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AuthController.java#L196-L290) (`GET/POST /login`)
  - Form DTO: [`LoginForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/auth/LoginForm.java)
  - Service: [`AuthService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/AuthService.java#L180-L245)
  - View: `auth/login.html`
- **Business Rules:** Tuân thủ BR-06, GB-12 (Tài khoản bị khóa/inactive không thể đăng nhập), có lockout khi nhập sai mật khẩu quá 5 lần.
- **Kết luận:** **PASS**

#### **UC07: Logout**
- **Actor:** Customer, Staff, Manager, Admin
- **Mục tiêu:** Hủy HTTP Session hiện tại và chuyển hướng về trang Login.
- **Hiện trạng Code:**
  - Controller: [`AuthController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AuthController.java#L291-L295) (`POST /logout`)
  - Service: [`SessionService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/SessionService.java#L68-L75)
- **Kết luận:** **PASS**

#### **UC09: Forgot / Reset Password**
- **Actor:** Customer, Staff, Manager, Admin, Email Service
- **Mục tiêu:** Người dùng quên mật khẩu yêu cầu OTP phục hồi qua email, xác thực OTP và nhập mật khẩu mới.
- **Hiện trạng Code:**
  - Controller: [`AuthController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AuthController.java#L298-L470)
  - Form DTOs: [`ForgotPasswordForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/auth/ForgotPasswordForm.java), [`OtpForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/auth/OtpForm.java), [`ResetPasswordForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/auth/ResetPasswordForm.java)
  - Views: `auth/forgot-password.html`, `auth/verify-reset-otp.html`, `auth/reset-password.html`
- **Kết luận:** **PASS**

#### **UC10: Change Password**
- **Actor:** Customer, Staff, Manager, Admin
- **Mục tiêu:** Người dùng đã đăng nhập đổi mật khẩu bằng cách nhập Current Password, New Password và Confirm New Password.
- **Hiện trạng Code:**
  - Service: Đã có method [`AuthService.changePassword(int accountId, String oldPassword, String newPassword)`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/AuthService.java#L337-L344) kiểm tra BR-05 (Current password verification) và mã hóa BCrypt.
  - **Thiếu sót:** 
    - Không có Controller endpoint tiếp nhận (`@PostMapping("/customer/profile/change-password")` hoặc `@PostMapping("/change-password")`).
    - Không có Form DTO (`ChangePasswordForm`).
    - Không có giao diện / modal trong `customer/profile.html` hay các trang dashboard để người dùng thực hiện thao tác này.
- **Kết luận:** **PARTIAL** (Đã có logic nghiệp vụ tầng Service, thiếu Controller, Form DTO và View).

---

### 2.3. Phân nhóm Customer Functions (UC11 – UC18)

#### **UC11: Book Tickets Online**
- **Actor:** Customer
- **Mục tiêu:** Chọn suất chiếu -> Chọn ghế khả dụng (tối đa 8-10 ghế, giữ ghế 10 phút - `HOLD_MINUTES`) -> Chọn Combo F&B -> Nhập Voucher -> Xem tóm tắt đặt vé.
- **Hiện trạng Code:**
  - Controller: [`CustomerBookingController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/CustomerBookingController.java) (`/customer/booking`, `/seats`, `/foods`, `/promotions`, `/checkout`)
  - Service: [`CustomerBookingService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/CustomerBookingService.java)
  - DAO: [`CustomerBookingDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/CustomerBookingDAO.java)
  - Background Job: [`ReleaseExpiredSeatReservationsJob.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/job/ReleaseExpiredSeatReservationsJob.java) (`JOB-01`)
  - Views: `customer/booking/showtimes.html`, `seats.html`, `foods.html`, `promotions.html`, `checkout.html`
- **Lỗ hổng nghiệp vụ:**
  - Voucher Code chỉ được lưu tạm vào HTTP Session (`session.setAttribute("customerBookingVoucherCode", ...)`), hoàn toàn **không được validate** đối chiếu với database (`dbo.Voucher`), không tính giảm giá, không kiểm tra điều kiện `MinBookingAmount`, `RequiredRankID`, `TotalUsageLimit` (Vi phạm BR-16 đến BR-21, BR-38).
- **Kết luận:** **PARTIAL** (Luồng chọn ghế và bắp nước hoạt động tốt, nhưng luồng áp dụng Voucher chưa xử lý nghiệp vụ).

#### **UC12: Make Payment**
- **Actor:** Customer, Payment Gateway (VNPay)
- **Mục tiêu:** Thanh toán đơn đặt vé qua cổng thanh toán VNPay, xác nhận thanh toán thành công chuyển trạng thái Booking thành `CONFIRMED`/`PAID`.
- **Hiện trạng Code:**
  - Controller: [`CustomerBookingController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/CustomerBookingController.java#L233-L239):
    ```java
    @PostMapping("/checkout")
    public String submitCheckout(...) {
        redirectAttributes.addFlashAttribute("errorMessage",
            "Online payment is not connected yet. Your seats remain held until the timer expires.");
        return "redirect:/customer/booking/checkout";
    }
    ```
  - Hoàn toàn chưa tích hợp `API-01` (Create Payment Request) và `API-02` (VNPay Webhook / Return URL).
- **Kết luận:** **NOT IMPLEMENTED** (Hiện đang là thông báo tạm "Online payment is not connected yet").

#### **UC13: View Digital Ticket**
- **Actor:** Customer
- **Mục tiêu:** Sau khi thanh toán thành công, khách hàng xem vé điện tử chứa mã QRCode duy nhất, thông tin phim, suất chiếu, phòng và số ghế.
- **Hiện trạng Code:**
  - Không có Controller endpoint nào cho Customer xem vé điện tử (ví dụ: `/customer/tickets/{ticketId}` hoặc `/customer/bookings/{id}/ticket`).
  - Không có file template HTML nào hiển thị vé điện tử kèm QR code cho Customer.
- **Kết luận:** **NOT IMPLEMENTED**

#### **UC14: View Booking & Transaction History**
- **Actor:** Customer
- **Mục tiêu:** Khách hàng xem danh sách các đơn đặt vé trong quá khứ kèm chi tiết thanh toán, số tiền, ngày giờ đặt vé và trạng thái (BR-29, BR-37).
- **Hiện trạng Code:**
  - Không có Controller endpoint `/customer/history` hoặc `/customer/bookings`.
  - Không có view template lịch sử giao dịch dành cho Customer. (Chỉ Staff mới có trang `/staff/bookings`).
- **Kết luận:** **NOT IMPLEMENTED**

#### **UC15: View Profile**
- **Actor:** Customer
- **Mục tiêu:** Khách hàng xem thông tin cá nhân (Họ tên, email, SĐT, ngày sinh, địa chỉ, hạng thành viên).
- **Hiện trạng Code:**
  - Controller: [`CustomerProfileController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/CustomerProfileController.java#L35-L41) (`GET /customer/profile`)
  - View: `customer/profile.html`
- **Kết luận:** **PASS**

#### **UC16: Update Profile**
- **Actor:** Customer
- **Mục tiêu:** Khách hàng cập nhật họ tên, số điện thoại, ngày sinh, địa chỉ. Kiểm tra trùng lặp số điện thoại (BR-03).
- **Hiện trạng Code:**
  - Controller: [`CustomerProfileController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/CustomerProfileController.java#L43-L90) (`GET/POST /customer/profile/edit`)
  - Form DTO: [`ProfileUpdateForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/ProfileUpdateForm.java)
  - View: `customer/profile-edit.html`
- **Kết luận:** **PASS**

#### **UC17: Submit Movie Review**
- **Actor:** Customer
- **Mục tiêu:** Khách hàng gửi điểm đánh giá (1-5 sao) và nhận xét cho bộ phim.
- **Hiện trạng Code:**
  - Controller: [`MoviePublicController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/MoviePublicController.java#L135-L156) (`POST /movies/{id}/reviews`)
  - Service: [`MovieCatalogService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/MovieCatalogService.java#L66-L75)
- **Lỗ hổng nghiệp vụ nghiêm trọng:**
  - **Vi phạm BR-30 và GB-09:** Quy tắc yêu cầu *"A Customer may submit a MovieReview only for a Movie associated with at least one successfully paid Booking by that Customer."* Nhưng trong code hiện tại, bất kỳ tài khoản Customer nào cũng có thể gửi đánh giá cho bất kỳ phim nào mà không hề kiểm tra lịch sử đã từng mua vé xem phim đó hay chưa!
- **Kết luận:** **PARTIAL** (Giao diện và API gửi review hoạt động, nhưng bỏ qua Business Rule kiểm tra điều kiện mua vé).

#### **UC18: View Loyalty Information**
- **Actor:** Customer
- **Mục tiêu:** Xem AvailablePoints, TierPoints, cấp bậc MembershipRank, tiến độ nâng hạng và các voucher ưu đãi dành riêng theo hạng.
- **Hiện trạng Code:**
  - Trong `customer/profile.html` chỉ hiển thị 2 con số thô: `Available Points` và `Tier Points`.
  - Chưa có trang/mục riêng hiển thị thanh tiến độ thăng hạng (rank progress bar), ngưỡng điểm cần tích lũy để lên Silver/Gold/Diamond, và danh sách voucher/quyền lợi theo hạng thành viên (BR-32, BR-33).
- **Kết luận:** **PARTIAL**

---

### 2.4. Phân nhóm Staff Workspace (UC19 – UC25)

#### **UC19: Counter Ticket Sales**
- **Actor:** Staff, Payment Gateway
- **Mục tiêu:** Nhân viên bán vé tại quầy: chọn phim, chọn suất chiếu, chọn ghế, chọn bắp nước, tra cứu khách hàng thành viên, thu tiền mặt hoặc thanh toán thẻ/QR.
- **Hiện trạng Code:**
  - Controller: [`StaffWorkspaceController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/StaffWorkspaceController.java#L61-L173) (`/staff/sales`, `/seats`, `/foods`)
  - Service: [`StaffCounterSalesService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/StaffCounterSalesService.java)
  - Views: `staff/sales-showtimes.html`, `sales-seats.html`, `sales-foods.html`
- **Kết luận:** **PASS**

#### **UC20: Member Lookup**
- **Actor:** Staff
- **Mục tiêu:** Nhân viên tìm kiếm khách hàng thành viên theo số điện thoại hoặc email để tích điểm và áp dụng quyền lợi hội viên khi bán vé tại quầy.
- **Hiện trạng Code:**
  - Controller: [`StaffMemberLookupController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/StaffMemberLookupController.java) (`GET /staff/members/lookup?q=...`)
  - Service: [`MemberLookupService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/MemberLookupService.java)
  - DAO: [`MemberLookupDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/MemberLookupDAO.java)
- **Kết luận:** **PASS**

#### **UC21: Verify Ticket & Check-in**
- **Actor:** Staff
- **Mục tiêu:** Nhân viên quét mã QR vé hoặc nhập mã vé, kiểm tra tính hợp lệ và đổi trạng thái vé sang `CHECKED_IN` / `USED`.
- **Hiện trạng Code:**
  - Controller: [`StaffWorkspaceController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/StaffWorkspaceController.java#L210-L230) (`GET/POST /staff/check-in`)
  - Service: [`StaffOperationsService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/StaffOperationsService.java#L60-L76)
  - DAO: [`StaffOperationsDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/StaffOperationsDAO.java#L184-L198)
  - View: `staff/check-in.html`
- **Lỗ hổng so với SRS:**
  - Trong SRS, hệ thống cho phép quét mã QR của **từng chiếc vé** (`Ticket.QRCode`). Code hiện tại chỉ nhận `BookingCode` và check-in gộp toàn bộ vé của booking đó.
  - Chưa tích hợp camera QR scanner trên giao diện web.
- **Kết luận:** **PARTIAL**

#### **UC22: Verify Age Eligibility**
- **Actor:** Staff
- **Mục tiêu:** Màn hình Modal xác minh độ tuổi (Screen 5.7). Đối chiếu ngày sinh/CCCD của khách với phân loại độ tuổi của phim (`P`, `K`, `T13`, `T16`, `T18`) trước khi cho vào rạp (BR-40).
- **Hiện trạng Code:**
  - Hoàn toàn **chưa có** Age Eligibility Modal hoặc bước kiểm tra độ tuổi trên giao diện bán vé / soát vé của Staff.
- **Kết luận:** **PARTIAL** (Độ tuổi được lưu trên Movie và Customer profile, nhưng thiếu UI Modal kiểm tra xác thực của Staff theo Use Case).

#### **UC23: View Bookings List & Details**
- **Actor:** Staff
- **Mục tiêu:** Tra cứu, lọc danh sách đơn đặt vé theo mã booking, số điện thoại, trạng thái và xem chi tiết đơn vé (ghế, bắp nước, giá tiền).
- **Hiện trạng Code:**
  - Controller: [`StaffWorkspaceController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/StaffWorkspaceController.java#L185-L209) (`/staff/bookings`, `/staff/bookings/{id}`)
  - Views: `staff/bookings.html`, `staff/booking-detail.html`
- **Kết luận:** **PASS**

#### **UC24: Print Tickets & Receipts**
- **Actor:** Staff
- **Mục tiêu:** In vé giấy vật lý và hóa đơn thanh toán sau khi hoàn tất bán vé tại quầy (Screen 5.5).
- **Hiện trạng Code:**
  - Trong `staff/booking-detail.html` không có nút in (Print) hoặc trigger `window.print()` / xuất mẫu in vé vật lý.
- **Kết luận:** **PARTIAL** (Dữ liệu chi tiết vé có đầy đủ, nhưng thiếu tính năng in ấn trên giao diện).

#### **UC25: Manage News Submission**
- **Actor:** Staff, Manager
- **Mục tiêu:** Staff soạn thảo, chỉnh sửa bài viết tin tức ở trạng thái `DRAFT` và gửi duyệt `PENDING` cho Manager.
- **Hiện trạng Code:**
  - Controller: [`StaffWorkspaceController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/StaffWorkspaceController.java#L50-L59), [`StaffNewsApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/StaffNewsApiController.java)
  - DTO: [`NewsRequest.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/NewsRequest.java) (Chỉ cho phép `DRAFT`, `PENDING`)
  - View: `staff/news.html`
- **Kết luận:** **PASS** (Phía Staff đã hoàn thành đúng quy trình nộp bài).

---

### 2.5. Phân nhóm Manager Management (UC26 – UC35)

#### **UC26: Movie Management**
- **Actor:** Manager
- **Mục tiêu:** Thêm, xem, sửa, đổi trạng thái phim, gán thể loại (MovieGenre) (BR-44).
- **Hiện trạng Code:**
  - Controllers: [`MovieController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/MovieController.java), [`MovieApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/MovieApiController.java)
  - Form DTO: [`MovieForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/MovieForm.java)
  - View: `manager/movies.html`
- **Kết luận:** **PASS**

#### **UC27: Cinema Room Management**
- **Actor:** Manager
- **Mục tiêu:** Quản lý phòng chiếu, sức chứa, trạng thái phòng (BR-52: Không được vô hiệu hóa phòng nếu có suất chiếu sắp diễn ra).
- **Hiện trạng Code:**
  - Controllers: [`RoomController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/RoomController.java), [`RoomApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/RoomApiController.java)
  - Form DTO: [`RoomForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/RoomForm.java)
  - Service: [`RoomService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/RoomService.java) (Chặn deactive phòng có suất chiếu)
  - View: `manager/rooms.html`
- **Kết luận:** **PASS**

#### **UC28: Seat Type Management**
- **Actor:** Manager
- **Mục tiêu:** Quản lý loại ghế (Standard, VIP, Sweetbox), hệ số giá (`PriceMultiplier`), mô tả. Không xóa cứng nếu đã có lịch sử đặt vé (BR-53).
- **Hiện trạng Code:**
  - Controllers: [`SeatTypeController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java), [`SeatTypeApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/SeatTypeApiController.java)
  - Form DTO: [`SeatTypeForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/SeatTypeForm.java)
  - View: `manager/seat-types.html`
- **Kết luận:** **PASS**

#### **UC29: Seat Layout Management**
- **Actor:** Manager
- **Mục tiêu:** Cấu hình sơ đồ ghế trong phòng, gán loại ghế, tọa độ hàng/cột, cập nhật vi sai (diff-based) không làm đổi `SeatID` cũ (BR-39, BR-53).
- **Hiện trạng Code:**
  - Controllers: [`SeatController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatController.java), [`SeatApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/SeatApiController.java)
  - Service: [`SeatService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/SeatService.java)
  - View: `manager/seats.html`
- **Kết luận:** **PASS**

#### **UC30: Showtime Management**
- **Actor:** Manager
- **Mục tiêu:** Tạo, sửa, hủy lịch chiếu. Kiểm tra phòng hoạt động, không trùng giờ chiếu cùng phòng (BR-07, BR-08, BR-09, BR-54, BR-55).
- **Hiện trạng Code:**
  - Controllers: [`ShowtimeController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/ShowtimeController.java), [`ShowtimeApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/ShowtimeApiController.java)
  - Form DTO: [`ShowtimeForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/ShowtimeForm.java)
  - Service: [`ShowtimeService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/ShowtimeService.java) (Kèm buffer dọn phòng 15 phút)
  - View: `manager/showtimes.html`
- **Kết luận:** **PASS**

#### **UC31: Ticket Price Management**
- **Actor:** Manager
- **Mục tiêu:** Thiết lập quy tắc giá vé cơ sở theo ngày (Weekday/Weekend), khung giờ và hệ số loại ghế, làm tròn đến 1.000 VND (BR-50, BR-51).
- **Hiện trạng Code:**
  - Controllers: [`TicketPricingController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/TicketPricingController.java), [`TicketPricingApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/TicketPricingApiController.java)
  - Form DTO: [`TicketPricingForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/TicketPricingForm.java)
  - Service: [`TicketPricingService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java)
  - View: `manager/pricing.html`
- **Kết luận:** **PASS**

#### **UC32: Food & Beverage Management**
- **Actor:** Manager
- **Mục tiêu:** Quản lý món lẻ, combo bắp nước, giá bán, giá vốn, kích hoạt/hủy kích hoạt.
- **Hiện trạng Code:**
  - Controllers: [`FBItemController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/FBItemController.java), [`FBItemApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/FBItemApiController.java)
  - Form DTO: [`FBItemForm.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/FBItemForm.java)
  - View: `manager/fb.html`
- **Kết luận:** **PASS**

#### **UC33: Promotion & Voucher Management**
- **Actor:** Manager
- **Mục tiêu:** Tạo, cập nhật, kích hoạt/hủy kích hoạt chương trình khuyến mãi và mã voucher; thiết lập `ValidFrom`, `ValidTo`, `MinBookingAmount`, `RequiredRankID`, `TotalUsageLimit`, `PerAccountLimit` (BR-17 đến BR-21, BR-38).
- **Hiện trạng Code:**
  - Tầng DAO: [`PromotionDAO.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/PromotionDAO.java) chỉ có các hàm `SELECT` phục vụ phía khách hàng xem. **Không có bất kỳ hàm INSERT / UPDATE / DELETE nào** cho Promotion hay Voucher.
  - Tầng Service: Không có Service quản lý Promotion/Voucher cho Manager.
  - Tầng Controller: Không có `PromotionController` hoặc `PromotionApiController`.
  - Tầng View: Không có template `manager/promotions.html` hay menu quản lý voucher.
- **Kết luận:** **NOT IMPLEMENTED** (Chức năng quản lý voucher của Manager chưa được code).

#### **UC34: View Reports & Statistics**
- **Actor:** Manager
- **Mục tiêu:** Xem báo cáo doanh thu, số lượng vé bán, doanh thu theo phim, theo ngày/tuần/tháng/năm, tỷ lệ lấp đầy phòng chiếu.
- **Hiện trạng Code:**
  - Controllers: [`RevenueReportController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/RevenueReportController.java), [`RevenueReportApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/RevenueReportApiController.java)
  - Service: [`RevenueReportService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/RevenueReportService.java)
  - View: `manager/reports.html`
- **Kết luận:** **PASS**

#### **UC35: Review & Approve News**
- **Actor:** Manager
- **Mục tiêu:** Manager duyệt bài viết tin tức do Staff gửi lên (`PENDING`), chọn Phê duyệt (`PUBLISHED`), Từ chối (`REJECTED`) hoặc Lưu trữ (`ARCHIVED`) (BR-45, BR-46, BR-48).
- **Hiện trạng Code:**
  - Staff đã có API nộp bài với trạng thái `PENDING`.
  - Tuy nhiên, Manager **hoàn toàn không có giao diện, Controller hay API** để thực hiện hành động Approve / Reject. Không có menu tin tức trong layout Manager (`manager-layout.html`).
- **Kết luận:** **NOT IMPLEMENTED**

---

### 2.6. Phân nhóm Admin Operations (UC36 – UC39)

#### **UC36: Manage Customer Accounts**
- **Actor:** Admin
- **Mục tiêu:** Quản lý tài khoản khách hàng: tìm kiếm, xem chi tiết, khóa (SUSPENDED), mở khóa (ACTIVE), kích hoạt thủ công.
- **Hiện trạng Code:**
  - Controller: [`AdminController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AdminController.java#L37-L47), [`CustomerAccountApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/CustomerAccountApiController.java)
  - Service: [`CustomerAccountService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/CustomerAccountService.java)
  - View: `admin/customers.html`
- **Kết luận:** **PASS**

#### **UC37: Manage Staff & Manager Accounts**
- **Actor:** Admin
- **Mục tiêu:** Tạo mới, chỉnh sửa thông tin, phân vai trò (`STAFF`, `MANAGER`), khóa / mở khóa tài khoản nội bộ.
- **Hiện trạng Code:**
  - Controller: [`AdminController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AdminController.java#L49-L53), [`StaffAccountApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/StaffAccountApiController.java)
  - Service: [`StaffAccountService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/StaffAccountService.java)
  - View: `admin/staffs.html`
- **Kết luận:** **PASS**

#### **UC38: Manage Roles and Permissions**
- **Actor:** Admin
- **Mục tiêu:** Xem danh sách vai trò, danh mục quyền hạn, gán hoặc thu hồi quyền cho từng vai trò trong hệ thống (GB-11).
- **Hiện trạng Code:**
  - Controller: [`AdminController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AdminController.java#L55-L59), [`RolePermissionApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/RolePermissionApiController.java)
  - Service: [`RolePermissionService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/RolePermissionService.java)
  - View: `admin/roles.html`
- **Kết luận:** **PASS**

#### **UC39: Manage System Configuration**
- **Actor:** Admin
- **Mục tiêu:** Cấu hình tham số hệ thống: thời gian giữ ghế (`HOLD_MINUTES`), ngưỡng thăng hạng thành viên, tỷ lệ tích điểm, quy định voucher (BR-49).
- **Hiện trạng Code:**
  - Controller: [`AdminController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/AdminController.java#L61-L65), [`SystemConfigApiController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SystemConfigApiController.java)
  - Service: [`SystemConfigService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/SystemConfigService.java)
  - View: `admin/config.html`
- **Kết luận:** **PASS**

---

## 3. ĐỐI SOÁT CÁC HẠNG MỤC PHỤ TRỢ (BACKGROUND JOBS & EXTERNAL APIS)

### 3.1. Background Jobs (JOB-01 đến JOB-06)
1. **JOB-01: Release Expired Seat Reservations:** **PASS** (Được triển khai chuẩn tại [`ReleaseExpiredSeatReservationsJob.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/job/ReleaseExpiredSeatReservationsJob.java) chạy định kỳ giải phóng ghế giữ quá hạn 10 phút).
2. **JOB-02: Cancel Expired Unpaid Bookings:** **PASS** (Được xử lý đồng thời trong logic giải phóng ghế của `CustomerBookingDAO.releaseExpiredSeatReservations()`).
3. **JOB-03: Update Expired Promotions and Vouchers:** **NOT IMPLEMENTED** (Không có scheduled job quét ngày `EndDate`/`ValidTo` để đổi trạng thái sang `EXPIRED`).
4. **JOB-04: Clean Up Expired Password Reset Tokens:** **NOT IMPLEMENTED** (Không có scheduled job định kỳ xóa OTP/Token hết hạn trong bảng `dbo.OtpToken`).
5. **JOB-05: Clean Up Old Temporary Booking Data:** **NOT IMPLEMENTED** (Chưa có job dọn dẹp dữ liệu rác tạm).
6. **JOB-06: Booking Reminder Notification:** **NOT IMPLEMENTED** (Chưa có job quét đơn vé sắp đến giờ chiếu để gửi email nhắc nhở).

### 3.2. External APIs (API-01 đến API-03)
1. **API-01: Payment Gateway API (`POST /api/v1/payments/create`):** **NOT IMPLEMENTED** (Chưa có endpoint tạo URL thanh toán VNPay).
2. **API-02: VNPay Webhook (`POST /api/v1/payments/webhook/vnpay`):** **NOT IMPLEMENTED** (Chưa có webhook xử lý IPN từ VNPay).
3. **API-03: Email Service API (`POST /api/v1/notifications/email`):** **PARTIAL** (Hệ thống gửi email trực tiếp qua JavaMailSender trong nội bộ server, không mở cổng REST API riêng).
