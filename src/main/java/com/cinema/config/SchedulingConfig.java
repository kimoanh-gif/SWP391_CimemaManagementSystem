package com.cinema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring Boot background scheduling for batch jobs (e.g., JOB-01: Release Expired Seat Reservations).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    @org.springframework.context.annotation.Bean("taskScheduler")
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("cinema-jobs-");
        return scheduler;
    }
}
