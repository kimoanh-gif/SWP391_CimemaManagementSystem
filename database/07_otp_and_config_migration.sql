-- ================================================================
-- Migration: Add OTP Send Tracking and System Configuration
-- Adds SendCount tracking to OtpToken table
-- Adds SystemSetting seed for OTP limits
-- ================================================================

-- Add SendCount columns to OtpToken table
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.OtpToken') AND name = 'SendCount')
BEGIN
    ALTER TABLE dbo.OtpToken ADD SendCount INT NULL DEFAULT 0
    PRINT 'Added SendCount column to OtpToken'
END

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.OtpToken') AND name = 'SendCountDate')
BEGIN
    ALTER TABLE dbo.OtpToken ADD SendCountDate DATETIME NULL
    PRINT 'Added SendCountDate column to OtpToken'
END

GO

-- ================================================================
-- System Settings for OTP Limits
-- ================================================================

-- Insert default settings if they don't exist
IF NOT EXISTS (SELECT 1 FROM dbo.SystemSetting WHERE SettingKey = 'SYSTEM_OTP_MAX_SEND_DAILY')
BEGIN
    INSERT INTO dbo.SystemSetting (SettingKey, SettingName, Category, ValueType, Value, DefaultValue, Description, ValidationRules, IsActive)
    VALUES ('SYSTEM_OTP_MAX_SEND_DAILY', 'OTP Max Sends Per Day', 'SYSTEM', 'NUMBER', '5', '5', 'Maximum OTP codes that can be sent per email per day', 'min:1,max:20', 1)
    PRINT 'Added SYSTEM_OTP_MAX_SEND_DAILY setting'
END

IF NOT EXISTS (SELECT 1 FROM dbo.SystemSetting WHERE SettingKey = 'SYSTEM_OTP_RESEND_COOLDOWN')
BEGIN
    INSERT INTO dbo.SystemSetting (SettingKey, SettingName, Category, ValueType, Value, DefaultValue, Description, ValidationRules, IsActive)
    VALUES ('SYSTEM_OTP_RESEND_COOLDOWN', 'OTP Resend Cooldown', 'SYSTEM', 'NUMBER', '60', '60', 'Seconds to wait between OTP resends', 'min:10,max:300', 1)
    PRINT 'Added SYSTEM_OTP_RESEND_COOLDOWN setting'
END

GO

PRINT 'Migration completed: OTP Send Tracking & System Configuration'
