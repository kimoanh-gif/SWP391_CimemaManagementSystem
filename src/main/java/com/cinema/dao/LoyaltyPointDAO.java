package com.cinema.dao;

import com.cinema.model.MembershipRank;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class LoyaltyPointDAO {
    private final JdbcTemplate jdbcTemplate;

    public LoyaltyPointDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureLoyaltySchema();
        ensureLifecycleSchema();
    }

    private void ensureLoyaltySchema() {
        try {
            jdbcTemplate.execute("""
                    IF OBJECT_ID(N'dbo.LoyaltyTransaction',N'U') IS NULL
                    BEGIN
                      CREATE TABLE dbo.LoyaltyTransaction(
                        LoyaltyTransactionID INT IDENTITY(1,1) PRIMARY KEY,CustomerID INT NOT NULL,
                        BookingID INT NULL,Points INT NOT NULL,TransactionType VARCHAR(20) NOT NULL,
                        [Description] NVARCHAR(250) NOT NULL,CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
                        ExpiresAt DATETIME2 NULL,ExpiredAt DATETIME2 NULL,
                        CONSTRAINT FK_LoyaltyTx_Customer FOREIGN KEY(CustomerID) REFERENCES dbo.Customer(CustomerID),
                        CONSTRAINT FK_LoyaltyTx_Booking FOREIGN KEY(BookingID) REFERENCES dbo.Booking(BookingID));
                      CREATE UNIQUE INDEX UX_LoyaltyTransaction_BookingEarn ON dbo.LoyaltyTransaction(BookingID,TransactionType)
                        WHERE BookingID IS NOT NULL AND TransactionType='EARN';
                    END
                    """);
        } catch (DataAccessException ignored) {
            // Managed deployments create this table through 00_full_database.sql.
        }
    }

    /** Keeps existing installations compatible without requiring a full database rebuild. */
    private void ensureLifecycleSchema() {
        jdbcTemplate.execute("""
                IF COL_LENGTH(N'dbo.Customer', N'TierCycleStart') IS NULL
                BEGIN
                    EXEC(N'ALTER TABLE dbo.Customer ADD TierCycleStart DATE NULL');
                    EXEC(N'UPDATE dbo.Customer SET TierCycleStart=DATEFROMPARTS(YEAR(GETDATE()),CASE WHEN MONTH(GETDATE())<=6 THEN 1 ELSE 7 END,1) WHERE TierCycleStart IS NULL');
                    EXEC(N'ALTER TABLE dbo.Customer ALTER COLUMN TierCycleStart DATE NOT NULL');
                    EXEC(N'ALTER TABLE dbo.Customer ADD CONSTRAINT DF_Customer_TierCycleStart DEFAULT (DATEFROMPARTS(YEAR(GETDATE()),CASE WHEN MONTH(GETDATE())<=6 THEN 1 ELSE 7 END,1)) FOR TierCycleStart');
                END;

                IF COL_LENGTH(N'dbo.LoyaltyTransaction', N'ExpiresAt') IS NULL
                    ALTER TABLE dbo.LoyaltyTransaction ADD ExpiresAt DATETIME2 NULL;
                IF COL_LENGTH(N'dbo.LoyaltyTransaction', N'ExpiredAt') IS NULL
                    ALTER TABLE dbo.LoyaltyTransaction ADD ExpiredAt DATETIME2 NULL;

                IF NOT EXISTS (
                    SELECT 1 FROM sys.check_constraints
                    WHERE parent_object_id=OBJECT_ID(N'dbo.LoyaltyTransaction')
                      AND definition LIKE '%EXPIRE%')
                BEGIN
                    DECLARE @CheckName SYSNAME;
                    SELECT TOP 1 @CheckName=[name] FROM sys.check_constraints
                    WHERE parent_object_id=OBJECT_ID(N'dbo.LoyaltyTransaction')
                      AND definition LIKE '%TransactionType%';
                    IF @CheckName IS NOT NULL
                    BEGIN
                        DECLARE @DropCheckSql NVARCHAR(500)=N'ALTER TABLE dbo.LoyaltyTransaction DROP CONSTRAINT '+QUOTENAME(@CheckName);
                        EXEC sys.sp_executesql @DropCheckSql;
                    END;
                    ALTER TABLE dbo.LoyaltyTransaction WITH CHECK ADD CONSTRAINT CK_LoyaltyTransaction_Type
                        CHECK (TransactionType IN ('EARN','REDEEM','ADJUST','EXPIRE'));
                END;
                """);
    }

    /**
     * Review all customers, or only the customer whose booking is being confirmed.
     * Starting points equal the qualifying rank's threshold so checkout and the
     * customer profile continue to resolve the same rank from TierPoints.
     */
    public void reviewTierCycle(Integer bookingId) {
        jdbcTemplate.update("""
                DECLARE @BookingID INT=?;
                DECLARE @Now DATE=CONVERT(DATE,GETDATE());
                DECLARE @CycleStart DATE=DATEFROMPARTS(YEAR(@Now),CASE WHEN MONTH(@Now)<=6 THEN 1 ELSE 7 END,1);
                DECLARE @PreviousStart DATE=DATEADD(MONTH,-6,@CycleStart);
                DECLARE @CustomerID INT=(SELECT CustomerID FROM dbo.Booking WHERE BookingID=@BookingID);

                UPDATE c SET TierPoints=qualified.MinPoints,RankID=qualified.RankID,TierCycleStart=@CycleStart
                FROM dbo.Customer c
                CROSS APPLY (
                    SELECT COUNT_BIG(*) AS OrderCount,SUM(b.TotalAmount) AS Spend
                    FROM dbo.Booking b
                    CROSS APPLY (
                        SELECT MIN(COALESCE(p.PaidAt,p.CreatedAt)) AS PaidTime
                        FROM dbo.Payment p
                        WHERE p.BookingID=b.BookingID AND p.[Status]='PAID'
                    ) paid
                    WHERE b.CustomerID=c.CustomerID AND b.[Status]='CONFIRMED'
                      AND paid.PaidTime>=@PreviousStart AND paid.PaidTime<@CycleStart
                ) activity
                CROSS APPLY (
                    SELECT TOP 1 r.RankID,r.MinPoints
                    FROM dbo.MembershipRank r
                    CROSS APPLY (VALUES (TRY_CONVERT(INT,JSON_VALUE(
                        CASE WHEN ISJSON(r.Benefits)=1 THEN r.Benefits END,'$.cycleMinOrders')))) policy(MinOrders)
                    WHERE r.IsActive=1 AND (
                        r.RankID=(SELECT TOP 1 RankID FROM dbo.MembershipRank WHERE IsActive=1 ORDER BY MinPoints,RankID)
                        OR (activity.OrderCount>0 AND (
                            activity.Spend>=CONVERT(DECIMAL(19,0),r.MinPoints)*?
                            OR (policy.MinOrders>0 AND activity.OrderCount>=policy.MinOrders)
                        ))
                    )
                    ORDER BY r.MinPoints DESC,r.RankID
                ) qualified
                WHERE c.TierCycleStart<@CycleStart AND (@BookingID IS NULL OR c.CustomerID=@CustomerID);
                """, bookingId, MembershipRank.CYCLE_SPEND_VND_PER_POINT);
    }

    /** The nearest unexpired earning batch, grouped by its expiry timestamp. */
    public Optional<ExpiringPoints> findNextExpiry(int customerId) {
        return jdbcTemplate.query("""
                SELECT TOP 1 SUM(Points) AS Points, ExpiresAt
                FROM dbo.LoyaltyTransaction
                WHERE CustomerID=? AND TransactionType='EARN' AND ExpiredAt IS NULL
                  AND ExpiresAt IS NOT NULL AND ExpiresAt>=SYSDATETIME()
                GROUP BY ExpiresAt
                ORDER BY ExpiresAt
                """, (rs, row) -> new ExpiringPoints(
                        rs.getInt("Points"), rs.getObject("ExpiresAt", LocalDateTime.class)), customerId)
                .stream().findFirst();
    }

    public record ExpiringPoints(int points, LocalDateTime expiresAt) {}
}
