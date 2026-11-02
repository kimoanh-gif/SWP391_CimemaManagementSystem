# Email lịch chiếu sau thanh toán

Hệ thống gửi **một email xác nhận thanh toán kèm lịch chiếu**, không gửi thêm email trước giờ chiếu.
Áp dụng cho booking `ONLINE` của Customer, đã `CONFIRMED` và có Payment `PAID`.
Email gồm mã booking, phim, ngày/giờ Việt Nam (UTC+7), phòng, ghế và tổng thanh toán.
Nếu có `PUBLIC_BASE_URL`, email kèm link vé điện tử yêu cầu đăng nhập.

## Luồng xử lý

1. VNPay IPN hợp lệ hoặc luồng thanh toán mô phỏng xác nhận payment và phát `BookingPaidEvent` trong transaction.
2. `BookingEmailJob` chỉ nhận event sau khi transaction commit thành công và chạy gửi trên executor riêng.
3. `BookingEmailDAO` đọc lại dữ liệu đã commit, bỏ qua booking không đủ điều kiện/hủy; chỉ chạy SELECT trên các bảng hiện có.
4. `BookingEmailService` gửi qua `JavaMailSender` và ghi dấu gửi thành công vào file cục bộ.
5. Job riêng kiểm tra lại payment trong 24 giờ gần nhất mỗi 60 giây để thử lại email lỗi hoặc event bị bỏ lỡ. Đây là retry email xác nhận, không phải email nhắc trước buổi chiếu.

Không thêm bảng, cột, migration hay bản ghi cấu hình vào database. Không dùng trạng thái booking/payment để đánh dấu email.
Lỗi SMTP hoặc lưu file không rollback payment đã hoàn tất. Payment thất bại, callback sai chữ ký/sai số tiền, booking tại quầy không phát sinh email Customer này.

## Cấu hình

| Biến môi trường | Ý nghĩa |
| --- | --- |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP hiện có của ứng dụng. |
| `BOOKING_EMAIL_ENABLED` | Mặc định `true`; đặt `false` để tắt gửi và retry. |
| `BOOKING_EMAIL_DELIVERY_FILE` | Mặc định `./var/booking-email/sent.properties`; thư mục phải ghi được. |
| `BOOKING_EMAIL_RETRY_DELAY_MS` | Mặc định `60000` ms giữa các đợt phục hồi. |
| `PUBLIC_BASE_URL` | Ví dụ `https://cinema.example.com`, để tạo link vé. Không đặt thì không gửi link localhost. |

Có thể ghi đè người gửi bằng property `app.booking-email.from`; mặc định dùng `spring.mail.username`.

## Giới hạn khi không lưu trạng thái email trong database

- Giữ nguyên file ledger trên ổ/volume bền vững khi restart hoặc deploy; xóa file có thể khiến email được gửi lại.
- Khi bật tính năng, các booking đủ điều kiện đã thanh toán trong 24 giờ gần nhất nhưng chưa có dấu gửi cũng được phục hồi.
- Nếu chạy nhiều instance, dùng chung đường dẫn ledger trên filesystem hỗ trợ khóa file; mỗi instance có file riêng có thể gửi trùng.
- Khóa file và ghi thay thế file giúp chống gửi lặp thông thường và khi restart. SMTP không cung cấp giao dịch nguyên tử với file: tiến trình chết sau khi SMTP nhận email nhưng trước khi lưu dấu có thể gây một lần gửi lại. Không cam kết exactly-once.
- Khôi phục lỗi tự động giới hạn ở payment trong 24 giờ; cần xử lý vận hành riêng nếu SMTP ngừng lâu hơn. File chỉ lưu booking ID/code và thời điểm gửi, không lưu email/mật khẩu.

## Kiểm tra thủ công

1. Dùng SMTP kiểm thử; bật tính năng và đặt ledger vào thư mục QA riêng.
2. Thanh toán thành công một booking online; kiểm tra email có đúng phim, lịch chiếu, phòng, ghế và tiền.
3. Gửi lại IPN/refresh trang, chạy retry và restart với cùng ledger: không nhận thêm email cho booking đó.
4. Thanh toán lỗi hoặc rollback transaction: không có email.
5. Làm SMTP thất bại: payment vẫn thành công. Khôi phục SMTP và chờ retry: email được gửi một lần.
6. Đặt `BOOKING_EMAIL_ENABLED=false`: không có email hoặc retry gửi.
