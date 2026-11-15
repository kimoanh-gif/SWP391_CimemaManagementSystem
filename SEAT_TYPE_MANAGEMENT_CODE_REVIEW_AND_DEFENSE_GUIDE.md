# BÁO CÁO REVIEW CODE & BỘ CÂU HỎI VẤN ĐÁP TOÀN DIỆN
# MODULE: SEAT TYPE MANAGEMENT (MÀN HÌNH 57) – CINEORA CINEMA MANAGEMENT SYSTEM

---

## MỤC LỤC TỔNG QUAN

1. **PHẦN 1: TỔNG QUAN KIẾN TRÚC & DANH MỤC FILE LIÊN QUAN**
2. **PHẦN 2: LUỒNG TẢI TRANG (PAGE LOAD FLOW: GET `/manager/seat-types`)**
3. **PHẦN 3: BỐN THẺ KPI THỐNG KÊ (DASHBOARD STAT CARDS)**
4. **PHẦN 4: BẢNG DỮ LIỆU SEAT TYPE LIST & PHÂN TRANG (PAGINATION)**
5. **PHẦN 5: LUỒNG THÊM MỚI LOẠI GHẾ (ADD SEAT TYPE FLOW)**
6. **PHẦN 6: LUỒNG CHỈNH SỬA LOẠI GHẾ & CẬP NHẬT HỆ SỐ NHANH (EDIT & QUICK MULTIPLIER UPDATE)**
7. **PHẦN 7: LUỒNG XÓA & VÔ HIỆU HÓA LOẠI GHẾ (DELETE & TOGGLE STATUS FLOW)**
8. **PHẦN 8: PHÂN TÍCH CHUYÊN SÂU GHẾ CHUẨN (STANDARD SEAT) & QUY TẮC BR-50**
9. **PHẦN 9: TÍCH HỢP SEAT MAP MANAGEMENT (QUAN HỆ GHẾ - LOẠI GHẾ)**
10. **PHẦN 10: TÍCH HỢP TICKET PRICING & MA TRẬN GIÁ VÉ (PRICING MATRIX & CHECKOUT)**
11. **PHẦN 11: THIẾT KẾ CƠ SỞ DỮ LIỆU, ERD & RÀNG BUỘC TOÀN VẸN (INTEGRITY CONSTRAINTS)**
12. **PHẦN 12: BẢO MẬT, VALIDATION & QUẢN LÝ GIAO DỊCH (SECURITY & TRANSACTIONS)**
13. **PHẦN 13: ĐÁNH GIÁ CHẤT LƯỢNG MÃ NGUỒN: BUGS, TIỀM ẨN RỦI RO & ĐỀ XUẤT NÂNG CẤP**
14. **PHẦN 14: BỘ 50 CÂU HỎI VẤN ĐÁP THỰC CHIẾN CÙNG GIẢNG VIÊN (KÈM ĐÁP ÁN VÀ CODE BẰNG CHỨNG)**
    - *Nhóm 1: Câu hỏi cơ bản & Kiến trúc tổng thể (15 câu)*
    - *Nhóm 2: Câu hỏi trung cấp & Luồng xử lý nghiệp vụ (20 câu)*
    - *Nhóm 3: Câu hỏi nâng cao, Cơ sở dữ liệu & Tình huống bẫy thực tế (15 câu)*
15. **PHẦN 15: BẢNG PHẢN XẠ NHANH (ONE-PAGE CHEAT SHEET CHO SINH VIÊN)**

---

# PHẦN 1: TỔNG QUAN KIẾN TRÚC & DANH MỤC FILE LIÊN QUAN

Module **Seat Type Management (Màn hình 57)** thuộc nghiệp vụ quản trị rạp (UC-28 trong SWP391), có trách nhiệm định nghĩa các phân hạng ghế trong rạp (Standard, VIP, Front Row, Sweetbox Couple...), quy định mã màu hiển thị trên sơ đồ ghế (Seat Map) và đặc biệt là thiết lập **hệ số nhân giá vé (Price Multiplier)** theo quy tắc nghiệp vụ cốt lõi **BR-50**.

Mô hình kiến trúc được tổ chức theo chuẩn **Layered Architecture** (Kiến trúc phân tầng) của Spring Boot kết hợp Spring JDBC Template:

```
[Trình duyệt Người Quản trị / Manager UI]
         │ (HTTP GET/POST /manager/seat-types)
         ▼
[SeatTypeController.java] <──────> [ManagerSecurityUtils.java] (Xác thực quyền Manager)
         │
         │ (gọi nghiệp vụ giá vé & phân hạng ghế)
         ▼
[TicketPricingService.java] 
         │
         ├─────────────────────────────────────────┐
         ▼                                         ▼
[SeatTypeDao.java]                          [SeatDao.java] (Kiểm tra ghế đang tham chiếu)
         │                                         │
         ▼                                         ▼
[Database Table: dbo.SeatType]             [Database Table: dbo.Seat]
```

### Danh mục các File Source Code thực tế trong Workspace:

1. **Controller Layer (Giao diện & API):**
   - [SeatTypeController.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java): Xử lý các request đồng bộ SSR (Server-Side Rendering) thông qua Thymeleaf (`/manager/seat-types`).
   - [SeatTypeApiController.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/SeatTypeApiController.java): Cung cấp các RESTful API chuẩn JSON (`/api/manager/seat-types`) cho các client mở rộng hoặc AJAX.
   - [TicketPricingController.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/TicketPricingController.java): Màn hình 60, sử dụng chung dữ liệu `SeatType` để tính bảng giá vé.
   - [SeatController.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatController.java): Màn hình sơ đồ ghế (Seat Map), tải danh sách SeatType để tô màu và đổi loại ghế.

2. **Service Layer (Nghiệp vụ cốt lõi):**
   - [TicketPricingService.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java): Đảm nhiệm toàn bộ logic CRUD loại ghế, kiểm tra trùng lặp mã/tên, chặn vô hiệu hóa nếu có suất chiếu, chặn xóa nếu có ghế sử dụng, và thuật toán tính giá vé theo hệ số làm tròn đến 1.000 VNĐ (`calculateTicketUnitPrice`).
   - [SeatService.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/SeatService.java): Nghiệp vụ liên kết ghế trong phòng chiếu với `SeatType`, kiểm tra chặn sửa loại ghế nếu ghế đã có người đặt trong các suất chiếu sắp tới (`isSeatBookedInUpcomingShowtimes`).

3. **DAO Layer (Data Access Object / Spring JDBC):**
   - [SeatTypeDao.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java): Tương tác trực tiếp với bảng `SeatType` thông qua `JdbcTemplate` (các câu lệnh SQL thuần parameterized).
   - [SeatDao.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatDao.java): Chứa truy vấn kiểm tra quan hệ giữa `Seat` và `SeatType`.
   - [CustomerBookingDAO.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/CustomerBookingDAO.java): Áp dụng hệ số `PriceMultiplier` khi khách hàng hoặc nhân viên chọn ghế để đóng băng đơn giá vào `BookingSeat.UnitPrice`.

4. **DTO & Model Layer:**
   - [SeatType.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/model/SeatType.java): Domain entity đại diện cho 1 bản ghi trong bảng `dbo.SeatType`.
   - [SeatTypeForm.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/SeatTypeForm.java): DTO chứa các Bean Validation annotations (`@DecimalMin`, `@DecimalMax`, `@Pattern`, `@NotBlank`).
   - [SeatTypeNotFoundException.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/exception/SeatTypeNotFoundException.java): Ngoại lệ Runtime khi không tìm thấy ID loại ghế.

5. **Presentation Layer (Thymeleaf & Static Assets):**
   - [templates/manager/seat-types.html](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html): Giao diện giao dịch chính (gồm 4 KPI card, bảng dữ liệu, 2 popup Modal).
   - [static/js/manager-seat-types.js](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/static/js/manager-seat-types.js): Script xử lý DOM, mở modal Add/Edit, gán data attributes vào form.
   - [static/css/manager-seat-types.css](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/static/css/manager-seat-types.css): Stylesheet định dạng các badge màu, nút hành động và layout.

6. **Database Schema & Migrations:**
   - [database/00_full_database.sql](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql): Định nghĩa bảng `dbo.SeatType` (dòng 302-315), `dbo.Seat` (dòng 320-345), `dbo.BookingSeat` (dòng 480-505) và `dbo.Ticket`.

---

# PHẦN 2: LUỒNG TẢI TRANG (PAGE LOAD FLOW: GET `/manager/seat-types`)

Khi người quản lý nhấp vào mục **Seat Type Management** trên sidebar, trình duyệt gửi request:
`GET http://localhost:8080/manager/seat-types?status=ALL&page=1&size=10`

### Trình tự 8 bước xử lý:

```
[Browser] ──(1. GET /manager/seat-types)──> [DispatcherServlet]
                                                   │
                                            (2. Map Handler)
                                                   │
                                                   ▼
                                       [SeatTypeController.java]
                                                   │
     ┌─────────────────────────────────────────────┴─────────────────────────────────────────────┐
     ▼ (Bước 3)                                                                                  ▼ (Bước 4)
[ManagerSecurityUtils.verifyManagerAccess]                                              [TicketPricingService.getAllSeatTypes]
Verifies session: userRole == 'MANAGER'                                                          │
(Throws 403 Forbidden if unauthorized)                                                           ▼
                                                                                        [SeatTypeDao.findAll]
                                                                                        Executes SQL Query:
                                                                                        SELECT * FROM SeatType
                                                                                        WHERE (? IS NULL OR Status = ?)
                                                                                        ORDER BY PriceMultiplier ASC
                                                                                                 │
                                                                                                 ▼
                                                                                        [Returns List<SeatType>]
                                                   ┌─────────────────────────────────────────────┘
                                                   ▼ (Bước 5)
                                       [In-Memory Pagination Calculation]
                                       start = (page - 1) * size;
                                       end = min(start + size, totalFiltered);
                                       pagedSeatTypes = seatTypes.subList(start, end);
                                                   │
                                                   ▼ (Bước 6)
                                       [Model Attribute Population]
                                       model.addAttribute("seatTypes", pagedSeatTypes);
                                       model.addAttribute("totalCount", 4);
                                       model.addAttribute("activeCount", 4);
                                       model.addAttribute("seatTypeForm", new SeatTypeForm());
                                                   │
                                                   ▼ (Bước 7)
                                       [Thymeleaf Template Engine]
                                       Renders "manager/seat-types.html"
                                                   │
                                                   ▼ (Bước 8)
                                       [HTTP 200 OK HTML Stream to Browser]
```

### Chi tiết bằng chứng mã nguồn:
- **Xác thực quyền quản lý:** `ManagerSecurityUtils.verifyManagerAccess(request)` tại [SeatTypeController.java#L52](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L52). Nếu `session.getAttribute("userRole") != "MANAGER"`, lập tức throw `UnauthorizedManagerException` (HTTP 403).
- **Truy vấn danh sách:** `pricingService.getAllSeatTypes(status)` gọi `seatTypeDao.findAll(status)` tại [SeatTypeDao.java#L40-L47](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L40-L47):
  ```sql
  SELECT * FROM SeatType WHERE (? IS NULL OR Status = ?) ORDER BY PriceMultiplier ASC
  ```
  *Nhận xét kiến trúc:* Kết quả luôn được sắp xếp theo hệ số giá tăng dần (`ORDER BY PriceMultiplier ASC`), giúp màn hình hiển thị trực quan từ loại ghế rẻ nhất (FRONT: 0.95x) đến đắt nhất (COUPLE: 1.80x).
- **Phân trang trong bộ nhớ:** Do danh mục loại ghế trong một cụm rạp thường chỉ có 4 - 8 loại ghế, hệ thống tải danh sách đầy đủ để tính thống kê và dùng `subList(start, end)` ([SeatTypeController.java#L64-L66](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L64-L66)) để phân trang an toàn.

---

# PHẦN 3: BỐN THẺ KPI THỐNG KÊ (DASHBOARD STAT CARDS)

Trên cùng của giao diện là 4 thẻ thống kê nghiệp vụ phản ánh tình trạng phân hạng giá vé của hệ thống:

| Thẻ KPI | Giá trị hiển thị | Ý nghĩa nghiệp vụ | Nguồn gốc dữ liệu & Code sinh ra |
| :--- | :---: | :--- | :--- |
| **1. Total seat types** | `4` *(4 active in the system)* | Tổng số loại ghế đã đăng ký trong rạp và số lượng đang ở trạng thái Hoạt động. | `model.addAttribute("totalCount", totalCount)` và `activeCount` tại [SeatTypeController.java#L69-L70](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L69-L70). Template hiển thị: `<div th:text="${totalCount}">4</div>`. |
| **2. Standard seat multiplier** | `1.00x` *(Base ticket price reference)* | Hệ số chuẩn làm mốc đối chiếu giá vé cơ sở. Bất kỳ giá vé chuẩn nào cũng có multiplier = 1.00. | Được hardcode quy chuẩn tại [seat-types.html#L112](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L112) theo đặc tả thiết kế rạp chiếu quốc tế (ISO/Cinema Standard). |
| **3. Average price multiplier** | `1.25x` *(Of the seat types listed)* | Hệ số nhân giá vé trung bình của các loại ghế đang hiển thị trên bảng. | Tính toán động bằng Thymeleaf Expression: `${#numbers.formatDecimal(#aggregates.avg(seatTypes.![priceMultiplier]), 1, 2) + 'x'}` tại [seat-types.html#L118](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L118). Giá trị thực: `(0.95 + 1.00 + 1.25 + 1.80) / 4 = 5.00 / 4 = 1.25x`. |
| **4. Allowed multiplier range** | `0.50 – 3.00` *(Business rule BR-50)* | Khoảng hệ số nhân giá tối thiểu và tối đa được phép cấu hình. | Quy định theo Business Rule **BR-50**, được bảo vệ đồng thời ở 3 tầng: DTO Form `@DecimalMin("0.50") @DecimalMax("3.00")`, Service check và Database `CHECK (PriceMultiplier BETWEEN 0.50 AND 3.00)`. |

---

# PHẦN 4: BẢNG DỮ LIỆU SEAT TYPE LIST & PHÂN TRANG (PAGINATION)

Bảng dữ liệu hiển thị danh sách 4 loại ghế mặc định của hệ thống CINEORA:

### 1. Cấu trúc cột dữ liệu:
1. **Code:** Mã định danh viết tắt của loại ghế (ví dụ: `FRONT`, `STD`, `VIP`, `COUPLE`), hiển thị dưới dạng monospace badge.
2. **Seat type name:** Tên thương mại kèm dấu chấm tròn màu sắc đại diện (`.color-dot`), ví dụ: `VIP Royal Amber`, `Sweetbox Couple`.
3. **Price multiplier:** Hệ số nhân giá có hậu tố `x` (ví dụ: `0.95x`, `1.00x`, `1.25x`, `1.80x`), được đóng khung bằng badge nổi bật.
4. **Seat map color:** Chấm màu và mã HEX đại diện trên sơ đồ ghế (`#B74A10`, `#3B82F6`, `#F59E0B`, `#EC4899`).
5. **Features & amenities:** Mô tả tính năng và trải nghiệm người dùng (ví dụ: ghế ngả da cao cấp, vách ngăn riêng tư).
6. **Status:** Trạng thái hoạt động, hiển thị `Active` (Badge xanh lá cây).
7. **Actions:** Cặp nút hành động:
   - **Nút Edit (✎):** Class `.btn-edit-seattype`, chứa toàn bộ `data-*` attributes (`data-id`, `data-code`, `data-name`, `data-multiplier`, `data-color`, `data-description`, `data-status`).
   - **Nút Delete (🗑):** Form submit POST tới `/manager/seat-types/delete/{id}` kèm cảnh báo Javascript `confirm(...)`.

### 2. Bốn loại ghế chuẩn trong database:
```
┌─────────┬──────────────────────────┬────────────┬───────────┬───────────────────────────────────────────────────────┬────────┐
│ Code    │ Seat Type Name           │ Multiplier │ Color HEX │ Features & Amenities                                  │ Status │
├─────────┼──────────────────────────┼────────────┼───────────┼───────────────────────────────────────────────────────┼────────┤
│ FRONT   │ Front Row (Front)        │ 0.95x      │ #B74A10   │ Front-row seats close to screen at a budget price.    │ Active │
│ STD     │ Standard Seat (Standard) │ 1.00x      │ #3B82F6   │ Standard seats with panoramic view across screen.     │ Active │
│ VIP     │ VIP Royal Amber          │ 1.25x      │ #F59E0B   │ Premium cushioned seats with optimal viewing angles.  │ Active │
│ COUPLE  │ Sweetbox Couple (Couple) │ 1.80x      │ #EC4899   │ Cozy couple seats with private partition in back row. │ Active │
└─────────┴──────────────────────────┴────────────┴───────────┴───────────────────────────────────────────────────────┴────────┘
```

---

# PHẦN 5: LUỒNG THÊM MỚI LOẠI GHẾ (ADD SEAT TYPE FLOW)

### 1. Luồng phía Client:
- Khi người dùng click nút `+ Add seat type` (ID: `#btnCreateSeatType`), Javascript trong [manager-seat-types.js#L11-L23](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/static/js/manager-seat-types.js#L11-L23) thực thi:
  1. Đổi tiêu đề modal thành `"Add seat type"`.
  2. Reset toàn bộ các input field: `seatTypeId = ''`, `typeCode = ''` (thuộc tính `readOnly = false`), `priceMultiplier = '1.00'`, `colorCode = '#3B82F6'`.
  3. Gán `form.action = '/manager/seat-types/save'`.
  4. Thêm class `.show` vào modal `#seatTypeModal`.

### 2. Luồng xử lý Backend (`POST /manager/seat-types/save`):
Phương thức `saveSeatType` tại [SeatTypeController.java#L87-L117](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L87-L117):
1. **Kiểm tra quyền hạn:** `ManagerSecurityUtils.verifyManagerAccess(request)`.
2. **Kiểm tra Bean Validation:** `@Valid @ModelAttribute("seatTypeForm") SeatTypeForm form, BindingResult bindingResult`.
   - Nếu có lỗi vi phạm validation, lấy message lỗi đầu tiên, đính kèm vào `RedirectAttributes.addFlashAttribute("errorMessage", errorMsg)` và redirect về `/manager/seat-types`.
3. **Thực thi Service:** Gọi `pricingService.createSeatType(form)` tại [TicketPricingService.java#L118-L135](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L118-L135):
   - **Kiểm tra khoảng hệ số BR-50:**
     ```java
     if (form.getPriceMultiplier() == null 
         || form.getPriceMultiplier().compareTo(new BigDecimal("0.50")) < 0 
         || form.getPriceMultiplier().compareTo(new BigDecimal("3.00")) > 0) {
         throw new InvalidPricingConfigurationException("Seat price multiplier must be between 0.50 and 3.00");
     }
     ```
   - **Kiểm tra trùng mã loại ghế:**
     `seatTypeDao.findByCode(form.getTypeCode().trim())` -> nếu tồn tại ném ngoại lệ `"Seat type code '...' already exists in the system"`.
   - **Kiểm tra trùng tên loại ghế:**
     `seatTypeDao.findByName(form.getTypeName().trim())` -> nếu tồn tại ném ngoại lệ `"Seat type name '...' already exists in the system"`.
4. **Ghi xuống Database:** Gọi `seatTypeDao.insert(form)` tại [SeatTypeDao.java#L67-L78](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L67-L78):
   ```sql
   INSERT INTO SeatType (TypeCode, TypeName, Description, PriceMultiplier, ColorCode, Status, CreatedAt)
   VALUES (?, ?, ?, ?, ?, ?, SYSUTCDATETIME())
   ```
5. **Flash Message & Redirect:** Trả về thông báo thành công: `Seat type '...' created successfully!`.

---

# PHẦN 6: LUỒNG CHỈNH SỬA & CẬP NHẬT HỆ SỐ (EDIT & QUICK UPDATE)

Module hỗ trợ 2 cơ chế cập nhật:

### Cơ chế 1: Cập nhật toàn diện thông tin loại ghế (`POST /manager/seat-types/update/{id}`)
- Khi click icon Edit (✎) trên một dòng:
  - JS đọc các thuộc tính `data-*` từ nút bấm và điền vào modal form.
  - **Khóa mã Code:** `document.getElementById('typeCode').readOnly = true;` ([manager-seat-types.js#L40](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/static/js/manager-seat-types.js#L40)). *Lý do kiến trúc:* Mã loại ghế (TypeCode) là định danh bất biến, nếu đổi Code có thể gây lệch mapping ở các tầng legacy.
  - Form action chuyển thành: `/manager/seat-types/update/{id}`.
- Xử lý tại Backend (`pricingService.updateSeatType(id, form)`):
  - Kiểm tra tồn tại theo ID (ném `SeatTypeNotFoundException` nếu không tìm thấy).
  - Kiểm tra xem nếu đổi sang `Status = 'Inactive'` mà phòng chiếu chứa ghế này đang có suất chiếu tương lai (`seatTypeDao.hasActiveOrUpcomingShowtimes(id)`) thì **từ chối cập nhật** và ném thông báo:
    `"Cannot deactivate seat type '...' because screening rooms with active or upcoming showtimes are using it."`.
  - Thực thi SQL UPDATE tại [SeatTypeDao.java#L80-L93](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L80-L93):
    ```sql
    UPDATE SeatType SET TypeCode = ?, TypeName = ?, Description = ?,
    PriceMultiplier = ?, ColorCode = ?, Status = ?, UpdatedAt = SYSUTCDATETIME()
    WHERE SeatTypeID = ?
    ```

### Cơ chế 2: Cập nhật nhanh hệ số giá (`POST /manager/seat-types/multiplier`)
- Cho phép quản lý điều chỉnh trực tiếp hệ số giá vé (`multiplier`) mà không cần mở toàn bộ form sửa.
- Xử lý tại [TicketPricingService.java#L106-L115](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L106-L115):
  - Validate nghiêm ngặt `0.50 <= multiplier <= 3.00`.
  - Thực thi: `UPDATE SeatType SET PriceMultiplier = ?, UpdatedAt = SYSUTCDATETIME() WHERE SeatTypeID = ?`.

### CÂU HỎI QUAN TRỌNG: "Nếu sửa hệ số VIP từ 1.25x lên 1.50x thì giá vé của các khách hàng đã đặt hoặc đã mua trước đó có bị thay đổi không?"
> **TRẢ LỜI:** **HOÀN TOÀN KHÔNG BỊ THAY ĐỔI.**
>
> **Bằng chứng kiến trúc trong mã nguồn:**
> Hệ thống áp dụng mẫu kiến trúc **Price Snapshot Pattern**:
> 1. Trong bảng `dbo.BookingSeat` ([00_full_database.sql#L485](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql#L485)), cột `UnitPrice DECIMAL(18,2) NOT NULL` lưu trữ trực tiếp số tiền vé tại thời điểm khách đặt ghế.
> 2. Trong [CustomerBookingDAO.java#L143-L162](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/CustomerBookingDAO.java#L143-L162), khi tạo đơn hàng, hệ số giá tại thời điểm đó được nhân với giá gốc (`st.BasePrice * seatType.PriceMultiplier`) và `INSERT` thẳng vào `dbo.BookingSeat (UnitPrice)`.
> 3. Bảng vé điện tử `dbo.Ticket` chỉ tham chiếu `BookingSeatID`, hoàn toàn không tính lại giá từ `SeatType`.
> 4. Do đó, việc thay đổi `PriceMultiplier` chỉ có hiệu lực với các suất chiếu hoặc lượt chọn ghế **phát sinh sau thời điểm lưu**. Doanh thu, hóa đơn và lịch sử giao dịch cũ được bảo toàn 100% tính toàn vẹn tài chính.

---

# PHẦN 7: LUỒNG XÓA & VÔ HIỆU HÓA LOẠI GHẾ (DELETE & TOGGLE STATUS)

### 1. Luồng Xóa loại ghế (`POST /manager/seat-types/delete/{id}`):
Khi người quản trị bấm icon Thùng rác (🗑) và xác nhận dialog:
1. Request gửi tới `SeatTypeController.deleteSeatType(id)`.
2. Gọi `pricingService.deleteSeatType(id)` tại [TicketPricingService.java#L170-L178](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L170-L178):
   ```java
   if (seatTypeDao.isReferencedBySeats(id)) {
       throw new InvalidPricingConfigurationException(
           "Cannot delete seat type '" + st.getTypeName() + "' because seats in screening rooms are using this seat type. Please change seat types for related seats first or switch status to Inactive."
       );
   }
   seatTypeDao.delete(id);
   ```
3. Phương thức `seatTypeDao.isReferencedBySeats(id)` tại [SeatTypeDao.java#L105-L112](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L105-L112) kiểm tra cả khóa ngoại và tên loại ghế:
   ```sql
   SELECT COUNT(*) FROM Seat s
   WHERE s.SeatTypeID = ?
   OR (s.SeatTypeID IS NULL AND (s.SeatTypeName = (SELECT TypeName FROM SeatType WHERE SeatTypeID = ?)
   OR s.SeatTypeName = (SELECT TypeCode FROM SeatType WHERE SeatTypeID = ?)))
   ```
4. **Kết quả:** Nếu có bất kỳ ghế nào trong bất kỳ phòng chiếu nào đang gắn loại ghế này, hệ thống **CHẶN XÓA TUYỆT ĐỐI** và thông báo người dùng chuyển trạng thái sang `Inactive`.
5. Nếu không có ghế nào liên kết, thực thi lệnh SQL:
   `DELETE FROM SeatType WHERE SeatTypeID = ?`

### 2. Luồng Vô hiệu hóa (`Status = Inactive`):
- Để ngăn chặn tình trạng khách hàng đặt phải ghế có loại đã ngừng kinh doanh trong các suất chiếu đang mở bán, phương thức `updateSeatTypeStatus` tại [TicketPricingService.java#L159-L167](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L159-L167) kiểm tra:
  ```java
  if ("Inactive".equalsIgnoreCase(status.trim()) && seatTypeDao.hasActiveOrUpcomingShowtimes(id)) {
      throw new InvalidPricingConfigurationException(
          "Cannot deactivate seat type '" + existing.getTypeName() + "' because screening rooms with active or upcoming showtimes are using it."
      );
  }
  ```
- Hàm `hasActiveOrUpcomingShowtimes(id)` ([SeatTypeDao.java#L114-L124](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L114-L124)) kiểm tra trong database xem phòng có loại ghế này có suất chiếu nào có `EndTime > GETDATE()` và `Status NOT IN ('CANCELLED', 'STOPPED')` hay không.

---

# PHẦN 8: PHÂN TÍCH CHUYÊN SÂU GHẾ CHUẨN (STANDARD SEAT) & QUY TẮC BR-50

### 1. Tại sao luôn có loại ghế Standard Seat với hệ số 1.00x?
- Trong kinh tế học điện ảnh và quản lý rạp chiếu phim (Cinema Revenue Management), **Base Ticket Price (Giá vé cơ sở)** được định nghĩa dựa trên trải nghiệm của 1 ghế tiêu chuẩn tại 1 suất chiếu tiêu chuẩn 2D vào ngày trong tuần.
- Ghế **Standard Seat (STD)** có hệ số nhân `1.00x` đóng vai trò là **Hệ quy chiếu (Reference Baseline)**:
  $$\text{Giá vé ghế chuẩn} = (\text{Base Price} + \text{Format Surcharge}) \times 1.00 = \text{Giá gốc}$$
- Mọi loại ghế khác đều tăng giá hoặc giảm giá tương đối so với ghế chuẩn:
  - Ghế hàng đầu (`FRONT`): `0.95x` (Giảm 5% do cấn cổ, góc nhìn sát màn hình).
  - Ghế VIP (`VIP`): `1.25x` (Tăng 25% do vị trí trung tâm tầm nhìn vàng - Prime Sweet Spot).
  - Ghế đôi tình nhân (`COUPLE`): `1.80x` (Tăng 80% do chiếm diện tích bằng 2 ghế thường và có vách ngăn riêng tư).

### 2. Công thức tính giá vé chuẩn quy tắc BR-50 & Thuật toán làm tròn đến 1.000 VNĐ:
Thuật toán được hiện thực tại [TicketPricingService.java#L185-L195](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L185-L195):
```java
public BigDecimal calculateTicketUnitPrice(BigDecimal basePrice, BigDecimal multiplier) {
    if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
        throw new InvalidPricingConfigurationException("Invalid base ticket price: " + basePrice);
    }
    if (multiplier == null || multiplier.compareTo(BigDecimal.ZERO) <= 0) {
        multiplier = BigDecimal.ONE;
    }
    BigDecimal rawPrice = basePrice.multiply(multiplier);
    BigDecimal thousand = new BigDecimal("1000");
    return rawPrice.divide(thousand, 0, RoundingMode.HALF_UP).multiply(thousand);
}
```

#### Ví dụ minh họa thực tế tính toán (Giá cơ sở = 85.000 VNĐ):
1. **Front Row (0.95x):**
   - Giá thô: $85.000 \times 0.95 = 80.750\text{ VNĐ}$
   - Chia 1.000: $80.750 / 1.000 = 80.75 \xrightarrow{\text{HALF\_UP}} 81$
   - Nhân lại 1.000: **81.000 VNĐ** (Người mua tiết kiệm được 4.000 VNĐ).
2. **Standard Seat (1.00x):**
   - Giá thô: $85.000 \times 1.00 = 85.000\text{ VNĐ}$
   - Làm tròn: **85.000 VNĐ**.
3. **VIP Royal Amber (1.25x):**
   - Giá thô: $85.000 \times 1.25 = 106.250\text{ VNĐ}$
   - Chia 1.000: $106.250 / 1.000 = 106.25 \xrightarrow{\text{HALF\_UP}} 106$
   - Nhân lại 1.000: **106.000 VNĐ**.
4. **Sweetbox Couple (1.80x):**
   - Giá thô: $85.000 \times 1.80 = 153.000\text{ VNĐ}$
   - Làm tròn: **153.000 VNĐ**.

---

# PHẦN 9: TÍCH HỢP SEAT MAP MANAGEMENT (QUAN HỆ GHẾ - LOẠI GHẾ)

Khi người quản lý thiết lập sơ đồ ghế cho một phòng chiếu tại màn hình **Seat Map Management (Màn hình 58)**:
1. `SeatController.java` gọi `pricingService.getAllSeatTypes("Active")` để lấy danh sách loại ghế hiển thị lên thanh công cụ (Toolbar chọn loại ghế).
2. Mỗi ghế trong phòng (`dbo.Seat`) lưu cả `SeatTypeID` (Khóa ngoại) và `SeatTypeName` (Tên loại ghế phi chuẩn hóa để tra cứu nhanh).
3. **Kiểm tra chặn sửa loại ghế khi đã có vé đặt:**
   Trong phương thức `batchUpdateSeats` tại [SeatService.java#L87-L95](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/SeatService.java#L87-L95):
   ```java
   boolean typeChanged = targetTypeName != null && !targetTypeName.equalsIgnoreCase(seat.getSeatTypeName());
   if (typeChanged && seatDao.isSeatBookedInUpcomingShowtimes(seat.getSeatId(), roomId)) {
       throw new InvalidSeatLayoutException(
           "Cannot change seat type because the seat has booked tickets in an upcoming showtime."
       );
   }
   ```
   *Ý nghĩa:* Nếu khách hàng đã mua 1 ghế với giá VIP hoặc Standard, người quản lý không thể vào sửa loại ghế đó thành Couple hay Front row cho đến khi các suất chiếu kết thúc, đảm bảo đúng vị trí và quyền lợi đã cam kết với người mua vé.

---

# PHẦN 10: TÍCH HỢP TICKET PRICING & MA TRẬN GIÁ VÉ (PRICING MATRIX)

Tại màn hình **Ticket Pricing (Màn hình 60)**, dữ liệu `SeatType` được kết hợp với các quy tắc giá vé (Quy định theo Thứ/Cuối tuần, Khung giờ Sáng/Chiều/Tối) để tự động sinh ra **Bảng ma trận giá vé (Pricing Matrix)**:

Phương thức `generatePricingMatrix()` tại [TicketPricingService.java#L202-L216](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L202-L216):
```java
public Map<Integer, Map<Integer, BigDecimal>> generatePricingMatrix() {
    List<TicketPricing> rules = ticketPricingDao.findAll(null, "Active");
    List<SeatType> seatTypes = seatTypeDao.findAll("Active");
    Map<Integer, Map<Integer, BigDecimal>> matrix = new HashMap<>();

    for (TicketPricing rule : rules) {
        Map<Integer, BigDecimal> row = new HashMap<>();
        BigDecimal effectiveBase = rule.getBasePrice().add(rule.getFormatSurcharge());
        for (SeatType st : seatTypes) {
            row.put(st.getSeatTypeId(), calculateTicketUnitPrice(effectiveBase, st.getPriceMultiplier()));
        }
        matrix.put(rule.getPricingId(), row);
    }
    return matrix;
}
```
Cấu trúc ma trận trả về: `Map<PricingRuleID, Map<SeatTypeID, FinalPrice>>`. Nhờ đó, giao diện bảng giá vé tính sẵn giá cho tất cả các loại ghế mà không cần bất kỳ câu lệnh truy vấn lồng phức tạp nào tại tầng database.

---

# PHẦN 11: THIẾT KẾ CƠ SỞ DỮ LIỆU, ERD & RÀNG BUỘC TOÀN VẸN

### 1. DDL Script thực tế của bảng `dbo.SeatType` ([00_full_database.sql#L302-L315](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql#L302-L315)):
```sql
CREATE TABLE dbo.SeatType (
    SeatTypeID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SeatType PRIMARY KEY,
    TypeCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_SeatType_TypeCode UNIQUE,
    TypeName NVARCHAR(100) NOT NULL CONSTRAINT UQ_SeatType_TypeName UNIQUE,
    [Description] NVARCHAR(255) NULL,
    PriceMultiplier DECIMAL(5,2) NOT NULL CONSTRAINT DF_SeatType_Multiplier DEFAULT 1.00,
    ColorCode NVARCHAR(20) NULL,
    [Status] NVARCHAR(20) NOT NULL CONSTRAINT DF_SeatType_Status DEFAULT 'Active',
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_SeatType_CreatedAt DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NULL,
    CONSTRAINT CK_SeatType_Multiplier CHECK (PriceMultiplier BETWEEN 0.50 AND 3.00)
);
```

### 2. Sơ đồ thực thể mối quan hệ (ERD Diagram):
```
┌─────────────────────────────────┐
│          dbo.SeatType           │
├─────────────────────────────────┤
│ PK  SeatTypeID (INT IDENTITY)   │
│     TypeCode (NVARCHAR UNIQUE)  │
│     TypeName (NVARCHAR UNIQUE)  │
│     PriceMultiplier (DECIMAL)   │◄──── CONSTRAINT CK_SeatType_Multiplier (0.50 - 3.00)
│     ColorCode (NVARCHAR)        │
│     Status (NVARCHAR)           │
└────────────────┬────────────────┘
                 │ 1
                 │
                 │ N (Không CASCADE Delete)
                 ▼
┌─────────────────────────────────┐
│            dbo.Seat             │
├─────────────────────────────────┤
│ PK  SeatID (INT IDENTITY)       │
│ FK  RoomID (INT)                │
│ FK  SeatTypeID (INT)            │
│     SeatTypeName (NVARCHAR)     │
│     RowLabel (NVARCHAR)         │
│     SeatNumber (INT)            │
│     GridRow, GridColumn         │
└────────────────┬────────────────┘
                 │ 1
                 │
                 │ N
                 ▼
┌─────────────────────────────────┐
│         dbo.BookingSeat         │
├─────────────────────────────────┤
│ PK  BookingSeatID (INT)         │
│ FK  BookingID (INT)             │
│ FK  SeatID (INT)                │
│ FK  ShowtimeID (INT)            │
│     UnitPrice (DECIMAL)         │◄──── LƯU GIÁ ĐÓNG BĂNG (SNAPSHOT TẠI THỜI ĐIỂM ĐẶT)
│     Status ('HELD','BOOKED')    │
└────────────────┬────────────────┘
                 │ 1
                 │
                 │ 1
                 ▼
┌─────────────────────────────────┐
│           dbo.Ticket            │
├─────────────────────────────────┤
│ PK  TicketID (INT IDENTITY)     │
│ FK  BookingSeatID (INT UNIQUE)  │
│     QRCode (VARCHAR UNIQUE)     │
│     TicketStatus ('VALID'...)   │
└─────────────────────────────────┘
```

---

# PHẦN 12: BẢO MẬT, VALIDATION & QUẢN LÝ GIAO DỊCH

### 1. Phân quyền và Bảo mật (Manager Security Check):
- Không dùng hardcode quyền trong controller; việc kiểm tra được đóng gói trong tiện ích dùng chung:
  `ManagerSecurityUtils.verifyManagerAccess(request)` ([ManagerSecurityUtils.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/util/ManagerSecurityUtils.java)).
- Cơ chế: Đọc `session.getAttribute("userRole")`. Chỉ khi giá trị bằng `"MANAGER"` (hoặc `"ADMIN"` tùy cấu hình hệ thống) thì request mới được phép đi tiếp; ngược lại trả về mã lỗi HTTP 403 Forbidden.

### 2. Validation 3 tầng (3-Tier Validation):
- **Tầng 1 (Client-side HTML5/JS):** Input form có `required`, `step="0.05"`, `min="0.50"`, `max="3.00"`, `type="color"`.
- **Tầng 2 (Backend Bean Validation DTO):** [SeatTypeForm.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/SeatTypeForm.java) với:
  - `@Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Seat type code can only contain letters, numbers, hyphens or underscores")`
  - `@Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Color code must be in HEX format")`
  - `@DecimalMin(value = "0.50")` và `@DecimalMax(value = "3.00")`
- **Tầng 3 (Database Constraint):** Ràng buộc toàn vẹn dữ liệu `CK_SeatType_Multiplier` ngăn chặn triệt để mọi hành vi bypass qua SQL Injection hay direct script.

### 3. Quản lý Giao dịch (Transaction Management):
- Tất cả các phương thức ghi dữ liệu trong `TicketPricingService.java` đều được gắn annotation `@Transactional`.
- Trong trường hợp cập nhật nhiều bản ghi (hoặc rollback khi kiểm tra lỗi trùng lặp/ràng buộc suất chiếu), Spring Transaction Manager tự động `ROLLBACK` bảo đảm cơ sở dữ liệu không rơi vào trạng thái phân mảnh hay sai lệch (Inconsistent state).

---

# PHẦN 13: ĐÁNH GIÁ CHẤT LƯỢNG MÃ NGUỒN: BUGS, TIỀM ẨN RỦI RO & ĐỀ XUẤT NÂNG CẤP

### 1. Điểm mạnh kiến trúc (Strengths):
- **Snapshot Price Pattern hoàn hảo:** Việc lưu `UnitPrice` độc lập trong `dbo.BookingSeat` giúp bảo vệ hệ thống trước biến động thay đổi giá của SeatType.
- **Phòng thủ 3 lớp cho BR-50:** Từ giao diện, Service cho đến Database đều đồng nhất kiểm tra dải giá trị `[0.50, 3.00]`.
- **Kiểm tra nghiệp vụ sâu trước khi Xóa/Tắt:** Không dựa hoàn toàn vào lỗi Foreign Key của Database mà Service chủ động kiểm tra `isReferencedBySeats` và `hasActiveOrUpcomingShowtimes`, trả về thông báo thân thiện bằng tiếng Anh rõ ràng cho người dùng.

### 2. Các điểm tiềm ẩn rủi ro (Risks / Minor Inconsistencies):
- **RISK-01 (Denormalized SeatTypeName):** Bảng `dbo.Seat` lưu cả `SeatTypeID` và `SeatTypeName`. Khi người quản trị đổi tên `TypeName` ở bảng `SeatType`, bảng `Seat` hiện tại không được cascade update cột `SeatTypeName`. Tuy nhiên, trong [SeatDao.java#L64-L65](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatDao.java#L64-L65) có LEFT JOIN theo `SeatTypeID` nên dữ liệu hiển thị thực tế vẫn đồng bộ.
- **RISK-02 (In-memory Pagination):** Phân trang bằng `seatTypes.subList(...)` hoạt động rất nhanh với số lượng loại ghế nhỏ (dưới 50 loại ghế), nhưng về mặt lý thuyết nếu mở rộng mô hình rạp phức hợp hàng nghìn loại ghế thì nên chuyển sang SQL `OFFSET...FETCH NEXT`.

---

# PHẦN 14: BỘ 50 CÂU HỎI VẤN ĐÁP THỰC CHIẾN CÙNG GIẢNG VIÊN (KÈM ĐÁP ÁN VÀ CODE BẰNG CHỨNG)

---

### NHÓM 1: CÂU HỎI CƠ BẢN & KIẾN TRÚC TỔNG THỂ (15 CÂU)

#### Câu 1: Màn hình Seat Type Management (Screen 57) có vai trò gì trong toàn bộ hệ thống CINEORA?
- **Trả lời:** Màn hình này phục vụ việc quản lý danh mục phân hạng ghế ngồi của cụm rạp (Standard, VIP, Couple, Front Row), thiết lập mã màu đại diện trực quan trên sơ đồ ghế và cấu hình hệ số nhân giá vé (Price Multiplier) để áp dụng linh hoạt vào công thức tính giá vé tự động theo quy tắc BR-50.
- **File dẫn chứng:** [SeatTypeController.java#L23-L25](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L23-L25).

#### Câu 2: Khi truy cập URL `/manager/seat-types`, Controller nào tiếp nhận request đầu tiên?
- **Trả lời:** `com.cinema.controller.SeatTypeController`, cụ thể là phương thức `listSeatTypes(@RequestParam... Model model, HttpServletRequest request)`.
- **File dẫn chứng:** [SeatTypeController.java#L44-L51](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L44-L51).

#### Câu 3: Màn hình này lấy dữ liệu từ bảng nào trong cơ sở dữ liệu?
- **Trả lời:** Bảng `dbo.SeatType` trong cơ sở dữ liệu PostgreSQL/SQL Server.
- **File dẫn chứng:** [00_full_database.sql#L302-L315](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql#L302-L315) và [SeatTypeDao.java#L41](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L41).

#### Câu 4: Các câu lệnh truy vấn dữ liệu Seat Type sử dụng ORM (Hibernate/JPA) hay JDBC Template?
- **Trả lời:** Dự án sử dụng thuần `org.springframework.jdbc.core.JdbcTemplate` kết hợp `RowMapper<SeatType>` để ánh xạ dữ liệu trực tiếp từ `ResultSet`, giúp tối ưu hiệu năng và kiểm soát chính xác các câu lệnh SQL.
- **File dẫn chứng:** [SeatTypeDao.java#L26-L38](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L26-L38).

#### Câu 5: Hệ số nhân giá vé (Price Multiplier) của ghế Standard là bao nhiêu và có ý nghĩa gì?
- **Trả lời:** Là `1.00x`. Nó đại diện cho mốc chuẩn (Base Ticket Price Reference). Giá vé của ghế Standard chính là giá vé cơ sở của suất chiếu đó mà không bị nhân thêm hay giảm trừ tỷ lệ phần trăm nào.
- **File dẫn chứng:** [seat-types.html#L112](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L112).

#### Câu 6: Quy tắc nghiệp vụ BR-50 quy định khoảng giá trị của Price Multiplier là bao nhiêu?
- **Trả lời:** Nằm trong đoạn từ `0.50` đến `3.00` (tương ứng từ giảm giá 50% đến gấp 3 lần giá cơ sở).
- **File dẫn chứng:** [SeatTypeForm.java#L32-L33](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/SeatTypeForm.java#L32-L33) và [00_full_database.sql#L313](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql#L313).

#### Câu 7: Bốn thẻ KPI thống kê trên đầu trang gồm những chỉ số nào?
- **Trả lời:**
  1. `Total seat types`: Tổng số loại ghế (kèm số lượng Active).
  2. `Standard seat multiplier`: Hệ số ghế chuẩn (cố định 1.00x).
  3. `Average price multiplier`: Hệ số nhân trung bình của các loại ghế hiển thị (hiện tại là 1.25x).
  4. `Allowed multiplier range`: Khoảng hệ số cho phép (0.50 - 3.00 theo BR-50).
- **File dẫn chứng:** [seat-types.html#L103-L128](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L103-L128).

#### Câu 8: Con số 1.25x ở thẻ "Average price multiplier" được tính như thế nào?
- **Trả lời:** Được tính toán tự động bằng Thymeleaf Expression thông qua hàm `#aggregates.avg(seatTypes.![priceMultiplier])`. Với 4 loại ghế mặc định: $(0.95 + 1.00 + 1.25 + 1.80) / 4 = 5.00 / 4 = 1.25x$.
- **File dẫn chứng:** [seat-types.html#L118](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L118).

#### Câu 9: Khi bấm nút "+ Add seat type", có request gửi về server không?
- **Trả lời:** Không có request gửi về server ngay. Sự kiện được xử lý hoàn toàn ở Frontend bằng Javascript: lắng nghe click trên `#btnCreateSeatType`, làm sạch form, gán action `/manager/seat-types/save` và thêm class `.show` để hiển thị modal popup.
- **File dẫn chứng:** [manager-seat-types.js#L10-L24](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/static/js/manager-seat-types.js#L10-L24).

#### Câu 10: Form nhập liệu thêm/sửa Seat Type gồm những trường thông tin nào?
- **Trả lời:**
  1. Type code (Mã loại ghế, vd: VIP).
  2. Seat type name (Tên loại ghế, vd: VIP Royal Amber).
  3. Price multiplier (Hệ số giá, từ 0.50 đến 3.00).
  4. Seat map color (Mã màu HEX chọn qua HTML5 color picker).
  5. Status (Active / Inactive).
  6. Features & amenities (Mô tả đặc điểm ghế).
- **File dẫn chứng:** [seat-types.html#L242-L272](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L242-L272).

#### Câu 11: Làm sao hệ thống đảm bảo người dùng thông thường (Customer/Staff) không vào được màn hình này?
- **Trả lời:** Ngay dòng đầu tiên của mỗi phương thức trong `SeatTypeController`, gọi hàm `ManagerSecurityUtils.verifyManagerAccess(request)`. Nếu trong session không có role `MANAGER`, hệ thống ném ngoại lệ `UnauthorizedManagerException` và trả về mã lỗi 403 Forbidden.
- **File dẫn chứng:** [SeatTypeController.java#L52](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L52) và [SeatTypeControllerTest.java#L50-L54](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/test/java/com/cinema/controller/SeatTypeControllerTest.java#L50-L54).

#### Câu 12: Entity `SeatType.java` có những thuộc tính chính nào?
- **Trả lời:** `seatTypeId` (Integer), `typeCode` (String), `typeName` (String), `description` (String), `priceMultiplier` (BigDecimal), `colorCode` (String), `status` (String), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime).
- **File dẫn chứng:** [SeatType.java#L12-L20](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/model/SeatType.java#L12-L20).

#### Câu 13: Tại sao trường `priceMultiplier` lại dùng kiểu `BigDecimal` mà không dùng `double` hay `float`?
- **Trả lời:** Vì `priceMultiplier` tham gia trực tiếp vào công thức tính toán tiền tệ và giá vé. Các kiểu dấu phẩy động như `float` hay `double` gây ra sai số làm tròn nhị phân (binary floating-point rounding error). Dùng `BigDecimal` đảm bảo độ chính xác tuyệt đối trong các phép nhân tài chính.
- **File dẫn chứng:** [SeatType.java#L16](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/model/SeatType.java#L16) và [TicketPricingService.java#L185-L195](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L185-L195).

#### Câu 14: Mã màu `ColorCode` được lưu dưới định dạng gì trong database?
- **Trả lời:** Chuỗi mã màu HEX tiêu chuẩn, ví dụ `#3B82F6`, `#F59E0B`, được xác thực bằng biểu thức chính quy Regex `^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$`.
- **File dẫn chứng:** [SeatTypeForm.java#L36](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/SeatTypeForm.java#L36).

#### Câu 15: Hệ thống sắp xếp danh sách loại ghế hiển thị theo tiêu chí nào?
- **Trả lời:** Sắp xếp tăng dần theo hệ số giá (`ORDER BY PriceMultiplier ASC`).
- **File dẫn chứng:** [SeatTypeDao.java#L41](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L41).

---

### NHÓM 2: CÂU HỎI TRUNG CẤP & LUỒNG XỬ LÝ NGHIỆP VỤ (20 CÂU)

#### Câu 16: Trình bày chi tiết luồng xử lý từ khi nhấn "Save seat type" trên modal Add?
- **Trả lời:**
  1. Trình duyệt gửi request `POST /manager/seat-types/save` kèm form data.
  2. `SeatTypeController.saveSeatType()` nhận dữ liệu vào `@Valid @ModelAttribute SeatTypeForm form`.
  3. Spring kiểm tra Bean Validation; nếu có lỗi ném ra flash error message và redirect về trang cũ.
  4. Nếu hợp lệ, Controller gọi `pricingService.createSeatType(form)`.
  5. Service kiểm tra khoảng hệ số (0.50 - 3.00), kiểm tra trùng lặp TypeCode và TypeName.
  6. Service gọi `seatTypeDao.insert(form)` để chạy câu lệnh `INSERT INTO SeatType...`.
  7. Trả về flash message `Seat type '...' created successfully!` và chuyển hướng về `/manager/seat-types`.
- **File dẫn chứng:** [SeatTypeController.java#L87-L117](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L87-L117).

#### Câu 17: Khi bấm icon Edit (✎) trên 1 loại ghế, dữ liệu được đưa lên modal bằng cách nào?
- **Trả lời:** Thymeleaf render trước các giá trị của dòng dữ liệu vào các thuộc tính HTML5 `data-*` của thẻ `<button>` (`data-id`, `data-code`, `data-name`, `data-multiplier`, `data-color`, `data-description`, `data-status`). File Javascript [manager-seat-types.js#L27-L50](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/static/js/manager-seat-types.js#L27-L50) đọc các thuộc tính này qua `btn.getAttribute(...)` và gán trực tiếp vào các ô `input` của modal form.

#### Câu 18: Tại sao khi sửa loại ghế, ô `typeCode` lại bị khóa (`readOnly = true`)?
- **Trả lời:** Trong thiết kế kiến trúc của CINEORA, `TypeCode` là mã định danh nghiệp vụ cố định (Business Key), được nhiều hệ thống con và logic cũ (fallback logic) tham chiếu tới. Cho phép sửa `TypeCode` có thể làm hỏng tính toàn vẹn dữ liệu và sai lệch ánh xạ trong sơ đồ ghế.
- **File dẫn chứng:** [manager-seat-types.js#L40](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/static/js/manager-seat-types.js#L40).

#### Câu 19: Nếu người dùng cố tình đổi TypeName trùng với một loại ghế khác khi Edit thì backend xử lý ra sao?
- **Trả lời:** Tại `TicketPricingService.updateSeatType()`, hệ thống truy vấn kiểm tra:
  ```java
  seatTypeDao.findByName(form.getTypeName().trim()).ifPresent(s -> {
      if (!s.getSeatTypeId().equals(id)) {
          throw new InvalidPricingConfigurationException("Seat type name '" + form.getTypeName() + "' is already in use");
      }
  });
  ```
  Nếu trùng với bản ghi khác ID thì ném ngoại lệ và thông báo lỗi hiển thị lên màn hình.
- **File dẫn chứng:** [TicketPricingService.java#L146-L151](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L146-L151).

#### Câu 20: Chức năng Quick Multiplier Update hoạt động như thế nào?
- **Trả lời:** Hệ thống cung cấp endpoint `POST /manager/seat-types/multiplier` tiếp nhận `seatTypeId` và `multiplier`. Thay vì cập nhật toàn bộ thông tin loại ghế, phương thức này chỉ chạy lệnh `UPDATE SeatType SET PriceMultiplier = ?, UpdatedAt = SYSUTCDATETIME() WHERE SeatTypeID = ?`, giúp người quản lý điều chỉnh nhanh chính sách giá mà không ảnh hưởng các thông tin khác.
- **File dẫn chứng:** [SeatTypeController.java#L119-L136](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L119-L136).

#### Câu 21: Nếu tôi xóa một loại ghế đang được gán cho các ghế trong phòng chiếu thì sao?
- **Trả lời:** Hệ thống sẽ **TỪ CHỐI XÓA**. Trước khi xóa, Service gọi `seatTypeDao.isReferencedBySeats(id)`. Nếu số lượng ghế đang tham chiếu lớn hơn 0, hệ thống ném ngoại lệ với thông báo: *"Cannot delete seat type '...' because seats in screening rooms are using this seat type. Please change seat types for related seats first or switch status to Inactive."*.
- **File dẫn chứng:** [TicketPricingService.java#L170-L177](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L170-L177) và [SeatTypeDao.java#L105-L112](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L105-L112).

#### Câu 22: Điều gì xảy ra nếu cố chuyển một loại ghế sang trạng thái "Inactive" trong khi phòng chiếu chứa ghế đó đang có suất chiếu sắp diễn ra?
- **Trả lời:** Hệ thống sẽ **CHẶN VÔ HIỆU HÓA**. Phương thức `hasActiveOrUpcomingShowtimes(id)` kiểm tra trong database xem có suất chiếu nào có `EndTime > GETDATE()` và chưa bị hủy (`NOT IN ('CANCELLED', 'STOPPED')`). Nếu có, Service ném ngoại lệ: *"Cannot deactivate seat type '...' because screening rooms with active or upcoming showtimes are using it."*.
- **File dẫn chứng:** [TicketPricingService.java#L162-L165](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L162-L165).

#### Câu 23: Trình bày thuật toán làm tròn giá vé trong phương thức `calculateTicketUnitPrice`?
- **Trả lời:**
  1. Nhân giá cơ sở với hệ số: `rawPrice = basePrice.multiply(multiplier)`.
  2. Chia cho 1.000 với cơ chế làm tròn `RoundingMode.HALF_UP` (từ 500 đồng trở lên làm tròn lên 1.000, dưới 500 đồng làm tròn xuống 0).
  3. Nhân lại với 1.000 để ra số tiền cuối cùng chẵn nghìn đồng:
     `return rawPrice.divide(new BigDecimal("1000"), 0, RoundingMode.HALF_UP).multiply(new BigDecimal("1000"));`
- **File dẫn chứng:** [TicketPricingService.java#L185-L195](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L185-L195).

#### Câu 24: Tại sao hệ thống rạp chiếu lại cần làm tròn đến 1.000 VNĐ?
- **Trả lời:** Trong hệ thống thanh toán tiền tệ tại Việt Nam, mệnh giá dưới 1.000 VNĐ không còn giá trị lưu thông thực tế trên tiền mặt và các cổng thanh toán điện tử (VNPAY, MoMo). Việc làm tròn đến 1.000 VNĐ giúp giá vé chuyên nghiệp, dễ thanh toán và chuẩn xác trên hóa đơn tài chính.

#### Câu 25: Khi khách hàng chọn ghế trên sơ đồ đặt vé, giá vé hiển thị được lấy từ đâu?
- **Trả lời:** Truy vấn SQL trong `CustomerBookingDAO.java` join trực tiếp bảng `Showtime st`, `Seat seat` và `SeatType seatType`:
  `CAST(st.BasePrice * seatType.PriceMultiplier AS DECIMAL(18,2)) AS Price`.
  Đồng thời chỉ lọc những ghế và loại ghế có `Status = 'ACTIVE'`.
- **File dẫn chứng:** [CustomerBookingDAO.java#L143-L153](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/CustomerBookingDAO.java#L143-L153).

#### Câu 26: Nếu trong lúc đặt vé, nhân viên quản lý sửa hệ số giá thì khách hàng có bị tăng giá giữa chừng không?
- **Trả lời:** Khi khách hàng chọn ghế và nhấn giữ chỗ, đơn giá ghế được cố định (Freeze/Held) và `INSERT` ngay vào bảng `dbo.BookingSeat` với cột `UnitPrice`. Giá vé đã giữ chỗ là bất biến, không bị ảnh hưởng bởi việc cập nhật hệ số sau đó.
- **File dẫn chứng:** [CustomerBookingDAO.java#L160-L165](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/CustomerBookingDAO.java#L160-L165).

#### Câu 27: Làm sao sơ đồ ghế (Seat Map) biết được ghế nào tô màu gì?
- **Trả lời:** Trong câu truy vấn lấy danh sách ghế của phòng chiếu ([SeatDao.java#L64-L65](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatDao.java#L64-L65)), bảng `Seat` được `LEFT JOIN SeatType st ON st.SeatTypeID = s.SeatTypeID` để lấy cột `ColorCode`. Trên giao diện Seat Map, mỗi ô ghế được render với style `background-color: ${seat.colorCode}`.

#### Câu 28: Nếu loại ghế có trường `SeatTypeID` bị null ở bảng Seat cũ thì hệ thống xử lý fallback thế nào?
- **Trả lời:** Câu truy vấn trong DAO có cơ chế fallback thông minh:
  `LEFT JOIN SeatType st ON st.SeatTypeID = s.SeatTypeID OR (s.SeatTypeID IS NULL AND (st.TypeName = s.SeatTypeName OR st.TypeCode = s.SeatTypeName))`
  Đồng thời `SeatService.resolveSeatTypeId(typeName)` sẽ tự động tra cứu lại TypeName để bù đắp ID nếu thiếu sót.
- **File dẫn chứng:** [SeatDao.java#L64-L67](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatDao.java#L64-L67) và [SeatService.java#L236-L240](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/SeatService.java#L236-L240).

#### Câu 29: Ngoài Controller đồng bộ cho giao diện, module này có cung cấp REST API không?
- **Trả lời:** Có, toàn bộ các chức năng được cung cấp thêm qua `SeatTypeApiController` với tiền tố URL `/api/manager/seat-types`, trả về đối tượng `ApiResponse<T>` theo chuẩn RESTful JSON (hỗ trợ GET, POST, PUT).
- **File dẫn chứng:** [SeatTypeApiController.java#L32-L98](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/api/SeatTypeApiController.java#L32-L98).

#### Câu 30: Phương thức `generatePricingMatrix()` trong `TicketPricingService` có mục đích gì?
- **Trả lời:** Nó tính toán trước bảng giá vé gồm 2 chiều: Chiều dọc là các luật giá vé (Quy định theo Thứ/Khung giờ/Định dạng 2D/3D) và Chiều ngang là các loại ghế (`SeatType`). Kết quả trả về `Map<PricingID, Map<SeatTypeID, CalculatedPrice>>` giúp màn hình Ticket Pricing hiển thị tức thì toàn bộ mức giá vé mà không cần tính lặp ở frontend.
- **File dẫn chứng:** [TicketPricingService.java#L202-L216](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L202-L216).

#### Câu 31: Khác biệt giữa `TicketPricing` và `SeatType` là gì?
- **Trả lời:**
  - `TicketPricing`: Quy định **Giá vé cơ sở (Base Price)** và phụ thu định dạng (Format Surcharge) phụ thuộc vào yếu tố thời gian (Ngày thường/Cuối tuần, Suất sáng/Suất tối).
  - `SeatType`: Quy định **Hệ số nhân (Price Multiplier)** phụ thuộc vào vị trí và tiện ích vật lý của ghế trong phòng chiếu (Ghế gần, Ghế thường, Ghế VIP, Ghế Couple).
  - Giá vé cuối cùng = (Base Price + Format Surcharge) $\times$ Multiplier.
- **File dẫn chứng:** [seat-types.html#L225](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L225).

#### Câu 32: Dữ liệu phân trang của bảng Seat Type được Controller truyền xuống qua những model attribute nào?
- **Trả lời:** `currentPage`, `totalPages`, `pageSize`, `totalFilteredCount`, `startItem`, `endItem`, `totalCount`, `activeCount`.
- **File dẫn chứng:** [SeatTypeController.java#L68-L78](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L68-L78).

#### Câu 33: Nếu không có loại ghế nào trong cơ sở dữ liệu thì giao diện hiển thị thế nào?
- **Trả lời:** Dòng thông báo rỗng xuất hiện: `<td colspan="7">No seat types configured yet.</td>`, và thẻ KPI trung bình hiển thị dấu gạch ngang `'—'` thay vì bị lỗi chia cho 0.
- **File dẫn chứng:** [seat-types.html#L118](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L118) và [seat-types.html#L154](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/resources/templates/manager/seat-types.html#L154).

#### Câu 34: Khi xóa Seat Type thành công, làm sao thông báo thành công hiển thị được sau lệnh redirect?
- **Trả lời:** Controller sử dụng `RedirectAttributes.addFlashAttribute("successMessage", ...)`. Dữ liệu này được lưu tạm thời vào `HttpSession` và tự động xóa ngay sau khi trang đích hoàn tất render lần đầu tiên, tránh hiện tượng submit lại form (PRG - Post/Redirect/Get pattern).
- **File dẫn chứng:** [SeatTypeController.java#L201](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L201).

#### Câu 35: Khi validation ở form thêm mới bị lỗi, các giá trị người dùng vừa nhập có bị mất không?
- **Trả lời:** Không bị mất. Controller lưu lại đối tượng form vào flash attribute: `redirectAttributes.addFlashAttribute("seatTypeForm", form)`, giúp form tự động fill lại các dữ liệu đã nhập trước đó.
- **File dẫn chứng:** [SeatTypeController.java#L103](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/controller/SeatTypeController.java#L103).

---

### NHÓM 3: CÂU HỎI NÂNG CAO, CƠ SỞ DỮ LIỆU & TÌNH HUỐNG BẪY THỰC TẾ (15 CÂU)

#### Câu 36: Vì sao trong bảng `dbo.Seat` lại có cả `SeatTypeID` lẫn `SeatTypeName`? Đây có phải là vi phạm chuẩn hóa cơ sở dữ liệu (Database Normalization) không?
- **Trả lời:** Về mặt lý thuyết chuẩn hóa thuần túy (3NF), đây là hiện tượng dư thừa dữ liệu (Denormalization). Tuy nhiên, đây là **chủ đích thiết kế kiến trúc thực tế**:
  1. Cột `SeatTypeID` đảm bảo tính toàn vẹn khóa ngoại (Referential Integrity).
  2. Cột `SeatTypeName` được giữ lại nhằm phục vụ tra cứu cực nhanh khi render sơ đồ ghế (Seat Map) hàng trăm nghìn ghế mà không bắt buộc phải JOIN bảng trong mọi trường hợp, đồng thời duy trì khả năng tương thích ngược (Backward Compatibility) với dữ liệu cũ của đồ án.
- **File dẫn chứng:** [00_full_database.sql#L320-L345](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql#L320-L345).

#### Câu 37: Khóa ngoại `FK_Seat_SeatType` trong cơ sở dữ liệu có đặt `ON DELETE CASCADE` không? Tại sao?
- **Trả lời:** **KHÔNG ĐẶT `ON DELETE CASCADE`**.
  - *Lý do:* Ghế phòng chiếu (`Seat`) là tài nguyên phần cứng vật lý cốt lõi, liên quan đến sơ đồ rạp và các vé đã bán trong quá khứ. Nếu đặt CASCADE, việc lỡ tay xóa một Seat Type sẽ lập tức xóa sạch toàn bộ các ghế trong rạp, kéo theo sụp đổ dữ liệu phòng chiếu và lịch sử giao dịch. Do đó hệ thống bắt buộc sử dụng cơ chế chặn (RESTRICT/NO ACTION) và kiểm tra nghiệp vụ tầng Service.
- **File dẫn chứng:** [00_full_database.sql#L335](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql#L335).

#### Câu 38: Ràng buộc kiểm tra `CK_SeatType_Multiplier` ở database được viết như thế nào?
- **Trả lời:**
  `CONSTRAINT CK_SeatType_Multiplier CHECK (PriceMultiplier BETWEEN 0.50 AND 3.00)`
- **File dẫn chứng:** [00_full_database.sql#L313](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql#L313).

#### Câu 39: Nếu người quản trị trực tiếp mở SQL Server Management Studio (SSMS) và đổi giá trị `PriceMultiplier` của VIP thành 5.00 thì database phản ứng ra sao?
- **Trả lời:** Lệnh `UPDATE` sẽ bị Database Engine từ chối ngay lập tức với lỗi vi phạm CHECK Constraint `CK_SeatType_Multiplier`, bảo vệ hệ thống tuyệt đối kể cả khi có can thiệp trực tiếp từ cơ sở dữ liệu ngoài ứng dụng.

#### Câu 40: Nếu một Manager đổi loại ghế của ghế A1 từ Standard sang VIP trong Seat Map, nhưng ghế A1 đó đã được một khách hàng mua vé trong suất chiếu tối nay thì sao?
- **Trả lời:** Hệ thống sẽ **TỪ CHỐI THAY ĐỔI**.
  Trong phương thức `updateSeat` và `batchUpdateSeats` của `SeatService.java`:
  ```java
  if (!seat.getSeatTypeName().equalsIgnoreCase(targetTypeName)
      && seatDao.isSeatBookedInUpcomingShowtimes(seatId, seat.getRoomId())) {
      throw new InvalidSeatLayoutException("Cannot change seat type because the seat has booked tickets in an upcoming showtime.");
  }
  ```
  Hệ thống ném ngoại lệ và giao dịch bị rollback hoàn toàn.
- **File dẫn chứng:** [SeatService.java#L58-L62](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/SeatService.java#L58-L62).

#### Câu 41: Làm thế nào `seatDao.isSeatBookedInUpcomingShowtimes` xác định được ghế đã có người đặt trong tương lai?
- **Trả lời:** Truy vấn SQL kiểm tra sự tồn tại trong `dbo.BookingSeat bs` nối với `dbo.Showtime sh`:
  ```sql
  SELECT COUNT(*) FROM dbo.BookingSeat bs
  JOIN dbo.Showtime sh ON sh.ShowtimeID = bs.ShowtimeID
  WHERE bs.SeatID = ? AND bs.Status IN ('HELD', 'BOOKED')
  AND sh.EndTime > GETDATE() AND sh.Status NOT IN ('CANCELLED', 'STOPPED')
  ```
  Nếu count > 0 nghĩa là ghế đang có giao dịch hợp lệ sắp chiếu.
- **File dẫn chứng:** [SeatDao.java](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatDao.java).

#### Câu 42: Tại sao phương thức `createSeatType` trong `TicketPricingService` lại cần annotation `@Transactional`?
- **Trả lời:** Phương thức này thực hiện nhiều thao tác logic: kiểm tra nghiệp vụ, gọi `insert`, sau đó gọi lại `findByCode` để lấy bản ghi vừa được tạo sinh ra từ `IDENTITY` cột ID. Đặt `@Transactional` giúp đảm bảo tính nguyên tử (Atomicity), nếu có bất kỳ ngoại lệ nào xảy ra trong chuỗi xử lý thì toàn bộ tác vụ sẽ được rollback, không để lại dữ liệu rác.
- **File dẫn chứng:** [TicketPricingService.java#L118](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/service/TicketPricingService.java#L118).

#### Câu 43: Nếu hai Manager cùng lúc nhấn cập nhật hệ số giá cho cùng một loại ghế, hệ thống xử lý tranh chấp thế nào?
- **Trả lời:** Nhờ vào câu lệnh SQL parameterized `UPDATE SeatType SET PriceMultiplier = ?, UpdatedAt = SYSUTCDATETIME() WHERE SeatTypeID = ?`, Database Engine sẽ sử dụng khóa dòng (Row-level Lock). Thao tác commit sau cùng sẽ ghi đè giá trị (Last Write Wins) và cập nhật chính xác mốc thời gian `UpdatedAt`.

#### Câu 44: Điểm khác nhau giữa `SeatTypeDao.isReferencedBySeats` và `SeatTypeDao.hasActiveOrUpcomingShowtimes` là gì?
- **Trả lời:**
  - `isReferencedBySeats`: Kiểm tra liên kết **Tĩnh** (Vật lý) - Xem có bất kỳ ghế nào trong bảng `Seat` đang mang `SeatTypeID` này hay không. Được dùng cho **Lệnh Xóa (DELETE)**.
  - `hasActiveOrUpcomingShowtimes`: Kiểm tra liên kết **Động** (Lịch chiếu) - Xem phòng chiếu chứa loại ghế này có suất chiếu nào đang mở bán hoặc sắp chiếu trong tương lai hay không. Được dùng cho **Lệnh Tắt Hoạt Động (INACTIVE)**.
- **File dẫn chứng:** [SeatTypeDao.java#L105-L125](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L105-L125).

#### Câu 45: Khi người dùng submit form thêm loại ghế, nếu trường `colorCode` bị null thì giá trị mặc định là gì?
- **Trả lời:** Giá trị mặc định là `#3B82F6` (Màu xanh dương chuẩn). Giá trị này được thiết lập tại khởi tạo trong [SeatTypeForm.java#L37](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dto/SeatTypeForm.java#L37) và được fallback an toàn tại [SeatTypeDao.java#L75](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/SeatTypeDao.java#L75): `form.getColorCode() != null ? form.getColorCode() : "#3B82F6"`.

#### Câu 46: Trong `SeatTypeApiControllerTest.java`, những trường hợp kiểm thử (Test cases) nào đã được viết?
- **Trả lời:**
  1. `testGetAllSeatTypes_Success`: Kiểm tra `GET /api/manager/seat-types` trả về HTTP 200 và JSON chứa mảng seat types.
  2. `testUpdateMultiplier_Success`: Kiểm tra `PUT /api/manager/seat-types/1/multiplier` cập nhật hệ số thành 1.30 thành công.
  3. `testCreateSeatType_ValidationError_Returns400`: Kiểm tra gửi body JSON sai định dạng (multiplier = 0.10, status sai) sẽ bị bắt lỗi và trả về HTTP 400 Bad Request.
- **File dẫn chứng:** [SeatTypeApiControllerTest.java#L38-L74](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/test/java/com/cinema/controller/api/SeatTypeApiControllerTest.java#L38-L74).

#### Câu 47: Trong `SeatTypeControllerTest.java`, có test case nào kiểm tra bảo mật phân quyền không?
- **Trả lời:** Có, test case `testListSeatTypes_Forbidden()` giả lập session có `userRole = "CUSTOMER"` và kiểm tra Controller trả về đúng mã lỗi `status().isForbidden()` (HTTP 403).
- **File dẫn chứng:** [SeatTypeControllerTest.java#L50-L54](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/test/java/com/cinema/controller/SeatTypeControllerTest.java#L50-L54).

#### Câu 48: Nếu cần thêm một loại ghế mới là "VIP Giường Nằm (BED)" với hệ số 2.20x thì cần thao tác những gì?
- **Trả lời:**
  1. Người quản lý vào màn hình Seat Type Management, nhấn `+ Add seat type`.
  2. Nhập Code: `BED`, Tên: `VIP Luxury Bed`, Hệ số: `2.20`, Chọn màu tím `#8B5CF6`, Status: `Active`.
  3. Bấm Save. Bản ghi được lưu vào `dbo.SeatType`.
  4. Sang màn hình Seat Map Management, chọn các ghế ở hàng cuối hoặc hàng đầu và gán loại ghế `VIP Luxury Bed`.
  5. Các suất chiếu mới được tạo tự động tính giá vé giường nằm = Giá gốc $\times$ 2.20 mà không cần sửa bất kỳ dòng code nào.

#### Câu 49: Tại sao trong bảng `dbo.SeatType` lại có 2 cột thời gian `CreatedAt` và `UpdatedAt`?
- **Trả lời:** Đây là chuẩn thiết kế **Audit Trail Pattern** (Dấu vết kiểm toán). `CreatedAt` ghi lại thời điểm bản ghi được tạo lập (lấy giờ UTC hệ thống qua `SYSUTCDATETIME()`), còn `UpdatedAt` ghi lại lần cuối cùng người quản lý can thiệp sửa đổi hệ số hay tên loại ghế, phục vụ đối soát và quản lý chất lượng dữ liệu.
- **File dẫn chứng:** [00_full_database.sql#L309-L310](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/00_full_database.sql#L309-L310).

#### Câu 50: Giả sử em được yêu cầu tối ưu hóa thêm cho module này trong tương lai, em sẽ đề xuất gì?
- **Trả lời:**
  1. **Audit Log chuyên sâu:** Thêm bảng `SeatTypePriceHistory` để lưu lại từng lần thay đổi hệ số giá (ai sửa, sửa từ mấy lên mấy, vào lúc nào).
  2. **Soft Delete (Xóa mềm):** Thay vì xóa cứng bản ghi, sử dụng cờ `IsDeleted` để bảo toàn lịch sử trích xuất dữ liệu phân tích BI (Business Intelligence) sau này.
  3. **SQL Pagination:** Thay thế việc phân trang trên bộ nhớ `subList()` bằng phân trang SQL `OFFSET...FETCH NEXT` để chuẩn hóa hoàn toàn theo tiêu chuẩn Enterprise.

---

# PHẦN 15: BẢNG PHẢN XẠ NHANH (ONE-PAGE CHEAT SHEET CHO SINH VIÊN)

Khi giảng viên chỉ tay ngẫu nhiên vào bất kỳ thành phần nào trên màn hình **Seat Type Management (Màn hình 57)**, hãy tra cứu ngay bảng phản xạ dưới đây:

| Vị trí / Nút bấm / Thành phần UI | File Code phụ trách | Method / Vị trí dòng Code | Câu trả lời chốt hạ cho Giảng viên |
| :--- | :--- | :--- | :--- |
| **Nút "+ Add seat type"** | `manager-seat-types.js` | Dòng 10-24 | "Xử lý Frontend mở modal `#seatTypeModal`, làm rỗng form và trỏ action về `/manager/seat-types/save`." |
| **Thẻ "Total seat types" (4)** | `SeatTypeController.java` | Dòng 55, 69 (`totalCount`) | "Lấy từ `seatTypes.size()` của danh sách lấy từ database qua DAO." |
| **Thẻ "Standard seat multiplier" (1.00x)** | `seat-types.html` | Dòng 112 | "Hệ số gốc chuẩn đối chiếu cho toàn bộ công thức giá vé theo quy chuẩn rạp." |
| **Thẻ "Average price multiplier" (1.25x)** | `seat-types.html` | Dòng 118 | "Tính động qua biểu thức Thymeleaf: `${#aggregates.avg(seatTypes.![priceMultiplier])}`. Giá trị là `(0.95+1.00+1.25+1.80)/4 = 1.25x`." |
| **Thẻ "Allowed multiplier range" (0.50-3.00)** | `SeatTypeForm.java` & DDL | Dòng 32-33 DTO, dòng 313 SQL | "Quy tắc BR-50 bảo vệ 3 tầng: DTO Validation, Service check và Database CHECK Constraint." |
| **Cột Code (FRONT, STD, VIP, COUPLE)** | `SeatType.java` | Cột `TypeCode` | "Mã định danh nghiệp vụ duy nhất (UNIQUE), viết hoa không dấu và bị khóa không cho sửa khi Edit." |
| **Cột Price multiplier (0.95x, 1.25x...)** | `TicketPricingService.java` | `calculateTicketUnitPrice` (L185-195) | "Hệ số nhân với giá cơ sở, sau đó làm tròn đến 1.000 VNĐ gần nhất (`HALF_UP`)." |
| **Cột Seat map color (#3B82F6...)** | `SeatDao.java` & `Seat.java` | `ColorCode` | "Mã màu HEX đại diện trên sơ đồ ghế để phân biệt trực quan các hạng ghế." |
| **Nút Edit (✎)** | `manager-seat-types.js` | Dòng 27-50 | "Đọc các `data-*` attribute trên nút, điền vào modal và trỏ action tới `/manager/seat-types/update/{id}`." |
| **Nút Delete (🗑)** | `SeatTypeController.java` | `deleteSeatType` (L190-207) | "Gửi POST `/delete/{id}`, kiểm tra `isReferencedBySeats`. Nếu có ghế dùng thì chặn xóa tuyệt đối." |
| **Nếu sửa hệ số giá thì vé đã bán có đổi giá không?** | `CustomerBookingDAO.java` & `BookingSeat` | Bảng `dbo.BookingSeat.UnitPrice` | "HOÀN TOÀN KHÔNG. Hệ thống áp dụng Price Snapshot Pattern, lưu đứt giá tại thời điểm đặt vào `UnitPrice` của `BookingSeat`." |
| **Nếu đổi loại ghế trong Seat Map của ghế đã có người mua thì sao?** | `SeatService.java` | `batchUpdateSeats` (L87-95) | "Bị chặn và ném `InvalidSeatLayoutException` bởi hàm `isSeatBookedInUpcomingShowtimes`." |
| **Kiểm tra quyền Manager ở đâu?** | `ManagerSecurityUtils.java` | `verifyManagerAccess(request)` | "Kiểm tra `session.getAttribute("userRole") == "MANAGER"`, nếu sai ném 403 Forbidden." |

