package com.cinema.job;

import com.cinema.dao.OtpTokenDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Bounded cleanup using the existing OtpToken table, without schema changes. */
@Component
public class CleanupExpiredOtpJob {
    private static final Logger log = LoggerFactory.getLogger(CleanupExpiredOtpJob.class);
    private final OtpTokenDAO tokens;

    public CleanupExpiredOtpJob(OtpTokenDAO tokens) { this.tokens = tokens; }

    @Scheduled(cron = "${app.jobs.cleanup-expired-otp.cron:0 0 * * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void cleanup() {
        int deleted = tokens.deleteExpiredHistoryBatch();
        if (deleted > 0) log.info("Removed {} expired OTP record(s) older than 7 days.", deleted);
    }
}
