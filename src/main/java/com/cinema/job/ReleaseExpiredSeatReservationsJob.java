package com.cinema.job;

import com.cinema.dao.CustomerBookingDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background Job Inventory: JOB-01: Release Expired Seat Reservations (SRS 6.1 / Item 74).
 * Periodically releases temporarily held seats when the customer does not complete payment
 * within the allowed booking time (HOLD_MINUTES = 10).
 */
@Component
public class ReleaseExpiredSeatReservationsJob {

    private static final Logger log = LoggerFactory.getLogger(ReleaseExpiredSeatReservationsJob.class);

    private final CustomerBookingDAO bookingDAO;

    public ReleaseExpiredSeatReservationsJob(CustomerBookingDAO bookingDAO) {
        this.bookingDAO = bookingDAO;
    }

    /**
     * Scheduled periodic execution.
     * Default delay: 30 seconds (configurable via app.jobs.release-expired-seats.delay-ms).
     */
    @Scheduled(fixedDelayString = "${app.jobs.release-expired-seats.delay-ms:30000}", initialDelay = 10000)
    @Transactional
    public int execute() {
        try {
            int releasedCount = bookingDAO.releaseExpiredSeatReservations();
            if (releasedCount > 0) {
                log.info("JOB-01 [Release Expired Seat Reservations]: Successfully released {} expired seat hold(s).", releasedCount);
            } else {
                log.debug("JOB-01 [Release Expired Seat Reservations]: No expired seat reservations found.");
            }
            return releasedCount;
        } catch (Exception ex) {
            // SRS 6.1 failure behavior: Record failure and retry on the next scheduled run without crashing
            log.warn("JOB-01 [Release Expired Seat Reservations]: Error releasing expired seat holds. Will retry on next cycle. Reason: {}", ex.getMessage());
            return 0;
        }
    }
}
