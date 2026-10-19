package com.cinema.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring Boot background scheduling for batch jobs (e.g., JOB-01: Release Expired Seat Reservations).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
