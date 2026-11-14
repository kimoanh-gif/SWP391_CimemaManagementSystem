package com.cinema.service;

import com.cinema.dao.BookingEmailDAO;
import com.cinema.model.BookingEmailDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingReminderServiceTest {
    @TempDir Path temp;
    BookingEmailDAO dao = mock(BookingEmailDAO.class);
    JavaMailSender mail = mock(JavaMailSender.class);
    SystemSecuritySettings settings = mock(SystemSecuritySettings.class);
    Clock clock = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    LocalDateTime now = LocalDateTime.now(clock);
    BookingReminderService service;

    BookingReminderService instance(boolean enabled) {
        return new BookingReminderService(dao, mail,
                new BookingEmailDeliveryStore(temp.resolve("sent.properties").toString()), settings,
                enabled, 60, "cinema@example.com", clock);
    }

    @BeforeEach void setup() {
        service = instance(true);
        when(settings.read("SYSTEM_BOOKING_REMINDER_MINUTES", 60)).thenReturn(90);
        when(dao.findReminderBookingIds(now, 90)).thenReturn(List.of(42));
        when(dao.findReminderBooking(42, now, 90)).thenReturn(Optional.of(new BookingEmailDetails(
                42, "BK42", "customer@example.com", "An", "Movie", "Room 1", now.plusMinutes(80),
                "A1", null, BigDecimal.TEN)));
    }

    @Test void sendsUsingConfiguredWindowAndDoesNotRepeatAfterRestart() {
        service.sendDueReminders();
        instance(true).sendDueReminders();
        var capture = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(capture.capture());
        assertTrue(capture.getValue().getSubject().contains("Nhắc lịch"));
        assertTrue(capture.getValue().getText().contains("14/11/2026 20:20"));
        assertTrue(capture.getValue().getText().contains("A1"));
        verify(dao, times(2)).findReminderBookingIds(now, 90);
    }

    @Test void failedDeliveryRetriesAndThenStops() {
        doThrow(new MailSendException("offline")).doNothing().when(mail).send(any(SimpleMailMessage.class));
        service.sendDueReminders();
        service.sendDueReminders();
        service.sendDueReminders();
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test void cancelledOrStartedBookingIsSkippedOnRecheck() {
        when(dao.findReminderBooking(42, now, 90)).thenReturn(Optional.empty());
        service.sendDueReminders();
        verifyNoInteractions(mail);
    }

    @Test void confirmationDoesNotSuppressReminder() throws Exception {
        new BookingEmailDeliveryStore(temp.resolve("sent.properties").toString()).deliverOnce("42:BK42", () -> {});
        service.sendDueReminders();
        verify(mail).send(any(SimpleMailMessage.class));
    }

    @Test void disabledJobDoesNotReadOrSend() {
        clearInvocations(dao, settings);
        instance(false).sendDueReminders();
        verifyNoInteractions(dao, settings, mail);
    }

    @Test void databaseFailureDoesNotStopScheduler() {
        when(dao.findReminderBookingIds(now, 90)).thenThrow(new IllegalStateException());
        assertDoesNotThrow(service::sendDueReminders);
        verifyNoInteractions(mail);
    }
}
