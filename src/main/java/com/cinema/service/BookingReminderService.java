package com.cinema.service;

import com.cinema.dao.BookingEmailDAO;
import com.cinema.util.AccountValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** JOB-06: remind customers once before their upcoming confirmed, paid showtime. */
@Service
public class BookingReminderService {
    private static final Logger log = LoggerFactory.getLogger(BookingReminderService.class);
    private final BookingEmailDAO bookings;
    private final JavaMailSender mail;
    private final BookingEmailDeliveryStore deliveries;
    private final SystemSecuritySettings settings;
    private final boolean enabled;
    private final int defaultMinutes;
    private final String from;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public BookingReminderService(BookingEmailDAO bookings, JavaMailSender mail,
                                 BookingEmailDeliveryStore deliveries, SystemSecuritySettings settings,
                                 @Value("${app.booking-reminder.enabled:true}") boolean enabled,
                                 @Value("${app.booking-reminder.minutes-before:60}") int defaultMinutes,
                                 @Value("${app.booking-email.from:${spring.mail.username:}}") String from) {
        this(bookings, mail, deliveries, settings, enabled, defaultMinutes, from,
                Clock.system(ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    BookingReminderService(BookingEmailDAO bookings, JavaMailSender mail,
                           BookingEmailDeliveryStore deliveries, SystemSecuritySettings settings,
                           boolean enabled, int defaultMinutes, String from, Clock clock) {
        this.bookings = bookings;
        this.mail = mail;
        this.deliveries = deliveries;
        this.settings = settings;
        this.enabled = enabled;
        this.defaultMinutes = defaultMinutes;
        this.from = from;
        this.clock = clock;
    }

    @Scheduled(scheduler = "bookingEmailScheduler",
            fixedDelayString = "${app.booking-reminder.delay-ms:60000}",
            initialDelayString = "${app.booking-reminder.initial-delay-ms:30000}")
    public void sendDueReminders() {
        if (!enabled) return;
        try {
            int minutes = settings.read("SYSTEM_BOOKING_REMINDER_MINUTES", defaultMinutes);
            for (int id : bookings.findReminderBookingIds(LocalDateTime.now(clock), minutes)) {
                sendReminder(id, minutes);
            }
        } catch (RuntimeException ex) {
            log.warn("Booking reminders could not load settings/bookings ({})", ex.getClass().getSimpleName());
        }
    }

    private void sendReminder(int id, int minutes) {
        try {
            // Recheck status and time immediately before sending, including on retries.
            var candidate = bookings.findReminderBooking(id, LocalDateTime.now(clock), minutes);
            if (candidate.isEmpty()) return;
            var b = candidate.get();
            if (b.email() == null || !AccountValidation.EMAIL.matcher(b.email().trim()).matches()) return;
            var message = new SimpleMailMessage();
            message.setTo(b.email().trim());
            if (from != null && !from.isBlank()) message.setFrom(from.trim());
            message.setSubject("[CINEORA] Nhắc lịch xem phim - " + b.bookingCode());
            message.setText("""
                    Xin chào %s,

                    Suất chiếu của bạn sắp bắt đầu.
                    Mã đặt vé: %s
                    Phim: %s
                    Lịch chiếu: %s (giờ Việt Nam, UTC+7)
                    Phòng chiếu: %s
                    Ghế: %s

                    Vui lòng đến rạp trước giờ chiếu khoảng 15 phút và chuẩn bị vé điện tử.
                    Chúc bạn xem phim vui vẻ!
                    CINEORA
                    """.formatted(b.customerName() == null ? "bạn" : b.customerName(), b.bookingCode(),
                    b.movieTitle(), b.startTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    b.roomName(), b.seats() == null ? "Xem trong vé điện tử" : b.seats()));
            deliveries.deliverOnce("reminder:" + id + ":" + b.bookingCode(), () -> mail.send(message));
        } catch (Exception ex) {
            log.warn("Booking reminder failed for booking {}; will retry before showtime ({})",
                    id, ex.getClass().getSimpleName());
        }
    }
}
