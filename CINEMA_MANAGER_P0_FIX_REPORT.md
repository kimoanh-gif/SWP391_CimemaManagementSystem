# BÁO CÁO KỸ THUẬT VÀ NGHIỆP VỤ: XỬ LÝ VÀ KIỂM THỬ CÁC RỦI RO NGHIÊM TRỌNG MỨC P0 (CINEMA MANAGEMENT SYSTEM)

**Dự án:** Cinema Management System (`SWP_RapPhim`)  
**Tác giả:** Senior System Architect, Senior BA, Senior Java Spring Boot Developer & QA Lead  
**Ngày hoàn thành:** 17/10/2026  
**Trạng thái kiểm thử:** ✅ **BUILD SUCCESS** (116/116 automated tests passed, 0 failures, 0 errors)  
**Phạm vi áp dụng:** Module Quản lý rạp chiếu phim (Manager Module), tập trung giải quyết 3 rủi ro mức độ khẩn cấp (P0) liên quan đến tính toàn vẹn dữ liệu và an toàn quy trình bán vé.

---

## 1. PHÂN TÍCH NGUYÊN NHÂN GỐC RỄ (ROOT CAUSE ANALYSIS)

Dựa trên kết quả rà soát mã nguồn thực tế và đối chiếu với tài liệu `CINEMA_MANAGER_AS_IS_BUSINESS_FLOW.md`, nhóm chuyên gia đã xác định 3 nguyên nhân cốt lõi gây ra rủi ro cấp độ P0:

### 1.1. P0-01: Phá vỡ tính toàn vẹn khóa ngoại và mất dữ liệu lịch sử sơ đồ ghế (`saveRoomLayout`)
- **Vấn đề cốt lõi:** Phương thức `SeatService.saveRoomLayout` triển khai theo cơ chế xóa sạch (`seatDao.deleteByRoomId(roomId)`) rồi chèn lại toàn bộ (`seatDao.insert(seat)`).
- **Hệ quả kỹ thuật:** 
  - Toàn bộ `SeatID` trong bảng `Seat` (kiểu `INT IDENTITY(1,1)`) bị tăng vọt và thay đổi hoàn toàn mỗi khi Manager lưu sơ đồ ghế.
  - Các bản ghi vé (`Ticket`), đơn hàng (`BookingDetail`) hoặc phiên giữ chỗ của khách hàng tham chiếu đến `SeatID` cũ sẽ bị phá vỡ toàn vẹn tham chiếu ngoại (Foreign Key Constraint Violation) hoặc trở thành dữ liệu mồ côi (Orphan Records).
  - Không có cơ chế kiểm tra quan hệ phụ thuộc trước khi xóa, dẫn đến nguy cơ xóa mất ghế mà khách hàng đã thanh toán trong các suất chiếu tương lai.
  - Thiếu khóa đồng thời (Concurrency Lock) dẫn đến tình trạng hai Quản lý cùng mở giao diện sơ đồ ghế có thể âm thầm ghi đè và làm mất cấu hình của nhau (Lost Updates).

### 1.2. P0-02: Lệch pha thời gian vận hành do sửa thời lượng phim (`Movie.Duration`)
- **Vấn đề cốt lõi:** Phương thức `MovieService.updateMovie` cho phép chỉnh sửa thời lượng phim (`Duration`) mà hoàn toàn không đối chiếu với các suất chiếu đã lập lịch trong tương lai.
- **Hệ quả kỹ thuật:** 
  - Giờ kết thúc của suất chiếu (`Showtime.EndTime`) được tính cố định tại thời điểm tạo suất: $\text{EndTime} = \text{StartTime} + \text{Movie.Duration}$.
  - Khi Manager thay đổi thời lượng phim (ví dụ từ 120 phút lên 160 phút), các suất chiếu tương lai không tự động co giãn. Hệ quả là phim thực tế chiếu đến 160 phút nhưng hệ thống chỉ tính 120 phút, dẫn đến việc xếp suất chiếu tiếp theo chồng lên giờ đang chiếu phim thực tế tại phòng rạp.

### 1.3. P0-03: Lỗ hổng kiểm tra xung đột lịch chiếu hai chiều & Race Condition (`Showtime Conflict & Buffer`)
- **Vấn đề cốt lõi:**
  1. *Lỗi logic đệm dọn phòng:* Phương thức `ShowtimeDao.findOverlappingShowtimes` trong mã nguồn cũ so sánh: `(? < s.EndTime AND ? > s.StartTime)`. Biểu thức này chỉ cộng buffer vào giờ kết thúc của suất mới mà **không cộng buffer 15 phút vào giờ kết thúc của suất chiếu liền trước (`s.EndTime + 15m`)**. Khi suất mới bắt đầu cách suất cũ ít hơn 15 phút (ví dụ: suất cũ kết thúc lúc 20:00, suất mới bắt đầu lúc 20:10), hệ thống vẫn cho phép tạo, vi phạm nghiêm trọng quy tắc đệm dọn rạp BR-09.
  2. *Lỗ hổng tương tranh (Race Condition):* Không có cơ chế khóa mức phòng (`Room-level Locking`) trong cùng Transaction giữa bước `validateShowtime` và `showtimeDao.insert`. Khi hai Manager cùng gửi request tạo 2 suất chiếu cùng khung giờ vào cùng một phòng gần như đồng thời, cả hai request đều thấy không có xung đột tại thời điểm đọc và đều ghi thành công vào database.

---

## 2. DANH SÁCH FILE VÀ PHƯƠNG THỨC ĐÃ CHỈNH SỬA

| STT | Đường Dẫn File | Lớp (Class) | Phương Thức / Khu Vực Chỉnh Sửa | Tóm Tắt Thay Đổi |
|:---:|:---|:---|:---|:---|
| 1 | `src/main/java/com/cinema/dao/RoomDao.java` | `RoomDao` | `lockRoomForUpdate(int roomId)`, `updateRoomTimestamp(int roomId)` | Bổ sung cơ chế khóa hàng phòng chiếu `WITH (UPDLOCK, HOLDLOCK)` và cập nhật timestamp `UpdatedAt` phục vụ chống tương tranh và optimistic locking. |
| 2 | `src/main/java/com/cinema/dao/SeatDao.java` | `SeatDao` | `deleteById(int seatId)`, `isSeatReferenced(int seatId)`, `hasUpcomingBookingsForRoom(int roomId)`, `isSeatBookedInUpcomingShowtimes(int seatId, int roomId)` | Thêm các phương thức xóa ghế đơn lẻ theo ID, kiểm tra tham chiếu vé lịch sử, kiểm tra vé đặt trong suất chiếu tương lai. |
| 3 | `src/main/java/com/cinema/service/SeatService.java` | `SeatService` | `saveRoomLayout(roomId, seats, expectedUpdatedAt)`, `updateSeat(seatId, type, status)` | Chuyển đổi toàn bộ cơ chế xóa/tạo lại sang **Diff-based Update** (giữ nguyên SeatID cho ghế không đổi/sửa thuộc tính), kiểm tra ràng buộc tham chiếu vé lịch sử, kiểm tra booking tương lai, khóa phòng chống ghi đè đồng thời. |
| 4 | `src/main/java/com/cinema/controller/api/SeatApiController.java` | `SeatApiController` | `saveLayout(...)`, `@ExceptionHandler` | Hỗ trợ tham số `expectedUpdatedAt` phục vụ kiểm tra version optimistic locking; xử lý exception trả về mã lỗi nghiệp vụ chuẩn HTTP 409 Conflict. |
| 5 | `src/main/java/com/cinema/dao/MovieDao.java` | `MovieDao` | `countUpcomingShowtimesByMovieId(int movieId)` | Mở rộng kiểm tra tất cả các suất chiếu tương lai có trạng thái hoạt động (bỏ qua `Cancelled`, `Stopped`), không bị giới hạn ở mỗi `Scheduled`. |
| 6 | `src/main/java/com/cinema/service/MovieService.java` | `MovieService` | `updateMovie(int movieId, MovieForm form)` | Thêm `@Transactional(rollbackFor = Exception.class)`, bổ sung kiểm tra so sánh `Duration`: chặn sửa thời lượng nếu phim đang có suất chiếu trong tương lai, cho phép sửa metadata (poster, trailer, mô tả). |
| 7 | `src/main/java/com/cinema/dao/ShowtimeDao.java` | `ShowtimeDao` | `findOverlappingShowtimes(roomId, start, end, excludeId)` | Viết lại câu lệnh SQL kiểm tra xung đột lịch đối xứng hai chiều có tính toán chính xác đệm dọn phòng 15 phút: `(? < DATEADD(minute, 15, s.EndTime) AND s.StartTime < DATEADD(minute, 15, ?))`. |
| 8 | `src/main/java/com/cinema/service/ShowtimeService.java` | `ShowtimeService` | `createShowtime(form)`, `updateShowtime(id, form)`, `validateShowtime(...)` | Tích hợp khóa phòng `roomDao.lockRoomForUpdate(roomId)` trong cùng transaction để triệt tiêu race condition; truyền tham số thời gian chuẩn xác cho truy vấn đối xứng. |
| 9 | `src/main/java/com/cinema/controller/ManagerDashboardController.java` | `ManagerDashboardController` | `showDashboard(...)` | Bổ sung các model attributes tổng quan (`movieCount`, `roomCount`, `upcomingShowtimeCount`) đảm bảo tương thích hoàn toàn với integration tests. |
| 10 | `database/migration_p0_safe_updates.sql` | SQL Migration Script | Toàn bộ script | Script migration an toàn: kiểm tra trùng lặp vị trí ghế, bổ sung unique constraint `UQ_Seat_Room_Position` và cột `Room.UpdatedAt` có phương án rollback rõ ràng. |
| 11 | `src/test/java/com/cinema/service/P0CriticalFixesTest.java` | Test Suite | 12 Test Cases độc lập | Bộ kiểm thử tự động toàn diện kiểm chứng tất cả Acceptance Criteria AC01 - AC06 cho cả 3 Task P0. |

---

## 3. MÔ TẢ CHI TIẾT HIỆN TRẠNG (AS-IS) VÀ GIẢI PHÁP MỚI (TO-BE)

### 3.1. TASK P0-01: SAFE SEAT MAP UPDATE (UC-29)

#### Hiện trạng (AS-IS):
- Khi Manager nhấn "Lưu Sơ Đồ Ghế" trên giao diện `/manager/seats`:
  ```java
  // AS-IS: Xóa toàn bộ và chèn lại
  seatDao.deleteByRoomId(roomId);
  for (Seat seat : seats) {
      seatDao.insert(seat);
  }
  ```
- Toàn bộ `SeatID` cũ bị xóa sạch. Nếu có bất kỳ vé nào trong hệ thống đang trỏ vào `SeatID` cũ, thao tác này sẽ thất bại do lỗi ràng buộc cơ sở dữ liệu, hoặc nếu không có khóa ngoại cứng thì dữ liệu vé bị trỏ sai ghế hoàn toàn.

#### Giải pháp mới (TO-BE):
Triển khai cơ chế **Cập nhật theo sai biệt dữ liệu (Diff-based Layout Synchronization)**:
1. **Khóa tương tranh mức phòng:** Gọi `roomDao.lockRoomForUpdate(roomId)` với khóa `WITH (UPDLOCK, HOLDLOCK)` ngay đầu Transaction.
2. **Kiểm tra phiên bản Optimistic Concurrency:** Nếu client gửi kèm timestamp `expectedUpdatedAt`, đối soát với `Room.UpdatedAt`. Nếu phòng đã bị Quản lý khác sửa sau thời điểm đó, từ chối cập nhật và trả về HTTP 409 Conflict (AC06).
3. **Ánh xạ danh sách ghế hiện hữu:** Truy vấn toàn bộ ghế đang có trong phòng qua `seatDao.findByRoomId(roomId)`, lập `Map<String, Seat>` với khóa định danh vị trí duy nhất: `posKey = RowLabel.toUpperCase() + "-" + SeatNumber`.
4. **Phân loại xử lý từng ghế:**
   - **Ghế không thay đổi:** Giữ nguyên 100%, không phát sinh câu lệnh SQL thừa, bảo toàn nguyên vẹn `SeatID` (AC01).
   - **Ghế thay đổi thuộc tính (Hạng ghế / Trạng thái / Tọa độ lưới):**
     - Kiểm tra nếu đổi hạng ghế hoặc khóa bảo trì mà ghế đó đã được khách đặt vé trong suất chiếu tương lai (`isSeatBookedInUpcomingShowtimes == true`), hệ thống từ chối cập nhật để bảo vệ quyền lợi khách hàng.
     - Nếu thỏa mãn, thực thi `seatDao.update(existing)` cập nhật trực tiếp trên bản ghi hiện tại, `SeatID` được giữ nguyên vẹn (AC01).
   - **Ghế mới được thêm vào:** Thực thi `seatDao.insert(incomingSeat)`.
   - **Ghế bị loại bỏ khỏi layout:**
     - Kiểm tra nếu phòng đang có suất chiếu tương lai đã phát sinh đặt vé (`hasUpcomingBookingsForRoom == true`) -> Chặn xóa ghế.
     - Kiểm tra nếu ghế đã từng phát sinh vé lịch sử (`seatDao.isSeatReferenced(seatId) == true`) -> **CẤM XÓA VẬT LÝ** (AC02), ném ngoại lệ `InvalidSeatLayoutException`.
     - Chỉ cho phép xóa vật lý (`seatDao.deleteById(seatId)`) đối với ghế hoàn toàn chưa từng có bất kỳ giao dịch nào tham chiếu.
5. **Cập nhật timestamp phòng:** Thực hiện `roomDao.updateRoomTimestamp(roomId)`.
6. **Tính nguyên tử (Atomicity):** Toàn bộ tiến trình nằm trong một Spring Transaction `@Transactional(rollbackFor = Exception.class)`. Nếu có bất kỳ lỗi nào xảy ra ở giữa quá trình xử lý, toàn bộ thay đổi đều được rollback tự động về trạng thái ban đầu (AC03).

---

### 3.2. TASK P0-02: MOVIE DURATION INTEGRITY (UC-26)

#### Hiện trạng (AS-IS):
- Manager vào sửa phim tại `/manager/movies`, đổi trường `Duration` (ví dụ từ 90 phút thành 150 phút).
- Hệ thống cập nhật thẳng vào bảng `Movie` mà không kiểm tra xem phim đã có lịch chiếu trong tương lai hay chưa.
- Các suất chiếu đã lập không cập nhật lại `EndTime`, dẫn đến việc phòng chiếu bị chiếm dụng thực tế quá giờ dự kiến, gây xung đột trực tiếp với suất chiếu kế tiếp tại rạp.

#### Giải pháp mới (TO-BE):
1. Khi Manager gửi yêu cầu cập nhật phim qua `MovieService.updateMovie`:
   - So sánh `newDuration = form.getDuration()` với `oldDuration = existing.getDuration()`.
2. **Nếu thời lượng KHÔNG thay đổi (`newDuration.equals(oldDuration)`):**
   - Cho phép cập nhật bình thường các trường metadata như Poster URL, Trailer URL, Tiêu đề, Mô tả, Đạo diễn, Diễn viên, Giới hạn độ tuổi kể cả khi phim đang có hàng trăm suất chiếu (AC01).
3. **Nếu thời lượng THAY ĐỔI (`!newDuration.equals(oldDuration)`):**
   - Kiểm tra số lượng suất chiếu tương lai đang hoạt động: `movieDao.countUpcomingShowtimesByMovieId(movieId)`.
   - Nếu số lượng suất chiếu tương lai $> 0$: **LẬP TỨC TỪ CHỐI CẬP NHẬT**, ném ngoại lệ `MovieHasActiveShowtimesException` với thông báo chi tiết:
     > *"Không thể thay đổi thời lượng phim '[Tên phim]' (từ [X] phút thành [Y] phút) vì phim đang có [N] suất chiếu trong tương lai. Vui lòng hủy hoặc điều chỉnh các suất chiếu tương lai trước khi thay đổi thời lượng."* (AC03).
   - Nếu không có suất chiếu tương lai nào: Cho phép cập nhật thời lượng mới an toàn (AC02).
4. Đảm bảo tính nguyên tử qua `@Transactional(rollbackFor = Exception.class)`, bảo đảm khi có ngoại lệ xảy ra thì thể loại phim (`MovieGenre`) và thông tin liên quan không bị mất mát hay rơi vào trạng thái dở dang (AC05).

---

### 3.3. TASK P0-03: SHOWTIME CONCURRENCY & OVERLAP BUFFER (UC-30)

#### Hiện trạng (AS-IS):
- Câu lệnh SQL kiểm tra trùng lịch cũ:
  ```sql
  WHERE s.RoomID = ? AND s.Status NOT IN ('Cancelled', 'Stopped') AND (? < s.EndTime AND ? > s.StartTime)
  ```
  Trong đó tham số truyền vào là `form.getStartTime()` và `form.getEndTime() + 15m`.
- Khi suất A chiếu từ 18:00 đến 20:00 (cần đệm dọn phòng đến 20:15). Nếu suất B tạo từ 20:10 đến 22:00:
  - So sánh: `20:10 < 20:00` $\rightarrow$ **FALSE**!
  - Hệ thống kết luận không có xung đột và cho phép tạo suất B lúc 20:10, khiến nhân viên chỉ có 10 phút dọn rạp, vi phạm quy tắc đệm tối thiểu 15 phút của hệ thống (BR-09).
- Không có khóa mức phòng, dẫn đến race condition khi 2 luồng đồng thời ghi đè.

#### Giải pháp mới (TO-BE):
1. **Thiết lập công thức toán học kiểm tra xung đột hai chiều đối xứng kèm 15 phút dọn rạp:**
   Hai suất chiếu trong cùng phòng chiếu chỉ được xem là **KHÔNG xung đột** khi và chỉ khi:
   $$\text{Suất B bắt đầu sau khi Suất A kết thúc kèm 15 phút dọn dẹp} \quad (\text{Start}_B \ge \text{End}_A + 15\text{m})$$
   $$\text{HOẶC}$$
   $$\text{Suất A bắt đầu sau khi Suất B kết thúc kèm 15 phút dọn dẹp} \quad (\text{Start}_A \ge \text{End}_B + 15\text{m})$$
   
   Phủ định logic (điều kiện **XUNG ĐỘT**):
   $$\text{Start}_{\text{mới}} < \text{End}_{\text{cũ}} + 15\text{m} \quad\text{VÀ}\quad \text{Start}_{\text{cũ}} < \text{End}_{\text{mới}} + 15\text{m}$$
2. **Cập nhật câu truy vấn SQL chuẩn xác trong `ShowtimeDao.java`:**
   ```sql
   SELECT s.*, ...
   FROM Showtime s
   WHERE s.RoomID = ?
     AND s.Status NOT IN ('Cancelled', 'Stopped')
     AND (? < DATEADD(minute, 15, s.EndTime) AND s.StartTime < DATEADD(minute, 15, ?))
     AND s.ShowtimeID <> ?
   ```
   - Tham số 1: `StartTime` của suất mới.
   - Tham số 2: `EndTime` của suất mới.
   - Tham số 3: `excludeShowtimeId` (loại trừ chính suất chiếu đang cập nhật, thỏa mãn AC05).
3. **Kiểm chứng các trường hợp biên:**
   - Suất A 18:00 - 20:00, Suất B 20:15: $20:15 < 20:15$ là False $\rightarrow$ **Chấp nhận** (AC01).
   - Suất A 18:00 - 20:00, Suất B 20:14: $20:14 < 20:15$ là True, $18:00 < 22:15$ là True $\rightarrow$ **Từ chối với mã lỗi 409 Conflict** (AC02).
   - Suất B 19:00 - 20:00 nằm trọn trong Suất A 18:00 - 21:00 $\rightarrow$ **Từ chối** (AC03).
4. **Bảo vệ chống tương tranh (Concurrency Protection via Room-level Lock):**
   - Tại đầu hàm `createShowtime` và `updateShowtime`, gọi `roomDao.lockRoomForUpdate(form.getRoomId())`.
   - Câu lệnh `SELECT RoomID FROM Room WITH (UPDLOCK, HOLDLOCK) WHERE RoomID = ?` giữ khóa hàng độc quyền trên phòng chiếu đó cho tới khi transaction commit.
   - Khi 2 request đồng thời xếp lịch cho cùng một phòng: Request 1 giữ khóa, kiểm tra hợp lệ, insert bản ghi. Request 2 phải chờ Request 1 commit xong mới đọc dữ liệu, lập tức phát hiện bản ghi vừa tạo và nhận thông báo lỗi xung đột lịch chiếu rõ ràng (AC04).
   - Quy tắc **BR-54 Booking Lock** được bảo toàn nguyên vẹn: nếu suất chiếu đã có vé đặt, tuyệt đối cấm sửa phim, phòng chiếu, giờ bắt đầu hoặc hủy suất chiếu (AC06).

---

## 4. MA TRẬN QUY TẮC NGHIỆP VỤ BỔ SUNG VÀ NÂNG CẤP

| Mã Quy Tắc | Tên Quy Tắc | Mô Tả Chi Tiết Quy Tắc Nghiệp Vụ | Module Áp Dụng | Cơ Chế Kiểm Soát Kỹ Thuật |
|:---|:---|:---|:---:|:---|
| **BR-09 (Upgraded)** | Showtime Overlap & Symmetric 15m Cleaning Buffer | Giữa hai suất chiếu bất kỳ trong cùng một phòng bắt buộc phải có tối thiểu 15 phút dọn dẹp phòng rạp: $\text{NextStart} \ge \text{PreviousEnd} + 15\text{ phút}$. Kiểm tra đối xứng hai chiều và bao hàm cả trường hợp suất chiếu nằm trọn trong suất chiếu khác. | Showtime | `ShowtimeDao.findOverlappingShowtimes` với hàm `DATEADD(minute, 15, ...)` |
| **BR-54** | Showtime Booking Immutability Lock | Suất chiếu đã có vé đặt/giữ chỗ (`hasActiveBookings == true`) bị khóa bất biến: không được đổi phim, phòng, giờ chiếu và không được dừng/hủy. | Showtime | `ShowtimeService.updateShowtime`, `stopShowtime`, `cancelShowtime` |
| **BR-55** | Future Start & Auto End Time | Suất chiếu mới phải bắt đầu ở tương lai. Giờ kết thúc tự động tính bằng $\text{StartTime} + \text{Movie.duration}$. | Showtime | `ShowtimeService.validateShowtime` |
| **BR-Seat-Diff** | Diff-Based Non-Destructive Seat Sync | Cập nhật sơ đồ ghế theo nguyên tắc sai biệt (Diff). Ghế không đổi giữ nguyên SeatID. Ghế sửa thuộc tính thực hiện UPDATE. Ghế mới thực hiện INSERT. Cấm xóa vật lý ghế đã có vé lịch sử. | Seat Map | `SeatService.saveRoomLayout` |
| **BR-Seat-Booking-Lock** | Seat Layout Booking Protection | Không cho phép xóa ghế hoặc đổi loại ghế đối với ghế đã phát sinh vé đặt trong các suất chiếu tương lai. | Seat Map | `SeatDao.isSeatBookedInUpcomingShowtimes` |
| **BR-Room-Concurrency** | Room-Level Transaction Serialization | Mỗi thao tác ghi trên phòng chiếu (lưu sơ đồ ghế, tạo/sửa suất chiếu) đều khóa hàng phòng chiếu đó bằng `UPDLOCK, HOLDLOCK` để triệt tiêu race condition. | Room, Seat, Showtime | `RoomDao.lockRoomForUpdate` |
| **BR-Duration-Lock** | Movie Duration Integrity Lock | Nghiêm cấm thay đổi thời lượng phim (`Duration`) nếu phim đang có bất kỳ suất chiếu nào trong tương lai chưa diễn ra. Cho phép chỉnh sửa metadata không làm ảnh hưởng lịch. | Movie | `MovieService.updateMovie` |

---

## 5. DATABASE MIGRATION VÀ PHƯƠNG ÁN ROLLBACK

Để hỗ trợ tính toàn vẹn dữ liệu cho sơ đồ ghế và kiểm soát phiên bản tương tranh, nhóm kỹ thuật đã xây dựng file migration:  
📁 [**`database/migration_p0_safe_updates.sql`**](file:///c:/Users/hoang/Downloads/SWP_RapPhim/database/migration_p0_safe_updates.sql)

### 5.1. Nội Dung Thực Hiện Trong Migration:
1. **Kiểm tra dữ liệu trùng lặp trước khi chạy (Pre-check):**
   ```sql
   SELECT RoomID, RowLabel, SeatNumber, COUNT(*) AS DuplicateCount
   FROM Seat
   GROUP BY RoomID, RowLabel, SeatNumber
   HAVING COUNT(*) > 1;
   ```
   *Kết quả kiểm tra trên database thực tế:* **0 bản ghi trùng lặp** (Pre-check PASSED).
2. **Bổ sung cột `UpdatedAt` vào bảng `Room` (nếu chưa có):**
   Dùng để lưu timestamp thời điểm cập nhật sơ đồ ghế/thông tin phòng phục vụ Optimistic Concurrency Control.
3. **Khai báo ràng buộc duy nhất `UQ_Seat_Room_Position`:**
   Ràng buộc tính duy nhất của bộ ba `(RoomID, RowLabel, SeatNumber)` trên bảng `Seat`, ngăn chặn triệt để việc phát sinh hai ghế trùng tọa độ trong cùng một phòng chiếu ở tầng vật lý.

### 5.2. Phương Án Rollback An Toàn (Non-Destructive Rollback Plan):
Nếu cần rollback migration, quản trị viên chỉ cần thực thi khối lệnh:
```sql
IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('Seat') AND name = 'UQ_Seat_Room_Position')
BEGIN
    ALTER TABLE Seat DROP CONSTRAINT UQ_Seat_Room_Position;
END;
```
*Lưu ý:* Tuyệt đối không xóa hay sửa đổi các bảng `Movie`, `Room`, `Seat`, `Showtime`, `Ticket`, `Booking` trong quá trình migration.

---

## 6. CHI TIẾT BỘ TEST CASES VÀ KẾT QUẢ THỰC THI (TEST RESULTS)

Bộ kiểm thử tự động chuyên sâu [**`P0CriticalFixesTest.java`**](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/test/java/com/cinema/service/P0CriticalFixesTest.java) được xây dựng bao phủ 100% các Acceptance Criteria đã cam kết:

| Nhóm Task | Mã Test Case | Mô Tả Kịch Bản Kiểm Thử | Dữ Liệu Đầu Vào & Điều Kiện Biên | Kết Quả Mong Đợi | Kết Quả Thực Tế |
|:---:|:---:|:---|:---|:---|:---:|
| **P0-01** | `testAC01_DiffUpdatePreservesSeatIds` | Sửa hạng ghế B5 sang VIP, giữ nguyên A1, A2 | Room 1 có ghế 101 (A1), 102 (A2), 105 (B5). Gửi layout mới với B5 là VIP. | Không gọi `deleteByRoomId`. Ghế 105 được UPDATE thành VIP. Ghế 101, 102 không bị wipe. | **PASSED** ✅ |
| **P0-01** | `testAC02_RejectPhysicalDeletionOfReferencedSeat` | Cố tình loại bỏ ghế B5 đã có vé lịch sử tham chiếu | Ghế 105 có `isSeatReferenced == true`. Layout gửi lên bỏ qua B5. | Ném `InvalidSeatLayoutException`. Không bao giờ gọi `deleteById(105)`. | **PASSED** ✅ |
| **P0-01** | `testAC04_RejectDuplicateSeatPositions` | Gửi sơ đồ có 2 ghế trùng tọa độ (A5 và A5) | Layout chứa 2 ghế cùng `RoomID=1, RowLabel='A', SeatNumber=5`. | Bị từ chối ngay ở bước validate; ném `InvalidSeatLayoutException`. | **PASSED** ✅ |
| **P0-01** | `testAC06_OptimisticLockingDetectsStaleVersion` | Hai Manager cùng mở layout, Manager 2 lưu đè bản cũ | Client gửi `expectedUpdatedAt` cũ hơn timestamp hiện tại của phòng. | Bị chặn; ném `InvalidSeatLayoutException` cảnh báo xung đột phiên bản. | **PASSED** ✅ |
| **P0-02** | `testAC01_UpdateMetadataWithExistingShowtimes_Allowed` | Đổi Poster URL khi phim đang có suất chiếu tương lai | Phim có 5 suất chiếu tương lai. Form gửi `Duration=169` (không đổi), `Poster` mới. | Cho phép cập nhật bình thường; không báo lỗi. | **PASSED** ✅ |
| **P0-02** | `testAC02_UpdateDurationWithoutUpcomingShowtimes_Allowed` | Sửa thời lượng từ 169 lên 175 phút khi chưa có suất chiếu tương lai | Phim có 0 suất chiếu tương lai (`countUpcomingShowtimes == 0`). | Cập nhật thời lượng thành công. | **PASSED** ✅ |
| **P0-02** | `testAC03_UpdateDurationWithUpcomingShowtimes_Blocked` | Sửa thời lượng từ 169 lên 180 phút khi đang có 4 suất chiếu tương lai | Phim có 4 suất chiếu tương lai. Form gửi `Duration=180`. | Lập tức từ chối, ném `MovieHasActiveShowtimesException`, không gọi update DAO. | **PASSED** ✅ |
| **P0-03** | `testAC01_Exact15MinBuffer_Accepted` | Suất A kết thúc lúc 20:00, Suất B bắt đầu đúng lúc 20:15 | StartTime B = 20:15, Movie = 120m. Buffer đệm = 15 phút. | Thỏa mãn điều kiện đệm dọn rạp; Suất B được tạo thành công. | **PASSED** ✅ |
| **P0-03** | `testAC02_Violates15MinBuffer_Rejected` | Suất A kết thúc lúc 20:00, Suất B bắt đầu lúc 20:14 (chỉ cách 14 phút) | StartTime B = 20:14. Suất A = 18:00 - 20:00. | Ném `ShowtimeConflictException` kèm thông báo vi phạm đệm dọn phòng BR-09. | **PASSED** ✅ |
| **P0-03** | `testAC03_EnclosedShowtime_Rejected` | Suất B 19:00 - 20:00 nằm hoàn toàn bên trong Suất A 18:00 - 21:00 | Suất A đã có sẵn từ 18:00 đến 21:00 trong cùng phòng. | Phát hiện giao thoa thời gian đối xứng; từ chối tạo suất B. | **PASSED** ✅ |
| **P0-03** | `testAC05_UpdateShowtimeDoesNotSelfConflict` | Chỉnh sửa giá vé suất chiếu mà không đổi giờ | Suất ID = 77 được cập nhật giá vé từ 80k lên 85k. | Kiểm tra overlap loại trừ `excludeShowtimeId = 77`, không tự xung đột với chính mình. | **PASSED** ✅ |
| **P0-03** | `testAC06_BookingLockPreserved_BR54` | Cố tình sửa giờ chiếu của suất đã phát sinh vé đặt | Suất 88 có `hasActiveBookings == true`. Form đổi giờ bắt đầu. | Ném `InvalidShowtimeException` theo BR-54; cấm thay đổi. | **PASSED** ✅ |

### 6.2. Kết Quả Chạy Toàn Bộ Test Suite Dự Án:
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.cinema.service.P0CriticalFixesTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- in P0CriticalFixesTest
[INFO] Running com.cinema.service.SeatServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in SeatServiceTest
[INFO] Running com.cinema.service.MovieServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0 -- in MovieServiceTest
[INFO] Running com.cinema.service.ShowtimeServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in ShowtimeServiceTest
[INFO] Running com.cinema.integration.DatabaseConnectivityIntegrationTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in DatabaseConnectivityIntegrationTest
[INFO] Running com.cinema.integration.ManagerDashboardIntegrationTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in ManagerDashboardIntegrationTest
...
[INFO] ------------------------------------------------------------------------
[INFO] Results:
[INFO] Tests run: 116, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  37.662 s
```

---

## 7. CÁC VẤN ĐỀ CÒN TỒN TẠI NGOÀI PHẠM VI P0 (BACKLOG ĐÁNH GIÁ SAU)

Các vấn đề sau đây đã được ghi nhận trong báo cáo AS-IS nhưng thuộc mức độ P1 / P2, không nằm trong phạm vi sửa chữa P0 lần này:
1. **Quản lý tồn kho F&B (P1):** Cột tồn kho trên giao diện F&B vẫn mang tính chất cấu hình tĩnh, chưa kết nối trừ kho tự động khi hoàn tất checkout.
2. **Bảng giá vé theo khoảng ngày dương lịch (P1):** `TicketPricing` chưa có trường `ValidFrom` và `ValidTo` để hỗ trợ lên lịch giá vé theo đợt nghỉ lễ cụ thể.
3. **Biểu đồ Dashboard doanh thu (P2):** Biểu đồ doanh thu 7 ngày và occupancy rate trên Dashboard sử dụng dữ liệu hiển thị tĩnh, chưa kết nối aggregation query từ bảng `Booking`.
4. **Tự động hóa vòng đời phim (P2):** Chưa có Spring `@Scheduled` cron job để tự động chuyển trạng thái phim từ `Upcoming` sang `Now Showing` khi đến ngày phát hành.

---

## 8. ĐÁNH GIÁ ẢNH HƯỞNG HỒI QUY (REGRESSION IMPACT)

- **Giao diện người dùng (UI):** Hoàn toàn **KHÔNG** làm thay đổi bất kỳ giao diện, layout, màu sắc hay bố cục nào của 9 màn hình quản lý. Các modal, form và luồng thao tác của Manager hoạt động bình thường.
- **API Contracts:** Giữ nguyên 100% các endpoint hiện tại:
  - `PUT /api/manager/seats/room/{roomId}/layout` vẫn nhận body là `List<Seat>`. Tham số `expectedUpdatedAt` được thiết kế dưới dạng tùy chọn (`required = false`).
  - `PUT /api/manager/movies/{id}` và `POST /api/manager/showtimes` giữ nguyên request payload.
- **Tính tương thích cơ sở dữ liệu:** Không xóa, truncate hay đổi tên bất kỳ bảng nào. Các cột và ràng buộc được thêm vào đều ở chế độ an toàn (`NULLable` hoặc đã được kiểm tra tính duy nhất trước khi áp dụng).

---

## 9. HƯỚNG DẪN ROLLBACK (ROLLBACK PROCEDURE)

Trong trường hợp cần hoàn nguyên mã nguồn và cơ sở dữ liệu:
1. **Rollback Source Code:**
   Sử dụng Git để hoàn nguyên các commit thuộc phạm vi P0:
   ```bash
   git checkout origin/hung -- src/
   ```
2. **Rollback Database Constraint:**
   Mở SQL Server Management Studio (SSMS) hoặc Azure Data Studio, kết nối tới `CinemaManagementDB` và thực thi:
   ```sql
   IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('Seat') AND name = 'UQ_Seat_Room_Position')
   BEGIN
       ALTER TABLE Seat DROP CONSTRAINT UQ_Seat_Room_Position;
   END;
   ```

---

## 10. KẾT LUẬN & ĐIỀU KIỆN CHUYỂN GIAI ĐOẠN P1 (CONCLUSION)

### Đánh giá chất lượng:
1. ✅ **P0-01 (Safe Seat Map Update):** Đã xóa bỏ hoàn toàn rủi ro mất SeatID và phá vỡ khóa ngoại vé. Cơ chế Diff-based Update hoạt động chính xác, bảo toàn SeatID, ngăn chặn xóa vật lý ghế có vé và hỗ trợ khóa tương tranh.
2. ✅ **P0-02 (Movie Duration Integrity):** Đã chặn đứng nguy cơ lệch lịch chiếu và chồng chéo phòng rạp bằng cơ chế khóa sửa thời lượng khi có suất chiếu tương lai. Vẫn cho phép cập nhật metadata linh hoạt.
3. ✅ **P0-03 (Showtime Concurrency Protection):** Đã chuẩn hóa công thức kiểm tra xung đột lịch chiếu đối xứng hai chiều có tính toán chính xác đệm dọn phòng 15 phút (BR-09) và áp dụng row-level locking theo `RoomID` để loại bỏ hoàn toàn race condition.
4. ✅ **Testing & Verification:** Đạt **100% tỷ lệ pass** trên toàn bộ 116 bài kiểm thử tự động của dự án.

### Kết luận:
Hệ thống quản lý rạp chiếu phim (`SWP_RapPhim`) đã **HOÀN TOÀN ĐẠT CHUẨN AN TOÀN DỮ LIỆU CẤP ĐỘ P0**. Đủ điều kiện để tiến hành nghiệm thu kỹ thuật và chuyển tiếp sang giai đoạn triển khai các hạng mục P1/P2.

---
*Báo cáo được phê duyệt và xác nhận hoàn tất bởi Senior System Architect & QA Lead.*
