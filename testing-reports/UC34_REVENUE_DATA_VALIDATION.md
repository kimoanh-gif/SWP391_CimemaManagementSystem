# BÁO CÁO ĐỐI SOÁT DỮ LIỆU DOANH THU & KẾ TOÁN (UC-34)
## CINEORA Cinema Management System — Revenue Data Validation & Reconciliation

---

## 1. CÔNG THỨC TÍNH TOÁN DOANH THU HỆ THỐNG

Dựa trên mã nguồn hiện hành tại [`RevenueReportDao.java`](file:///c:/Users/hoang/Downloads/SWP_RapPhim/src/main/java/com/cinema/dao/RevenueReportDao.java) và tài liệu quy tắc nghiệp vụ (BR-22, BR-24, BR-25, BR-37):

### 1.1. Công thức tổng doanh thu (Total Recognized Revenue)
$$\text{TotalRevenue} = \sum_{b \in \text{EligibleBookings}} b.\text{TotalAmount}$$

Trong đó:
- `b.TotalAmount`: Là số tiền thực tế khách hàng thanh toán cho đơn hàng (đã bao gồm tiền vé + tiền bắp nước F&B - tiền giảm giá voucher).
- $\text{EligibleBookings}$: Các đơn hàng thỏa mãn điều kiện công nhận doanh thu (Revenue Recognition Rule BR-22):
  $$\text{UPPER}(b.\text{Status}) \in \{'PAID', 'CONFIRMED', 'COMPLETED'\}$$
  và có ngày đặt vé $\text{CAST}(b.\text{BookingTime AS DATE})$ nằm trong khoảng ngày được chọn $[\text{StartDate}, \text{EndDate}]$.

### 1.2. Công thức doanh thu vé (Ticket Revenue)
$$\text{TicketRevenue} = \sum_{bs \in \text{BookingSeat}, b \in \text{EligibleBookings}} bs.\text{UnitPrice}$$

### 1.3. Công thức doanh thu bắp nước / F&B (F&B Revenue)
$$\text{FbRevenue} = \sum_{bfb \in \text{BookingFoodBeverage}, b \in \text{EligibleBookings}} (bfb.\text{Quantity} \times bfb.\text{UnitPrice})$$

### 1.4. Công thức tiền giảm giá (Discount Amount)
$$\text{DiscountAmount} = \sum_{b \in \text{EligibleBookings}} b.\text{DiscountAmount}$$

### 1.5. Đối soát Tổng quan kế toán (Accounting Sanity Check: Gross vs Net)
Trong cấu trúc đơn hàng của CINEORA:
$$b.\text{TotalAmount} = b.\text{Subtotal} - b.\text{DiscountAmount}$$
với $b.\text{Subtotal} = \text{Tiền Vé (Gross)} + \text{Tiền F\&B (Gross)}$.
Do đó:
$$\text{TotalRevenue} = \text{TicketRevenue} + \text{FbRevenue} - \text{DiscountAmount}$$

---

## 2. TRUY VẤN SQL THỰC TẾ TRONG HỆ THỐNG

### 2.1. Truy vấn tính toán 5 chỉ số KPI tổng quan (`RevenueReportDao.getSummary`)
```sql
-- 1. Tổng tiền đơn hàng, giảm giá và số đơn hợp lệ
SELECT
    COALESCE(SUM(b.TotalAmount), 0) AS TotalRevenue,
    COALESCE(SUM(b.DiscountAmount), 0) AS TotalDiscount,
    COALESCE(COUNT(DISTINCT b.BookingID), 0) AS TotalPaidBookings
FROM dbo.Booking b
WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
  AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?;

-- 2. Doanh thu F&B thực tế
SELECT COALESCE(SUM(bfb.Quantity * bfb.UnitPrice), 0) AS FbRevenue
FROM dbo.BookingFoodBeverage bfb
JOIN dbo.Booking b ON b.BookingID = bfb.BookingID
WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
  AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?;

-- 3. Số vé bán và doanh thu vé thực tế
SELECT
    COALESCE(COUNT(bs.BookingSeatID), 0) AS TotalTickets,
    COALESCE(SUM(bs.UnitPrice), 0) AS TicketRevenue
FROM dbo.BookingSeat bs
JOIN dbo.Booking b ON b.BookingID = bs.BookingID
WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
  AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?;
```

### 2.2. Truy vấn phân bổ theo ngày (`RevenueReportDao.getDailyRevenue`)
```sql
SELECT
    CAST(b.BookingTime AS DATE) AS ReportDate,
    COALESCE(SUM(b.TotalAmount), 0) AS TotalRevenue,
    COALESCE(SUM(b.DiscountAmount), 0) AS DiscountAmount,
    COALESCE(COUNT(DISTINCT b.BookingID), 0) AS BookingCount,
    COALESCE((
        SELECT SUM(bs.UnitPrice)
        FROM dbo.BookingSeat bs
        JOIN dbo.Booking b2 ON b2.BookingID = bs.BookingID
        WHERE UPPER(b2.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
          AND CAST(b2.BookingTime AS DATE) = CAST(b.BookingTime AS DATE)
    ), 0) AS TicketRevenue,
    COALESCE((
        SELECT COUNT(bs2.BookingSeatID)
        FROM dbo.BookingSeat bs2
        JOIN dbo.Booking b3 ON b3.BookingID = bs2.BookingID
        WHERE UPPER(b3.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
          AND CAST(b3.BookingTime AS DATE) = CAST(b.BookingTime AS DATE)
    ), 0) AS TicketCount,
    COALESCE((
        SELECT SUM(bfb.Quantity * bfb.UnitPrice)
        FROM dbo.BookingFoodBeverage bfb
        JOIN dbo.Booking b4 ON b4.BookingID = bfb.BookingID
        WHERE UPPER(b4.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
          AND CAST(b4.BookingTime AS DATE) = CAST(b.BookingTime AS DATE)
    ), 0) AS FbRevenue
FROM dbo.Booking b
WHERE UPPER(b.[Status]) IN ('PAID', 'CONFIRMED', 'COMPLETED')
  AND CAST(b.BookingTime AS DATE) BETWEEN ? AND ?
GROUP BY CAST(b.BookingTime AS DATE)
ORDER BY ReportDate DESC;
```

---

## 3. DỮ LIỆU ĐỐI SOÁT TRỰC TIẾP TỪ CSDL SQL SERVER THỰC TẾ

### 3.1. Dữ liệu bảng `dbo.Booking` (Tháng 10/2026)

| BookingID | BookingCode | ShowtimeID | TotalAmount | DiscountAmount | Status | BookingTime | Tính vào doanh thu? |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **5** | `BK261021110033D1FCFC` | 19 | 85,000.00 | 0.00 | `EXPIRED` | 2026-10-21 11:00:33 | ❌ **LOẠI BỎ** (Hết hạn giữ chỗ, chưa thanh toán) |
| **6** | `BK261021110343E937F2` | 19 | 408,000.00 | 0.00 | `CONFIRMED` | 2026-10-21 11:03:43 | ✅ **HỢP LỆ** |
| **7** | `BK26102111042100F016` | 19 | 408,000.00 | 0.00 | `CONFIRMED` | 2026-10-21 11:04:21 | ✅ **HỢP LỆ** |

**Tổng kết đơn hàng hợp lệ:**
- Số đơn hợp lệ = **2 đơn** (Booking 6, Booking 7).
- Tổng doanh thu = $408,000 + 408,000 = \mathbf{816,000\text{ VND}}$.
- Tổng giảm giá = $0 + 0 = \mathbf{0\text{ VND}}$.

---

### 3.2. Dữ liệu bảng `dbo.BookingSeat` (Chi tiết vé bán)

| BookingSeatID | BookingID | SeatID | ShowtimeID | UnitPrice | Status | Phim / Suất chiếu |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **7** | 6 | 2 (Ghế A2) | 19 | 85,000.00 | CONFIRMED | Avatar 2 (11:09) |
| **8** | 6 | 3 (Ghế A3) | 19 | 85,000.00 | CONFIRMED | Avatar 2 (11:09) |
| **9** | 7 | 4 (Ghế A4) | 19 | 85,000.00 | CONFIRMED | Avatar 2 (11:09) |
| **10** | 7 | 5 (Ghế A5) | 19 | 85,000.00 | CONFIRMED | Avatar 2 (11:09) |

**Tổng kết chi tiết vé:**
- Đơn 6: 2 vé $\times$ 85,000 = 170,000 VND.
- Đơn 7: 2 vé $\times$ 85,000 = 170,000 VND.
- Tổng số vé đã bán = **4 vé**.
- Tổng doanh thu vé xem phim = $170,000 + 170,000 = \mathbf{340,000\text{ VND}}$.

---

### 3.3. Dữ liệu bảng `dbo.BookingFoodBeverage` (Chi tiết bắp nước & F&B)

| BookingFoodBeverageID | BookingID | FoodBeverageItemID | Quantity | UnitPrice | Thành tiền |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **2** | 6 | 1 (Bắp rang bơ phô mai lớn) | 2 | 119,000.00 | 238,000.00 |
| **3** | 7 | 1 (Bắp rang bơ phô mai lớn) | 2 | 119,000.00 | 238,000.00 |

**Tổng kết chi tiết F&B:**
- Đơn 6: $2 \times 119,000 = 238,000\text{ VND}$.
- Đơn 7: $2 \times 119,000 = 238,000\text{ VND}$.
- Tổng doanh thu bắp nước / F&B = $238,000 + 238,000 = \mathbf{476,000\text{ VND}}$.

---

## 4. BẢNG SO SÁNH 3 CHIỀU: GIAO DIỆN UI vs CSDL SQL vs CSV EXPORT

| Chỉ số / Trường dữ liệu | Giá trị tính từ SQL CSDL | Giá trị hiển thị trên UI | Giá trị xuất ra CSV | Đánh giá độ đồng nhất |
| :--- | :---: | :---: | :---: | :---: |
| **Tổng doanh thu (Total Revenue)** | **816,000 VND** | **816,000 ₫** | **816,000** | ✅ **Trùng khớp 100%** |
| **Doanh thu vé (Ticket Revenue)** | **340,000 VND** | **340,000 ₫** | **340,000** | ✅ **Trùng khớp 100%** |
| **Doanh thu F&B (F&B Revenue)** | **476,000 VND** | **476,000 ₫** | **476,000** | ✅ **Trùng khớp 100%** |
| **Giảm giá (Discount Amount)** | **0 VND** | **0** | **0** | ✅ **Trùng khớp 100%** |
| **Tổng số vé bán (Tickets Sold)** | **4 vé** | **4** | **4** | ✅ **Trùng khớp 100%** |
| **Tổng số đơn hàng (Orders)** | **2 đơn** | **2** | **2** | ✅ **Trùng khớp 100%** |
| **Dòng ngày 21/10/2026 trong bảng** | Có (816k, 340k, 476k) | Có (816k, 340k, 476k) | Có (816k, 340k, 476k) | ✅ **Trùng khớp 100%** |
| **Dòng TOTAL chân bảng** | Bằng tổng dòng trên | 816k, 340k, 476k | Có trong khối tổng quan | ✅ **Trùng khớp 100%** |

---

## 5. CÁC ĐIỂM BẤT THƯỜNG VÀ ĐỀ XUẤT (DISCREPANCIES & RECOMMENDATIONS)

1. **Vấn đề định dạng CSV không bọc dấu ngoặc kép:**
   - Số liệu trong file CSV hoàn toàn chính xác về mặt toán học so với CSDL. Tuy nhiên, việc in số dạng `340,000` mà không bọc ngoặc kép `"340,000"` khiến file CSV bị vi phạm tiêu chuẩn RFC 4180 khi chứa ký tự phân cách `,` trong giá trị cột.
   - Khuyến nghị: Thay đổi trong `RevenueReportService.java` từ `df.format(amount)` thành `"\"" + df.format(amount) + "\""` hoặc xuất giá trị số thuần `m.getTicketRevenue().toPlainString()` để tương thích tốt nhất với Excel.
