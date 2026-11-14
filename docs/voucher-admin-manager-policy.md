# Chính sách voucher: Admin → Manager → Customer

Admin lưu giới hạn vào SystemSetting, nhóm VOUCHER. Không tạo voucher khi lưu chính sách.
Manager xem chính sách trên danh sách, chi tiết và form voucher; form mới điền sẵn điều kiện tối thiểu và giới hạn sử dụng.
Server đọc lại chính sách khi tạo/sửa voucher hoặc kích hoạt DRAFT; không dựa vào giới hạn ở trình duyệt.

| Khóa | Ý nghĩa |
| --- | --- |
| VOUCHER_VALIDITY_DAYS | Số ngày tối đa từ ValidFrom đến ValidTo, không tính từ ngày tạo |
| VOUCHER_MIN_BOOKING_AMOUNT | Sàn giá trị đơn tối thiểu Manager phải đặt |
| VOUCHER_MAX_DISCOUNT_PERCENT | Phần trăm giảm tối đa của voucher PERCENT |
| VOUCHER_MAX_DISCOUNT_AMOUNT | Trần tiền giảm cho voucher PERCENT; 0 không đặt trần chính sách |
| VOUCHER_TOTAL_LIMIT | Số lượt sử dụng tối đa cho mỗi voucher, không phải số mã voucher |
| VOUCHER_PER_USER_LIMIT | Số lượt sử dụng tối đa mỗi tài khoản cho mỗi voucher |

Khi có trần tiền, Manager phải lưu MaxDiscountAmount dương không vượt trần.
FIXED vẫn giới hạn 1.000.000 VND theo quy tắc hiện có và không chịu trần dành cho PERCENT.
Các giới hạn lượt dùng không cho phép bỏ trống để vượt chính sách bằng giá trị unlimited.
Thiếu cấu hình dùng mặc định trong VoucherPolicyService; cấu hình đang hoạt động có giá trị không hợp lệ sẽ chặn lưu voucher.

## Khi Admin thay đổi chính sách

- Voucher tạo/sửa sau đó và lần kích hoạt bản nháp phải theo chính sách mới.
- Voucher đã phát hành không tự bị sửa: Customer dùng điều kiện đã lưu trên voucher.
- Chi tiết và form voucher hiển thị cảnh báo nếu vượt chính sách hiện hành.
- Manager có thể tạm dừng/ngừng voucher cũ; kích hoạt lại voucher PAUSED giữ điều kiện đã phát hành.
- Muốn sửa voucher cũ phải đáp ứng toàn bộ chính sách hiện hành; quy tắc khóa code/discount sau khi sử dụng vẫn giữ nguyên.
- Không cập nhật ngược giá trị booking đã thanh toán, không migration hay cập nhật hàng loạt voucher cũ.

## Kiểm tra thủ công bằng dữ liệu hiện có

1. Admin lưu 30%, trần 50.000 VND. Manager tải lại form: thấy chính sách và trần điền sẵn.
2. Thử lưu 40%, trần 60.000, hạn quá dài, đơn tối thiểu thấp hoặc lượt dùng quá cao: bị từ chối, kể cả gửi POST trực tiếp.
3. Lưu voucher 20%, trần 50.000 hợp lệ. Với đơn 400.000 VND, Customer được giảm 50.000 VND.
4. Admin hạ trần còn 20.000 VND: voucher đã phát hành vẫn giảm tối đa 50.000; Manager xem chi tiết thấy cảnh báo.
5. Voucher mới phải có trần <=20.000; bản nháp cũ không thể kích hoạt nếu vượt chính sách.
6. Kiểm tra booking đã thanh toán không thay đổi số tiền giảm.

Unit tests dùng mock, không tạo dữ liệu trong database và không gọi SMTP.
