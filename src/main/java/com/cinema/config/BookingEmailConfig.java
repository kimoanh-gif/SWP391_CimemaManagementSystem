package com.cinema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
public class BookingEmailConfig {
    @Bean("bookingEmailExecutor")
    public ThreadPoolTaskExecutor bookingEmailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("booking-email-");
        // A saturated mail queue must never turn a committed payment into an IPN error.
        // Missed events are recovered by scanning recent paid bookings.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy());
        return executor;
    }

    @Bean("bookingEmailScheduler")
    public ThreadPoolTaskScheduler bookingEmailScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("booking-email-recovery-");
        return scheduler;
    }
}
