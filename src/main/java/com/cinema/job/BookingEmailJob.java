package com.cinema.job;

import com.cinema.model.BookingPaidEvent;
import com.cinema.service.BookingEmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingEmailJob {
    private final BookingEmailService emails;

    public BookingEmailJob(BookingEmailService emails) { this.emails = emails; }

    @Async("bookingEmailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCommitted(BookingPaidEvent event) {
        emails.sendConfirmation(event.bookingId());
    }

    @Scheduled(scheduler = "bookingEmailScheduler",
            fixedDelayString = "${app.booking-email.retry-delay-ms:60000}",
            initialDelayString = "${app.booking-email.retry-initial-delay-ms:30000}")
    public void retryUnsentConfirmations() {
        emails.recoverRecentPayments();
    }
}
