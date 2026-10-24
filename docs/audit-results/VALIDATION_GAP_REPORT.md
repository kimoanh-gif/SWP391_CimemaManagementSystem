# BÁO CÁO KHOẢNG TRỐNG VALIDATION (VALIDATION GAP REPORT)
**Dự án:** Cinema Management System (SWP_RapPhim)  
**Tài liệu tham chiếu:** `docs/SRS_CinemaManagementSystem_SWP391_G5.docx.md`  
**Ngày kiểm toán:** 23/10/2026  

---

## 1. TỔNG QUAN VỀ KIẾN TRÚC VALIDATION TRONG DỰ ÁN

Hệ thống được thiết kế theo mô hình 3 lớp phòng vệ validation:
1. **Frontend Validation:** Thuộc tính HTML5 (`required`, `min`, `max`, `maxlength`, `pattern`) và kiểm tra bằng JavaScript tại trình duyệt.
2. **Backend Form/DTO Validation:** Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Max`, `@Past`, `@Email`) kết hợp `@Valid` / BindingResult trong Controller.
3. **Business Validation (Service layer):** Kiểm tra nghiệp vụ sâu (Trùng lặp Unique trong DB, đối chiếu ngày tháng, kiểm tra logic trạng thái, ràng buộc điều kiện vé/suất chiếu).
4. **Database Constraint:** Khóa chính, Khóa ngoại, `UNIQUE`, `CHECK Constraint`, `NOT NULL`, độ dài cột.

---

## 2. CHI TIẾT ĐỐI SOÁT TỪNG FORM / DTO NỀN TẢNG

### 2.1. Đăng ký tài khoản (`RegisterForm` / Screen 02 / UC05)

| Trường dữ liệu | Đặc tả SRS | Frontend (`register.html`) | Backend DTO (`RegisterForm`) | Database (`Account` & `Customer`) | Đánh giá & Khoảng trống (Gap) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **username** | 4-50 ký tự, chữ, số, `_`, `-`, `.` | `required`, `minlength="4"`, `maxlength="50"`, `pattern="^[A-Za-z0-9_.-]+$"` | `@NotBlank`, `@Size(min=4, max=50)`, `@Pattern` chuẩn | `NVARCHAR(50) NOT NULL UNIQUE` | **ĐỒNG BỘ HOÀN HẢO** |
| **email** | Định dạng email chuẩn, tối đa 100 ký tự, Unique | `type="email"`, `required` | `@NotBlank`, `@Email` | `NVARCHAR(100) NOT NULL UNIQUE` | **ĐỒNG BỘ**. DTO thiếu `@Size(max=100)` nhưng DB giới hạn 100. |
| **password** | 8-128 ký tự, ít nhất 1 chữ cái & 1 chữ số | `type="password"`, `required`, `minlength="8"` | `@NotBlank`, `@Size(min=8, max=128)`, `@Pattern(chữ + số)` | Lưu hash BCrypt 60 ký tự (`VARCHAR(255)`) | **ĐỒNG BỘ HOÀN HẢO** |
| **fullName** | 2-100 ký tự, không chứa ký tự đặc biệt nguy hiểm | `required`, `minlength="2"`, `maxlength="100"` | `@NotBlank`, `@Size(min=2, max=100)` | `NVARCHAR(100) NOT NULL` | **ĐỒNG BỘ** |
| **phone** | Số điện thoại VN (10 số, đầu 0 hoặc +84) | `type="tel"`, `pattern` | `@Pattern(^(0\|\+84)[0-9]{9,10}$)` | `VARCHAR(20) NULL UNIQUE` | **ĐỒNG BỘ**. Service kiểm tra duplicate phone chuẩn. |
| **confirmPassword** | Khớp hoàn toàn với password | Kiểm tra JS so khớp | `@NotBlank`, Controller so sánh `equals()` | Không lưu DB | **ĐỒNG BỘ** |

---

### 2.2. Cập nhật hồ sơ cá nhân (`ProfileUpdateForm` / Screen 27 / UC16)

| Trường dữ liệu | Đặc tả SRS | Frontend (`profile-edit.html`) | Backend DTO (`ProfileUpdateForm`) | Database (`Account` & `Customer`) | Đánh giá & Khoảng trống (Gap) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **fullName** | 2-100 ký tự | `required`, `maxlength="100"` | `@NotBlank`, `@Size(min=2, max=100)` | `NVARCHAR(100) NOT NULL` | **ĐỒNG BỘ** |
| **phone** | 10 số (0... hoặc +84...) | `type="tel"`, `required` | `@Pattern(^(0[0-9]{9}\|\+84[0-9]{9})$)` | `VARCHAR(20) NULL` | **ĐỒNG BỘ**. Service bắt lỗi `DuplicatePhoneException`. |
| **dateOfBirth** | Ngày sinh phải trong quá khứ (`< today`) | `type="date"`, `max="${today - 1}"` | `@Past(message="Date of birth must be in the past.")` | `DATE NULL` | **ĐỒNG BỘ HOÀN HẢO** |
| **address** | Tối đa 255 ký tự | `maxlength="255"` | `@Size(max=255)` | `NVARCHAR(255) NULL` | **ĐỒNG BỘ** |

---

### 2.3. Quản lý phim (`MovieForm` / Screen 55 / UC26)

| Trường dữ liệu | Đặc tả SRS | Frontend (`movies.html`) | Backend DTO (`MovieForm`) | Database (`dbo.Movie`) | Đánh giá & Khoảng trống (Gap) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **title** | Bắt buộc, 1-150 ký tự | `required`, `maxlength="150"` | `@NotBlank`, `@Size(max=150)` | `NVARCHAR(150) NOT NULL` | **ĐỒNG BỘ** |
| **originalTitle**| Không bắt buộc, tối đa 150 ký tự | `maxlength="150"` | `@Size(max=150)` | `NVARCHAR(150) NULL` | **ĐỒNG BỘ** |
| **duration** | Số nguyên dương, 1-600 phút | `type="number"`, `min="1"`, `max="600"` | `@NotNull`, `@Min(1)`, `@Max(600)` | `INT NOT NULL, CHECK (Duration > 0)` | **ĐỒNG BỘ HOÀN HẢO** |
| **ageRating** | Một trong: `P`, `K`, `T13`, `T16`, `T18` | Thẻ `<select>` 5 option | `@NotBlank`, `@Pattern(^(P\|K\|T13\|T16\|T18)$)` | `VARCHAR(10) NOT NULL, CHECK in list` | **ĐỒNG BỘ HOÀN HẢO** |
| **status** | `Upcoming`, `Now Showing`, `Ended`, `Archived` | Thẻ `<select>` 4 option | `@NotBlank`, `@Pattern` 4 giá trị | `VARCHAR(30) NOT NULL` | **ĐỒNG BỘ** |
| **releaseDate & endDate** | `endDate >= releaseDate` | `type="date"` | `@DateTimeFormat` | `DATE NULL, CHECK (EndDate >= ReleaseDate)` | **Khoảng trống:** DTO thiếu custom validator kiểm tra `endDate >= releaseDate` tầng Java, phụ thuộc DB Constraint. |
| **poster / trailer** | Đường dẫn URL, tối đa 500 ký tự | `maxlength="500"` | `@Size(max=500)` | `NVARCHAR(500) NULL` | **ĐỒNG BỘ** |

---

### 2.4. Quản lý suất chiếu (`ShowtimeForm` / Screen 59 / UC30)

| Trường dữ liệu | Đặc tả SRS | Frontend (`showtimes.html`) | Backend DTO (`ShowtimeForm`) | Database & Service Layer | Đánh giá & Khoảng trống (Gap) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **movieId** | Phim phải đang ở trạng thái hợp lệ | `<select>` bắt buộc chọn | `@NotNull` | `ShowtimeService` kiểm tra phim có status `Upcoming` hoặc `Now Showing` (BR-07). | **ĐỒNG BỘ** |
| **roomId** | Phòng phải đang Active/Ready | `<select>` bắt buộc chọn | `@NotNull` | `ShowtimeService` kiểm tra phòng không bảo trì (BR-08) và có ít nhất 1 ghế khả dụng. | **ĐỒNG BỘ** |
| **startTime** | Thời gian bắt đầu phải ở tương lai | `type="datetime-local"` | `@NotNull`, `@DateTimeFormat` | `ShowtimeService` kiểm tra `startTime > now` (BR-55). | **ĐỒNG BỘ** |
| **endTime** | `endTime = startTime + duration` | Tự động tính trên UI/Service | `@DateTimeFormat` | Service tự động cộng thời lượng phim và kiểm tra buffer dọn phòng 15 phút (BR-09). | **ĐỒNG BỘ** |
| **baseTicketPrice** | Số tiền >= 0 | `type="number"`, `min="0"` | `@NotNull`, `@DecimalMin("0.0")` | `DECIMAL(18,2) NOT NULL, CHECK >= 0` | **ĐỒNG BỘ** |

---

### 2.5. Quản lý phòng chiếu (`RoomForm` / Screen 56 / UC27)

| Trường dữ liệu | Đặc tả SRS | Frontend (`rooms.html`) | Backend DTO (`RoomForm`) | Database (`dbo.Room`) | Đánh giá & Khoảng trống (Gap) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **roomCode** | Bắt buộc, tối đa 50 ký tự, Unique | `required`, `maxlength="50"` | `@NotBlank`, `@Size(max=50)` | `NVARCHAR(50) NOT NULL UNIQUE` | **ĐỒNG BỘ** |
| **roomName** | Bắt buộc, tối đa 100 ký tự | `required`, `maxlength="100"` | `@NotBlank`, `@Size(max=100)` | `NVARCHAR(100) NOT NULL` | **ĐỒNG BỘ** |
| **capacity** | Số ghế tự động đồng bộ từ sơ đồ ghế | `readonly` | `Integer capacity` | `INT NOT NULL DEFAULT 0, CHECK >= 0` | **ĐỒNG BỘ** |
| **totalRows & seatsPerRow**| Số hàng và số ghế mỗi hàng (1-26 hàng, 1-30 ghế) | `min="1"`, `max="30"` | `@Min(1)`, `@Max(30)` | `INT NOT NULL` | **ĐỒNG BỘ** |

---

### 2.6. Quản lý loại ghế & Bảng giá vé (`SeatTypeForm`, `TicketPricingForm` / Screen 57, 60 / UC28, UC31)

| Hạng mục | Đặc tả SRS | Backend Form DTO | Service / Database | Đánh giá & Khoảng trống |
| :--- | :--- | :--- | :--- | :--- |
| **SeatType.priceMultiplier** | Hệ số nhân giá vé (ví dụ 1.0, 1.2, 1.5) | `@NotNull`, `@DecimalMin("0.1")` | `DECIMAL(5,2) NOT NULL, CHECK > 0` | **ĐỒNG BỘ** |
| **Pricing Formula (BR-50)** | `UnitPrice = (BasePrice + Surcharge) * Multiplier` làm tròn 1.000 VND | `TicketPricingForm` chứa BasePrice, FormatSurcharge, DayType, TimeSlot | `TicketPricingService` tính công thức và làm tròn `RoundingMode.HALF_UP` đến hàng nghìn. | **ĐỒNG BỘ HOÀN HẢO** |
| **Pricing Snapshot (BR-51)**| Giá lưu chết tại thời điểm đặt vé | Lưu trong `BookingSeat.UnitPrice` | `CustomerBookingDAO` và `StaffCounterSalesService` snapshot giá. | **ĐỒNG BỘ** |

---

### 2.7. Quản lý bắp nước F&B (`FBItemForm` / Screen 61 / UC32)

| Trường dữ liệu | Đặc tả SRS | Frontend (`fb.html`) | Backend DTO (`FBItemForm`) | Database (`dbo.FoodBeverageItem`) | Đánh giá & Khoảng trống (Gap) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **itemCode** | Bắt buộc, tối đa 50 ký tự | `required`, `maxlength="50"` | `@NotBlank`, `@Size(max=50)` | `NVARCHAR(50) NOT NULL UNIQUE` | **ĐỒNG BỘ** |
| **itemName** | Bắt buộc, tối đa 150 ký tự | `required`, `maxlength="150"` | `@NotBlank`, `@Size(max=150)` | `NVARCHAR(150) NOT NULL` | **ĐỒNG BỘ** |
| **sellingPrice** | Giá bán >= 0 | `type="number"`, `min="0"` | `@NotNull`, `@DecimalMin("0.0")` | `DECIMAL(18,2) NOT NULL, CHECK >= 0` | **ĐỒNG BỘ** |
| **costPrice** | Giá vốn >= 0 | `type="number"`, `min="0"` | `@NotNull`, `@DecimalMin("0.0")` | `DECIMAL(18,2) NOT NULL, CHECK >= 0` | **ĐỒNG BỘ** |
| **category** | Combo, Food, Drink, Snack | `<select>` 4 option | `@NotBlank` | `NVARCHAR(50) NOT NULL` | **ĐỒNG BỘ** |

---

## 3. CÁC KHOẢNG TRỐNG VALIDATION NGHIỆP VỤ (BUSINESS LOGIC GAPS)

### Khoảng trống 1: Thiếu hoàn toàn Validation Voucher trong luồng Đặt vé Online
- **Mô tả:** Trong [`CustomerBookingController.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/CustomerBookingController.java#L205-L214), endpoint `@PostMapping("/promotions")` chỉ lấy chuỗi `voucherCode` và gán vào session `session.setAttribute("customerBookingVoucherCode", voucherCode)`.
- **Ràng buộc bị bỏ qua:**
  - Không kiểm tra mã có tồn tại trong `dbo.Voucher` hay không (`BR-18`).
  - Không kiểm tra hạn sử dụng: `ValidFrom <= now <= ValidTo` (`BR-17`).
  - Không kiểm tra giới hạn lượt dùng toàn hệ thống `TotalUsageLimit` (`BR-19`).
  - Không kiểm tra giới hạn lượt dùng của tài khoản khách hàng `PerAccountLimit` (`BR-38`).
  - Không kiểm tra giá trị đơn hàng tối thiểu `MinBookingAmount` (`BR-20`).
  - Không kiểm tra cấp bậc thành viên `RequiredRankID` (`BR-20`).
  - Không kiểm tra quy tắc 1 voucher / 1 đơn hàng (`BR-21`).
- **Mức độ rủi ro:** **CRITICAL**. Cho phép người dùng nhập bất kỳ chuỗi rác nào hoặc bỏ lọt mã giảm giá không hợp lệ.

---

### Khoảng trống 2: Bỏ qua kiểm tra điều kiện đánh giá phim (Movie Review)
- **Mô tả:** Trong [`MovieCatalogService.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/MovieCatalogService.java#L66-L75), phương thức `addReview(accountId, movieId, rating, content)` chỉ kiểm tra:
  ```java
  if (rating < 1) rating = 1;
  if (rating > 5) rating = 5;
  movieReviewDAO.saveOrUpdateReview(customerId, movieId, rating, content);
  ```
- **Ràng buộc bị vi phạm:** **BR-30** và **GB-09** quy định: *"A Customer may submit a MovieReview only for a Movie associated with at least one successfully paid Booking by that Customer."*
- **Hệ quả:** Bất kỳ khách hàng nào vừa tạo tài khoản xong cũng có thể đánh giá, chấm điểm và viết bình luận cho bất kỳ bộ phim nào mà không cần xem phim. Dẫn đến rủi ro spam đánh giá ảo.

---

### Khoảng trống 3: Trạng thái bài viết Tin tức (News Status Transition)
- **Mô tả:** Trong [`NewsRequest.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/NewsRequest.java#L11), validation `@Pattern(regexp = "(?i)DRAFT|PENDING")` giới hạn cứng chỉ cho phép nộp trạng thái `DRAFT` hoặc `PENDING`.
- **Vấn đề:** Do Manager không có DTO riêng để cập nhật trạng thái bài viết (`NewsApprovalRequest`) sang `PUBLISHED` hoặc `REJECTED`, nên toàn bộ quy trình chuyển trạng thái theo BR-45, BR-46, BR-48 bị tắc nghẽn ở tầng Validation.

---

### Khoảng trống 4: Ràng buộc độ tuổi khi đặt vé (Age Rating Validation)
- **Mô tả:** Phim có các mốc độ tuổi `P` (phổ thông), `K` (dưới 13 tuổi có bảo hộ), `T13` (từ 13 tuổi), `T16` (từ 16 tuổi), `T18` (từ 18 tuổi).
- **Thiếu sót:**
  - Trên màn hình đặt vé khách hàng (`CustomerBookingController`), không có cảnh báo hoặc kiểm tra ngày sinh (`CustomerProfile.DateOfBirth`) của khách hàng để cảnh báo hoặc chặn nếu chưa đủ tuổi xem phim `T16`/`T18`.
  - Trên màn hình Staff check-in, hoàn toàn thiếu Modal kiểm tra độ tuổi (Screen 5.7 / UC22 / BR-40).

---

### Khoảng trống 5: Bất đồng nhất ngôn ngữ trong thông báo lỗi (Error Messages)
- **Đặc tả SRS Appendix III.2:** Quy định bảng 19 mã thông báo hệ thống chuẩn `MSG1` đến `MSG19` (ví dụ: MSG02 "This field is required.", MSG04 "Incorrect email or password. Please try again.", MSG07 "This seat is no longer available. Please select another seat.", MSG11 "Invalid or expired voucher code.", MSG13 "This ticket has already been used.").
- **Hiện trạng Code:**
  - AuthController trả về tiếng Việt ("Email hoặc mật khẩu không chính xác.", "Tên đăng nhập đã tồn tại.").
  - CustomerBookingController trả về tiếng Anh ("Please select a valid show date.", "The submitted selection is invalid.").
  - StaffOperationsService trả về tiếng Anh ("Booking code was not found.", "Tickets for this booking were already checked in.").
  - Không có centralized `messages.properties` Resource Bundle để chuẩn hóa song ngữ hoặc áp dụng đúng mã thông báo MSG01-MSG19 theo tài liệu.
