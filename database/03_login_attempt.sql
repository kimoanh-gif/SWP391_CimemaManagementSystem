-- =====================================================================
-- 03_login_attempt.sql
-- Audit log for every login attempt (success / failure / lockout).
-- Used by AuthService for rate-limiting and brute-force detection.
-- Run after 01_roles_and_seed.sql.
-- =====================================================================
USE CinemaManagementDB;
GO

IF OBJECT_ID('dbo.LoginAttempt', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoginAttempt (
        AttemptID       BIGINT IDENTITY(1,1) NOT NULL,
        Identifier      NVARCHAR(255) NOT NULL,   -- email or username (lowercased, trimmed)
        IPAddress       NVARCHAR(45)  NOT NULL,   -- IPv4 or IPv6
        UserAgent       NVARCHAR(500) NULL,
        Success         BIT          NOT NULL,
        FailureReason   NVARCHAR(50) NULL,        -- NULL when Success=1
        AuthMethod      VARCHAR(30)  NULL,         -- PASSWORD | OAUTH_GOOGLE | PASSWORD_NA
        AttemptedAt     DATETIME2    NOT NULL
            CONSTRAINT DF_LoginAttempt_AttemptedAt DEFAULT (SYSDATETIME()),
        CONSTRAINT PK_LoginAttempt PRIMARY KEY (AttemptID)
    );
END
GO

-- Index for identifier-based lockout lookups
CREATE INDEX IX_LoginAttempt_Identifier_Time
    ON dbo.LoginAttempt(Identifier, AttemptedAt DESC, Success);
-- Index for IP-based rate-limit lookups
CREATE INDEX IX_LoginAttempt_IPAddress_Time
    ON dbo.LoginAttempt(IPAddress, AttemptedAt DESC, Success);
GO
