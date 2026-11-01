-- Optional, idempotent upgrade for databases without the membership policy columns.
-- The current local database already has these columns from the previous upgrade.
IF COL_LENGTH(N'dbo.MembershipRank',N'DiscountUsageLimit') IS NULL
    ALTER TABLE dbo.MembershipRank ADD DiscountUsageLimit INT NOT NULL DEFAULT 0 CHECK (DiscountUsageLimit>=0);
IF COL_LENGTH(N'dbo.MembershipRank',N'MinBookingAmount') IS NULL
    ALTER TABLE dbo.MembershipRank ADD MinBookingAmount DECIMAL(18,0) NOT NULL DEFAULT 0 CHECK (MinBookingAmount>=0);
IF COL_LENGTH(N'dbo.MembershipRank',N'MaxDiscountAmount') IS NULL
    ALTER TABLE dbo.MembershipRank ADD MaxDiscountAmount DECIMAL(18,0) NOT NULL DEFAULT 0 CHECK (MaxDiscountAmount>=0);
GO
IF OBJECT_ID(N'dbo.MembershipDiscountUsage',N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MembershipDiscountUsage (
        BookingID INT NOT NULL PRIMARY KEY REFERENCES dbo.Booking(BookingID),
        CustomerID INT NOT NULL REFERENCES dbo.Customer(CustomerID),
        RankID INT NOT NULL REFERENCES dbo.MembershipRank(RankID),
        DiscountAmount DECIMAL(18,0) NOT NULL CHECK (DiscountAmount>0),
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
    CREATE INDEX IX_MembershipDiscountUsage_CustomerRank ON dbo.MembershipDiscountUsage(CustomerID,RankID);
END;
GO
