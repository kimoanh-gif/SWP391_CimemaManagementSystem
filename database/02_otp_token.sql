-- =====================================================================
-- 02_otp_token.sql
-- One-time password tokens for account activation and password reset.
-- =====================================================================
USE CinemaManagementDB;
GO

IF OBJECT_ID('dbo.OtpToken', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.OtpToken (
        OtpTokenID     BIGINT IDENTITY(1,1) NOT NULL,
        AccountID      INT NULL,                              -- NULL when used for pre-registration
        Email          NVARCHAR(255) NOT NULL,
        CodeHash       NVARCHAR(255) NOT NULL,                 -- BCrypt hash of the OTP
        Purpose        NVARCHAR(30)  NOT NULL,                 -- ACTIVATION | RESET_PASSWORD
        ExpiresAt      DATETIME2     NOT NULL,
        ConsumedAt     DATETIME2     NULL,
        AttemptCount   INT           NOT NULL CONSTRAINT DF_OtpToken_AttemptCount DEFAULT (0),
        CreatedAt      DATETIME2     NOT NULL CONSTRAINT DF_OtpToken_CreatedAt DEFAULT (SYSDATETIME()),
        CONSTRAINT PK_OtpToken PRIMARY KEY (OtpTokenID),
        CONSTRAINT FK_OtpToken_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID),
        CONSTRAINT CK_OtpToken_Purpose CHECK (Purpose IN ('ACTIVATION','RESET_PASSWORD'))
    );
END
GO

CREATE INDEX IX_OtpToken_Email_Purpose     ON dbo.OtpToken(Email, Purpose);
CREATE INDEX IX_OtpToken_Account_Purpose   ON dbo.OtpToken(AccountID, Purpose);
GO
