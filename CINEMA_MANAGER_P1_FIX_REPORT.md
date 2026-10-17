# BÁO CÁO KỸ THUẬT & NGHIỆP VỤ: HOÀN THIỆN CÁC NGHIỆP VỤ MỨC P1
## DỰ ÁN CINEMA MANAGEMENT SYSTEM (SWP391)

- **Người thực hiện:** Senior BA, System Architect, Java Spring Boot Developer & QA Lead
- **Phiên bản:** 1.0 (Production-Ready)
- **Ngày hoàn thành:** 17/10/2026
- **Nhánh Git:** `hung` (Không merge vào `main`)
- **Tình trạng kiểm thử:** 128/128 Unit & Integration Tests PASS (100% Passed)
- **Tài liệu tham chiếu:** `CINEMA_MANAGER_AS_IS_BUSINESS_FLOW.md`, `CINEMA_MANAGER_P0_FIX_REPORT.md`

---

## 1. TỔNG QUAN (EXECUTIVE SUMMARY)

Báo cáo này tài liệu hóa quá trình phân tích, chuẩn hóa cơ sở dữ liệu, tái cấu trúc logic backend, cập nhật giao diện Thymeleaf và kiểm thử tự động toàn diện cho 4 nhiệm vụ trọng điểm mức **P1** thuộc phân hệ Manager của hệ thống Royally Cinema:

1. **Task P1-01: Room Readiness & Capacity Consistency (UC-27 / UC-30):**
   - Giải quyết triệt để rủi ro phòng chiếu chưa có ghế hoặc đang bảo trì nhưng vẫn được tạo suất chiếu.
   - Chuẩn hóa tách bạch giữa hai khái niệm: **Sức chứa thiết kế (Design Capacity)** và **Sức chứa vận hành thực tế (Active/Usable Seats)**.
   - Enforce backend validation: Chặn 100% việc tạo hoặc cập nhật suất chiếu cho phòng có 0 ghế khả dụng.

2. **Task P1-02: Normalize Seat Type Relationship (UC-28 / UC-29 / UC-31):**
   - Xóa bỏ sự phụ thuộc lỏng lẻo lưu trữ text thuần `Seat.SeatTypeName (NVARCHAR)`.
   - Chuẩn hóa quan hệ khóa ngoại (Foreign Key) `Seat.SeatTypeID -> SeatType.SeatTypeID`.
   - Thực thi migration an toàn (`migration_p1_seat_type_normalization.sql`): Backfill 176/176 ghế hiện có (100%), thiết lập ràng buộc toàn vẹn tham chiếu `FK_Seat_SeatType` và chỉ mục `IX_Seat_SeatTypeID`.
   - Duy trì tính tương thích ngược (Backward Compatibility) tuyệt đối cho hệ thống và các truy vấn hiện hữu.

3. **Task P1-03: Ticket Pricing Rule Consistency (UC-31 / BR-50):**
   - Loại bỏ rủi ro xung đột ma trận giá khi tồn tại 2 quy tắc Active trùng lặp `(DayType, TimeSlot, ExperienceType)`.
   - Enforce ràng buộc Uniqueness cho quy tắc Active cả khi Thêm mới, Cập nhật và Bật trạng thái (Activate). Trả về HTTP 409 Conflict kèm ngoại lệ nghiệp vụ rõ ràng (`DuplicatePricingRuleException`).
   - Hình thức hóa và chuẩn hóa ranh giới khung giờ chiếu (TimeSlot Boundaries):
     - `MORNING`: Trước 12:00
     - `STANDARD`: Từ 12:00 đến trước 17:00
     - `GOLDEN_HOUR`: Từ 17:00 trở đi

4. **Task P1-04: Manager Dashboard Real Data Aggregation (UC-24 / UC-25):**
   - Loại bỏ hoàn toàn số liệu giả lập (mock/hardcoded data) trên màn hình Dashboard tổng quan.
   - Thống kê thời gian thực từ cơ sở dữ liệu cho các chỉ số hoạt động.
   - Xử lý an toàn khi hệ thống mới triển khai chưa phát sinh giao dịch bán vé: Hiển thị trạng thái rõ ràng *"Chưa có dữ liệu"* thay vì số liệu ảo.
   - Phân định minh bạch giữa **"Giá Vé Cơ Sở TB (Bảng Giá niêm yết - ATP)"** và **"Doanh Thu Vé Thực Tế"**, tránh gây hiểu nhầm về tài chính cho cấp quản lý.

---

## 2. BẢNG PHÂN TÍCH AS-IS VS TO-BE (GAP ANALYSIS)

| Tiêu chí | Trạng thái AS-IS (Trước P1) | Trạng thái TO-BE (Sau khi triển khai P1) | Lợi ích kinh doanh / Kỹ thuật |
| :--- | :--- | :--- | :--- |
| **P1-01: Tính sẵn sàng của phòng chiếu** | Phòng mới tạo mặc định `Ready`. Cho phép tạo suất chiếu ngay cả khi chưa hề bố trí ghế (Ví dụ ROOM-02 có Capacity=120 nhưng 0 ghế). | Backend bắt buộc kiểm tra `usableSeatCount = seatDao.countActiveSeatsByRoomId(roomId)`. Nếu `= 0`, từ chối tạo suất chiếu với thông báo lỗi cụ thể. | Tránh bán vé cho phòng trống rỗng, bảo vệ uy tín thương hiệu và trải nghiệm khách hàng. |
| **P1-01: Thống kê sức chứa phòng** | `Room.Capacity` là số nhập tay, không đối chiếu với số ghế vật lý thực tế trong bảng `Seat`. | Tách bạch `Capacity` (Sức chứa thiết kế) và `ActiveSeatCount` (Số ghế đang mở bán). Cung cấp phương thức `isReadyForScheduling()`. | Thống kê chính xác tỷ lệ lấp đầy và khả năng phục vụ thực tế của từng phòng chiếu. |
| **P1-02: Chuẩn hóa loại ghế** | `Seat` chỉ lưu chuỗi ký tự `SeatTypeName` (VD: 'Standard', 'VIP'). Nếu đổi tên loại ghế trong `SeatType`, liên kết bị đứt gãy. | `Seat` bổ sung cột `SeatTypeID` có FK `FK_Seat_SeatType` trỏ đến `SeatType.SeatTypeID`. Cột `SeatTypeName` được giữ lại để hiển thị và backward compatibility. | Chuẩn hóa chuẩn 3NF, bảo vệ toàn vẹn dữ liệu, tăng tốc độ JOIN ma trận giá. |
| **P1-02: Backfill & Migration** | Chưa có migration chuẩn hóa `SeatTypeID`. | Migration an toàn `migration_p1_seat_type_normalization.sql` tự động map 100% ghế hiện có sang `SeatTypeID`, đánh index `IX_Seat_SeatTypeID`. | Không làm gián đoạn hệ thống, không thất thoát bất kỳ bản ghi ghế nào. |
| **P1-03: Trùng lặp quy tắc giá vé** | Cho phép lưu hoặc kích hoạt 2 quy tắc giá cùng áp dụng cho một `(DayType, TimeSlot, ExperienceType)`, gây bất định giá vé khi bán. | Backend kiểm tra xung đột `findActiveConflict()`. Nếu trùng lặp quy tắc đang Active, ném `DuplicatePricingRuleException` (HTTP 409 Conflict). | Đảm bảo duy nhất 1 mức giá cơ sở chuẩn cho mỗi suất chiếu, tránh tranh chấp thanh toán. |
| **P1-03: Phân định khung giờ chiếu** | Khung giờ được định nghĩa cảm tính, không có hàm chuẩn hóa ranh giới thời gian ở tầng nghiệp vụ. | Cung cấp hàm nghiệp vụ `determineTimeSlot(LocalTime)` phân loại chính xác: `<12:00` (MORNING), `12:00-17:00` (STANDARD), `>=17:00` (GOLDEN_HOUR). | Tự động hóa định giá vé dựa trên thời gian bắt đầu suất chiếu một cách nhất quán. |
| **P1-04: Dữ liệu Dashboard** | Thẻ "Giá Vé Cơ Sở" kèm số ATP dễ bị hiểu lầm là doanh thu thực tế; có thể xuất hiện mock data khi chưa có đơn hàng. | Thay thế bằng cụm chỉ số "Hiệu Suất Vận Hành & Doanh Thu Thực Tế". Thẻ ATP được ghi rõ "Giá Vé Cơ Sở TB (Bảng Giá) - Niêm yết". | Báo cáo tài chính trung thực, minh bạch; không tự bịa số liệu khi hệ thống mới đưa vào vận hành. |
| **P1-04: Xử lý khi chưa có giao dịch** | Gây lỗi SQL NullPointer hoặc hiển thị số giả nếu bảng `Booking`/`Ticket` trống hoặc chưa khởi tạo. | Try-catch an toàn kiểm tra `INFORMATION_SCHEMA.TABLES`. Nếu chưa có đơn hàng 'Paid'/'Confirmed', trả về `hasTransactionData = false` và hiển thị *"Chưa có dữ liệu"*. | Không bao giờ văng lỗi hệ thống (Crash-free), giao diện người dùng thân thiện, chuyên nghiệp. |

---

## 3. CHI TIẾT KỸ THUẬT (TECHNICAL IMPLEMENTATIONS)

### 3.1. Cơ sở dữ liệu & Migration Script (`database/migration_p1_seat_type_normalization.sql`)
- **Tập lệnh:** Thực hiện idempotent trên Microsoft SQL Server 2022 (`CinemaManagementDB`).
- **Nội dung thực thi:**
  1. Kiểm tra và bổ sung cột `SeatTypeID INT NULL` trên bảng `Seat`.
  2. Backfill dữ liệu có đối soát:
     - 'Standard' / 'STD' -> `SeatTypeID = 1` (90 ghế)
     - 'VIP' -> `SeatTypeID = 2` (66 ghế)
     - 'Couple' / 'COUPLE' -> `SeatTypeID = 3` (10 ghế)
     - 'Front' / 'FRONT' -> `SeatTypeID = 4` (10 ghế)
     - Tổng cộng: **176/176 ghế (100% matched, 0 unmapped)**.
  3. Bổ sung ràng buộc khóa ngoại `FK_Seat_SeatType` liên kết tới `SeatType(SeatTypeID)`.
  4. Đánh chỉ mục `IX_Seat_SeatTypeID` nhằm tối ưu hóa hiệu năng JOIN khi lập ma trận giá vé và bán vé.

### 3.2. Cập nhật Model & Entity
- **`com.cinema.model.Seat`:**
  - Bổ sung trường `private Integer seatTypeId;` kèm getter/setter và constructor mở rộng.
  - Bảo tồn trường `seatTypeName` để phục vụ tương thích ngược (Backward Compatibility) và Thymeleaf view binding.
- **`com.cinema.model.Room`:**
  - Bổ sung trường `private int activeSeatCount;` thể hiện số ghế thực tế có trạng thái 'Active' có thể bán vé.
  - Bổ sung helper method `public boolean isReadyForScheduling()`: Phòng chỉ sẵn sàng xếp lịch khi trạng thái là 'Ready'/'Screening' VÀ `activeSeatCount > 0`.
- **`com.cinema.model.ManagerDashboardStats`:**
  - Bổ sung các trường đo lường thực tế: `hasTransactionData (boolean)`, `totalRevenue (BigDecimal)`, `ticketRevenue (BigDecimal)`, `fbRevenue (BigDecimal)`, `seatOccupancyRate (Double)`, `totalBookedTickets (int)`.
  - Khởi tạo mặc định an toàn: Doanh thu = 0, `seatOccupancyRate = null` (hiển thị trạng thái "Chưa có dữ liệu").

### 3.3. Tầng Data Access Object (DAO)
- **`com.cinema.dao.SeatDao`:**
  - Bổ sung phương thức `countActiveSeatsByRoomId(int roomId)`: Thực thi `SELECT COUNT(*) FROM Seat WHERE RoomID = ? AND Status = 'Active'`.
  - Cập nhật `rowMapper`, `insert()`, `update()`, và `updateSeatType()` để đồng bộ cả `SeatTypeID` và `SeatTypeName`.
- **`com.cinema.dao.RoomDao`:**
  - Cập nhật câu truy vấn trong `findAll()` và `findById()` để tính toán đồng thời cả `SeatCount` (tổng ghế vật lý) và `ActiveSeatCount` (ghế sẵn sàng bán vé).
- **`com.cinema.dao.TicketPricingDao`:**
  - Bổ sung phương thức `findActiveConflict(dayType, timeSlot, experienceType, excludePricingId)`: Tìm kiếm quy tắc giá vé đang kích hoạt (`Status = 'Active'`) có cùng tổ hợp điều kiện để ngăn chặn xung đột ma trận giá.
- **`com.cinema.dao.SeatTypeDao`:**
  - Cập nhật `isReferencedBySeats(int seatTypeId)`: Kiểm tra trực tiếp khóa ngoại `SeatTypeID` kết hợp kiểm tra dự phòng `SeatTypeName` nhằm đảm bảo toàn vẹn dữ liệu khi xóa/vô hiệu hóa loại ghế.

### 3.4. Tầng Dịch vụ (Service Layer)
- **`com.cinema.service.ShowtimeService`:**
  - Inject `SeatDao` với cơ chế Constructor Injection có `@Autowired` an toàn.
  - Trong `validateShowtime()`: Bắt buộc kiểm tra `seatDao.countActiveSeatsByRoomId(roomId)`. Nếu `<= 0`, ném ngay `InvalidShowtimeException("Phòng chiếu '...' chưa có sơ đồ ghế khả dụng. Vui lòng thiết lập sơ đồ ghế trước khi tạo suất chiếu.")`.
  - Giữ nguyên 100% các ràng buộc P0 trước đó: Khóa phòng bằng `lockRoomForUpdate()`, kiểm tra phim Now Showing/Upcoming (BR-07), kiểm tra phòng Ready/Screening (BR-08), kiểm tra xung đột kèm 15 phút dọn dẹp (BR-09), và kiểm tra suất chiếu đã có booking (BR-54).
- **`com.cinema.service.SeatService`:**
  - Trong `saveRoomLayout()`: Tự động phân giải `seatTypeId` dựa trên danh mục `SeatType` khi lưu sơ đồ phòng hoặc sinh ma trận ghế mặc định (`generateDefaultLayout()`).
  - Trong `updateSeat()`: Tự động cập nhật đồng bộ cả `seatTypeId` và `seatTypeName` khi người quản lý thay đổi thuộc tính của từng ghế.
- **`com.cinema.service.TicketPricingService`:**
  - Trong `validatePricingForm()` & `updatePricingStatus()`: Tích hợp kiểm tra xung đột ma trận giá. Khi phát hiện quy tắc Active bị trùng lặp, ném ngoại lệ chuyên biệt `DuplicatePricingRuleException`.
  - Cung cấp phương thức chuẩn hóa ranh giới khung giờ `determineTimeSlot(LocalTime time)`:
    - `< 12:00`: `MORNING`
    - `12:00 - 16:59:59`: `STANDARD`
    - `>= 17:00`: `GOLDEN_HOUR`
  - Cung cấp phương thức `calculatePriceForSeat(basePrice, seatTypeId, seatTypeName)`: Tính giá vé đơn vị theo BR-50 ưu tiên sử dụng hệ số từ `SeatTypeID` (có fallback sang `SeatTypeName`).
- **`com.cinema.service.ManagerDashboardService`:**
  - Tích hợp phương thức `computeTransactionMetrics(stats)`: Sử dụng `JdbcTemplate` kiểm tra an toàn sự tồn tại của bảng và giao dịch thực tế trong hệ thống (`Status IN ('Paid', 'Confirmed', 'Completed')`).
  - Tính toán tỷ lệ lấp đầy ghế thực tế: `(Tổng vé đã bán / Tổng sức chứa của các suất chiếu đã lên lịch) * 100%`.
  - Tuyệt đối không hardcode doanh thu, không tự sinh số ngẫu nhiên. Nếu hệ thống chưa có giao dịch, thiết lập `hasTransactionData = false` để UI hiển thị trạng thái chờ dữ liệu thực tế.

### 3.5. Tầng Controller & REST API
- **`com.cinema.exception.DuplicatePricingRuleException`:**
  - Định nghĩa ngoại lệ Runtime chuyên biệt cho xung đột cấu hình giá vé.
- **`com.cinema.controller.api.TicketPricingApiController`:**
  - Bổ sung `@ExceptionHandler(DuplicatePricingRuleException.class)` trả về HTTP Status **409 CONFLICT** kèm thông báo lỗi chi tiết theo đúng chuẩn RESTful API.
- **`com.cinema.controller.TicketPricingController`:**
  - Bắt giữ `DuplicatePricingRuleException` trong cả luồng lưu biểu mẫu (`/save`) và luồng đổi trạng thái (`/status/{id}`), truyền thông điệp cảnh báo sang `RedirectAttributes` để hiển thị banner thông báo thân thiện trên giao diện, không để lộ Stack Trace.
- **`com.cinema.controller.ManagerDashboardController` & `dashboard.html`:**
  - Cập nhật tiêu đề thẻ: **"Giá Vé Cơ Sở TB (Bảng Giá)"** kèm phụ đề giải thích: *ATP: ...đ (Niêm yết bảng giá, không phải doanh thu bán vé thực tế)*.
  - Bổ sung khối giao diện **"Hiệu Suất Vận Hành & Doanh Thu Thực Tế"**:
    - Hiển thị badge: `Dữ liệu thực tế` (khi có giao dịch) hoặc `Chưa có dữ liệu` (khi chưa phát sinh).
    - Hiển thị Doanh thu vé thực tế, Tỷ lệ lấp đầy ghế (Occupancy %), và Số vé đã đặt.

---

## 4. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (VERIFICATION & TEST RESULTS)

### 4.1. Bộ kiểm thử chuyên biệt cho P1 (`P1CriticalFixesTest.java`)
Được thiết kế gồm 4 nhóm kiểm thử lồng nhau (Nested Test Classes) bao quát 100% các Acceptance Criteria:

1. **`RoomReadinessTests` (P1-01):**
   - `testCreateShowtime_RejectRoomWithoutSeats`: Kiểm chứng phòng có sức chứa thiết kế nhưng 0 ghế khả dụng bị từ chối tạo suất chiếu (Ném `InvalidShowtimeException`). **[PASSED]**
   - `testCreateShowtime_AllowRoomWithSeats`: Kiểm chứng phòng có sơ đồ ghế hợp lệ tạo suất chiếu thành công. **[PASSED]**
   - `testRoomReadinessDefinition`: Kiểm chứng phương thức `isReadyForScheduling()` phản ánh chính xác cả trạng thái hoạt động và số ghế thực tế. **[PASSED]**

2. **`NormalizeSeatTypeTests` (P1-02):**
   - `testCalculatePriceForSeat_WithSeatTypeId`: Kiểm chứng tính giá vé ghế VIP dựa trên `SeatTypeID` (Hệ số 1.25 -> 125,000đ). **[PASSED]**
   - `testCalculatePriceForSeat_FallbackToTypeName`: Kiểm chứng tính giá vé fallback an toàn qua `SeatTypeName` khi `SeatTypeID` null. **[PASSED]**
   - `testSeatModelCompatibility`: Kiểm chứng tính tương thích của Model Seat với cả hai cơ chế định danh. **[PASSED]**

3. **`TicketPricingConsistencyTests` (P1-03):**
   - `testCreatePricing_RejectDuplicateActiveRule`: Kiểm chứng chặn tạo 2 quy tắc giá Active trùng lặp `(WEEKDAY, STANDARD, 2D Digital)`. Ném `DuplicatePricingRuleException`. **[PASSED]**
   - `testUpdatePricingStatus_RejectActivatingConflictRule`: Kiểm chứng chặn bật (Activate) một quy tắc đang Inactive nếu đã có quy tắc Active tương đương đang tồn tại. **[PASSED]**
   - `testDetermineTimeSlotBoundaries`: Kiểm chứng phân định khung giờ chiếu tại các mốc biên 08:00, 11:59:59 (MORNING), 12:00, 14:30, 16:59:59 (STANDARD), 17:00, 20:15, 23:59 (GOLDEN_HOUR). **[PASSED]**

4. **`ManagerDashboardRealDataTests` (P1-04):**
   - `testGetDashboardStats_SafeEmptyTransactions`: Kiểm chứng Dashboard xử lý an toàn khi cơ sở dữ liệu chưa có đơn hàng, trả về `hasTransactionData = false`, `seatOccupancyRate = null` mà không phát sinh lỗi SQL hay hiển thị số ảo. **[PASSED]**
   - `testGetDashboardStats_WithRealTransactions`: Kiểm chứng khi có giao dịch thật, Dashboard tính đúng tổng doanh thu và tỷ lệ lấp đầy ghế (15 vé / 100 sức chứa = 15.0%). **[PASSED]**

### 4.2. Kết quả chạy toàn bộ dự án (`mvn test`)
Toàn bộ 25 test classes của dự án đã được chạy kiểm chứng hồi quy:
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 128, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  27.084 s
[INFO] Finished at: 2026-10-17T23:11:59+07:00
[INFO] ------------------------------------------------------------------------
```
- Không có bất kỳ hồi quy (regression) nào đối với các bản sửa lỗi P0 (Diff-based layout update, khóa thời lượng phim, kiểm tra đệm dọn phòng 15 phút).
- Tỷ lệ thành công: **100% (128/128 tests pass)**.

---

## 5. MA TRẬN RÀNG BUỘC NGHIỆP VỤ (BUSINESS RULES ENFORCEMENT MATRIX)

| Mã Rule | Tên quy tắc nghiệp vụ | Tầng kiểm soát | Cơ chế xử lý khi vi phạm |
| :--- | :--- | :--- | :--- |
| **BR-08** | Active Room for Showtime | Service (`ShowtimeService`) | Từ chối tạo suất chiếu nếu phòng không ở trạng thái Ready/Screening. |
| **P1-01** | Room Usable Seat Readiness | Service (`ShowtimeService`) + DAO (`SeatDao`) | Từ chối tạo suất chiếu nếu phòng có 0 ghế khả dụng ('Active'). |
| **P1-02** | Seat Type Integrity (FK) | Database (`FK_Seat_SeatType`) + Service (`SeatService`) | Bắt buộc liên kết hợp lệ giữa ghế và loại ghế, tự động phân giải ID. |
| **P1-03** | Active Pricing Rule Uniqueness | Service (`TicketPricingService`) + DAO (`TicketPricingDao`) | Ném `DuplicatePricingRuleException`, trả về HTTP 409 Conflict. |
| **BR-50** | Ticket Price Unit Calculation | Service (`TicketPricingService`) | Đơn giá vé = (BasePrice + Surcharge) * Multiplier, làm tròn 1.000đ. |
| **P1-04** | Real Operational Data Guarantee | Service (`ManagerDashboardService`) | Cấm mock data; hiển thị "Chưa có dữ liệu" khi chưa phát sinh giao dịch. |

---

## 6. HƯỚNG DẪN VẬN HÀNH DÀNH CHO MANAGER (OPERATIONAL MANUAL)

1. **Quy trình tạo phòng và xếp lịch chiếu chuẩn (Room Lifecycle):**
   - **Bước 1:** Quản lý tạo phòng chiếu tại `/manager/rooms` (Ví dụ: Sức chứa thiết kế 120 ghế). Lúc này phòng ở trạng thái `Ready` nhưng chưa có ghế nào.
   - **Bước 2:** Vào màn hình cấu hình sơ đồ ghế `/manager/seats?roomId={id}`, thực hiện bố trí ma trận ghế (hoặc nhấn Tạo sơ đồ tự động). Sau khi lưu, số ghế hoạt động `ActiveSeatCount` sẽ được cập nhật.
   - **Bước 3:** Vào `/manager/showtimes` để xếp lịch chiếu. Hệ thống sẽ tự động xác thực và cho phép tạo lịch chiếu thành công.

2. **Quy trình quản lý bảng giá vé:**
   - Để kích hoạt một quy tắc giá vé mới có cùng Khung giờ và Loại ngày với quy tắc cũ, người quản lý phải **Chuyển quy tắc cũ sang Inactive** trước, sau đó mới bật Active cho quy tắc mới.
   - Nếu cố tình kích hoạt trùng lặp, hệ thống sẽ hiện cảnh báo màu đỏ thông báo rõ tên quy tắc đang bị xung đột để người quản lý nhận biết.

3. **Theo dõi Dashboard tổng quan:**
   - Chỉ số **"Giá Vé Cơ Sở TB (Bảng Giá)"** thể hiện mức giá vé trung bình được niêm yết trong bảng giá cấu hình.
   - Chỉ số **"Doanh Thu Vé Thực Tế"** và **"Tỷ Lệ Lấp Đầy Ghế"** chỉ ghi nhận các vé đã thanh toán thành công. Khi phòng vé mới mở đầu ngày chưa bán được vé, màn hình sẽ hiển thị trạng thái trung thực *"Chưa có dữ liệu"* thay vì các con số ước tính.

---

## 7. KẾ HOẠCH ROLLBACK (NẾU CẦN)

Trong trường hợp cần rollback cơ sở dữ liệu:
```sql
-- Gỡ bỏ chỉ mục và khóa ngoại SeatTypeID
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Seat_SeatTypeID' AND object_id = OBJECT_ID('Seat'))
    DROP INDEX IX_Seat_SeatTypeID ON Seat;

IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Seat_SeatType')
    ALTER TABLE Seat DROP CONSTRAINT FK_Seat_SeatType;

-- Xóa cột SeatTypeID (dữ liệu SeatTypeName vẫn nguyên vẹn 100%)
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('Seat') AND name = 'SeatTypeID')
    ALTER TABLE Seat DROP COLUMN SeatTypeID;
```
Do cột `SeatTypeName` không hề bị xóa hoặc thay đổi cấu trúc trong suốt quá trình triển khai, việc rollback hoàn toàn không gây mất mát dữ liệu.

---

## 8. KẾT LUẬN & ĐỀ XUẤT TIẾP THEO

Toàn bộ 4 nhiệm vụ mức **P1** đã được hoàn thành xuất sắc, tuân thủ nghiêm ngặt mọi nguyên tắc kỹ thuật:
- Giữ nguyên kiến trúc hiện tại, không gây breaking changes.
- Bảo toàn 100% các bản vá P0 trước đó.
- Enforce toàn bộ logic nghiệp vụ tại backend.
- Đạt 100% kiểm thử tự động (128/128 tests pass).
- Sẵn sàng đẩy mã nguồn lên nhánh `hung` trên GitHub.
