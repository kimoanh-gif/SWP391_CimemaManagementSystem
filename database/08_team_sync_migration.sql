-- ================================================================
-- Migration: Safe Database Schema Synchronization with origin/develop
-- Target Database: CinemaManagementDB
-- Rules: Non-destructive, idempotent (IF NOT EXISTS), zero data loss.
-- ================================================================

USE CinemaManagementDB;
GO

PRINT 'Starting non-destructive schema synchronization with origin/develop...';
GO

-- 1. Table: dbo.MembershipRank - Add new columns if missing
IF COL_LENGTH('dbo.MembershipRank', 'DisplayName') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipRank ADD DisplayName NVARCHAR(100) NULL;
    PRINT 'Added DisplayName to dbo.MembershipRank';
END
GO

IF COL_LENGTH('dbo.MembershipRank', 'MinPoints') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipRank ADD MinPoints INT NULL DEFAULT 0;
    PRINT 'Added MinPoints to dbo.MembershipRank';
END
GO

IF COL_LENGTH('dbo.MembershipRank', 'MaxPoints') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipRank ADD MaxPoints INT NULL;
    PRINT 'Added MaxPoints to dbo.MembershipRank';
END
GO

IF COL_LENGTH('dbo.MembershipRank', 'Benefits') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipRank ADD Benefits NVARCHAR(MAX) NULL;
    PRINT 'Added Benefits to dbo.MembershipRank';
END
GO

IF COL_LENGTH('dbo.MembershipRank', 'DiscountPercent') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipRank ADD DiscountPercent DECIMAL(5,2) NULL DEFAULT 0;
    PRINT 'Added DiscountPercent to dbo.MembershipRank';
END
GO

IF COL_LENGTH('dbo.MembershipRank', 'IsActive') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipRank ADD IsActive BIT NOT NULL CONSTRAINT DF_MembershipRank_IsActive DEFAULT 1;
    PRINT 'Added IsActive to dbo.MembershipRank';
END
GO

IF COL_LENGTH('dbo.MembershipRank', 'CreatedAt') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipRank ADD CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_MembershipRank_CreatedAt DEFAULT SYSDATETIME();
    PRINT 'Added CreatedAt to dbo.MembershipRank';
END
GO

IF COL_LENGTH('dbo.MembershipRank', 'UpdatedAt') IS NULL
BEGIN
    ALTER TABLE dbo.MembershipRank ADD UpdatedAt DATETIME2 NULL;
    PRINT 'Added UpdatedAt to dbo.MembershipRank';
END
GO

-- Backfill data for MembershipRank if old columns exist
IF COL_LENGTH('dbo.MembershipRank', 'MinTierPoints') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        UPDATE dbo.MembershipRank
        SET MinPoints = MinTierPoints
        WHERE MinPoints IS NULL OR MinPoints = 0;
    ';
    PRINT 'Backfilled MinPoints from MinTierPoints in dbo.MembershipRank';
END
GO

IF COL_LENGTH('dbo.MembershipRank', 'BenefitDescription') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        UPDATE dbo.MembershipRank
        SET Benefits = BenefitDescription
        WHERE Benefits IS NULL;
    ';
    PRINT 'Backfilled Benefits from BenefitDescription in dbo.MembershipRank';
END
GO

-- 2. Table: dbo.SystemSetting - Add new columns if missing
IF COL_LENGTH('dbo.SystemSetting', 'Value') IS NULL
BEGIN
    ALTER TABLE dbo.SystemSetting ADD Value NVARCHAR(50) NULL;
    PRINT 'Added Value to dbo.SystemSetting';
END
GO

IF COL_LENGTH('dbo.SystemSetting', 'SettingName') IS NULL
BEGIN
    ALTER TABLE dbo.SystemSetting ADD SettingName NVARCHAR(255) NULL;
    PRINT 'Added SettingName to dbo.SystemSetting';
END
GO

IF COL_LENGTH('dbo.SystemSetting', 'Category') IS NULL
BEGIN
    ALTER TABLE dbo.SystemSetting ADD Category NVARCHAR(50) NULL;
    PRINT 'Added Category to dbo.SystemSetting';
END
GO

IF COL_LENGTH('dbo.SystemSetting', 'ValueType') IS NULL
BEGIN
    ALTER TABLE dbo.SystemSetting ADD ValueType NVARCHAR(20) NULL;
    PRINT 'Added ValueType to dbo.SystemSetting';
END
GO

IF COL_LENGTH('dbo.SystemSetting', 'DefaultValue') IS NULL
BEGIN
    ALTER TABLE dbo.SystemSetting ADD DefaultValue NVARCHAR(50) NULL;
    PRINT 'Added DefaultValue to dbo.SystemSetting';
END
GO

IF COL_LENGTH('dbo.SystemSetting', 'ValidationRules') IS NULL
BEGIN
    ALTER TABLE dbo.SystemSetting ADD ValidationRules NVARCHAR(100) NULL;
    PRINT 'Added ValidationRules to dbo.SystemSetting';
END
GO

IF COL_LENGTH('dbo.SystemSetting', 'IsActive') IS NULL
BEGIN
    ALTER TABLE dbo.SystemSetting ADD IsActive BIT NOT NULL CONSTRAINT DF_SystemSetting_IsActive DEFAULT 1;
    PRINT 'Added IsActive to dbo.SystemSetting';
END
GO

-- Backfill Value from SettingValue if old column exists
IF COL_LENGTH('dbo.SystemSetting', 'SettingValue') IS NOT NULL
BEGIN
    EXEC sp_executesql N'
        UPDATE dbo.SystemSetting
        SET Value = SettingValue
        WHERE Value IS NULL AND SettingValue IS NOT NULL;
    ';
    PRINT 'Backfilled Value from SettingValue in dbo.SystemSetting';
END
GO

-- 3. Table: dbo.OtpToken - Add SendCount and SendCountDate if missing
IF COL_LENGTH('dbo.OtpToken', 'SendCount') IS NULL
BEGIN
    ALTER TABLE dbo.OtpToken ADD SendCount INT NULL DEFAULT 0;
    PRINT 'Added SendCount to dbo.OtpToken';
END
GO

IF COL_LENGTH('dbo.OtpToken', 'SendCountDate') IS NULL
BEGIN
    ALTER TABLE dbo.OtpToken ADD SendCountDate DATETIME NULL;
    PRINT 'Added SendCountDate to dbo.OtpToken';
END
GO

-- 4. Check Constraints
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE [name] = N'CK_OtpToken_Purpose' AND parent_object_id = OBJECT_ID(N'dbo.OtpToken'))
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OtpToken WHERE Purpose NOT IN (N'ACTIVATION', N'RESET_PASSWORD'))
        ALTER TABLE dbo.OtpToken WITH NOCHECK
            ADD CONSTRAINT CK_OtpToken_Purpose CHECK (Purpose IN (N'ACTIVATION', N'RESET_PASSWORD'));
    ELSE
        ALTER TABLE dbo.OtpToken WITH CHECK
            ADD CONSTRAINT CK_OtpToken_Purpose CHECK (Purpose IN (N'ACTIVATION', N'RESET_PASSWORD'));
    PRINT 'Created CK_OtpToken_Purpose constraint';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE [name] = N'CK_OAuthAccount_Provider' AND parent_object_id = OBJECT_ID(N'dbo.OAuthAccount'))
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OAuthAccount WHERE Provider <> 'GOOGLE')
        ALTER TABLE dbo.OAuthAccount WITH NOCHECK
            ADD CONSTRAINT CK_OAuthAccount_Provider CHECK (Provider = 'GOOGLE');
    ELSE
        ALTER TABLE dbo.OAuthAccount WITH CHECK
            ADD CONSTRAINT CK_OAuthAccount_Provider CHECK (Provider = 'GOOGLE');
    PRINT 'Created CK_OAuthAccount_Provider constraint';
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys
               WHERE [name] = N'FK_OtpAttempt_OtpToken' AND parent_object_id = OBJECT_ID(N'dbo.OtpAttempt'))
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OtpAttempt x
               WHERE x.OtpTokenID IS NOT NULL
                 AND NOT EXISTS (SELECT 1 FROM dbo.OtpToken t WHERE t.OtpTokenID = x.OtpTokenID))
        ALTER TABLE dbo.OtpAttempt WITH NOCHECK
            ADD CONSTRAINT FK_OtpAttempt_OtpToken FOREIGN KEY (OtpTokenID) REFERENCES dbo.OtpToken(OtpTokenID);
    ELSE
        ALTER TABLE dbo.OtpAttempt WITH CHECK
            ADD CONSTRAINT FK_OtpAttempt_OtpToken FOREIGN KEY (OtpTokenID) REFERENCES dbo.OtpToken(OtpTokenID);
    PRINT 'Created FK_OtpAttempt_OtpToken foreign key';
END
GO

-- 5. Performance Indexes
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Account') AND [name] = N'IX_Account_RoleID')
    CREATE INDEX IX_Account_RoleID ON dbo.Account(RoleID);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.OtpToken') AND [name] = N'IX_OtpToken_Email_Purpose')
    CREATE INDEX IX_OtpToken_Email_Purpose ON dbo.OtpToken(Email, Purpose);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.OtpToken') AND [name] = N'IX_OtpToken_Account_Purpose')
    CREATE INDEX IX_OtpToken_Account_Purpose ON dbo.OtpToken(AccountID, Purpose);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.OtpAttempt') AND [name] = N'IX_OtpAttempt_EmailPurpose')
    CREATE INDEX IX_OtpAttempt_EmailPurpose ON dbo.OtpAttempt(Email, Purpose, AttemptedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.OtpAttempt') AND [name] = N'IX_OtpAttempt_IpAddress')
    CREATE INDEX IX_OtpAttempt_IpAddress ON dbo.OtpAttempt(IpAddress, AttemptedAt DESC);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.LoginAttempt') AND [name] = N'IX_LoginAttempt_Identifier_Time')
    CREATE INDEX IX_LoginAttempt_Identifier_Time ON dbo.LoginAttempt(Identifier, AttemptedAt DESC, Success);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.LoginAttempt') AND [name] = N'IX_LoginAttempt_IPAddress_Time')
    CREATE INDEX IX_LoginAttempt_IPAddress_Time ON dbo.LoginAttempt(IPAddress, AttemptedAt DESC, Success);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.OAuthAccount') AND [name] = N'IX_OAuthAccount_Account')
    CREATE INDEX IX_OAuthAccount_Account ON dbo.OAuthAccount(AccountID);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.IpRateLimit') AND [name] = N'IX_IpRateLimit_Locked')
    CREATE INDEX IX_IpRateLimit_Locked ON dbo.IpRateLimit(LockedUntil) WHERE LockedUntil IS NOT NULL;
GO

PRINT 'Schema synchronization script completed safely without data loss.';
GO
