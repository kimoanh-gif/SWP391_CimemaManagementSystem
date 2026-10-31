package com.cinema.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class LoyaltyPointDAO {
    private final JdbcTemplate jdbcTemplate;

    public LoyaltyPointDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureLifecycleSchema();
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
                        EXEC(N'ALTER TABLE dbo.LoyaltyTransaction DROP CONSTRAINT '+QUOTENAME(@CheckName));
                    ALTER TABLE dbo.LoyaltyTransaction WITH CHECK ADD CONSTRAINT CK_LoyaltyTransaction_Type
                        CHECK (TransactionType IN ('EARN','REDEEM','ADJUST','EXPIRE'));
                END;
                """);
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
