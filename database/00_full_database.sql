/*
    Cinema Management System - complete local database
    DBMS: Microsoft SQL Server 2019+

    This is the single database setup file shared by the team.

    - On a clean SQL Server instance it creates CinemaManagementDB,
      the complete schema, constraints, indexes, and demo data.
    - On an existing team database it adds compatible missing columns
      and inserts only seed records that do not already exist.
    - The script is safe to run more than once.
*/

USE master;
GO

IF DB_ID(N'CinemaManagementDB') IS NULL
    CREATE DATABASE CinemaManagementDB;
GO

USE CinemaManagementDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

/* ================================================================
   1. AUTHORIZATION AND ACCOUNT TABLES
   ================================================================ */

IF OBJECT_ID(N'dbo.Role', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[Role] (
        RoleID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Role PRIMARY KEY,
        [Name] NVARCHAR(50) NOT NULL CONSTRAINT UQ_Role_Name UNIQUE,
        [Description] NVARCHAR(255) NULL
    );
END;

IF OBJECT_ID(N'dbo.Permission', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Permission (
        PermissionID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Permission PRIMARY KEY,
        [Name] NVARCHAR(100) NOT NULL CONSTRAINT UQ_Permission_Name UNIQUE,
        [Description] NVARCHAR(255) NULL
    );
END;

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
        CONSTRAINT CK_MembershipRank_MinPoints CHECK (MinPoints >= 0)
    );
END;

/* Upgrade an existing database that still has the legacy rank columns. */
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

IF COL_LENGTH(N'dbo.MembershipRank', N'MinTierPoints') IS NOT NULL
BEGIN
    EXEC(N'UPDATE dbo.MembershipRank SET MinPoints = COALESCE(MinPoints, MinTierPoints)');
    /* Keep the legacy column only for backward compatibility; new writes no longer populate it. */
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
           IsActive = COALESCE(IsActive, 1)');
ALTER TABLE dbo.MembershipRank ALTER COLUMN MinPoints INT NOT NULL;
ALTER TABLE dbo.MembershipRank ALTER COLUMN IsActive BIT NOT NULL;

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
END;

IF COL_LENGTH(N'dbo.Account', N'CurrentAddress') IS NULL
    ALTER TABLE dbo.Account ADD CurrentAddress NVARCHAR(255) NULL;
IF COL_LENGTH(N'dbo.Account', N'AvatarUrl') IS NULL
    ALTER TABLE dbo.Account ADD AvatarUrl NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Account', N'Notes') IS NULL
    ALTER TABLE dbo.Account ADD Notes NVARCHAR(500) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Account') AND [name] = N'UX_Account_Phone')
    CREATE UNIQUE INDEX UX_Account_Phone ON dbo.Account(Phone) WHERE Phone IS NOT NULL;

IF OBJECT_ID(N'dbo.RolePermission', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.RolePermission (
        RoleID INT NOT NULL,
        PermissionID INT NOT NULL,
        CONSTRAINT PK_RolePermission PRIMARY KEY (RoleID, PermissionID),
        CONSTRAINT FK_RolePermission_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID),
        CONSTRAINT FK_RolePermission_Permission FOREIGN KEY (PermissionID) REFERENCES dbo.Permission(PermissionID)
    );
END;

IF OBJECT_ID(N'dbo.Staff', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Staff (
        StaffID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Staff PRIMARY KEY,
        AccountID INT NOT NULL CONSTRAINT UQ_Staff_Account UNIQUE,
        Position NVARCHAR(100) NOT NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Staff_Status DEFAULT 'ACTIVE',
        CONSTRAINT FK_Staff_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
    );
END;

IF OBJECT_ID(N'dbo.Customer', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Customer (
        CustomerID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Customer PRIMARY KEY,
        AccountID INT NOT NULL CONSTRAINT UQ_Customer_Account UNIQUE,
        RankID INT NULL,
        DateOfBirth DATE NULL,
        [Address] NVARCHAR(255) NULL,
        AvailablePoints INT NOT NULL CONSTRAINT DF_Customer_AvailablePoints DEFAULT 0,
        TierPoints INT NOT NULL CONSTRAINT DF_Customer_TierPoints DEFAULT 0,
        ReceivePromotionEmail BIT NOT NULL CONSTRAINT DF_Customer_PromotionEmail DEFAULT 0,
        CONSTRAINT FK_Customer_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID),
        CONSTRAINT FK_Customer_MembershipRank FOREIGN KEY (RankID) REFERENCES dbo.MembershipRank(RankID),
        CONSTRAINT CK_Customer_AvailablePoints CHECK (AvailablePoints >= 0),
        CONSTRAINT CK_Customer_TierPoints CHECK (TierPoints >= 0)
    );
END;
GO

/* ================================================================
   2. MOVIE CATALOG
   PosterURL and TrailerURL follow the project ERD naming.
   ================================================================ */

IF OBJECT_ID(N'dbo.Genre', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Genre (
        GenreID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Genre PRIMARY KEY,
        [Name] NVARCHAR(100) NOT NULL CONSTRAINT UQ_Genre_Name UNIQUE,
        [Description] NVARCHAR(500) NULL,
        [Status] NVARCHAR(20) NOT NULL CONSTRAINT DF_Genre_Status DEFAULT 'ACTIVE',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Genre_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
END;

IF COL_LENGTH(N'dbo.Genre', N'Description') IS NULL ALTER TABLE dbo.Genre ADD [Description] NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Genre', N'Status') IS NULL ALTER TABLE dbo.Genre ADD [Status] NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE' WITH VALUES;
IF COL_LENGTH(N'dbo.Genre', N'CreatedAt') IS NULL ALTER TABLE dbo.Genre ADD CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
IF COL_LENGTH(N'dbo.Genre', N'UpdatedAt') IS NULL ALTER TABLE dbo.Genre ADD UpdatedAt DATETIME2 NULL;

IF OBJECT_ID(N'dbo.Movie', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Movie (
        MovieID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Movie PRIMARY KEY,
        Title NVARCHAR(250) NOT NULL,
        OriginalTitle NVARCHAR(250) NULL,
        Duration INT NOT NULL,
        ReleaseDate DATE NULL,
        EndDate DATE NULL,
        [Language] NVARCHAR(100) NULL,
        Director NVARCHAR(150) NULL,
        Actors NVARCHAR(500) NULL,
        AgeRating NVARCHAR(10) NOT NULL CONSTRAINT DF_Movie_AgeRating DEFAULT 'P',
        [Description] NVARCHAR(1000) NULL,
        PosterURL NVARCHAR(500) NULL,
        TrailerURL NVARCHAR(500) NULL,
        [Status] NVARCHAR(50) NOT NULL CONSTRAINT DF_Movie_Status DEFAULT 'Upcoming',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Movie_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT CK_Movie_Duration CHECK (Duration > 0),
        CONSTRAINT CK_Movie_DateRange CHECK (EndDate IS NULL OR ReleaseDate IS NULL OR EndDate >= ReleaseDate),
        CONSTRAINT CK_Movie_Status CHECK ([Status] IN ('Upcoming', 'Now Showing', 'Ended', 'Archived'))
    );
END;

IF COL_LENGTH(N'dbo.Movie', N'OriginalTitle') IS NULL ALTER TABLE dbo.Movie ADD OriginalTitle NVARCHAR(250) NULL;
IF COL_LENGTH(N'dbo.Movie', N'EndDate') IS NULL ALTER TABLE dbo.Movie ADD EndDate DATE NULL;
IF COL_LENGTH(N'dbo.Movie', N'Actors') IS NULL ALTER TABLE dbo.Movie ADD Actors NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Movie', N'PosterURL') IS NULL ALTER TABLE dbo.Movie ADD PosterURL NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Movie', N'TrailerURL') IS NULL ALTER TABLE dbo.Movie ADD TrailerURL NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Movie', N'CreatedAt') IS NULL ALTER TABLE dbo.Movie ADD CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
IF COL_LENGTH(N'dbo.Movie', N'UpdatedAt') IS NULL ALTER TABLE dbo.Movie ADD UpdatedAt DATETIME2 NULL;

IF OBJECT_ID(N'dbo.MovieGenre', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MovieGenre (
        MovieID INT NOT NULL,
        GenreID INT NOT NULL,
        AssignedAt DATETIME2 NOT NULL CONSTRAINT DF_MovieGenre_AssignedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_MovieGenre PRIMARY KEY (MovieID, GenreID),
        CONSTRAINT FK_MovieGenre_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID) ON DELETE CASCADE,
        CONSTRAINT FK_MovieGenre_Genre FOREIGN KEY (GenreID) REFERENCES dbo.Genre(GenreID)
    );
END;
IF COL_LENGTH(N'dbo.MovieGenre', N'AssignedAt') IS NULL ALTER TABLE dbo.MovieGenre ADD AssignedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
GO

/* ================================================================
   3. ROOMS, SEATS, PRICING, AND SHOWTIMES
   ================================================================ */

IF OBJECT_ID(N'dbo.Room', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Room (
        RoomID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Room PRIMARY KEY,
        RoomCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_Room_RoomCode UNIQUE,
        RoomName NVARCHAR(100) NOT NULL CONSTRAINT UQ_Room_RoomName UNIQUE,
        RoomType NVARCHAR(50) NOT NULL,
        Capacity INT NOT NULL,
        TotalRows INT NOT NULL,
        SeatsPerRow INT NOT NULL,
        Projector NVARCHAR(150) NULL,
        SoundSystem NVARCHAR(150) NULL,
        ScreenSize NVARCHAR(50) NULL,
        [Status] NVARCHAR(50) NOT NULL CONSTRAINT DF_Room_Status DEFAULT 'Ready',
        [Description] NVARCHAR(500) NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Room_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT CK_Room_Capacity CHECK (Capacity > 0),
        CONSTRAINT CK_Room_TotalRows CHECK (TotalRows > 0),
        CONSTRAINT CK_Room_SeatsPerRow CHECK (SeatsPerRow > 0),
        CONSTRAINT CK_Room_Status CHECK ([Status] IN ('Ready', 'Screening', 'Cleaning', 'Maintenance'))
    );
END;

IF COL_LENGTH(N'dbo.Room', N'RoomCode') IS NULL ALTER TABLE dbo.Room ADD RoomCode NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.Room', N'TotalRows') IS NULL ALTER TABLE dbo.Room ADD TotalRows INT NOT NULL DEFAULT 1 WITH VALUES;
IF COL_LENGTH(N'dbo.Room', N'SeatsPerRow') IS NULL ALTER TABLE dbo.Room ADD SeatsPerRow INT NOT NULL DEFAULT 1 WITH VALUES;
IF COL_LENGTH(N'dbo.Room', N'Projector') IS NULL ALTER TABLE dbo.Room ADD Projector NVARCHAR(150) NULL;
IF COL_LENGTH(N'dbo.Room', N'SoundSystem') IS NULL ALTER TABLE dbo.Room ADD SoundSystem NVARCHAR(150) NULL;
IF COL_LENGTH(N'dbo.Room', N'ScreenSize') IS NULL ALTER TABLE dbo.Room ADD ScreenSize NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.Room', N'Description') IS NULL ALTER TABLE dbo.Room ADD [Description] NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Room', N'CreatedAt') IS NULL ALTER TABLE dbo.Room ADD CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
IF COL_LENGTH(N'dbo.Room', N'UpdatedAt') IS NULL ALTER TABLE dbo.Room ADD UpdatedAt DATETIME2 NULL;
GO
UPDATE dbo.Room SET RoomCode = CONCAT(N'ROOM-', RIGHT(CONCAT(N'00', RoomID), 2)) WHERE RoomCode IS NULL;
UPDATE dbo.Room SET [Status] = N'Ready' WHERE UPPER([Status]) = N'ACTIVE';

IF OBJECT_ID(N'dbo.SeatType', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SeatType (
        SeatTypeID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SeatType PRIMARY KEY,
        TypeCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_SeatType_TypeCode UNIQUE,
        TypeName NVARCHAR(100) NOT NULL CONSTRAINT UQ_SeatType_TypeName UNIQUE,
        [Description] NVARCHAR(255) NULL,
        PriceMultiplier DECIMAL(5,2) NOT NULL CONSTRAINT DF_SeatType_Multiplier DEFAULT 1.00,
        ColorCode NVARCHAR(20) NULL,
        [Status] NVARCHAR(20) NOT NULL CONSTRAINT DF_SeatType_Status DEFAULT 'Active',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_SeatType_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT CK_SeatType_Multiplier CHECK (PriceMultiplier BETWEEN 0.50 AND 3.00)
    );
END;

IF COL_LENGTH(N'dbo.SeatType', N'TypeCode') IS NULL ALTER TABLE dbo.SeatType ADD TypeCode NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.SeatType', N'ColorCode') IS NULL ALTER TABLE dbo.SeatType ADD ColorCode NVARCHAR(20) NULL;
IF COL_LENGTH(N'dbo.SeatType', N'CreatedAt') IS NULL ALTER TABLE dbo.SeatType ADD CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
IF COL_LENGTH(N'dbo.SeatType', N'UpdatedAt') IS NULL ALTER TABLE dbo.SeatType ADD UpdatedAt DATETIME2 NULL;
GO
UPDATE dbo.SeatType
SET TypeCode = CASE
    WHEN UPPER(TypeName) LIKE N'%VIP%' THEN N'VIP'
    WHEN UPPER(TypeName) LIKE N'%COUPLE%' OR UPPER(TypeName) LIKE N'%SWEETBOX%' THEN N'COUPLE'
    ELSE N'STD'
END
WHERE TypeCode IS NULL;

IF OBJECT_ID(N'dbo.Seat', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Seat (
        SeatID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Seat PRIMARY KEY,
        RoomID INT NOT NULL,
        SeatTypeID INT NOT NULL,
        SeatTypeName NVARCHAR(100) NOT NULL,
        RowLabel NVARCHAR(5) NOT NULL,
        SeatNumber INT NOT NULL,
        GridRow INT NOT NULL,
        GridColumn INT NOT NULL,
        [Status] NVARCHAR(20) NOT NULL CONSTRAINT DF_Seat_Status DEFAULT 'Active',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Seat_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_Seat_Room_Position UNIQUE (RoomID, RowLabel, SeatNumber),
        CONSTRAINT UQ_Seat_Room_Grid UNIQUE (RoomID, GridRow, GridColumn),
        CONSTRAINT FK_Seat_Room FOREIGN KEY (RoomID) REFERENCES dbo.Room(RoomID) ON DELETE CASCADE,
        CONSTRAINT FK_Seat_SeatType FOREIGN KEY (SeatTypeID) REFERENCES dbo.SeatType(SeatTypeID),
        CONSTRAINT CK_Seat_Number CHECK (SeatNumber > 0),
        CONSTRAINT CK_Seat_Grid CHECK (GridRow > 0 AND GridColumn > 0)
    );
END;

IF COL_LENGTH(N'dbo.Seat', N'SeatTypeName') IS NULL ALTER TABLE dbo.Seat ADD SeatTypeName NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.Seat', N'CreatedAt') IS NULL ALTER TABLE dbo.Seat ADD CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
GO
UPDATE s SET SeatTypeName = st.TypeName
FROM dbo.Seat s JOIN dbo.SeatType st ON st.SeatTypeID = s.SeatTypeID
WHERE s.SeatTypeName IS NULL;

IF OBJECT_ID(N'dbo.TicketPricing', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.TicketPricing (
        PricingID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_TicketPricing PRIMARY KEY,
        PricingCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_TicketPricing_Code UNIQUE,
        RuleName NVARCHAR(150) NOT NULL,
        DayType NVARCHAR(50) NOT NULL,
        TimeSlot NVARCHAR(50) NOT NULL,
        BasePrice DECIMAL(12,2) NOT NULL,
        FormatSurcharge DECIMAL(12,2) NOT NULL CONSTRAINT DF_TicketPricing_Surcharge DEFAULT 0,
        ExperienceType NVARCHAR(50) NOT NULL CONSTRAINT DF_TicketPricing_Experience DEFAULT '2D Digital',
        [Status] NVARCHAR(20) NOT NULL CONSTRAINT DF_TicketPricing_Status DEFAULT 'Active',
        [Description] NVARCHAR(255) NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_TicketPricing_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT CK_TicketPricing_BasePrice CHECK (BasePrice >= 0),
        CONSTRAINT CK_TicketPricing_Surcharge CHECK (FormatSurcharge >= 0)
    );
END;

IF OBJECT_ID(N'dbo.Showtime', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Showtime (
        ShowtimeID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Showtime PRIMARY KEY,
        MovieID INT NOT NULL,
        RoomID INT NOT NULL,
        StartTime DATETIME2 NOT NULL,
        EndTime DATETIME2 NOT NULL,
        BasePrice DECIMAL(18,2) NOT NULL,
        [Status] NVARCHAR(30) NOT NULL CONSTRAINT DF_Showtime_Status DEFAULT 'Scheduled',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Showtime_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT FK_Showtime_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID),
        CONSTRAINT FK_Showtime_Room FOREIGN KEY (RoomID) REFERENCES dbo.Room(RoomID),
        CONSTRAINT CK_Showtime_TimeRange CHECK (EndTime > StartTime),
        CONSTRAINT CK_Showtime_BasePrice CHECK (BasePrice >= 0)
    );
END;

IF COL_LENGTH(N'dbo.Showtime', N'CreatedAt') IS NULL ALTER TABLE dbo.Showtime ADD CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
IF COL_LENGTH(N'dbo.Showtime', N'UpdatedAt') IS NULL ALTER TABLE dbo.Showtime ADD UpdatedAt DATETIME2 NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Showtime') AND [name] = N'IX_Showtime_Movie_StartTime')
    CREATE INDEX IX_Showtime_Movie_StartTime ON dbo.Showtime(MovieID, StartTime);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Showtime') AND [name] = N'IX_Showtime_Room_StartTime')
    CREATE INDEX IX_Showtime_Room_StartTime ON dbo.Showtime(RoomID, StartTime);
GO

/* ================================================================
   4. FOOD AND BEVERAGE
   ================================================================ */

IF OBJECT_ID(N'dbo.FoodBeverageItem', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.FoodBeverageItem (
        FoodBeverageItemID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_FoodBeverageItem PRIMARY KEY,
        ItemCode NVARCHAR(50) NOT NULL CONSTRAINT UQ_FoodBeverageItem_Code UNIQUE,
        [Name] NVARCHAR(150) NOT NULL,
        [Description] NVARCHAR(500) NULL,
        ItemType NVARCHAR(50) NOT NULL,
        Price DECIMAL(18,2) NOT NULL,
        CostPrice DECIMAL(18,2) NOT NULL CONSTRAINT DF_FoodBeverageItem_Cost DEFAULT 0,
        ImageURL NVARCHAR(500) NULL,
        [Status] NVARCHAR(30) NOT NULL CONSTRAINT DF_FoodBeverageItem_Status DEFAULT 'Active',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_FoodBeverageItem_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT CK_FoodBeverageItem_Price CHECK (Price >= 0),
        CONSTRAINT CK_FoodBeverageItem_Cost CHECK (CostPrice >= 0)
    );
END;

IF COL_LENGTH(N'dbo.FoodBeverageItem', N'ItemCode') IS NULL ALTER TABLE dbo.FoodBeverageItem ADD ItemCode NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.FoodBeverageItem', N'CostPrice') IS NULL ALTER TABLE dbo.FoodBeverageItem ADD CostPrice DECIMAL(18,2) NOT NULL DEFAULT 0 WITH VALUES;
IF COL_LENGTH(N'dbo.FoodBeverageItem', N'CreatedAt') IS NULL ALTER TABLE dbo.FoodBeverageItem ADD CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
IF COL_LENGTH(N'dbo.FoodBeverageItem', N'UpdatedAt') IS NULL ALTER TABLE dbo.FoodBeverageItem ADD UpdatedAt DATETIME2 NULL;
GO
UPDATE dbo.FoodBeverageItem
SET ItemCode = CONCAT(N'FB-', RIGHT(CONCAT(N'000', FoodBeverageItemID), 3))
WHERE ItemCode IS NULL;

IF OBJECT_ID(N'dbo.ComboItem', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ComboItem (
        ComboItemID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ComboItem PRIMARY KEY,
        ComboID INT NOT NULL,
        ComponentItemID INT NOT NULL,
        Quantity INT NOT NULL CONSTRAINT DF_ComboItem_Quantity DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_ComboItem_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT UQ_ComboItem UNIQUE (ComboID, ComponentItemID),
        CONSTRAINT FK_ComboItem_Combo FOREIGN KEY (ComboID) REFERENCES dbo.FoodBeverageItem(FoodBeverageItemID) ON DELETE CASCADE,
        CONSTRAINT FK_ComboItem_Component FOREIGN KEY (ComponentItemID) REFERENCES dbo.FoodBeverageItem(FoodBeverageItemID),
        CONSTRAINT CK_ComboItem_Quantity CHECK (Quantity > 0),
        CONSTRAINT CK_ComboItem_DifferentItems CHECK (ComboID <> ComponentItemID)
    );
END;
GO

/* ================================================================
   5. PROMOTIONS, BOOKINGS, PAYMENTS, AND TICKETS
   ================================================================ */

IF OBJECT_ID(N'dbo.Promotion', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Promotion (
        PromotionID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Promotion PRIMARY KEY,
        [Name] NVARCHAR(150) NOT NULL,
        [Description] NVARCHAR(500) NULL,
        StartDate DATETIME2 NOT NULL,
        EndDate DATETIME2 NOT NULL,
        [Status] VARCHAR(30) NOT NULL,
        CONSTRAINT CK_Promotion_DateRange CHECK (EndDate >= StartDate)
    );
END;

IF OBJECT_ID(N'dbo.Voucher', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Voucher (
        VoucherID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Voucher PRIMARY KEY,
        PromotionID INT NOT NULL,
        RequiredRankID INT NULL,
        Code VARCHAR(50) NOT NULL CONSTRAINT UQ_Voucher_Code UNIQUE,
        DiscountType VARCHAR(30) NOT NULL,
        DiscountValue DECIMAL(18,2) NOT NULL,
        MaxDiscountAmount DECIMAL(18,2) NULL,
        MinBookingAmount DECIMAL(18,2) NULL,
        ValidFrom DATETIME2 NOT NULL,
        ValidTo DATETIME2 NOT NULL,
        TotalUsageLimit INT NULL,
        PerAccountLimit INT NULL,
        [Status] VARCHAR(30) NOT NULL,
        CONSTRAINT FK_Voucher_Promotion FOREIGN KEY (PromotionID) REFERENCES dbo.Promotion(PromotionID),
        CONSTRAINT FK_Voucher_Rank FOREIGN KEY (RequiredRankID) REFERENCES dbo.MembershipRank(RankID),
        CONSTRAINT CK_Voucher_DateRange CHECK (ValidTo >= ValidFrom),
        CONSTRAINT CK_Voucher_Value CHECK (DiscountValue > 0)
    );
END;

IF OBJECT_ID(N'dbo.Booking', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Booking (
        BookingID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Booking PRIMARY KEY,
        CustomerID INT NULL,
        StaffID INT NULL,
        ShowtimeID INT NOT NULL,
        VoucherID INT NULL,
        BookingCode VARCHAR(30) NOT NULL CONSTRAINT UQ_Booking_Code UNIQUE,
        BookingType VARCHAR(30) NOT NULL,
        Subtotal DECIMAL(18,2) NOT NULL,
        DiscountAmount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Booking_Discount DEFAULT 0,
        TotalAmount DECIMAL(18,2) NOT NULL,
        BookingTime DATETIME2 NOT NULL CONSTRAINT DF_Booking_Time DEFAULT SYSDATETIME(),
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Booking_Status DEFAULT 'PENDING',
        CONSTRAINT FK_Booking_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customer(CustomerID),
        CONSTRAINT FK_Booking_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID),
        CONSTRAINT FK_Booking_Showtime FOREIGN KEY (ShowtimeID) REFERENCES dbo.Showtime(ShowtimeID),
        CONSTRAINT FK_Booking_Voucher FOREIGN KEY (VoucherID) REFERENCES dbo.Voucher(VoucherID),
        CONSTRAINT CK_Booking_Status CHECK ([Status] IN ('PENDING', 'PAYMENT_PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
        CONSTRAINT CK_Booking_Amount CHECK (Subtotal >= 0 AND DiscountAmount >= 0 AND TotalAmount >= 0)
    );
END;

IF OBJECT_ID(N'dbo.BookingSeat', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BookingSeat (
        BookingSeatID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookingSeat PRIMARY KEY,
        BookingID INT NOT NULL,
        SeatID INT NOT NULL,
        ShowtimeID INT NOT NULL,
        UnitPrice DECIMAL(18,2) NOT NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_BookingSeat_Status DEFAULT 'HELD',
        HeldUntil DATETIME2 NULL,
        CONSTRAINT UQ_BookingSeat_Showtime_Seat UNIQUE (ShowtimeID, SeatID),
        CONSTRAINT FK_BookingSeat_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID),
        CONSTRAINT FK_BookingSeat_Seat FOREIGN KEY (SeatID) REFERENCES dbo.Seat(SeatID),
        CONSTRAINT FK_BookingSeat_Showtime FOREIGN KEY (ShowtimeID) REFERENCES dbo.Showtime(ShowtimeID),
        CONSTRAINT CK_BookingSeat_Status CHECK ([Status] IN ('HELD', 'BOOKED', 'RELEASED')),
        CONSTRAINT CK_BookingSeat_Price CHECK (UnitPrice >= 0)
    );
END;

IF OBJECT_ID(N'dbo.BookingFoodBeverage', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.BookingFoodBeverage (
        BookingFoodBeverageID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookingFoodBeverage PRIMARY KEY,
        BookingID INT NOT NULL,
        FoodBeverageItemID INT NOT NULL,
        Quantity INT NOT NULL,
        UnitPrice DECIMAL(18,2) NOT NULL,
        Subtotal AS (CONVERT(DECIMAL(18,2), Quantity * UnitPrice)) PERSISTED,
        CONSTRAINT UQ_BookingFoodBeverage UNIQUE (BookingID, FoodBeverageItemID),
        CONSTRAINT FK_BookingFoodBeverage_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID),
        CONSTRAINT FK_BookingFoodBeverage_Item FOREIGN KEY (FoodBeverageItemID) REFERENCES dbo.FoodBeverageItem(FoodBeverageItemID),
        CONSTRAINT CK_BookingFoodBeverage_Quantity CHECK (Quantity > 0),
        CONSTRAINT CK_BookingFoodBeverage_Price CHECK (UnitPrice >= 0)
    );
END;

IF OBJECT_ID(N'dbo.Payment', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Payment (
        PaymentID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Payment PRIMARY KEY,
        BookingID INT NOT NULL,
        Method VARCHAR(30) NOT NULL,
        Amount DECIMAL(18,2) NOT NULL,
        TransactionRef VARCHAR(100) NULL,
        ProviderTransactionNo VARCHAR(100) NULL,
        ResponseCode VARCHAR(20) NULL,
        TransactionStatus VARCHAR(20) NULL,
        BankCode VARCHAR(30) NULL,
        CardType VARCHAR(30) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_Payment_Status DEFAULT 'PENDING',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Payment_CreatedAt DEFAULT SYSDATETIME(),
        InitiatedAt DATETIME2 NULL,
        ExpiresAt DATETIME2 NULL,
        PaidAt DATETIME2 NULL,
        UpdatedAt DATETIME2 NULL,
        LastCallbackAt DATETIME2 NULL,
        CONSTRAINT FK_Payment_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID),
        CONSTRAINT CK_Payment_Status CHECK ([Status] IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED')),
        CONSTRAINT CK_Payment_Amount CHECK (Amount >= 0)
    );
END;

IF COL_LENGTH(N'dbo.Payment', N'ProviderTransactionNo') IS NULL ALTER TABLE dbo.Payment ADD ProviderTransactionNo VARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.Payment', N'ResponseCode') IS NULL ALTER TABLE dbo.Payment ADD ResponseCode VARCHAR(20) NULL;
IF COL_LENGTH(N'dbo.Payment', N'TransactionStatus') IS NULL ALTER TABLE dbo.Payment ADD TransactionStatus VARCHAR(20) NULL;
IF COL_LENGTH(N'dbo.Payment', N'BankCode') IS NULL ALTER TABLE dbo.Payment ADD BankCode VARCHAR(30) NULL;
IF COL_LENGTH(N'dbo.Payment', N'CardType') IS NULL ALTER TABLE dbo.Payment ADD CardType VARCHAR(30) NULL;
IF COL_LENGTH(N'dbo.Payment', N'InitiatedAt') IS NULL ALTER TABLE dbo.Payment ADD InitiatedAt DATETIME2 NULL;
IF COL_LENGTH(N'dbo.Payment', N'ExpiresAt') IS NULL ALTER TABLE dbo.Payment ADD ExpiresAt DATETIME2 NULL;
IF COL_LENGTH(N'dbo.Payment', N'UpdatedAt') IS NULL ALTER TABLE dbo.Payment ADD UpdatedAt DATETIME2 NULL;
IF COL_LENGTH(N'dbo.Payment', N'LastCallbackAt') IS NULL ALTER TABLE dbo.Payment ADD LastCallbackAt DATETIME2 NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Payment') AND [name] = N'UX_Payment_TransactionRef')
    CREATE UNIQUE INDEX UX_Payment_TransactionRef ON dbo.Payment(TransactionRef) WHERE TransactionRef IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Payment') AND [name] = N'UX_Payment_ProviderTransactionNo')
    CREATE UNIQUE INDEX UX_Payment_ProviderTransactionNo ON dbo.Payment(ProviderTransactionNo) WHERE ProviderTransactionNo IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Booking') AND [name] = N'IX_Booking_Status_Time')
    CREATE INDEX IX_Booking_Status_Time ON dbo.Booking([Status], BookingTime);

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
END;
GO

/* ================================================================
   6. REVIEWS, NEWS, SETTINGS, AND AUTH SUPPORT
   ================================================================ */

IF OBJECT_ID(N'dbo.MovieReview', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MovieReview (
        ReviewID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MovieReview PRIMARY KEY,
        CustomerID INT NOT NULL,
        MovieID INT NOT NULL,
        Rating INT NOT NULL,
        Content NVARCHAR(1000) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_MovieReview_Status DEFAULT 'ACTIVE',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_MovieReview_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT UQ_MovieReview_Customer_Movie UNIQUE (CustomerID, MovieID),
        CONSTRAINT FK_MovieReview_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customer(CustomerID),
        CONSTRAINT FK_MovieReview_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID),
        CONSTRAINT CK_MovieReview_Rating CHECK (Rating BETWEEN 1 AND 5)
    );
END;

IF OBJECT_ID(N'dbo.News', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.News (
        NewsID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_News PRIMARY KEY,
        CreatedByStaffID INT NOT NULL,
        ReviewedByAccountID INT NULL,
        Title NVARCHAR(255) NOT NULL,
        Content NVARCHAR(MAX) NOT NULL,
        ThumbnailURL NVARCHAR(500) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_News_Status DEFAULT 'DRAFT',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_News_CreatedAt DEFAULT SYSDATETIME(),
        UpdatedAt DATETIME2 NULL,
        SubmittedAt DATETIME2 NULL,
        ReviewedAt DATETIME2 NULL,
        PublishedAt DATETIME2 NULL,
        RejectionReason NVARCHAR(500) NULL,
        CONSTRAINT FK_News_Staff FOREIGN KEY (CreatedByStaffID) REFERENCES dbo.Staff(StaffID),
        CONSTRAINT FK_News_Reviewer FOREIGN KEY (ReviewedByAccountID) REFERENCES dbo.Account(AccountID)
    );
END;

/* Remove the non-SRS Summary field from databases created by an earlier script. */
IF COL_LENGTH(N'dbo.News', N'Summary') IS NOT NULL
BEGIN
    DECLARE @NewsSummaryDefault SYSNAME;
    SELECT @NewsSummaryDefault = dc.[name]
    FROM sys.default_constraints dc
    JOIN sys.columns c
      ON c.object_id = dc.parent_object_id
     AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID(N'dbo.News')
      AND c.[name] = N'Summary';

    IF @NewsSummaryDefault IS NOT NULL
    BEGIN
        DECLARE @DropNewsSummaryDefault NVARCHAR(1000) =
            N'ALTER TABLE dbo.News DROP CONSTRAINT ' + QUOTENAME(@NewsSummaryDefault);
        EXEC sys.sp_executesql @DropNewsSummaryDefault;
    END;

    ALTER TABLE dbo.News DROP COLUMN Summary;
END;

IF OBJECT_ID(N'dbo.SystemSetting', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.SystemSetting (
        SettingID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_SystemSetting PRIMARY KEY,
        SettingKey VARCHAR(100) NOT NULL CONSTRAINT UQ_SystemSetting_Key UNIQUE,
        Value NVARCHAR(MAX) NULL,
        [Description] NVARCHAR(500) NULL,
        UpdatedByAccountID INT NULL,
        UpdatedAt DATETIME2 NOT NULL CONSTRAINT DF_SystemSetting_UpdatedAt DEFAULT SYSDATETIME(),
        SettingName NVARCHAR(255) NULL,
        Category NVARCHAR(100) NULL,
        ValueType NVARCHAR(50) NULL,
        DefaultValue NVARCHAR(MAX) NULL,
        ValidationRules NVARCHAR(MAX) NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_SystemSetting_IsActive DEFAULT 1,
        CONSTRAINT FK_SystemSetting_Account FOREIGN KEY (UpdatedByAccountID) REFERENCES dbo.Account(AccountID)
    );
END;

/* Upgrade an existing database that still uses SettingValue. */
IF COL_LENGTH(N'dbo.SystemSetting', N'Value') IS NULL
    ALTER TABLE dbo.SystemSetting ADD Value NVARCHAR(MAX) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'SettingName') IS NULL
    ALTER TABLE dbo.SystemSetting ADD SettingName NVARCHAR(255) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'Category') IS NULL
    ALTER TABLE dbo.SystemSetting ADD Category NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'ValueType') IS NULL
    ALTER TABLE dbo.SystemSetting ADD ValueType NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'DefaultValue') IS NULL
    ALTER TABLE dbo.SystemSetting ADD DefaultValue NVARCHAR(MAX) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'ValidationRules') IS NULL
    ALTER TABLE dbo.SystemSetting ADD ValidationRules NVARCHAR(MAX) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'IsActive') IS NULL
    ALTER TABLE dbo.SystemSetting ADD IsActive BIT NULL;

IF COL_LENGTH(N'dbo.SystemSetting', N'SettingValue') IS NOT NULL
    EXEC(N'UPDATE dbo.SystemSetting SET Value = COALESCE(Value, SettingValue)');
EXEC(N'UPDATE dbo.SystemSetting
       SET SettingName = COALESCE(SettingName, SettingKey),
           Category = COALESCE(Category,
               CASE WHEN CHARINDEX(''_'', SettingKey) > 0 THEN LEFT(SettingKey, CHARINDEX(''_'', SettingKey) - 1) ELSE N''SYSTEM'' END),
           ValueType = COALESCE(ValueType, CASE WHEN TRY_CONVERT(DECIMAL(38, 10), Value) IS NOT NULL THEN N''NUMBER'' ELSE N''STRING'' END),
           DefaultValue = COALESCE(DefaultValue, Value),
           IsActive = COALESCE(IsActive, 1)');

IF OBJECT_ID(N'dbo.OtpToken', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OtpToken (
        OtpTokenID BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OtpToken PRIMARY KEY,
        AccountID INT NULL,
        Email NVARCHAR(255) NOT NULL,
        CodeHash NVARCHAR(255) NOT NULL,
        Purpose NVARCHAR(30) NOT NULL,
        ExpiresAt DATETIME2 NOT NULL,
        ConsumedAt DATETIME2 NULL,
        AttemptCount INT NOT NULL CONSTRAINT DF_OtpToken_AttemptCount DEFAULT 0,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_OtpToken_CreatedAt DEFAULT SYSDATETIME(),
        LastSentAt DATETIME2 NULL,
        LockedUntil DATETIME2 NULL,
        CONSTRAINT FK_OtpToken_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
    );
END;

IF COL_LENGTH(N'dbo.OtpToken', N'LockedUntil') IS NULL
    ALTER TABLE dbo.OtpToken ADD LockedUntil DATETIME2 NULL;

IF OBJECT_ID(N'dbo.LoginAttempt', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoginAttempt (
        AttemptID BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_LoginAttempt PRIMARY KEY,
        Identifier NVARCHAR(255) NOT NULL,
        IPAddress NVARCHAR(45) NOT NULL,
        UserAgent NVARCHAR(500) NULL,
        Success BIT NOT NULL,
        FailureReason NVARCHAR(50) NULL,
        AuthMethod VARCHAR(30) NULL,
        AttemptedAt DATETIME2 NOT NULL CONSTRAINT DF_LoginAttempt_At DEFAULT SYSDATETIME()
    );
END;

IF OBJECT_ID(N'dbo.OtpAttempt', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OtpAttempt (
        AttemptID BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OtpAttempt PRIMARY KEY,
        OtpTokenID BIGINT NULL,
        Email NVARCHAR(255) NOT NULL,
        Purpose NVARCHAR(50) NOT NULL,
        IpAddress NVARCHAR(45) NOT NULL,
        UserAgent NVARCHAR(500) NULL,
        AttemptResult VARCHAR(30) NOT NULL,
        AttemptedAt DATETIME2 NOT NULL CONSTRAINT DF_OtpAttempt_At DEFAULT SYSDATETIME()
    );
END;

IF OBJECT_ID(N'dbo.IpRateLimit', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.IpRateLimit (
        RateLimitID BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_IpRateLimit PRIMARY KEY,
        IpAddress NVARCHAR(45) NOT NULL,
        [Action] VARCHAR(50) NOT NULL,
        WindowStart DATETIME2 NOT NULL CONSTRAINT DF_IpRateLimit_Window DEFAULT SYSDATETIME(),
        AttemptCount INT NOT NULL CONSTRAINT DF_IpRateLimit_Count DEFAULT 0,
        LockedUntil DATETIME2 NULL,
        LastUpdatedAt DATETIME2 NOT NULL CONSTRAINT DF_IpRateLimit_Updated DEFAULT SYSDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.IpRateLimit') AND [name] = N'UX_IpRateLimit_Key')
    CREATE UNIQUE INDEX UX_IpRateLimit_Key ON dbo.IpRateLimit(IpAddress, [Action]);

IF OBJECT_ID(N'dbo.OAuthAccount', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.OAuthAccount (
        OAuthAccountID BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_OAuthAccount PRIMARY KEY,
        AccountID INT NOT NULL,
        Provider VARCHAR(20) NOT NULL,
        ProviderSubject VARCHAR(255) NOT NULL,
        ProviderEmail VARCHAR(255) NULL,
        EmailVerifiedByProvider BIT NOT NULL CONSTRAINT DF_OAuthAccount_Verified DEFAULT 0,
        LinkedAt DATETIME2 NOT NULL CONSTRAINT DF_OAuthAccount_LinkedAt DEFAULT SYSDATETIME(),
        LastLoginAt DATETIME2 NULL,
        CONSTRAINT UQ_OAuthAccount_Provider_Subject UNIQUE (Provider, ProviderSubject),
        CONSTRAINT FK_OAuthAccount_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
    );
END;
GO

/* ================================================================
   7. LOOKUP AND DEMO DATA
   Every insert is idempotent so the script can be rerun safely.
   ================================================================ */

IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'CUSTOMER')
    INSERT dbo.[Role] ([Name], [Description]) VALUES (N'CUSTOMER', N'Khách hàng');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'STAFF')
    INSERT dbo.[Role] ([Name], [Description]) VALUES (N'STAFF', N'Nhân viên');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'MANAGER')
    INSERT dbo.[Role] ([Name], [Description]) VALUES (N'MANAGER', N'Quản lý rạp');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'ADMIN')
    INSERT dbo.[Role] ([Name], [Description]) VALUES (N'ADMIN', N'Quản trị hệ thống');

IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE [Name] = N'movie:read')
    INSERT dbo.Permission ([Name], [Description]) VALUES (N'movie:read', N'Xem phim và suất chiếu');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE [Name] = N'booking:create')
    INSERT dbo.Permission ([Name], [Description]) VALUES (N'booking:create', N'Tạo đặt vé');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE [Name] = N'ticket:checkin')
    INSERT dbo.Permission ([Name], [Description]) VALUES (N'ticket:checkin', N'Kiểm tra vé');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE [Name] = N'showtime:manage')
    INSERT dbo.Permission ([Name], [Description]) VALUES (N'showtime:manage', N'Quản lý suất chiếu');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE [Name] = N'account:manage')
    INSERT dbo.Permission ([Name], [Description]) VALUES (N'account:manage', N'Quản lý tài khoản');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE [Name] = N'report:view')
    INSERT dbo.Permission ([Name], [Description]) VALUES (N'report:view', N'Xem báo cáo');

INSERT dbo.RolePermission (RoleID, PermissionID)
SELECT r.RoleID, p.PermissionID
FROM dbo.[Role] r
JOIN dbo.Permission p ON
    (r.[Name] = N'CUSTOMER' AND p.[Name] IN (N'movie:read', N'booking:create')) OR
    (r.[Name] = N'STAFF' AND p.[Name] IN (N'movie:read', N'booking:create', N'ticket:checkin')) OR
    (r.[Name] = N'MANAGER' AND p.[Name] IN (N'movie:read', N'showtime:manage', N'report:view')) OR
    (r.[Name] = N'ADMIN')
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.RolePermission rp
    WHERE rp.RoleID = r.RoleID AND rp.PermissionID = p.PermissionID
);

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

DECLARE @Genres TABLE ([Name] NVARCHAR(100), [Description] NVARCHAR(500));
INSERT @Genres VALUES
    (N'Action', N'Phim hành động'),
    (N'Adventure', N'Phim phiêu lưu'),
    (N'Animation', N'Phim hoạt hình'),
    (N'Comedy', N'Phim hài'),
    (N'Drama', N'Phim tâm lý'),
    (N'Horror', N'Phim kinh dị'),
    (N'Romance', N'Phim lãng mạn'),
    (N'Sci-Fi', N'Phim khoa học viễn tưởng');

INSERT dbo.Genre ([Name], [Description], [Status])
SELECT g.[Name], g.[Description], N'ACTIVE'
FROM @Genres g
WHERE NOT EXISTS (SELECT 1 FROM dbo.Genre x WHERE x.[Name] = g.[Name]);

IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeCode = N'STD')
    INSERT dbo.SeatType (TypeCode, TypeName, [Description], PriceMultiplier, ColorCode, [Status])
    VALUES (N'STD', N'Standard', N'Ghế tiêu chuẩn', 1.00, N'#3B82F6', N'Active');
IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeCode = N'VIP')
    INSERT dbo.SeatType (TypeCode, TypeName, [Description], PriceMultiplier, ColorCode, [Status])
    VALUES (N'VIP', N'VIP', N'Ghế VIP', 1.25, N'#F59E0B', N'Active');
IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeCode = N'COUPLE')
    INSERT dbo.SeatType (TypeCode, TypeName, [Description], PriceMultiplier, ColorCode, [Status])
    VALUES (N'COUPLE', N'Couple', N'Ghế đôi', 1.80, N'#EC4899', N'Active');

IF NOT EXISTS (SELECT 1 FROM dbo.TicketPricing WHERE PricingCode = N'PRICE-WD-MORN')
    INSERT dbo.TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, [Status], [Description])
    VALUES (N'PRICE-WD-MORN', N'Ngày thường - Buổi sáng', N'WEEKDAY', N'MORNING', 65000, 0, N'2D Digital', N'Active', N'Suất trước 12:00');
IF NOT EXISTS (SELECT 1 FROM dbo.TicketPricing WHERE PricingCode = N'PRICE-WD-STD')
    INSERT dbo.TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, [Status], [Description])
    VALUES (N'PRICE-WD-STD', N'Ngày thường - Tiêu chuẩn', N'WEEKDAY', N'STANDARD', 85000, 0, N'2D Digital', N'Active', N'Suất từ 12:00 đến 17:00');
IF NOT EXISTS (SELECT 1 FROM dbo.TicketPricing WHERE PricingCode = N'PRICE-WE-ALL')
    INSERT dbo.TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, [Status], [Description])
    VALUES (N'PRICE-WE-ALL', N'Cuối tuần', N'WEEKEND', N'ALL_DAY', 110000, 0, N'2D Digital', N'Active', N'Giá vé cuối tuần');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE RoomCode = N'ROOM-01')
    INSERT dbo.Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, Projector, SoundSystem, ScreenSize, [Status], [Description])
    VALUES (N'ROOM-01', N'Phòng Chiếu 01', N'2D Digital', 48, 6, 8, N'Laser 4K', N'Dolby 7.1', N'15m x 8m', N'Ready', N'Phòng chiếu tiêu chuẩn');
IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE RoomCode = N'ROOM-02')
    INSERT dbo.Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, Projector, SoundSystem, ScreenSize, [Status], [Description])
    VALUES (N'ROOM-02', N'Phòng Chiếu 02', N'Dolby Atmos', 48, 6, 8, N'Laser 4K', N'Dolby Atmos', N'18m x 10m', N'Ready', N'Phòng chiếu âm thanh Dolby Atmos');
IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE RoomCode = N'ROOM-03')
    INSERT dbo.Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, Projector, SoundSystem, ScreenSize, [Status], [Description])
    VALUES (N'ROOM-03', N'Cinema 03', N'VIP Recliner', 48, 6, 8, N'Laser 4K', N'Dolby Atmos', N'16m x 9m', N'Ready', N'Premium VIP recliner auditorium');

UPDATE dbo.Room SET RoomName=N'Cinema 01', [Description]=N'Standard digital auditorium' WHERE RoomCode=N'ROOM-01';
UPDATE dbo.Room SET RoomName=N'Cinema 02', [Description]=N'Premium Dolby Atmos auditorium' WHERE RoomCode=N'ROOM-02';
UPDATE dbo.Room SET RoomName=N'Cinema 03', [Description]=N'Premium VIP recliner auditorium' WHERE RoomCode=N'ROOM-03';

DECLARE @RoomID INT;
DECLARE @RowNumber INT;
DECLARE @SeatNumber INT;
DECLARE @SeatTypeID INT;
DECLARE @SeatTypeName NVARCHAR(100);

DECLARE room_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT RoomID FROM dbo.Room WHERE RoomCode IN (N'ROOM-01', N'ROOM-02', N'ROOM-03');
OPEN room_cursor;
FETCH NEXT FROM room_cursor INTO @RoomID;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.Seat WHERE RoomID = @RoomID)
    BEGIN
        SET @RowNumber = 1;
        WHILE @RowNumber <= 6
        BEGIN
            SET @SeatNumber = 1;
            WHILE @SeatNumber <= 8
            BEGIN
                IF @RowNumber = 6
                    SELECT @SeatTypeID = SeatTypeID, @SeatTypeName = TypeName FROM dbo.SeatType WHERE TypeCode = N'COUPLE';
                ELSE IF @RowNumber IN (4, 5)
                    SELECT @SeatTypeID = SeatTypeID, @SeatTypeName = TypeName FROM dbo.SeatType WHERE TypeCode = N'VIP';
                ELSE
                    SELECT @SeatTypeID = SeatTypeID, @SeatTypeName = TypeName FROM dbo.SeatType WHERE TypeCode = N'STD';

                INSERT dbo.Seat (RoomID, SeatTypeID, SeatTypeName, RowLabel, SeatNumber, GridRow, GridColumn, [Status])
                VALUES (@RoomID, @SeatTypeID, @SeatTypeName, NCHAR(64 + @RowNumber), @SeatNumber, @RowNumber, @SeatNumber, N'Active');
                SET @SeatNumber += 1;
            END;
            SET @RowNumber += 1;
        END;
    END;
    FETCH NEXT FROM room_cursor INTO @RoomID;
END;
CLOSE room_cursor;
DEALLOCATE room_cursor;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Light Speedway')
    INSERT dbo.Movie (Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], PosterURL, TrailerURL, [Status])
    VALUES (N'Light Speedway', N'Light Speedway', 125, DATEADD(DAY, -10, CAST(GETDATE() AS DATE)), DATEADD(DAY, 40, CAST(GETDATE() AS DATE)), N'Vietnamese', N'Nguyen Minh', N'An Nguyen, Binh Tran', N'T13', N'A young racer chases the championship in the deciding season of his career.', N'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600', N'https://www.youtube.com/', N'Now Showing');

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Blue Planet')
    INSERT dbo.Movie (Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], PosterURL, TrailerURL, [Status])
    VALUES (N'Blue Planet', N'Blue Planet', 110, DATEADD(DAY, -5, CAST(GETDATE() AS DATE)), DATEADD(DAY, 45, CAST(GETDATE() AS DATE)), N'English', N'Alex Morgan', N'Emma Lee, David Chen', N'P', N'An expedition explores a mysterious ocean planet.', N'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600', N'https://www.youtube.com/', N'Now Showing');

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Cyber Horizon 2099')
    INSERT dbo.Movie (Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], PosterURL, TrailerURL, [Status])
    VALUES (N'Cyber Horizon 2099', N'Cyber Horizon 2099', 138, DATEADD(DAY, 20, CAST(GETDATE() AS DATE)), DATEADD(DAY, 70, CAST(GETDATE() AS DATE)), N'English', N'Sophia Park', N'Mia Wilson, Noah Kim', N'T16', N'A team of engineers must protect a futuristic city from an AI system that has escaped control.', N'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600', N'https://www.youtube.com/', N'Upcoming');

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'The Secret House')
    INSERT dbo.Movie (Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], PosterURL, TrailerURL, [Status])
    VALUES (N'The Secret House', N'The Secret House', 102, DATEADD(DAY, 30, CAST(GETDATE() AS DATE)), DATEADD(DAY, 75, CAST(GETDATE() AS DATE)), N'Vietnamese', N'Le Hoang', N'Minh Anh, Quoc Huy', N'T18', N'A family discovers a buried secret inside their new home.', N'https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=600', N'https://www.youtube.com/', N'Upcoming');

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'Clayface')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'Clayface',N'Clayface',108,'2026-10-30','2026-12-20',N'English',N'James Watkins',N'Tom Rhys Harries, Naomi Ackie, Eddie Marsan',N'T18',N'A rising Hollywood actor undergoes an experimental treatment that transforms his identity and humanity.',N'https://www.impawards.com/2026/posters/clayface_xlg.jpg',NULL,N'Upcoming');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'Godzilla Minus Zero')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'Godzilla Minus Zero',N'Godzilla Minus Zero',135,'2026-11-06','2027-01-10',N'Japanese',N'Takashi Yamazaki',N'Ryunosuke Kamiki, Minami Hamabe',N'T13',N'Two years after the war, Japan faces another devastating threat from Godzilla.',N'https://www.thepopverse.com/_next/image?q=75&url=https%3A%2F%2Fmedia.thepopverse.com%2Fmedia%2Fgodzilla-minus-zero-teaser-poster-b1eimqcqlboe3ox4e40nakhotm.png&w=1280',NULL,N'Upcoming');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'The Cat in the Hat')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'The Cat in the Hat',N'The Cat in the Hat',98,'2026-11-06','2026-12-31',N'English',N'Alessandro Carloni, Erica Rivinoja',N'Bill Hader',N'P',N'The Cat brings imagination, chaos, and a new adventure to two children adjusting to a new home.',N'https://image.tmdb.org/t/p/original/hXrxHQhUkffGUmrM7vs2ThpcVQL.jpg',NULL,N'Upcoming');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'Ebenezer: A Christmas Carol')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'Ebenezer: A Christmas Carol',N'Ebenezer: A Christmas Carol',120,'2026-11-13','2027-01-15',N'English',N'Ti West',N'Johnny Depp, Andrea Riseborough',N'T13',N'A dark cinematic retelling of the timeless Christmas story of regret and redemption.',N'https://poster.gsc.com.my/2026/260727_Ebenezer_big.jpg',NULL,N'Upcoming');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'The Hunger Games: Sunrise on the Reaping')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'The Hunger Games: Sunrise on the Reaping',N'The Hunger Games: Sunrise on the Reaping',145,'2026-11-20','2027-01-31',N'English',N'Francis Lawrence',N'Joseph Zada, Mckenna Grace',N'T16',N'The Second Quarter Quell begins with young Haymitch Abernathy selected to fight for District 12.',N'https://assets.gadgets360cdn.com/pricee/assets/product/202511/The_Hunger_Games_Sunrise_on_the__1764145414.jpg',NULL,N'Upcoming');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'The Angry Birds Movie 3')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'The Angry Birds Movie 3',N'The Angry Birds Movie 3',100,'2027-01-01','2027-02-28',N'English',N'John Rice',N'Jason Sudeikis, Josh Gad',N'P',N'Red and the flock return for a colorful new big-screen adventure.',N'https://image.tmdb.org/t/p/original/9Fd7M2LwtKXBzjMquZM64z22odQ.jpg',NULL,N'Upcoming');

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'Heart of the Beast')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'Heart of the Beast',N'Heart of the Beast',101,'2026-10-02','2026-11-20',N'English',N'David Ayer',N'Brad Pitt, J.K. Simmons, Anna Lambe',N'T13',N'After a plane crash, a special forces officer and his combat dog fight to survive in the Alaskan wilderness.',N'https://cdn.moveek.com/storage/media/cache/tall/6a545e387a5bb803695755.webp',NULL,N'Now Showing');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'The Almost Perfect Murder')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'The Almost Perfect Murder',N'The Almost Perfect Murder',124,'2026-10-09','2026-11-25',N'Vietnamese',N'Đức Nguyễn',N'Anh Tú Atus, DJ Mie, Vũ Ngọc Anh',N'T18',N'A romantic-content creator and a detective become unlikely partners while fleeing the police and uncovering a show-business mystery.',N'https://cdn.moveek.com/storage/media/cache/tall/6a8d5b41107e9384094943.webp',NULL,N'Now Showing');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'Street Fighter')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'Street Fighter',N'Street Fighter',119,'2026-10-16','2026-12-05',N'English',N'Kitao Sakurai',N'Noah Centineo, Andrew Koji, Callina Liang',N'T16',N'Ryu and Ken reunite for the World Warrior tournament and discover a deadly conspiracy behind the competition.',N'https://cdn.moveek.com/storage/media/cache/tall/6a538ba804bfe303915257.webp',NULL,N'Now Showing');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'Whale Fall')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'Whale Fall',N'Whale Fall',106,'2026-10-16','2026-11-30',N'English',N'Bronte Higgs',NULL,N'T13',N'A diver searching for his father is swallowed by a giant sperm whale and has one hour to escape before his oxygen runs out.',N'https://cdn.moveek.com/storage/media/cache/tall/6a805db1ea9dd446109915.webp',NULL,N'Now Showing');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'Death Whisperer: Saming the Werebeast')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'Death Whisperer: Saming the Werebeast',N'Death Whisperer: Saming the Werebeast',111,'2026-10-16','2026-12-05',N'Thai',N'Narit Yuvaboon',N'Nadech Kugimiya, Maylada Susri',N'T18',N'Yak and Sergeant Paphan enter a mysterious forest to find a missing unit and confront a shape-shifting tiger spirit.',N'https://cdn.moveek.com/storage/media/cache/tall/6ab49c6a59ce0852256648.webp',NULL,N'Now Showing');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'Bambi: The Reckoning')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'Bambi: The Reckoning',N'Bambi: The Reckoning',80,'2026-10-23','2026-12-05',N'English',N'Dan Allen',N'Roxanne McKee, Tom Mulheron',N'T16',N'A mutated deer hunts the humans who enter its poisoned woodland territory.',N'https://cdn.moveek.com/storage/media/cache/tall/6accc290ae838036630540.webp',NULL,N'Now Showing');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title=N'The Curse 2026')
    INSERT dbo.Movie (Title,OriginalTitle,Duration,ReleaseDate,EndDate,[Language],Director,Actors,AgeRating,[Description],PosterURL,TrailerURL,[Status]) VALUES
    (N'The Curse 2026',N'The Curse 2026',89,'2026-10-23','2026-12-05',N'Japanese',N'Ken''ichi Ugana',N'Yukino Kaizu, YU, Yutaka Kyan',N'T18',N'A young woman races to uncover the origin of a deadly urban-legend curse before she becomes its next victim.',N'https://cdn.moveek.com/storage/media/cache/tall/6ad57dbec3a89041565644.webp',NULL,N'Now Showing');

DECLARE @MovieGenreSeed TABLE (MovieTitle NVARCHAR(250), GenreName NVARCHAR(100));
INSERT @MovieGenreSeed VALUES
    (N'Light Speedway', N'Action'),
    (N'Light Speedway', N'Drama'),
    (N'Blue Planet', N'Adventure'),
    (N'Blue Planet', N'Animation'),
    (N'Cyber Horizon 2099', N'Sci-Fi'),
    (N'Cyber Horizon 2099', N'Action'),
    (N'The Secret House', N'Horror');

INSERT @MovieGenreSeed VALUES
    (N'Clayface',N'Horror'),
    (N'Godzilla Minus Zero',N'Action'),
    (N'Godzilla Minus Zero',N'Sci-Fi'),
    (N'The Cat in the Hat',N'Animation'),
    (N'The Cat in the Hat',N'Comedy'),
    (N'Ebenezer: A Christmas Carol',N'Drama'),
    (N'The Hunger Games: Sunrise on the Reaping',N'Action'),
    (N'The Hunger Games: Sunrise on the Reaping',N'Adventure'),
    (N'The Angry Birds Movie 3',N'Animation'),
    (N'The Angry Birds Movie 3',N'Comedy');

INSERT dbo.MovieGenre (MovieID, GenreID)
SELECT m.MovieID, g.GenreID
FROM @MovieGenreSeed s
JOIN dbo.Movie m ON m.Title = s.MovieTitle
JOIN dbo.Genre g ON g.[Name] = s.GenreName
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.MovieGenre mg WHERE mg.MovieID = m.MovieID AND mg.GenreID = g.GenreID
);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE ItemCode = N'PC-CARAMEL')
    INSERT dbo.FoodBeverageItem (ItemCode, [Name], [Description], ItemType, Price, CostPrice, ImageURL, [Status])
    VALUES (N'PC-CARAMEL', N'Bắp rang caramel', N'Bắp rang vị caramel cỡ lớn', N'Food', 55000, 15000, N'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=400', N'Active');
IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE ItemCode = N'DR-COKE')
    INSERT dbo.FoodBeverageItem (ItemCode, [Name], [Description], ItemType, Price, CostPrice, ImageURL, [Status])
    VALUES (N'DR-COKE', N'Coca-Cola cỡ lớn', N'Nước ngọt có ga 32oz', N'Drink', 32000, 8000, N'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400', N'Active');
IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE ItemCode = N'CB-SOLO')
    INSERT dbo.FoodBeverageItem (ItemCode, [Name], [Description], ItemType, Price, CostPrice, ImageURL, [Status])
    VALUES (N'CB-SOLO', N'Combo Solo', N'1 bắp caramel và 1 Coca-Cola', N'Combo', 79000, 22000, N'https://images.unsplash.com/photo-1572177191856-3cde618dee1f?w=400', N'Active');

UPDATE dbo.FoodBeverageItem SET [Name]=N'Large Caramel Popcorn', [Description]=N'Freshly popped corn coated with caramel', ImageURL=N'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=600' WHERE ItemCode=N'PC-CARAMEL';
UPDATE dbo.FoodBeverageItem SET [Name]=N'Large Coca-Cola', [Description]=N'Chilled 32 oz Coca-Cola', ImageURL=N'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=600' WHERE ItemCode=N'DR-COKE';
UPDATE dbo.FoodBeverageItem SET [Name]=N'Solo Movie Combo', [Description]=N'One large caramel popcorn and one Coca-Cola', ImageURL=N'https://images.unsplash.com/photo-1572177191856-3cde618dee1f?w=600' WHERE ItemCode=N'CB-SOLO';

IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE ItemCode=N'PC-CHEESE')
    INSERT dbo.FoodBeverageItem (ItemCode,[Name],[Description],ItemType,Price,CostPrice,ImageURL,[Status]) VALUES
    (N'PC-CHEESE',N'Cheese Popcorn',N'Large popcorn with rich cheese seasoning',N'Food',59000,17000,N'https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=600',N'Active');
IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE ItemCode=N'CB-COUPLE')
    INSERT dbo.FoodBeverageItem (ItemCode,[Name],[Description],ItemType,Price,CostPrice,ImageURL,[Status]) VALUES
    (N'CB-COUPLE',N'Couple Movie Combo',N'One large popcorn and two soft drinks',N'Combo',129000,39000,N'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600',N'Active');

DECLARE @ComboID INT = (SELECT FoodBeverageItemID FROM dbo.FoodBeverageItem WHERE ItemCode = N'CB-SOLO');
DECLARE @PopcornID INT = (SELECT FoodBeverageItemID FROM dbo.FoodBeverageItem WHERE ItemCode = N'PC-CARAMEL');
DECLARE @DrinkID INT = (SELECT FoodBeverageItemID FROM dbo.FoodBeverageItem WHERE ItemCode = N'DR-COKE');
IF @ComboID IS NOT NULL AND @PopcornID IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ComboItem WHERE ComboID = @ComboID AND ComponentItemID = @PopcornID)
    INSERT dbo.ComboItem (ComboID, ComponentItemID, Quantity) VALUES (@ComboID, @PopcornID, 1);
IF @ComboID IS NOT NULL AND @DrinkID IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.ComboItem WHERE ComboID = @ComboID AND ComponentItemID = @DrinkID)
    INSERT dbo.ComboItem (ComboID, ComponentItemID, Quantity) VALUES (@ComboID, @DrinkID, 1);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Promotion WHERE [Name] = N'Ưu đãi thành viên mới')
    INSERT dbo.Promotion ([Name], [Description], StartDate, EndDate, [Status])
    VALUES (N'Ưu đãi thành viên mới', N'Giảm giá cho lần đặt vé đầu tiên', DATEADD(DAY, -30, SYSDATETIME()), DATEADD(DAY, 180, SYSDATETIME()), 'ACTIVE');

DECLARE @PromotionID INT = (SELECT PromotionID FROM dbo.Promotion WHERE [Name] = N'Ưu đãi thành viên mới');
IF @PromotionID IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Voucher WHERE Code = 'WELCOME10')
    INSERT dbo.Voucher (PromotionID, RequiredRankID, Code, DiscountType, DiscountValue, MaxDiscountAmount, MinBookingAmount, ValidFrom, ValidTo, TotalUsageLimit, PerAccountLimit, [Status])
    VALUES (@PromotionID, NULL, 'WELCOME10', 'PERCENT', 10, 50000, 100000, DATEADD(DAY, -30, SYSDATETIME()), DATEADD(DAY, 180, SYSDATETIME()), 1000, 1, 'ACTIVE');
GO

IF NOT EXISTS (SELECT 1 FROM dbo.Showtime)
BEGIN
    DECLARE @Tomorrow DATE = DATEADD(DAY, 1, CAST(GETDATE() AS DATE));
    DECLARE @DayAfter DATE = DATEADD(DAY, 2, CAST(GETDATE() AS DATE));
    DECLARE @Movie1 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Light Speedway');
    DECLARE @Movie2 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Blue Planet');
    DECLARE @Room1 INT = (SELECT RoomID FROM dbo.Room WHERE RoomCode = N'ROOM-01');
    DECLARE @Room2 INT = (SELECT RoomID FROM dbo.Room WHERE RoomCode = N'ROOM-02');

    INSERT dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status]) VALUES
        (@Movie1, @Room1, DATEADD(HOUR, 10, CAST(@Tomorrow AS DATETIME2)), DATEADD(MINUTE, 125, DATEADD(HOUR, 10, CAST(@Tomorrow AS DATETIME2))), 85000, N'Scheduled'),
        (@Movie2, @Room2, DATEADD(HOUR, 14, CAST(@Tomorrow AS DATETIME2)), DATEADD(MINUTE, 110, DATEADD(HOUR, 14, CAST(@Tomorrow AS DATETIME2))), 110000, N'Scheduled'),
        (@Movie1, @Room2, DATEADD(HOUR, 19, CAST(@DayAfter AS DATETIME2)), DATEADD(MINUTE, 125, DATEADD(HOUR, 19, CAST(@DayAfter AS DATETIME2))), 110000, N'Scheduled'),
        (@Movie2, @Room1, DATEADD(HOUR, 20, CAST(@DayAfter AS DATETIME2)), DATEADD(MINUTE, 110, DATEADD(HOUR, 20, CAST(@DayAfter AS DATETIME2))), 95000, N'Scheduled');
END;
GO

DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @TodaySchedule TABLE (
    MovieTitle NVARCHAR(250), RoomCode NVARCHAR(50), StartHour INT,
    StartMinute INT, BasePrice DECIMAL(18,2)
);
INSERT @TodaySchedule VALUES
    (N'Bambi: The Reckoning', N'ROOM-01', 9, 0, 85000),
    (N'Bambi: The Reckoning', N'ROOM-02', 11, 0, 105000),
    (N'Bambi: The Reckoning', N'ROOM-03', 13, 0, 125000),
    (N'Street Fighter', N'ROOM-01', 12, 0, 95000),
    (N'Street Fighter', N'ROOM-02', 14, 30, 115000),
    (N'Street Fighter', N'ROOM-03', 17, 0, 135000),
    (N'Death Whisperer: Saming the Werebeast', N'ROOM-01', 16, 0, 95000),
    (N'Death Whisperer: Saming the Werebeast', N'ROOM-02', 19, 0, 115000),
    (N'Death Whisperer: Saming the Werebeast', N'ROOM-03', 21, 0, 135000);

INSERT dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
SELECT m.MovieID, r.RoomID, times.StartTime,
       DATEADD(MINUTE, m.Duration, times.StartTime), s.BasePrice, N'Scheduled'
FROM @TodaySchedule s
JOIN dbo.Movie m ON m.Title = s.MovieTitle
JOIN dbo.Room r ON r.RoomCode = s.RoomCode
CROSS APPLY (SELECT DATEADD(MINUTE, s.StartMinute,
                    DATEADD(HOUR, s.StartHour, CAST(@Today AS DATETIME2))) AS StartTime) times
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.Showtime existing
    WHERE existing.MovieID = m.MovieID
      AND existing.RoomID = r.RoomID
      AND existing.StartTime = times.StartTime
);
GO

/* Demo accounts with BCrypt strength-10 password hashes. */
DECLARE @DemoAccounts TABLE (
    RoleName NVARCHAR(50), Username VARCHAR(50), Email VARCHAR(255),
    PasswordHash VARCHAR(255), FullName NVARCHAR(100), Phone VARCHAR(15)
);
INSERT @DemoAccounts VALUES
    (N'ADMIN', 'admin', 'admin@cinema.com', '$2a$10$bNY00N78MMHSkzCtHRAbXO8ZJyvkvKHE5fk/qkg5ugT71qHFqIlh2', N'Quản trị viên', '0900000001'),
    (N'MANAGER', 'manager', 'manager@cinema.com', '$2a$10$QmlR80DeTgUduxI8NUUZGO4Ir5C7er7aq.2.RvAi2txns3mVlL9yq', N'Quản lý rạp', '0900000002'),
    (N'STAFF', 'staff', 'staff@cinema.com', '$2a$10$bBKI/YJyY065Ra.SQoaaxOE2MqGL7pvAvkrVHjjIwaiUOj2HNgte6', N'Nhân viên bán vé', '0900000003'),
    (N'CUSTOMER', 'customer', 'customer@cinema.com', '$2a$10$nya.sfK50SAuM6gNW27SCemCJsk/4tRDbKsp/8sV29Cg8NJnq/7Z2', N'Khách hàng mẫu', '0900000004');

INSERT dbo.Account (RoleID, Username, Email, PasswordHash, FullName, Phone, [Status], EmailVerifiedAt)
SELECT r.RoleID, d.Username, d.Email, d.PasswordHash, d.FullName, d.Phone, 'ACTIVE', SYSDATETIME()
FROM @DemoAccounts d
JOIN dbo.[Role] r ON r.[Name] = d.RoleName
WHERE NOT EXISTS (SELECT 1 FROM dbo.Account a WHERE a.Email = d.Email);

DECLARE @CustomerAccountID INT = (SELECT AccountID FROM dbo.Account WHERE Email = 'customer@cinema.com');
IF @CustomerAccountID IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Customer WHERE AccountID = @CustomerAccountID)
    INSERT dbo.Customer (AccountID, RankID, DateOfBirth, [Address], AvailablePoints, TierPoints, ReceivePromotionEmail)
    VALUES (@CustomerAccountID, (SELECT RankID FROM dbo.MembershipRank WHERE [Name] = N'STANDARD'), '2000-01-01', N'Thành phố Hồ Chí Minh', 100, 100, 1);

DECLARE @StaffAccountID INT = (SELECT AccountID FROM dbo.Account WHERE Email = 'staff@cinema.com');
IF @StaffAccountID IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Staff WHERE AccountID = @StaffAccountID)
    INSERT dbo.Staff (AccountID, Position, [Status]) VALUES (@StaffAccountID, N'Nhân viên bán vé', 'ACTIVE');
GO

DECLARE @DemoNewsStaffID INT = (
    SELECT s.StaffID
    FROM dbo.Staff s
    JOIN dbo.Account a ON a.AccountID = s.AccountID
    WHERE a.Email = 'staff@cinema.com'
);

IF @DemoNewsStaffID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'Lịch chiếu và ưu đãi tuần này')
BEGIN
    INSERT dbo.News (
        CreatedByStaffID, ReviewedByAccountID, Title, Content,
        ThumbnailURL, [Status], SubmittedAt, ReviewedAt, PublishedAt
    )
    VALUES (
        @DemoNewsStaffID,
        (SELECT AccountID FROM dbo.Account WHERE Email = 'manager@cinema.com'),
        N'Lịch chiếu và ưu đãi tuần này',
        N'CineVerse cập nhật các suất chiếu mới trong tuần. Thành viên có thể đăng nhập, chọn phim, suất chiếu, ghế ngồi và sử dụng voucher hợp lệ khi đặt vé.',
        N'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=900',
        'PUBLISHED', SYSDATETIME(), SYSDATETIME(), SYSDATETIME()
    );
END;

UPDATE dbo.News
SET Title=N'Weekly Showtimes and Member Offers',
    Content=N'Explore this week''s newest showtimes and enjoy exclusive benefits when booking with your CINEORA member account.'
WHERE Title=N'Lịch chiếu và ưu đãi tuần này';

IF @DemoNewsStaffID IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title=N'Experience Movies with Dolby Atmos')
    INSERT dbo.News (CreatedByStaffID,ReviewedByAccountID,Title,Content,ThumbnailURL,[Status],SubmittedAt,ReviewedAt,PublishedAt)
    VALUES (@DemoNewsStaffID,(SELECT AccountID FROM dbo.Account WHERE Email='manager@cinema.com'),N'Experience Movies with Dolby Atmos',N'Discover immersive, multidimensional sound in our premium Dolby Atmos auditorium.',N'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1000','PUBLISHED',SYSDATETIME(),SYSDATETIME(),SYSDATETIME());
IF @DemoNewsStaffID IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title=N'Five Tips for the Perfect Cinema Night')
    INSERT dbo.News (CreatedByStaffID,ReviewedByAccountID,Title,Content,ThumbnailURL,[Status],SubmittedAt,ReviewedAt,PublishedAt)
    VALUES (@DemoNewsStaffID,(SELECT AccountID FROM dbo.Account WHERE Email='manager@cinema.com'),N'Five Tips for the Perfect Cinema Night',N'Book early, choose your preferred seats, arrive on time, and complete the experience with your favorite cinema snacks.',N'https://images.unsplash.com/photo-1586899028174-e7098604235b?w=1000','PUBLISHED',SYSDATETIME(),SYSDATETIME(),SYSDATETIME());
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SystemSetting WHERE SettingKey = 'BOOKING_HOLD_MINUTES')
    INSERT dbo.SystemSetting
        (SettingKey, Value, [Description], SettingName, Category, ValueType, DefaultValue, ValidationRules, IsActive)
    VALUES ('BOOKING_HOLD_MINUTES', N'10', N'Thời gian giữ ghế chưa thanh toán',
            N'Seat Hold Time', N'BOOKING', N'NUMBER', N'10', N'min:1,max:60', 1);

/* ================================================================
   7. AUTH CONSTRAINTS, INDEXES AND SETTINGS
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
END;

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE [name] = N'CK_OAuthAccount_Provider' AND parent_object_id = OBJECT_ID(N'dbo.OAuthAccount'))
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OAuthAccount WHERE Provider <> 'GOOGLE')
        ALTER TABLE dbo.OAuthAccount WITH NOCHECK
            ADD CONSTRAINT CK_OAuthAccount_Provider CHECK (Provider = 'GOOGLE');
    ELSE
        ALTER TABLE dbo.OAuthAccount WITH CHECK
            ADD CONSTRAINT CK_OAuthAccount_Provider CHECK (Provider = 'GOOGLE');
END;

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
END;

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
    ('SYSTEM_LOGIN_ATTEMPTS',          N'5',  N'Maximum failed login attempts before lockout',         N'Max Login Attempts',      N'5',  N'min:1,max:10');

INSERT dbo.SystemSetting
    (SettingKey, Value, [Description], SettingName, Category, ValueType, DefaultValue, ValidationRules, IsActive)
SELECT s.SettingKey, s.Value, s.[Description], s.SettingName, N'SYSTEM', N'NUMBER', s.DefaultValue, s.ValidationRules, 1
FROM @AuthSettings s
WHERE NOT EXISTS (SELECT 1 FROM dbo.SystemSetting x WHERE x.SettingKey = s.SettingKey);

INSERT dbo.Customer (AccountID, RankID)
SELECT a.AccountID, (SELECT TOP (1) RankID FROM dbo.MembershipRank WHERE [Name] = N'STANDARD')
FROM dbo.Account a
JOIN dbo.[Role] r ON r.RoleID = a.RoleID
WHERE r.[Name] = N'CUSTOMER'
  AND NOT EXISTS (SELECT 1 FROM dbo.Customer c WHERE c.AccountID = a.AccountID);

INSERT dbo.Staff (AccountID, Position, [Status])
SELECT a.AccountID, N'Nhân viên', 'ACTIVE'
FROM dbo.Account a
JOIN dbo.[Role] r ON r.RoleID = a.RoleID
WHERE r.[Name] = N'STAFF'
  AND NOT EXISTS (SELECT 1 FROM dbo.Staff s WHERE s.AccountID = a.AccountID);

UPDATE dbo.Customer
SET RankID = (SELECT TOP (1) RankID FROM dbo.MembershipRank WHERE [Name] = N'STANDARD')
WHERE RankID IS NULL
  AND EXISTS (SELECT 1 FROM dbo.MembershipRank WHERE [Name] = N'STANDARD');

UPDATE dbo.Account
SET EmailVerifiedAt = SYSDATETIME()
WHERE EmailVerifiedAt IS NULL
  AND [Status] = 'ACTIVE'
  AND Email IN ('admin@cinema.com', 'manager@cinema.com', 'staff@cinema.com', 'customer@cinema.com');

PRINT N'CinemaManagementDB schema and demo data are ready.';
GO
