-- =====================================================================
-- 04_account_columns.sql
-- Adds audit columns to dbo.Account used by AuthService.
-- Run after 03_login_attempt.sql.
-- Idempotent: each column is added only when missing.
-- =====================================================================
USE CinemaManagementDB;
GO

IF COL_LENGTH('dbo.Account', 'EmailVerifiedAt') IS NULL
    ALTER TABLE dbo.Account ADD EmailVerifiedAt DATETIME2 NULL;
GO

IF COL_LENGTH('dbo.Account', 'CreatedAt') IS NULL
    ALTER TABLE dbo.Account ADD CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE();
GO

IF COL_LENGTH('dbo.Account', 'LastLoginAt') IS NULL
    ALTER TABLE dbo.Account ADD LastLoginAt DATETIME2 NULL;
GO

-- AuthMethod column tracks how a login was performed.
IF COL_LENGTH('dbo.LoginAttempt', 'AuthMethod') IS NULL
    ALTER TABLE dbo.LoginAttempt ADD AuthMethod VARCHAR(30) NULL;
GO

-- Allow PasswordHash to be NULL (for OAuth-only accounts).
ALTER TABLE dbo.Account ALTER COLUMN PasswordHash VARCHAR(255) NULL;
GO

-- Resend cooldown support on OtpToken
IF COL_LENGTH('dbo.OtpToken', 'LastSentAt') IS NULL
    ALTER TABLE dbo.OtpToken ADD LastSentAt DATETIME2 NULL;
GO
