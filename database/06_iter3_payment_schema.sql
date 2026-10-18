/* Iteration 3 payment contract. Idempotent; safe to rerun. */
USE CinemaManagementDB;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF COL_LENGTH('dbo.Payment', 'ProviderTransactionNo') IS NULL
    ALTER TABLE dbo.Payment ADD ProviderTransactionNo varchar(100) NULL;
IF COL_LENGTH('dbo.Payment', 'ResponseCode') IS NULL
    ALTER TABLE dbo.Payment ADD ResponseCode varchar(20) NULL;
IF COL_LENGTH('dbo.Payment', 'TransactionStatus') IS NULL
    ALTER TABLE dbo.Payment ADD TransactionStatus varchar(20) NULL;
IF COL_LENGTH('dbo.Payment', 'BankCode') IS NULL
    ALTER TABLE dbo.Payment ADD BankCode varchar(30) NULL;
IF COL_LENGTH('dbo.Payment', 'CardType') IS NULL
    ALTER TABLE dbo.Payment ADD CardType varchar(30) NULL;
IF COL_LENGTH('dbo.Payment', 'InitiatedAt') IS NULL
    ALTER TABLE dbo.Payment ADD InitiatedAt datetime2 NULL;
IF COL_LENGTH('dbo.Payment', 'ExpiresAt') IS NULL
    ALTER TABLE dbo.Payment ADD ExpiresAt datetime2 NULL;
IF COL_LENGTH('dbo.Payment', 'UpdatedAt') IS NULL
    ALTER TABLE dbo.Payment ADD UpdatedAt datetime2 NULL;
IF COL_LENGTH('dbo.Payment', 'LastCallbackAt') IS NULL
    ALTER TABLE dbo.Payment ADD LastCallbackAt datetime2 NULL;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Payment') AND [name] = 'UX_Payment_TransactionRef'
)
    CREATE UNIQUE INDEX UX_Payment_TransactionRef
        ON dbo.Payment(TransactionRef)
        WHERE TransactionRef IS NOT NULL;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Payment') AND [name] = 'UX_Payment_ProviderTransactionNo'
)
    CREATE UNIQUE INDEX UX_Payment_ProviderTransactionNo
        ON dbo.Payment(ProviderTransactionNo)
        WHERE ProviderTransactionNo IS NOT NULL;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Payment') AND [name] = 'IX_Payment_Booking_Status'
)
    CREATE INDEX IX_Payment_Booking_Status ON dbo.Payment(BookingID, [Status]);

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.Booking') AND [name] = 'IX_Booking_Status_BookingTime'
)
    CREATE INDEX IX_Booking_Status_BookingTime ON dbo.Booking([Status], BookingTime);
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE [name] = 'CK_Booking_Status')
    ALTER TABLE dbo.Booking WITH CHECK ADD CONSTRAINT CK_Booking_Status CHECK (
        [Status] IN ('PENDING', 'PAYMENT_PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')
    );

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE [name] = 'CK_BookingSeat_Status')
    ALTER TABLE dbo.BookingSeat WITH CHECK ADD CONSTRAINT CK_BookingSeat_Status CHECK (
        [Status] IN ('HELD', 'BOOKED', 'RELEASED')
    );

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE [name] = 'CK_Payment_Status')
    ALTER TABLE dbo.Payment WITH CHECK ADD CONSTRAINT CK_Payment_Status CHECK (
        [Status] IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED')
    );

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE [name] = 'CK_Ticket_Status')
    ALTER TABLE dbo.Ticket WITH CHECK ADD CONSTRAINT CK_Ticket_Status CHECK (
        TicketStatus IN ('VALID', 'USED', 'CANCELLED')
    );
GO
