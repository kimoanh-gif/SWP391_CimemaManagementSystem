package com.cinema.job;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Applies point expiry and six-month membership cycles, following Shopee-style rules. */
@Component
public class LoyaltyLifecycleJob {

    private final JdbcTemplate jdbcTemplate;

    public LoyaltyLifecycleJob(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "${app.jobs.loyalty-lifecycle.cron:0 15 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void applyLifecycle() {
        expirePoints();
        startNewTierCycle();
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

    void startNewTierCycle() {
        jdbcTemplate.update("""
                DECLARE @CycleStart DATE=DATEFROMPARTS(YEAR(GETDATE()),CASE WHEN MONTH(GETDATE())<=6 THEN 1 ELSE 7 END,1);
                DECLARE @BaseRank INT=(SELECT TOP 1 RankID FROM dbo.MembershipRank WHERE IsActive=1 ORDER BY MinPoints);
                UPDATE dbo.Customer SET TierPoints=0,RankID=@BaseRank,TierCycleStart=@CycleStart
                WHERE TierCycleStart<>@CycleStart;
                """);
    }
}
