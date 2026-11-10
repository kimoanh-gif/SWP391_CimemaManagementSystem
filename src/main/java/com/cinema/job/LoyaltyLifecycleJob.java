package com.cinema.job;

import com.cinema.dao.LoyaltyPointDAO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Expires reward points and reviews membership at the start of each six-month cycle. */
@Component
public class LoyaltyLifecycleJob {

    private final JdbcTemplate jdbcTemplate;
    private final LoyaltyPointDAO loyaltyPointDAO;

    public LoyaltyLifecycleJob(JdbcTemplate jdbcTemplate, LoyaltyPointDAO loyaltyPointDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.loyaltyPointDAO = loyaltyPointDAO;
    }

    @Scheduled(cron = "${app.jobs.loyalty-lifecycle.cron:0 15 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void applyLifecycle() {
        expirePoints();
        loyaltyPointDAO.reviewTierCycle(null);
    }

    void expirePoints() {
        jdbcTemplate.update("""
                DECLARE @Expired TABLE(CustomerID INT, Points INT, SourceID INT);
                UPDATE dbo.LoyaltyTransaction
                SET ExpiredAt=SYSDATETIME()
                OUTPUT inserted.CustomerID,inserted.Points,inserted.LoyaltyTransactionID
                    INTO @Expired(CustomerID,Points,SourceID)
                WHERE TransactionType='EARN' AND ExpiredAt IS NULL
                  AND ExpiresAt IS NOT NULL AND ExpiresAt<SYSDATETIME();

                INSERT dbo.LoyaltyTransaction(CustomerID,BookingID,Points,TransactionType,[Description])
                SELECT CustomerID,NULL,-Points,'EXPIRE',CONCAT(N'Points expired from earning transaction #',SourceID)
                FROM @Expired;

                UPDATE c SET AvailablePoints=CASE WHEN c.AvailablePoints-x.Points<0 THEN 0
                                                  ELSE c.AvailablePoints-x.Points END
                FROM dbo.Customer c
                JOIN (SELECT CustomerID,SUM(Points) Points FROM @Expired GROUP BY CustomerID) x
                  ON x.CustomerID=c.CustomerID;
                """);
    }

}
