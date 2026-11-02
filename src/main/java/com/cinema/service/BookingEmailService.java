package com.cinema.service;

import com.cinema.dao.BookingEmailDAO;
import com.cinema.model.BookingEmailDetails;
import com.cinema.util.AccountValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class BookingEmailService {
    private static final Logger log = LoggerFactory.getLogger(BookingEmailService.class);
    private final BookingEmailDAO bookings;
    private final JavaMailSender mail;
    private final BookingEmailDeliveryStore deliveries;
    private final boolean enabled;
    private final String from;
    private final String publicBaseUrl;

    public BookingEmailService(BookingEmailDAO bookings, JavaMailSender mail, BookingEmailDeliveryStore deliveries,
                              @Value("${app.booking-email.enabled:true}") boolean enabled,
                              @Value("${app.booking-email.from:${spring.mail.username:}}") String from,
                              @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.bookings = bookings;
        this.mail = mail;
        this.deliveries = deliveries;
        this.enabled = enabled;
        this.from = from;
        this.publicBaseUrl = publicBaseUrl;
    }

    public void sendConfirmation(int bookingId) {
        if (!enabled) return;
        try {
            var booking = bookings.findPaidBooking(bookingId);
            if (booking.isEmpty()) return; // Recheck committed payment/booking status before sending.
            var details = booking.get();
            if (details.email() == null || !AccountValidation.EMAIL.matcher(details.email().trim()).matches()) {
                log.warn("Cannot send booking email: invalid recipient for booking {}", bookingId);
                return;
            }
            deliveries.deliverOnce(bookingId + ":" + details.bookingCode(), () -> mail.send(message(details)));
        } catch (Exception ex) {
            // Do not log SMTP exceptions: their text may contain customer addresses/message bodies.
            log.warn("Booking email delivery failed for booking {}; recovery will retry ({})",
                    bookingId, ex.getClass().getSimpleName());
        }
    }

    public void recoverRecentPayments() {
        if (!enabled) return;
        try {
            bookings.findRecentlyPaidBookingIds().forEach(this::sendConfirmation);
        } catch (RuntimeException ex) {
            log.warn("Booking email recovery could not load payments ({})", ex.getClass().getSimpleName());
        }
    }

    private SimpleMailMessage message(BookingEmailDetails b) {
        var message = new SimpleMailMessage();
        message.setTo(b.email().trim());
        if (from != null && !from.isBlank()) message.setFrom(from.trim());
        message.setSubject("[CINEORA] Thanh toán thành công - Lịch chiếu " + b.bookingCode());
        String ticketLink = "";
        if (publicBaseUrl != null && publicBaseUrl.matches("https?://[^\\s]+")) {
            ticketLink = "\nXem vé điện tử (cần đăng nhập): " + publicBaseUrl.replaceAll("/+$", "")
                    + "/customer/booking/tickets/" + b.bookingId() + "\n";
        }
        message.setText("""
                Xin chào %s,

                CINEORA đã nhận thanh toán thành công cho đơn đặt vé của bạn.
                Thông tin lịch chiếu:

                Mã đặt vé: %s
                Phim: %s
                Ngày chiếu: %s
                Giờ chiếu: %s (giờ Việt Nam, UTC+7)
                Phòng chiếu: %s
                Ghế: %s
                Tổng thanh toán: %s VND
                %s
                Vui lòng đến rạp trước giờ chiếu khoảng 15 phút và mở vé điện tử có mã QR
                trong tài khoản Customer để nhân viên kiểm tra khi vào phòng chiếu.

                Chúc bạn xem phim vui vẻ!
                CINEORA
                """.formatted(b.customerName() == null || b.customerName().isBlank() ? "bạn" : b.customerName(),
                b.bookingCode(), b.movieTitle(), b.startTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                b.startTime().format(DateTimeFormatter.ofPattern("HH:mm")), b.roomName(),
                b.seats() == null ? "Xem trong vé điện tử" : b.seats(),
                NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(b.totalAmount()), ticketLink));
        return message;
    }
}
