package com.cinema.service;

import com.cinema.job.BookingEmailJob;
import com.cinema.model.BookingPaidEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;

import static org.mockito.Mockito.*;

class BookingEmailTransactionTest {
    @Configuration
    @EnableTransactionManagement
    @Import(com.cinema.config.BookingEmailConfig.class)
    static class Config {
        @Bean BookingEmailService emails() { return mock(BookingEmailService.class); }
        @Bean BookingEmailJob job(BookingEmailService emails) { return new BookingEmailJob(emails); }
        @Bean AbstractPlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object tx, TransactionDefinition definition) {}
                protected void doCommit(DefaultTransactionStatus status) {}
                protected void doRollback(DefaultTransactionStatus status) {}
            };
        }
    }

    @Test void emailIsTriggeredOnlyAfterCommitAndNeverOnRollback() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var emails = context.getBean(BookingEmailService.class);
            var template = new TransactionTemplate(context.getBean(AbstractPlatformTransactionManager.class));
            template.executeWithoutResult(status -> {
                context.publishEvent(new BookingPaidEvent(42));
                verifyNoInteractions(emails);
            });
            verify(emails, timeout(2000)).sendConfirmation(42);
            clearInvocations(emails);
            template.executeWithoutResult(status -> {
                context.publishEvent(new BookingPaidEvent(43));
                status.setRollbackOnly();
            });
            context.publishEvent(new BookingPaidEvent(44)); // No committed transaction.
            verifyNoInteractions(emails);
        }
    }
}
