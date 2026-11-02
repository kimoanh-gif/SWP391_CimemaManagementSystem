package com.cinema.service;

import com.cinema.dao.BookingEmailDAO;
import com.cinema.model.BookingEmailDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingEmailServiceTest {
    @TempDir Path temp;
    BookingEmailDAO dao;
    JavaMailSender mail;
    BookingEmailService service;

    @BeforeEach void setup() {
        dao = mock(BookingEmailDAO.class);
        mail = mock(JavaMailSender.class);
        service = instance(true, "https://cinema.example.com/");
    }

    BookingEmailService instance(boolean enabled, String url) {
        return new BookingEmailService(dao, mail,
                new BookingEmailDeliveryStore(temp.resolve("sent.properties").toString()), enabled,
                "cinema@example.com", url);
    }

    BookingEmailDetails details(String email) {
        return new BookingEmailDetails(42, "BK-QA42", email, "Nguyễn An", "Phim thử nghiệm", "Phòng 2",
                LocalDateTime.of(2026, 11, 3, 19, 30), "A1, A2", "2 x Large Coca-Cola, 1 x Cheese Popcorn",
                new BigDecimal("200000"));
    }

    @Test void sendsCorrectShowtimeAndDoesNotRepeatAfterRecoveryOrRestart() {
        when(dao.findPaidBooking(42)).thenReturn(Optional.of(details("customer@example.com")));
        when(dao.findRecentlyPaidBookingIds()).thenReturn(List.of(42));
        service.sendConfirmation(42);
        service.sendConfirmation(42);
        service.recoverRecentPayments();
        instance(true, "https://cinema.example.com").sendConfirmation(42);
        var capture = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, times(1)).send(capture.capture());
        var message = capture.getValue();
        assertArrayEquals(new String[]{"customer@example.com"}, message.getTo());
        assertEquals("cinema@example.com", message.getFrom());
        assertTrue(message.getSubject().contains("BK-QA42"));
        for (String value : List.of("Nguyễn An", "Phim thử nghiệm", "03/11/2026", "19:30", "UTC+7",
                "Phòng 2", "A1, A2", "2 x Large Coca-Cola", "1 x Cheese Popcorn", "200.000",
                "https://cinema.example.com/customer/booking/tickets/42")) {
            assertTrue(message.getText().contains(value), value);
        }
    }

    @Test void smtpFailureIsRetriedWithoutThrowingIntoPaymentFlow() {
        when(dao.findPaidBooking(42)).thenReturn(Optional.of(details("customer@example.com")));
        when(dao.findRecentlyPaidBookingIds()).thenReturn(List.of(42));
        doThrow(new MailSendException("temporary SMTP failure")).doNothing()
                .when(mail).send(any(SimpleMailMessage.class));
        assertDoesNotThrow(() -> service.sendConfirmation(42));
        service.recoverRecentPayments();
        instance(true, "").sendConfirmation(42);
        verify(mail, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test void ignoresUnpaidCancelledAndCounterBookingsExcludedByDao() {
        when(dao.findPaidBooking(42)).thenReturn(Optional.empty());
        service.sendConfirmation(42);
        verifyNoInteractions(mail);
    }

    @Test void invalidEmailDoesNotSendOrMarkDelivered() {
        when(dao.findPaidBooking(42)).thenReturn(Optional.of(details("invalid")),
                Optional.of(details("corrected@example.com")));
        service.sendConfirmation(42);
        verifyNoInteractions(mail);
        service.sendConfirmation(42);
        verify(mail).send(any(SimpleMailMessage.class));
    }

    @Test void disabledFeatureDoesNotQueryOrSend() {
        var disabled = instance(false, "");
        disabled.sendConfirmation(42);
        disabled.recoverRecentPayments();
        verifyNoInteractions(dao, mail);
    }

    @Test void databaseFailureDoesNotEscapeBackgroundJob() {
        when(dao.findRecentlyPaidBookingIds()).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        assertDoesNotThrow(service::recoverRecentPayments);
        verifyNoInteractions(mail);
    }

    @Test void noConfiguredPublicUrlMeansNoLocalhostLink() {
        when(dao.findPaidBooking(42)).thenReturn(Optional.of(details("customer@example.com")));
        instance(true, "").sendConfirmation(42);
        var capture = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(capture.capture());
        assertFalse(capture.getValue().getText().contains("http"));
    }
}
