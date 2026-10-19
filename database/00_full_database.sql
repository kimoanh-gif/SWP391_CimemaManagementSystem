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
        MinTierPoints INT NOT NULL,
        BenefitDescription NVARCHAR(500) NULL,
        [Status] VARCHAR(30) NOT NULL CONSTRAINT DF_MembershipRank_Status DEFAULT 'ACTIVE',
        CONSTRAINT CK_MembershipRank_MinTierPoints CHECK (MinTierPoints >= 0)
    );
END;

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
        CONSTRAINT FK_Account_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID)
    );
END;

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
        SettingValue NVARCHAR(MAX) NULL,
        [Description] NVARCHAR(500) NULL,
        UpdatedByAccountID INT NULL,
        UpdatedAt DATETIME2 NOT NULL CONSTRAINT DF_SystemSetting_UpdatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_SystemSetting_Account FOREIGN KEY (UpdatedByAccountID) REFERENCES dbo.Account(AccountID)
    );
END;

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
        CONSTRAINT FK_OtpToken_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
    );
END;

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
    INSERT dbo.MembershipRank ([Name], MinTierPoints, BenefitDescription, [Status])
    VALUES (N'STANDARD', 0, N'Hạng thành viên mặc định', 'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM dbo.MembershipRank WHERE [Name] = N'SILVER')
    INSERT dbo.MembershipRank ([Name], MinTierPoints, BenefitDescription, [Status])
    VALUES (N'SILVER', 1000, N'Ưu đãi dành cho thành viên Silver', 'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM dbo.MembershipRank WHERE [Name] = N'GOLD')
    INSERT dbo.MembershipRank ([Name], MinTierPoints, BenefitDescription, [Status])
    VALUES (N'GOLD', 3000, N'Ưu đãi dành cho thành viên Gold', 'ACTIVE');
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

DECLARE @RoomID INT;
DECLARE @RowNumber INT;
DECLARE @SeatNumber INT;
DECLARE @SeatTypeID INT;
DECLARE @SeatTypeName NVARCHAR(100);

DECLARE room_cursor CURSOR LOCAL FAST_FORWARD FOR
    SELECT RoomID FROM dbo.Room WHERE RoomCode IN (N'ROOM-01', N'ROOM-02');
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

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Đường Đua Ánh Sáng')
    INSERT dbo.Movie (Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], PosterURL, TrailerURL, [Status])
    VALUES (N'Đường Đua Ánh Sáng', N'Light Speedway', 125, DATEADD(DAY, -10, CAST(GETDATE() AS DATE)), DATEADD(DAY, 40, CAST(GETDATE() AS DATE)), N'Tiếng Việt', N'Nguyễn Minh', N'An Nguyễn, Bình Trần', N'T13', N'Một tay đua trẻ theo đuổi chức vô địch trong mùa giải quyết định.', N'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600', N'https://www.youtube.com/', N'Now Showing');

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Hành Tinh Xanh')
    INSERT dbo.Movie (Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], PosterURL, TrailerURL, [Status])
    VALUES (N'Hành Tinh Xanh', N'Blue Planet', 110, DATEADD(DAY, -5, CAST(GETDATE() AS DATE)), DATEADD(DAY, 45, CAST(GETDATE() AS DATE)), N'Tiếng Anh', N'Alex Morgan', N'Emma Lee, David Chen', N'P', N'Chuyến phiêu lưu khám phá một hành tinh đại dương bí ẩn.', N'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600', N'https://www.youtube.com/', N'Now Showing');

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Chân Trời 2099')
    INSERT dbo.Movie (Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], PosterURL, TrailerURL, [Status])
    VALUES (N'Chân Trời 2099', N'Cyber Horizon 2099', 138, DATEADD(DAY, 20, CAST(GETDATE() AS DATE)), DATEADD(DAY, 70, CAST(GETDATE() AS DATE)), N'Tiếng Anh', N'Sophia Park', N'Mia Wilson, Noah Kim', N'T16', N'Một nhóm kỹ sư phải bảo vệ thành phố tương lai khỏi hệ thống AI mất kiểm soát.', N'https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600', N'https://www.youtube.com/', N'Upcoming');

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Ngôi Nhà Bí Ẩn')
    INSERT dbo.Movie (Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], PosterURL, TrailerURL, [Status])
    VALUES (N'Ngôi Nhà Bí Ẩn', N'The Secret House', 102, DATEADD(DAY, 30, CAST(GETDATE() AS DATE)), DATEADD(DAY, 75, CAST(GETDATE() AS DATE)), N'Tiếng Việt', N'Lê Hoàng', N'Minh Anh, Quốc Huy', N'T18', N'Một gia đình phát hiện bí mật bị chôn giấu trong căn nhà mới.', N'https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=600', N'https://www.youtube.com/', N'Upcoming');

DECLARE @MovieGenreSeed TABLE (MovieTitle NVARCHAR(250), GenreName NVARCHAR(100));
INSERT @MovieGenreSeed VALUES
    (N'Đường Đua Ánh Sáng', N'Action'),
    (N'Đường Đua Ánh Sáng', N'Drama'),
    (N'Hành Tinh Xanh', N'Adventure'),
    (N'Hành Tinh Xanh', N'Animation'),
    (N'Chân Trời 2099', N'Sci-Fi'),
    (N'Chân Trời 2099', N'Action'),
    (N'Ngôi Nhà Bí Ẩn', N'Horror');

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
    DECLARE @Movie1 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Đường Đua Ánh Sáng');
    DECLARE @Movie2 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Hành Tinh Xanh');
    DECLARE @Room1 INT = (SELECT RoomID FROM dbo.Room WHERE RoomCode = N'ROOM-01');
    DECLARE @Room2 INT = (SELECT RoomID FROM dbo.Room WHERE RoomCode = N'ROOM-02');

    INSERT dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status]) VALUES
        (@Movie1, @Room1, DATEADD(HOUR, 10, CAST(@Tomorrow AS DATETIME2)), DATEADD(MINUTE, 125, DATEADD(HOUR, 10, CAST(@Tomorrow AS DATETIME2))), 85000, N'Scheduled'),
        (@Movie2, @Room2, DATEADD(HOUR, 14, CAST(@Tomorrow AS DATETIME2)), DATEADD(MINUTE, 110, DATEADD(HOUR, 14, CAST(@Tomorrow AS DATETIME2))), 110000, N'Scheduled'),
        (@Movie1, @Room2, DATEADD(HOUR, 19, CAST(@DayAfter AS DATETIME2)), DATEADD(MINUTE, 125, DATEADD(HOUR, 19, CAST(@DayAfter AS DATETIME2))), 110000, N'Scheduled'),
        (@Movie2, @Room1, DATEADD(HOUR, 20, CAST(@DayAfter AS DATETIME2)), DATEADD(MINUTE, 110, DATEADD(HOUR, 20, CAST(@DayAfter AS DATETIME2))), 95000, N'Scheduled');
END;
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
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SystemSetting WHERE SettingKey = 'BOOKING_HOLD_MINUTES')
    INSERT dbo.SystemSetting (SettingKey, SettingValue, [Description]) VALUES ('BOOKING_HOLD_MINUTES', N'10', N'Thời gian giữ ghế chưa thanh toán');

PRINT N'CinemaManagementDB schema and demo data are ready.';
GO
