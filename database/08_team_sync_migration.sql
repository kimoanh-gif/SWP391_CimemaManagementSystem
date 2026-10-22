/* ================================================================
   MIGRATION: 08_team_sync_migration.sql
   PURPOSE: Synchronize local CinemaManagementDB with origin/develop
   TARGET DBMS: Microsoft SQL Server 2019+
   RULES:
     - 100% Non-destructive: NO DROP, NO TRUNCATE, NO DELETE of user data.
     - Fully Idempotent: Can be executed multiple times without errors.
     - Preserves all existing records in Account, Customer, Movie, Genre, MembershipRank.
     - Safely upgrades legacy table schemas before creating foreign keys.
   ================================================================ */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

PRINT '>>> [START] Non-destructive Database Schema Synchronization';
GO

/* ================================================================
   STEP 1. ROLE & PERMISSION FOUNDATION
   ================================================================ */

IF OBJECT_ID(N'dbo.[Role]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[Role] (
        RoleID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Role PRIMARY KEY,
        [Name] NVARCHAR(50) NOT NULL CONSTRAINT UQ_Role_Name UNIQUE,
        [Description] NVARCHAR(255) NULL
    );
    PRINT 'Created table dbo.[Role]';
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'ADMIN')
    INSERT dbo.[Role] ([Name], [Description]) VALUES (N'ADMIN', N'Quản trị viên toàn quyền');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'MANAGER')
    INSERT dbo.[Role] ([Name], [Description]) VALUES (N'MANAGER', N'Quản lý rạp và vận hành');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'STAFF')
    INSERT dbo.[Role] ([Name], [Description]) VALUES (N'STAFF', N'Nhân viên bán vé và hỗ trợ');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'CUSTOMER')
    INSERT dbo.[Role] ([Name], [Description]) VALUES (N'CUSTOMER', N'Khách hàng xem phim');
GO

IF OBJECT_ID(N'dbo.Permission', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Permission (
        PermissionID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Permission PRIMARY KEY,
        [Name] NVARCHAR(100) NOT NULL CONSTRAINT UQ_Permission_Name UNIQUE,
        [Description] NVARCHAR(255) NULL
    );
    PRINT 'Created table dbo.Permission';
END;
GO

IF OBJECT_ID(N'dbo.RolePermission', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RolePermission (
        RoleID INT NOT NULL,
        PermissionID INT NOT NULL,
        AssignedAt DATETIME2 NOT NULL CONSTRAINT DF_RolePermission_AssignedAt DEFAULT SYSDATETIME(),
        CONSTRAINT PK_RolePermission PRIMARY KEY (RoleID, PermissionID),
        CONSTRAINT FK_RolePermission_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID) ON DELETE CASCADE,
        CONSTRAINT FK_RolePermission_Permission FOREIGN KEY (PermissionID) REFERENCES dbo.Permission(PermissionID) ON DELETE CASCADE
    );
    PRINT 'Created table dbo.RolePermission';
END;
GO

/* ================================================================
   STEP 2. UPGRADE dbo.MembershipRank (SAFE COLUMN EXTENSION)
   ================================================================ */

IF OBJECT_ID(N'dbo.MembershipRank', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MembershipRank (
        RankID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MembershipRank PRIMARY KEY,
        [Name] NVARCHAR(50) NOT NULL CONSTRAINT UQ_MembershipRank_Name UNIQUE,
        DisplayName NVARCHAR(100) NULL,
        MinPoints INT NOT NULL CONSTRAINT DF_MembershipRank_MinPoints DEFAULT 0,
        MaxPoints INT NULL,
        Benefits NVARCHAR(MAX) NULL,
        DiscountPercent DECIMAL(5, 2) NULL CONSTRAINT DF_MembershipRank_Discount DEFAULT 0,
        IsActive BIT NOT NULL CONSTRAINT DF_MembershipRank_IsActive DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_MembershipRank_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT CK_MembershipRank_MinPoints CHECK (MinPoints >= 0)
    );
    PRINT 'Created table dbo.MembershipRank';
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.MembershipRank', N'DisplayName') IS NULL
        ALTER TABLE dbo.MembershipRank ADD DisplayName NVARCHAR(100) NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'MinPoints') IS NULL
        ALTER TABLE dbo.MembershipRank ADD MinPoints INT NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'MaxPoints') IS NULL
        ALTER TABLE dbo.MembershipRank ADD MaxPoints INT NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'Benefits') IS NULL
        ALTER TABLE dbo.MembershipRank ADD Benefits NVARCHAR(MAX) NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'DiscountPercent') IS NULL
        ALTER TABLE dbo.MembershipRank ADD DiscountPercent DECIMAL(5, 2) NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'IsActive') IS NULL
        ALTER TABLE dbo.MembershipRank ADD IsActive BIT NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'CreatedAt') IS NULL
        ALTER TABLE dbo.MembershipRank ADD CreatedAt DATETIME2 NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'UpdatedAt') IS NULL
        ALTER TABLE dbo.MembershipRank ADD UpdatedAt DATETIME2 NULL;
    PRINT 'Extended columns in dbo.MembershipRank';
END;
GO

-- Safe backfill for MembershipRank legacy data
IF COL_LENGTH(N'dbo.MembershipRank', N'MinTierPoints') IS NOT NULL
BEGIN
    EXEC(N'UPDATE dbo.MembershipRank SET MinPoints = COALESCE(MinPoints, MinTierPoints)');
    EXEC(N'ALTER TABLE dbo.MembershipRank ALTER COLUMN MinTierPoints INT NULL');
END;
IF COL_LENGTH(N'dbo.MembershipRank', N'BenefitDescription') IS NOT NULL
    EXEC(N'UPDATE dbo.MembershipRank SET Benefits = COALESCE(Benefits, BenefitDescription)');
IF COL_LENGTH(N'dbo.MembershipRank', N'Status') IS NOT NULL
    EXEC(N'UPDATE dbo.MembershipRank SET IsActive = COALESCE(IsActive, CASE WHEN [Status] = ''ACTIVE'' THEN 1 ELSE 0 END)');

EXEC(N'UPDATE dbo.MembershipRank
       SET DisplayName = COALESCE(DisplayName, [Name]),
           MinPoints = COALESCE(MinPoints, 0),
           DiscountPercent = COALESCE(DiscountPercent, 0),
           IsActive = COALESCE(IsActive, 1),
           CreatedAt = COALESCE(CreatedAt, SYSDATETIME())');

IF NOT EXISTS (SELECT 1 FROM dbo.MembershipRank WHERE [Name] = N'STANDARD')
    INSERT dbo.MembershipRank ([Name], DisplayName, MinPoints, MaxPoints, Benefits, DiscountPercent, IsActive)
    VALUES (N'STANDARD', N'Standard Member', 0, 999, N'{"pointsRate":1,"birthdayBonus":false,"priorityBooking":false}', 0, 1);
IF NOT EXISTS (SELECT 1 FROM dbo.MembershipRank WHERE [Name] = N'SILVER')
    INSERT dbo.MembershipRank ([Name], DisplayName, MinPoints, MaxPoints, Benefits, DiscountPercent, IsActive)
    VALUES (N'SILVER', N'Silver Member', 1000, 2999, N'{"pointsRate":1,"birthdayBonus":true,"priorityBooking":false}', 0, 1);
IF NOT EXISTS (SELECT 1 FROM dbo.MembershipRank WHERE [Name] = N'GOLD')
    INSERT dbo.MembershipRank ([Name], DisplayName, MinPoints, MaxPoints, Benefits, DiscountPercent, IsActive)
    VALUES (N'GOLD', N'Gold Member', 3000, NULL, N'{"pointsRate":1.5,"birthdayBonus":true,"priorityBooking":true}', 5, 1);
GO

/* ================================================================
   STEP 3. UPGRADE dbo.Account (SAFE COLUMN EXTENSION & ROLE BACKFILL)
   ================================================================ */

IF OBJECT_ID(N'dbo.Account', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Account (
        AccountID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Account PRIMARY KEY,
        RoleID INT NOT NULL,
        Username VARCHAR(50) NOT NULL CONSTRAINT UQ_Account_Username UNIQUE,
        Email VARCHAR(255) NOT NULL CONSTRAINT UQ_Account_Email UNIQUE,
        PasswordHash VARCHAR(255) NULL,
        FullName NVARCHAR(100) NOT NULL,
        Phone VARCHAR(15) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Account_Status DEFAULT 'ACTIVE',
        EmailVerifiedAt DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Account_CreatedAt DEFAULT SYSDATETIME(),
        LastLoginAt DATETIME2 NULL,
        CurrentAddress NVARCHAR(255) NULL,
        AvatarUrl NVARCHAR(500) NULL,
        Notes NVARCHAR(500) NULL,
        CONSTRAINT FK_Account_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID)
    );
    PRINT 'Created table dbo.Account';
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.Account', N'RoleID') IS NULL
        ALTER TABLE dbo.Account ADD RoleID INT NULL;
    IF COL_LENGTH(N'dbo.Account', N'PasswordHash') IS NULL
        ALTER TABLE dbo.Account ADD PasswordHash VARCHAR(255) NULL;
    IF COL_LENGTH(N'dbo.Account', N'Status') IS NULL
        ALTER TABLE dbo.Account ADD [Status] VARCHAR(30) NULL;
    IF COL_LENGTH(N'dbo.Account', N'EmailVerifiedAt') IS NULL
        ALTER TABLE dbo.Account ADD EmailVerifiedAt DATETIME2 NULL;
    IF COL_LENGTH(N'dbo.Account', N'CreatedAt') IS NULL
        ALTER TABLE dbo.Account ADD CreatedAt DATETIME2 NULL;
    IF COL_LENGTH(N'dbo.Account', N'LastLoginAt') IS NULL
        ALTER TABLE dbo.Account ADD LastLoginAt DATETIME2 NULL;
    IF COL_LENGTH(N'dbo.Account', N'CurrentAddress') IS NULL
        ALTER TABLE dbo.Account ADD CurrentAddress NVARCHAR(255) NULL;
    IF COL_LENGTH(N'dbo.Account', N'AvatarUrl') IS NULL
        ALTER TABLE dbo.Account ADD AvatarUrl NVARCHAR(500) NULL;
    IF COL_LENGTH(N'dbo.Account', N'Notes') IS NULL
        ALTER TABLE dbo.Account ADD Notes NVARCHAR(500) NULL;
    PRINT 'Extended columns in dbo.Account';
END;
GO

-- Backfill RoleID from legacy Role column if present
IF COL_LENGTH(N'dbo.Account', N'Role') IS NOT NULL
BEGIN
    EXEC(N'
        UPDATE a
        SET RoleID = r.RoleID
        FROM dbo.Account a
        JOIN dbo.[Role] r ON UPPER(r.[Name]) = UPPER(a.[Role])
        WHERE a.RoleID IS NULL;
    ');
END;

-- Default unmatched or null RoleID to CUSTOMER
DECLARE @CustomerRoleId INT = (SELECT TOP 1 RoleID FROM dbo.[Role] WHERE [Name] = N'CUSTOMER');
UPDATE dbo.Account
SET RoleID = COALESCE(RoleID, @CustomerRoleId),
    [Status] = COALESCE([Status], 'ACTIVE'),
    CreatedAt = COALESCE(CreatedAt, SYSDATETIME());
GO

-- Add FK_Account_Role if not present
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE [name] = N'FK_Account_Role' AND parent_object_id = OBJECT_ID(N'dbo.Account'))
BEGIN
    ALTER TABLE dbo.Account WITH CHECK
        ADD CONSTRAINT FK_Account_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID);
    PRINT 'Created foreign key FK_Account_Role';
END;
GO

/* ================================================================
   STEP 4. UPGRADE dbo.Customer & dbo.Staff
   ================================================================ */

IF OBJECT_ID(N'dbo.Customer', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Customer (
        CustomerID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Customer PRIMARY KEY,
        AccountID INT NOT NULL CONSTRAINT UQ_Customer_Account UNIQUE,
        DateOfBirth DATE NULL,
        [Address] NVARCHAR(255) NULL,
        RankID INT NOT NULL,
        AvailablePoints INT NOT NULL CONSTRAINT DF_Customer_AvailablePoints DEFAULT 0,
        TierPoints INT NOT NULL CONSTRAINT DF_Customer_TierPoints DEFAULT 0,
        ReceivePromotionEmail BIT NOT NULL CONSTRAINT DF_Customer_PromoEmail DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Customer_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_Customer_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID) ON DELETE CASCADE,
        CONSTRAINT FK_Customer_Rank FOREIGN KEY (RankID) REFERENCES dbo.MembershipRank(RankID)
    );
    PRINT 'Created table dbo.Customer';
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.Customer', N'ReceivePromotionEmail') IS NULL
        ALTER TABLE dbo.Customer ADD ReceivePromotionEmail BIT NOT NULL CONSTRAINT DF_Customer_PromoEmail DEFAULT 1;
    IF COL_LENGTH(N'dbo.Customer', N'CreatedAt') IS NULL
        ALTER TABLE dbo.Customer ADD CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Customer_CreatedAt DEFAULT SYSDATETIME();
    IF COL_LENGTH(N'dbo.Customer', N'UpdatedAt') IS NULL
        ALTER TABLE dbo.Customer ADD UpdatedAt DATETIME2 NULL;
    PRINT 'Extended columns in dbo.Customer';
END;
GO

-- Ensure RankID points to a valid rank
DECLARE @StandardRankId INT = (SELECT TOP 1 RankID FROM dbo.MembershipRank WHERE [Name] = N'STANDARD');
UPDATE dbo.Customer
SET RankID = COALESCE(RankID, @StandardRankId)
WHERE RankID IS NULL OR NOT EXISTS (SELECT 1 FROM dbo.MembershipRank m WHERE m.RankID = dbo.Customer.RankID);
GO

IF OBJECT_ID(N'dbo.Staff', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Staff (
        StaffID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Staff PRIMARY KEY,
        AccountID INT NOT NULL CONSTRAINT UQ_Staff_Account UNIQUE,
        Position NVARCHAR(100) NOT NULL,
        HireDate DATE NOT NULL CONSTRAINT DF_Staff_HireDate DEFAULT CAST(SYSDATETIME() AS DATE),
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Staff_Status DEFAULT 'ACTIVE',
        CONSTRAINT FK_Staff_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID) ON DELETE CASCADE
    );
    PRINT 'Created table dbo.Staff';
END;
GO

/* ================================================================
   STEP 5. UPGRADE dbo.Movie & MOVIE GENRE
   ================================================================ */

IF OBJECT_ID(N'dbo.Movie', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Movie (
        MovieID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Movie PRIMARY KEY,
        Title NVARCHAR(200) NOT NULL,
        Duration INT NOT NULL CONSTRAINT CK_Movie_Duration CHECK (Duration > 0),
        ReleaseDate DATE NOT NULL,
        [Language] NVARCHAR(50) NULL,
        Director NVARCHAR(100) NULL,
        AgeRating VARCHAR(10) NULL,
        [Description] NVARCHAR(MAX) NULL,
        PosterURL NVARCHAR(500) NULL,
        TrailerURL NVARCHAR(500) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Movie_Status DEFAULT 'ACTIVE',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Movie_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
    PRINT 'Created table dbo.Movie';
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.Movie', N'Language') IS NULL
        ALTER TABLE dbo.Movie ADD [Language] NVARCHAR(50) NULL;
    IF COL_LENGTH(N'dbo.Movie', N'Director') IS NULL
        ALTER TABLE dbo.Movie ADD Director NVARCHAR(100) NULL;
    IF COL_LENGTH(N'dbo.Movie', N'AgeRating') IS NULL
        ALTER TABLE dbo.Movie ADD AgeRating VARCHAR(10) NULL;
    IF COL_LENGTH(N'dbo.Movie', N'Description') IS NULL
        ALTER TABLE dbo.Movie ADD [Description] NVARCHAR(MAX) NULL;
    IF COL_LENGTH(N'dbo.Movie', N'PosterURL') IS NULL
        ALTER TABLE dbo.Movie ADD PosterURL NVARCHAR(500) NULL;
    IF COL_LENGTH(N'dbo.Movie', N'TrailerURL') IS NULL
        ALTER TABLE dbo.Movie ADD TrailerURL NVARCHAR(500) NULL;
    IF COL_LENGTH(N'dbo.Movie', N'UpdatedAt') IS NULL
        ALTER TABLE dbo.Movie ADD UpdatedAt DATETIME2 NULL;
    PRINT 'Extended columns in dbo.Movie';
END;
GO

IF OBJECT_ID(N'dbo.Genre', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Genre (
        GenreID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Genre PRIMARY KEY,
        [Name] NVARCHAR(100) NOT NULL CONSTRAINT UQ_Genre_Name UNIQUE,
        [Description] NVARCHAR(500) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Genre_Status DEFAULT 'ACTIVE',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Genre_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
    PRINT 'Created table dbo.Genre';
END;
GO

IF OBJECT_ID(N'dbo.MovieGenre', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MovieGenre (
        MovieID INT NOT NULL,
        GenreID INT NOT NULL,
        AssignedAt DATETIME2 NOT NULL CONSTRAINT DF_MovieGenre_AssignedAt DEFAULT SYSDATETIME(),
        CONSTRAINT PK_MovieGenre PRIMARY KEY (MovieID, GenreID),
        CONSTRAINT FK_MovieGenre_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID) ON DELETE CASCADE,
        CONSTRAINT FK_MovieGenre_Genre FOREIGN KEY (GenreID) REFERENCES dbo.Genre(GenreID) ON DELETE CASCADE
    );
    PRINT 'Created table dbo.MovieGenre';
END;
GO

/* ================================================================
   STEP 6. AUDITORIUM, SEATS, SHOWTIMES & PRICING
   ================================================================ */

IF OBJECT_ID(N'dbo.Room', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Room (
        RoomID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Room PRIMARY KEY,
        RoomName NVARCHAR(50) NOT NULL CONSTRAINT UQ_Room_RoomName UNIQUE,
        RoomType NVARCHAR(50) NOT NULL CONSTRAINT DF_Room_RoomType DEFAULT N'2D Standard',
        Capacity INT NOT NULL CONSTRAINT DF_Room_Capacity DEFAULT 0,
        TotalRows INT NOT NULL CONSTRAINT DF_Room_TotalRows DEFAULT 0,
        TotalColumns INT NOT NULL CONSTRAINT DF_Room_TotalColumns DEFAULT 0,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Room_Status DEFAULT 'ACTIVE',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Room_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
    PRINT 'Created table dbo.Room';
END;
GO

IF OBJECT_ID(N'dbo.SeatType', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SeatType (
        SeatTypeID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SeatType PRIMARY KEY,
        TypeName NVARCHAR(50) NOT NULL CONSTRAINT UQ_SeatType_TypeName UNIQUE,
        PriceMultiplier DECIMAL(5,2) NOT NULL CONSTRAINT DF_SeatType_Multiplier DEFAULT 1.00,
        ColorCode VARCHAR(20) NOT NULL CONSTRAINT DF_SeatType_Color DEFAULT '#22c55e',
        [Description] NVARCHAR(255) NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_SeatType_IsActive DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_SeatType_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
    PRINT 'Created table dbo.SeatType';
END;
GO

IF OBJECT_ID(N'dbo.Seat', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Seat (
        SeatID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Seat PRIMARY KEY,
        RoomID INT NOT NULL,
        SeatTypeID INT NOT NULL,
        RowLabel VARCHAR(5) NOT NULL,
        SeatNumber INT NOT NULL,
        GridRow INT NOT NULL,
        GridColumn INT NOT NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Seat_Status DEFAULT 'ACTIVE',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Seat_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_Seat_Position UNIQUE (RoomID, RowLabel, SeatNumber),
        CONSTRAINT FK_Seat_Room FOREIGN KEY (RoomID) REFERENCES dbo.Room(RoomID) ON DELETE CASCADE,
        CONSTRAINT FK_Seat_SeatType FOREIGN KEY (SeatTypeID) REFERENCES dbo.SeatType(SeatTypeID)
    );
    PRINT 'Created table dbo.Seat';
END;
GO

IF OBJECT_ID(N'dbo.Showtime', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Showtime (
        ShowtimeID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Showtime PRIMARY KEY,
        MovieID INT NOT NULL,
        RoomID INT NOT NULL,
        StartTime DATETIME2 NOT NULL,
        EndTime DATETIME2 NOT NULL,
        BasePrice DECIMAL(12,2) NOT NULL CONSTRAINT DF_Showtime_BasePrice DEFAULT 80000,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Showtime_Status DEFAULT 'SCHEDULED',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Showtime_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_Showtime_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID),
        CONSTRAINT FK_Showtime_Room FOREIGN KEY (RoomID) REFERENCES dbo.Room(RoomID)
    );
    PRINT 'Created table dbo.Showtime';
END;
GO

IF OBJECT_ID(N'dbo.TicketPricing', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TicketPricing (
        PricingID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TicketPricing PRIMARY KEY,
        DayType VARCHAR(20) NOT NULL,
        TimeSlot VARCHAR(20) NOT NULL,
        SeatTypeID INT NOT NULL,
        BasePrice DECIMAL(12,2) NOT NULL,
        PriceMultiplier DECIMAL(5,2) NOT NULL CONSTRAINT DF_TicketPricing_Multiplier DEFAULT 1.00,
        FinalPrice DECIMAL(12,2) NOT NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_TicketPricing_IsActive DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_TicketPricing_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_TicketPricing_Rule UNIQUE (DayType, TimeSlot, SeatTypeID),
        CONSTRAINT FK_TicketPricing_SeatType FOREIGN KEY (SeatTypeID) REFERENCES dbo.SeatType(SeatTypeID)
    );
    PRINT 'Created table dbo.TicketPricing';
END;
GO

/* ================================================================
   STEP 7. FOOD & BEVERAGES, PROMOTIONS & VOUCHERS
   ================================================================ */

IF OBJECT_ID(N'dbo.FoodBeverageItem', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.FoodBeverageItem (
        FoodBeverageItemID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_FoodBeverageItem PRIMARY KEY,
        [Name] NVARCHAR(100) NOT NULL CONSTRAINT UQ_FoodBeverageItem_Name UNIQUE,
        [Description] NVARCHAR(500) NULL,
        ItemType VARCHAR(20) NOT NULL,
        Price DECIMAL(12,2) NOT NULL,
        CostPrice DECIMAL(12,2) NULL,
        StockQuantity INT NOT NULL CONSTRAINT DF_FoodBeverageItem_Stock DEFAULT 0,
        ImageURL NVARCHAR(500) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_FoodBeverageItem_Status DEFAULT 'ACTIVE',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_FoodBeverageItem_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
    PRINT 'Created table dbo.FoodBeverageItem';
END;
GO

IF OBJECT_ID(N'dbo.ComboItem', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ComboItem (
        ComboItemID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ComboItem PRIMARY KEY,
        ParentItemID INT NOT NULL,
        ChildItemID INT NOT NULL,
        Quantity INT NOT NULL CONSTRAINT DF_ComboItem_Quantity DEFAULT 1,
        CONSTRAINT UQ_ComboItem_Pair UNIQUE (ParentItemID, ChildItemID),
        CONSTRAINT FK_ComboItem_Parent FOREIGN KEY (ParentItemID) REFERENCES dbo.FoodBeverageItem(FoodBeverageItemID),
        CONSTRAINT FK_ComboItem_Child FOREIGN KEY (ChildItemID) REFERENCES dbo.FoodBeverageItem(FoodBeverageItemID)
    );
    PRINT 'Created table dbo.ComboItem';
END;
GO

IF OBJECT_ID(N'dbo.Promotion', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Promotion (
        PromotionID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Promotion PRIMARY KEY,
        PromoCode VARCHAR(50) NOT NULL CONSTRAINT UQ_Promotion_PromoCode UNIQUE,
        [Name] NVARCHAR(150) NOT NULL,
        [Description] NVARCHAR(500) NULL,
        DiscountType VARCHAR(20) NOT NULL,
        DiscountValue DECIMAL(12,2) NOT NULL,
        MinOrderAmount DECIMAL(12,2) NULL,
        MaxDiscountAmount DECIMAL(12,2) NULL,
        StartDate DATETIME2 NOT NULL,
        EndDate DATETIME2 NOT NULL,
        UsageLimit INT NULL,
        UsedCount INT NOT NULL CONSTRAINT DF_Promotion_UsedCount DEFAULT 0,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Promotion_Status DEFAULT 'ACTIVE',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Promotion_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
    PRINT 'Created table dbo.Promotion';
END;
GO

IF OBJECT_ID(N'dbo.Voucher', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Voucher (
        VoucherID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Voucher PRIMARY KEY,
        VoucherCode VARCHAR(50) NOT NULL CONSTRAINT UQ_Voucher_Code UNIQUE,
        [Name] NVARCHAR(150) NOT NULL,
        [Description] NVARCHAR(500) NULL,
        DiscountType VARCHAR(20) NOT NULL,
        DiscountValue DECIMAL(12,2) NOT NULL,
        MinOrderAmount DECIMAL(12,2) NULL,
        MaxDiscountAmount DECIMAL(12,2) NULL,
        ValidFrom DATETIME2 NOT NULL,
        ValidTo DATETIME2 NOT NULL,
        ApplicableRankID INT NULL,
        TotalQuantity INT NOT NULL CONSTRAINT DF_Voucher_TotalQty DEFAULT 0,
        RemainingQuantity INT NOT NULL CONSTRAINT DF_Voucher_RemainQty DEFAULT 0,
        IsActive BIT NOT NULL CONSTRAINT DF_Voucher_IsActive DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Voucher_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_Voucher_Rank FOREIGN KEY (ApplicableRankID) REFERENCES dbo.MembershipRank(RankID)
    );
    PRINT 'Created table dbo.Voucher';
END;
GO

IF OBJECT_ID(N'dbo.CustomerVoucher', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CustomerVoucher (
        CustomerVoucherID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_CustomerVoucher PRIMARY KEY,
        CustomerID INT NOT NULL,
        VoucherID INT NOT NULL,
        AcquiredAt DATETIME2 NOT NULL CONSTRAINT DF_CustomerVoucher_AcquiredAt DEFAULT SYSDATETIME(),
        UsedAt DATETIME2 NULL,
        IsUsed BIT NOT NULL CONSTRAINT DF_CustomerVoucher_IsUsed DEFAULT 0,
        CONSTRAINT FK_CustomerVoucher_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customer(CustomerID),
        CONSTRAINT FK_CustomerVoucher_Voucher FOREIGN KEY (VoucherID) REFERENCES dbo.Voucher(VoucherID)
    );
    PRINT 'Created table dbo.CustomerVoucher';
END;
GO

/* ================================================================
   STEP 8. BOOKINGS, SEAT RESERVATIONS & PAYMENTS
   ================================================================ */

IF OBJECT_ID(N'dbo.Booking', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Booking (
        BookingID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Booking PRIMARY KEY,
        CustomerID INT NULL,
        StaffID INT NULL,
        ShowtimeID INT NOT NULL,
        PromotionID INT NULL,
        VoucherID INT NULL,
        BookingCode VARCHAR(30) NOT NULL CONSTRAINT UQ_Booking_Code UNIQUE,
        BookingType VARCHAR(20) NOT NULL CONSTRAINT DF_Booking_Type DEFAULT 'ONLINE',
        Subtotal DECIMAL(12,2) NOT NULL CONSTRAINT DF_Booking_Subtotal DEFAULT 0,
        DiscountAmount DECIMAL(12,2) NOT NULL CONSTRAINT DF_Booking_Discount DEFAULT 0,
        TotalAmount DECIMAL(12,2) NOT NULL CONSTRAINT DF_Booking_Total DEFAULT 0,
        PointsUsed INT NOT NULL CONSTRAINT DF_Booking_PointsUsed DEFAULT 0,
        PointsEarned INT NOT NULL CONSTRAINT DF_Booking_PointsEarned DEFAULT 0,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Booking_Status DEFAULT 'PENDING',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Booking_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_Booking_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customer(CustomerID),
        CONSTRAINT FK_Booking_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID),
        CONSTRAINT FK_Booking_Showtime FOREIGN KEY (ShowtimeID) REFERENCES dbo.Showtime(ShowtimeID),
        CONSTRAINT FK_Booking_Promotion FOREIGN KEY (PromotionID) REFERENCES dbo.Promotion(PromotionID),
        CONSTRAINT FK_Booking_Voucher FOREIGN KEY (VoucherID) REFERENCES dbo.Voucher(VoucherID)
    );
    PRINT 'Created table dbo.Booking';
END;
GO

IF OBJECT_ID(N'dbo.BookingSeat', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BookingSeat (
        BookingSeatID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookingSeat PRIMARY KEY,
        BookingID INT NOT NULL,
        ShowtimeID INT NOT NULL,
        SeatID INT NOT NULL,
        UnitPrice DECIMAL(12,2) NOT NULL,
        HeldUntil DATETIME2 NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_BookingSeat_Status DEFAULT 'HELD',
        CONSTRAINT UQ_BookingSeat_Showtime_Seat UNIQUE (ShowtimeID, SeatID),
        CONSTRAINT FK_BookingSeat_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID) ON DELETE CASCADE,
        CONSTRAINT FK_BookingSeat_Showtime FOREIGN KEY (ShowtimeID) REFERENCES dbo.Showtime(ShowtimeID),
        CONSTRAINT FK_BookingSeat_Seat FOREIGN KEY (SeatID) REFERENCES dbo.Seat(SeatID)
    );
    PRINT 'Created table dbo.BookingSeat';
END;
GO

IF OBJECT_ID(N'dbo.BookingFoodBeverage', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BookingFoodBeverage (
        BookingFoodBeverageID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookingFoodBeverage PRIMARY KEY,
        BookingID INT NOT NULL,
        FoodBeverageItemID INT NOT NULL,
        Quantity INT NOT NULL,
        UnitPrice DECIMAL(12,2) NOT NULL,
        CONSTRAINT FK_BookingFoodBeverage_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID) ON DELETE CASCADE,
        CONSTRAINT FK_BookingFoodBeverage_Item FOREIGN KEY (FoodBeverageItemID) REFERENCES dbo.FoodBeverageItem(FoodBeverageItemID)
    );
    PRINT 'Created table dbo.BookingFoodBeverage';
END;
GO

IF OBJECT_ID(N'dbo.Ticket', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Ticket (
        TicketID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Ticket PRIMARY KEY,
        BookingSeatID INT NOT NULL CONSTRAINT UQ_Ticket_BookingSeat UNIQUE,
        QRCode VARCHAR(500) NOT NULL CONSTRAINT UQ_Ticket_QRCode UNIQUE,
        TicketStatus VARCHAR(30) NOT NULL CONSTRAINT DF_Ticket_Status DEFAULT 'VALID',
        CheckedInAt DATETIME2 NULL,
        CONSTRAINT FK_Ticket_BookingSeat FOREIGN KEY (BookingSeatID) REFERENCES dbo.BookingSeat(BookingSeatID),
        CONSTRAINT CK_Ticket_Status CHECK (TicketStatus IN ('VALID', 'USED', 'CANCELLED'))
    );
    PRINT 'Created table dbo.Ticket';
END;
GO

IF OBJECT_ID(N'dbo.Payment', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Payment (
        PaymentID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Payment PRIMARY KEY,
        BookingID INT NOT NULL,
        PaymentMethod VARCHAR(30) NOT NULL,
        Amount DECIMAL(12,2) NOT NULL,
        TransactionCode VARCHAR(100) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Payment_Status DEFAULT 'PENDING',
        PaidAt DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Payment_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_Payment_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID)
    );
    PRINT 'Created table dbo.Payment';
END;
GO

IF OBJECT_ID(N'dbo.PaymentRefund', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.PaymentRefund (
        RefundID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_PaymentRefund PRIMARY KEY,
        PaymentID INT NOT NULL,
        Amount DECIMAL(12,2) NOT NULL,
        Reason NVARCHAR(255) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_PaymentRefund_Status DEFAULT 'COMPLETED',
        RefundedAt DATETIME2 NOT NULL CONSTRAINT DF_PaymentRefund_RefundedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_PaymentRefund_Payment FOREIGN KEY (PaymentID) REFERENCES dbo.Payment(PaymentID)
    );
    PRINT 'Created table dbo.PaymentRefund';
END;
GO

/* ================================================================
   STEP 9. REVIEWS & NEWS
   ================================================================ */

IF OBJECT_ID(N'dbo.MovieReview', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MovieReview (
        ReviewID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MovieReview PRIMARY KEY,
        MovieID INT NOT NULL,
        CustomerID INT NOT NULL,
        Rating INT NOT NULL CONSTRAINT CK_MovieReview_Rating CHECK (Rating BETWEEN 1 AND 5),
        Comment NVARCHAR(1000) NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_MovieReview_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_MovieReview_Customer_Movie UNIQUE (MovieID, CustomerID),
        CONSTRAINT FK_MovieReview_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID) ON DELETE CASCADE,
        CONSTRAINT FK_MovieReview_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customer(CustomerID)
    );
    PRINT 'Created table dbo.MovieReview';
END;
GO

IF OBJECT_ID(N'dbo.NewsArticle', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.NewsArticle (
        ArticleID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_NewsArticle PRIMARY KEY,
        Title NVARCHAR(200) NOT NULL,
        Summary NVARCHAR(500) NULL,
        Content NVARCHAR(MAX) NOT NULL,
        ThumbnailURL NVARCHAR(500) NULL,
        Author NVARCHAR(100) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_NewsArticle_Status DEFAULT 'PUBLISHED',
        PublishedAt DATETIME2 NOT NULL CONSTRAINT DF_NewsArticle_PublishedAt DEFAULT SYSDATETIME(),
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_NewsArticle_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
    PRINT 'Created table dbo.NewsArticle';
END;
GO

/* ================================================================
   STEP 10. SYSTEM SETTING TABLE & EXTENSION
   ================================================================ */

IF OBJECT_ID(N'dbo.SystemSetting', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SystemSetting (
        SettingID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SystemSetting PRIMARY KEY,
        SettingKey VARCHAR(100) NOT NULL CONSTRAINT UQ_SystemSetting_Key UNIQUE,
        Value NVARCHAR(50) NULL,
        [Description] NVARCHAR(500) NULL,
        SettingName NVARCHAR(255) NULL,
        Category NVARCHAR(50) NULL,
        ValueType NVARCHAR(20) NULL,
        DefaultValue NVARCHAR(50) NULL,
        ValidationRules NVARCHAR(100) NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_SystemSetting_IsActive DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_SystemSetting_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
    PRINT 'Created table dbo.SystemSetting';
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.SystemSetting', N'Value') IS NULL
        ALTER TABLE dbo.SystemSetting ADD Value NVARCHAR(50) NULL;
    IF COL_LENGTH(N'dbo.SystemSetting', N'SettingName') IS NULL
        ALTER TABLE dbo.SystemSetting ADD SettingName NVARCHAR(255) NULL;
    IF COL_LENGTH(N'dbo.SystemSetting', N'Category') IS NULL
        ALTER TABLE dbo.SystemSetting ADD Category NVARCHAR(50) NULL;
    IF COL_LENGTH(N'dbo.SystemSetting', N'ValueType') IS NULL
        ALTER TABLE dbo.SystemSetting ADD ValueType NVARCHAR(20) NULL;
    IF COL_LENGTH(N'dbo.SystemSetting', N'DefaultValue') IS NULL
        ALTER TABLE dbo.SystemSetting ADD DefaultValue NVARCHAR(50) NULL;
    IF COL_LENGTH(N'dbo.SystemSetting', N'ValidationRules') IS NULL
        ALTER TABLE dbo.SystemSetting ADD ValidationRules NVARCHAR(100) NULL;
    IF COL_LENGTH(N'dbo.SystemSetting', N'IsActive') IS NULL
        ALTER TABLE dbo.SystemSetting ADD IsActive BIT NOT NULL CONSTRAINT DF_SystemSetting_IsActive DEFAULT 1;
    IF COL_LENGTH(N'dbo.SystemSetting', N'CreatedAt') IS NULL
        ALTER TABLE dbo.SystemSetting ADD CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_SystemSetting_CreatedAt DEFAULT SYSDATETIME();
    IF COL_LENGTH(N'dbo.SystemSetting', N'UpdatedAt') IS NULL
        ALTER TABLE dbo.SystemSetting ADD UpdatedAt DATETIME2 NULL;
    PRINT 'Extended columns in dbo.SystemSetting';
END;
GO

-- Backfill Value from SettingValue if old column exists
IF COL_LENGTH(N'dbo.SystemSetting', N'SettingValue') IS NOT NULL
BEGIN
    EXEC(N'UPDATE dbo.SystemSetting SET Value = COALESCE(Value, SettingValue) WHERE Value IS NULL;');
END;
GO

/* ================================================================
   STEP 11. AUTH TABLES (OTP, RATE LIMIT, OAUTH2)
   ================================================================ */

IF OBJECT_ID(N'dbo.OtpToken', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OtpToken (
        OtpTokenID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OtpToken PRIMARY KEY,
        AccountID INT NULL,
        Email VARCHAR(255) NOT NULL,
        OtpHash VARCHAR(255) NOT NULL,
        Purpose VARCHAR(30) NOT NULL,
        ExpiresAt DATETIME2 NOT NULL,
        AttemptsCount INT NOT NULL CONSTRAINT DF_OtpToken_Attempts DEFAULT 0,
        MaxAttempts INT NOT NULL CONSTRAINT DF_OtpToken_MaxAttempts DEFAULT 5,
        UsedAt DATETIME2 NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_OtpToken_CreatedAt DEFAULT SYSDATETIME(),
        SendCount INT NULL CONSTRAINT DF_OtpToken_SendCount DEFAULT 0,
        SendCountDate DATETIME NULL,
        CONSTRAINT FK_OtpToken_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID) ON DELETE SET NULL
    );
    PRINT 'Created table dbo.OtpToken';
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.OtpToken', N'SendCount') IS NULL
        ALTER TABLE dbo.OtpToken ADD SendCount INT NULL CONSTRAINT DF_OtpToken_SendCount DEFAULT 0;
    IF COL_LENGTH(N'dbo.OtpToken', N'SendCountDate') IS NULL
        ALTER TABLE dbo.OtpToken ADD SendCountDate DATETIME NULL;
    PRINT 'Extended columns in dbo.OtpToken';
END;
GO

IF OBJECT_ID(N'dbo.OtpAttempt', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OtpAttempt (
        AttemptID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OtpAttempt PRIMARY KEY,
        OtpTokenID INT NULL,
        Email VARCHAR(255) NOT NULL,
        Purpose VARCHAR(30) NOT NULL,
        IpAddress VARCHAR(45) NULL,
        UserAgent NVARCHAR(500) NULL,
        Success BIT NOT NULL,
        AttemptedAt DATETIME2 NOT NULL CONSTRAINT DF_OtpAttempt_AttemptedAt DEFAULT SYSDATETIME()
    );
    PRINT 'Created table dbo.OtpAttempt';
END;
GO

IF OBJECT_ID(N'dbo.LoginAttempt', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoginAttempt (
        AttemptID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LoginAttempt PRIMARY KEY,
        Identifier VARCHAR(255) NOT NULL,
        IPAddress VARCHAR(45) NOT NULL,
        UserAgent NVARCHAR(500) NULL,
        Success BIT NOT NULL,
        FailureReason VARCHAR(50) NULL,
        AttemptedAt DATETIME2 NOT NULL CONSTRAINT DF_LoginAttempt_AttemptedAt DEFAULT SYSDATETIME()
    );
    PRINT 'Created table dbo.LoginAttempt';
END;
GO

IF OBJECT_ID(N'dbo.IpRateLimit', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.IpRateLimit (
        IPAddress VARCHAR(45) NOT NULL CONSTRAINT PK_IpRateLimit PRIMARY KEY,
        FailedCount INT NOT NULL CONSTRAINT DF_IpRateLimit_FailedCount DEFAULT 0,
        FirstFailedAt DATETIME2 NULL,
        LockedUntil DATETIME2 NULL
    );
    PRINT 'Created table dbo.IpRateLimit';
END;
GO

IF OBJECT_ID(N'dbo.OAuthAccount', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OAuthAccount (
        OAuthAccountID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OAuthAccount PRIMARY KEY,
        AccountID INT NOT NULL,
        Provider VARCHAR(20) NOT NULL,
        ProviderUserID VARCHAR(255) NOT NULL,
        Email VARCHAR(255) NOT NULL,
        LinkedAt DATETIME2 NOT NULL CONSTRAINT DF_OAuthAccount_LinkedAt DEFAULT SYSDATETIME(),
        CONSTRAINT UQ_OAuthAccount_Provider_User UNIQUE (Provider, ProviderUserID),
        CONSTRAINT FK_OAuthAccount_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID) ON DELETE CASCADE
    );
    PRINT 'Created table dbo.OAuthAccount';
END;
GO

/* ================================================================
   STEP 12. CHECK CONSTRAINTS, FOREIGN KEYS & PERFORMANCE INDEXES
   ================================================================ */

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE [name] = N'CK_OtpToken_Purpose' AND parent_object_id = OBJECT_ID(N'dbo.OtpToken'))
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OtpToken WHERE Purpose NOT IN (N'ACTIVATION', N'RESET_PASSWORD'))
        ALTER TABLE dbo.OtpToken WITH NOCHECK
            ADD CONSTRAINT CK_OtpToken_Purpose CHECK (Purpose IN (N'ACTIVATION', N'RESET_PASSWORD'));
    ELSE
        ALTER TABLE dbo.OtpToken WITH CHECK
            ADD CONSTRAINT CK_OtpToken_Purpose CHECK (Purpose IN (N'ACTIVATION', N'RESET_PASSWORD'));
    PRINT 'Added constraint CK_OtpToken_Purpose';
END;
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
    PRINT 'Added constraint CK_OAuthAccount_Provider';
END;
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
    PRINT 'Added foreign key FK_OtpAttempt_OtpToken';
END;
GO

-- 8 High performance indexes
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

/* ================================================================
   STEP 13. SEED ESSENTIAL SYSTEM CONFIGURATIONS
   ================================================================ */

DECLARE @AuthSettings TABLE (
    SettingKey VARCHAR(100), Value NVARCHAR(50), [Description] NVARCHAR(500),
    SettingName NVARCHAR(255), DefaultValue NVARCHAR(50), ValidationRules NVARCHAR(100)
);
INSERT @AuthSettings VALUES
    ('SYSTEM_PASSWORD_MIN_LENGTH',     N'8',  N'Minimum password length',                              N'Password Minimum Length', N'8',  N'min:6,max:32'),
    ('SYSTEM_OTP_EXPIRY_MINUTES',      N'5',  N'OTP validity in minutes',                              N'OTP Expiry Time',         N'5',  N'min:1,max:30'),
    ('SYSTEM_OTP_MAX_SEND_DAILY',      N'5',  N'Maximum OTP codes that can be sent per email per day', N'OTP Max Sends Per Day',   N'5',  N'min:1,max:20'),
    ('SYSTEM_OTP_RESEND_COOLDOWN',     N'60', N'Seconds to wait between OTP resends',                  N'OTP Resend Cooldown',     N'60', N'min:10,max:300'),
    ('SYSTEM_SESSION_TIMEOUT_MINUTES', N'30', N'Session timeout in minutes',                           N'Session Timeout',         N'30', N'min:5,max:480'),
    ('SYSTEM_LOGIN_ATTEMPTS',          N'5',  N'Maximum failed login attempts before lockout',         N'Max Login Attempts',      N'5',  N'min:1,max:10'),
    ('BOOKING_HOLD_MINUTES',           N'10', N'Thời gian giữ ghế chưa thanh toán',                     N'Seat Hold Time',          N'10', N'min:1,max:60');

INSERT dbo.SystemSetting
    (SettingKey, Value, [Description], SettingName, Category, ValueType, DefaultValue, ValidationRules, IsActive)
SELECT s.SettingKey, s.Value, s.[Description], s.SettingName, N'SYSTEM', N'NUMBER', s.DefaultValue, s.ValidationRules, 1
FROM @AuthSettings s
WHERE NOT EXISTS (SELECT 1 FROM dbo.SystemSetting x WHERE x.SettingKey = s.SettingKey);
GO

PRINT '>>> [DONE] Non-destructive Database Schema Synchronization completed successfully!';
GO
