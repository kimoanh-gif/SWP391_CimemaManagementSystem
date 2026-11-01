package com.cinema.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background job "Update Expired Promotions &amp; Vouchers" (M11, Iter3).
 *
 * Marks running or paused promotions whose end date has passed, and vouchers whose
 * valid-to time has passed, as EXPIRED. Drafts and stopped (INACTIVE) items are left
 * untouched. A manager can still extend the end date and activate the item again,
 * which ManagerPromotionService.checkTransition allows once the new end is in the future.
 */
@Component
public class ExpirePromotionsJob {

    private static final Logger log = LoggerFactory.getLogger(ExpirePromotionsJob.class);

    private final JdbcTemplate jdbcTemplate;

    public ExpirePromotionsJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelayString = "${app.jobs.expire-promotions.delay-ms:300000}", initialDelay = 20000)
    @Transactional
    public void expirePromotionsAndVouchers() {
        int promotions = jdbcTemplate.update("""
                UPDATE dbo.Promotion SET [Status] = 'EXPIRED'
                WHERE UPPER([Status]) IN ('ACTIVE', 'PAUSED') AND EndDate < SYSDATETIME()
                """);
        int vouchers = jdbcTemplate.update("""
                UPDATE dbo.Voucher SET [Status] = 'EXPIRED'
                WHERE UPPER([Status]) IN ('ACTIVE', 'PAUSED') AND ValidTo < SYSDATETIME()
                """);
        if (promotions > 0 || vouchers > 0) {
            log.info("Expired {} promotion(s) and {} voucher(s).", promotions, vouchers);
        }
    }
}
