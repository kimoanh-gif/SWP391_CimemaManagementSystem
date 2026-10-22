/* ================================================================
   MIGRATION: 08_team_sync_migration.sql
   PURPOSE: Complete, safe, non-destructive synchronization of CinemaManagementDB
            with the official team schema (00_full_database.sql).
   TARGET DBMS: Microsoft SQL Server 2019+
   SAFETY RULES:
     - 100% Non-destructive: NO DROP, NO TRUNCATE, NO DELETE of user data.
     - Fully Idempotent: Can be run multiple times safely.
     - Preserves all existing records in Account, Customer, Movie, Genre, MembershipRank.
     - Seamlessly upgrades legacy table columns and backfills default values.
   ================================================================ */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET XACT_ABORT ON;
GO

PRINT '>>> [START] Complete Schema Synchronization with 00_full_database.sql';
GO

/* ================================================================
   1. PRE-MIGRATION UPGRADE OF LEGACY TABLES
   Safely adds any missing columns to existing tables before foreign keys are bound.
   ================================================================ */

-- 1.1 Upgrade dbo.MembershipRank
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
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.MembershipRank', N'DisplayName') IS NULL ALTER TABLE dbo.MembershipRank ADD DisplayName NVARCHAR(100) NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'MinPoints') IS NULL ALTER TABLE dbo.MembershipRank ADD MinPoints INT NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'MaxPoints') IS NULL ALTER TABLE dbo.MembershipRank ADD MaxPoints INT NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'Benefits') IS NULL ALTER TABLE dbo.MembershipRank ADD Benefits NVARCHAR(MAX) NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'DiscountPercent') IS NULL ALTER TABLE dbo.MembershipRank ADD DiscountPercent DECIMAL(5, 2) NULL;
    IF COL_LENGTH(N'dbo.MembershipRank', N'IsActive') IS NULL ALTER TABLE dbo.MembershipRank ADD IsActive BIT NULL;

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
               IsActive = COALESCE(IsActive, 1)');
END;
GO

-- 1.2 Upgrade dbo.[Role]
IF OBJECT_ID(N'dbo.[Role]', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[Role] (
        RoleID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Role PRIMARY KEY,
        [Name] NVARCHAR(50) NOT NULL CONSTRAINT UQ_Role_Name UNIQUE,
        [Description] NVARCHAR(255) NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'ADMIN') INSERT dbo.[Role] ([Name], [Description]) VALUES (N'ADMIN', N'Quản trị hệ thống');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'MANAGER') INSERT dbo.[Role] ([Name], [Description]) VALUES (N'MANAGER', N'Quản lý rạp');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'STAFF') INSERT dbo.[Role] ([Name], [Description]) VALUES (N'STAFF', N'Nhân viên');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE [Name] = N'CUSTOMER') INSERT dbo.[Role] ([Name], [Description]) VALUES (N'CUSTOMER', N'Khách hàng');
GO

-- 1.3 Upgrade dbo.Account
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
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.Account', N'RoleID') IS NULL ALTER TABLE dbo.Account ADD RoleID INT NULL;
    IF COL_LENGTH(N'dbo.Account', N'PasswordHash') IS NULL ALTER TABLE dbo.Account ADD PasswordHash VARCHAR(255) NULL;
    IF COL_LENGTH(N'dbo.Account', N'Status') IS NULL ALTER TABLE dbo.Account ADD [Status] VARCHAR(30) NULL;
    IF COL_LENGTH(N'dbo.Account', N'EmailVerifiedAt') IS NULL ALTER TABLE dbo.Account ADD EmailVerifiedAt DATETIME2 NULL;
    IF COL_LENGTH(N'dbo.Account', N'CreatedAt') IS NULL ALTER TABLE dbo.Account ADD CreatedAt DATETIME2 NULL;
    IF COL_LENGTH(N'dbo.Account', N'LastLoginAt') IS NULL ALTER TABLE dbo.Account ADD LastLoginAt DATETIME2 NULL;
    IF COL_LENGTH(N'dbo.Account', N'CurrentAddress') IS NULL ALTER TABLE dbo.Account ADD CurrentAddress NVARCHAR(255) NULL;
    IF COL_LENGTH(N'dbo.Account', N'AvatarUrl') IS NULL ALTER TABLE dbo.Account ADD AvatarUrl NVARCHAR(500) NULL;
    IF COL_LENGTH(N'dbo.Account', N'Notes') IS NULL ALTER TABLE dbo.Account ADD Notes NVARCHAR(500) NULL;
END;
GO

-- Backfill legacy Account records
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

DECLARE @DefaultCustomerRoleID INT = (SELECT TOP 1 RoleID FROM dbo.[Role] WHERE [Name] = N'CUSTOMER');
UPDATE dbo.Account
SET RoleID = COALESCE(RoleID, @DefaultCustomerRoleID),
    PasswordHash = COALESCE(PasswordHash, '$2a$10$nya.sfK50SAuM6gNW27SCemCJsk/4tRDbKsp/8sV29Cg8NJnq/7Z2'), -- default: password123
    [Status] = COALESCE([Status], 'ACTIVE'),
    EmailVerifiedAt = COALESCE(EmailVerifiedAt, SYSDATETIME()),
    CreatedAt = COALESCE(CreatedAt, SYSDATETIME());
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE [name] = N'FK_Account_Role' AND parent_object_id = OBJECT_ID(N'dbo.Account'))
    ALTER TABLE dbo.Account WITH CHECK ADD CONSTRAINT FK_Account_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Account') AND [name] = N'UX_Account_Phone')
    CREATE UNIQUE INDEX UX_Account_Phone ON dbo.Account(Phone) WHERE Phone IS NOT NULL;
GO

-- 1.4 Upgrade dbo.Customer
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
END
ELSE
BEGIN
    IF COL_LENGTH(N'dbo.Customer', N'ReceivePromotionEmail') IS NULL
        ALTER TABLE dbo.Customer ADD ReceivePromotionEmail BIT NOT NULL CONSTRAINT DF_Customer_PromotionEmail DEFAULT 0;
END;
GO

-- 1.5 Upgrade dbo.Staff
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
GO

-- 1.6 Upgrade dbo.Permission & dbo.RolePermission
IF OBJECT_ID(N'dbo.Permission', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Permission (
        PermissionID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Permission PRIMARY KEY,
        [Name] NVARCHAR(100) NOT NULL CONSTRAINT UQ_Permission_Name UNIQUE,
        [Description] NVARCHAR(255) NULL
    );
END;
GO

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
GO

-- 1.7 Upgrade dbo.Genre
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
GO

-- 1.8 Upgrade dbo.Movie
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
IF COL_LENGTH(N'dbo.Movie', N'Language') IS NULL ALTER TABLE dbo.Movie ADD [Language] NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.Movie', N'Director') IS NULL ALTER TABLE dbo.Movie ADD Director NVARCHAR(150) NULL;
IF COL_LENGTH(N'dbo.Movie', N'Actors') IS NULL ALTER TABLE dbo.Movie ADD Actors NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Movie', N'AgeRating') IS NULL ALTER TABLE dbo.Movie ADD AgeRating NVARCHAR(10) NOT NULL DEFAULT 'P' WITH VALUES;
IF COL_LENGTH(N'dbo.Movie', N'Description') IS NULL ALTER TABLE dbo.Movie ADD [Description] NVARCHAR(1000) NULL;
IF COL_LENGTH(N'dbo.Movie', N'PosterURL') IS NULL ALTER TABLE dbo.Movie ADD PosterURL NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Movie', N'TrailerURL') IS NULL ALTER TABLE dbo.Movie ADD TrailerURL NVARCHAR(500) NULL;
IF COL_LENGTH(N'dbo.Movie', N'CreatedAt') IS NULL ALTER TABLE dbo.Movie ADD CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME() WITH VALUES;
IF COL_LENGTH(N'dbo.Movie', N'UpdatedAt') IS NULL ALTER TABLE dbo.Movie ADD UpdatedAt DATETIME2 NULL;
GO

-- 1.9 Upgrade dbo.MovieGenre
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
   2. AUDITORIUM, SEATS, SHOWTIMES, PRICING
   ================================================================ */

-- 2.1 dbo.Room
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
GO

-- 2.2 dbo.SeatType
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
IF COL_LENGTH(N'dbo.SeatType', N'Status') IS NULL ALTER TABLE dbo.SeatType ADD [Status] NVARCHAR(20) NOT NULL DEFAULT 'Active' WITH VALUES;
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
GO

-- 2.3 dbo.Seat
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
GO

-- 2.4 dbo.TicketPricing
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
IF COL_LENGTH(N'dbo.TicketPricing', N'PricingCode') IS NULL ALTER TABLE dbo.TicketPricing ADD PricingCode NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.TicketPricing', N'RuleName') IS NULL ALTER TABLE dbo.TicketPricing ADD RuleName NVARCHAR(150) NULL;
IF COL_LENGTH(N'dbo.TicketPricing', N'FormatSurcharge') IS NULL ALTER TABLE dbo.TicketPricing ADD FormatSurcharge DECIMAL(12,2) NOT NULL DEFAULT 0 WITH VALUES;
IF COL_LENGTH(N'dbo.TicketPricing', N'ExperienceType') IS NULL ALTER TABLE dbo.TicketPricing ADD ExperienceType NVARCHAR(50) NOT NULL DEFAULT '2D Digital' WITH VALUES;
IF COL_LENGTH(N'dbo.TicketPricing', N'Status') IS NULL ALTER TABLE dbo.TicketPricing ADD [Status] NVARCHAR(20) NOT NULL DEFAULT 'Active' WITH VALUES;
IF COL_LENGTH(N'dbo.TicketPricing', N'Description') IS NULL ALTER TABLE dbo.TicketPricing ADD [Description] NVARCHAR(255) NULL;
GO

-- 2.5 dbo.Showtime
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
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Showtime') AND [name] = N'IX_Showtime_Movie_StartTime')
    CREATE INDEX IX_Showtime_Movie_StartTime ON dbo.Showtime(MovieID, StartTime);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.Showtime') AND [name] = N'IX_Showtime_Room_StartTime')
    CREATE INDEX IX_Showtime_Room_StartTime ON dbo.Showtime(RoomID, StartTime);
GO

/* ================================================================
   3. FOOD & BEVERAGES
   ================================================================ */

-- 3.1 dbo.FoodBeverageItem
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
GO

-- 3.2 dbo.ComboItem
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
   4. PROMOTIONS, BOOKINGS, PAYMENTS, AND TICKETS
   ================================================================ */

-- 4.1 dbo.Promotion
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
GO

-- 4.2 dbo.Voucher
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
GO

-- 4.3 dbo.Booking
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
GO

-- 4.4 dbo.BookingSeat
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
GO

-- 4.5 dbo.BookingFoodBeverage
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
GO

-- 4.6 dbo.Payment
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
IF COL_LENGTH(N'dbo.Payment', N'Method') IS NULL ALTER TABLE dbo.Payment ADD Method VARCHAR(30) NOT NULL DEFAULT 'VNPAY' WITH VALUES;
IF COL_LENGTH(N'dbo.Payment', N'TransactionRef') IS NULL ALTER TABLE dbo.Payment ADD TransactionRef VARCHAR(100) NULL;
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
GO

-- 4.7 dbo.Ticket
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
   5. REVIEWS, NEWS, SETTINGS & AUTH LOGS
   ================================================================ */

-- 5.1 dbo.MovieReview
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
IF COL_LENGTH(N'dbo.MovieReview', N'Content') IS NULL ALTER TABLE dbo.MovieReview ADD Content NVARCHAR(1000) NULL;
IF COL_LENGTH(N'dbo.MovieReview', N'Status') IS NULL ALTER TABLE dbo.MovieReview ADD [Status] VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' WITH VALUES;
GO

-- 5.2 dbo.News
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
GO

-- 5.3 dbo.SystemSetting
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
IF COL_LENGTH(N'dbo.SystemSetting', N'Value') IS NULL ALTER TABLE dbo.SystemSetting ADD Value NVARCHAR(MAX) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'SettingName') IS NULL ALTER TABLE dbo.SystemSetting ADD SettingName NVARCHAR(255) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'Category') IS NULL ALTER TABLE dbo.SystemSetting ADD Category NVARCHAR(100) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'ValueType') IS NULL ALTER TABLE dbo.SystemSetting ADD ValueType NVARCHAR(50) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'DefaultValue') IS NULL ALTER TABLE dbo.SystemSetting ADD DefaultValue NVARCHAR(MAX) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'ValidationRules') IS NULL ALTER TABLE dbo.SystemSetting ADD ValidationRules NVARCHAR(MAX) NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'IsActive') IS NULL ALTER TABLE dbo.SystemSetting ADD IsActive BIT NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'UpdatedByAccountID') IS NULL ALTER TABLE dbo.SystemSetting ADD UpdatedByAccountID INT NULL;
IF COL_LENGTH(N'dbo.SystemSetting', N'UpdatedAt') IS NULL ALTER TABLE dbo.SystemSetting ADD UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME() WITH VALUES;

IF COL_LENGTH(N'dbo.SystemSetting', N'SettingValue') IS NOT NULL
    EXEC(N'UPDATE dbo.SystemSetting SET Value = COALESCE(Value, SettingValue)');
EXEC(N'UPDATE dbo.SystemSetting
       SET SettingName = COALESCE(SettingName, SettingKey),
           Category = COALESCE(Category,
               CASE WHEN CHARINDEX(''_'', SettingKey) > 0 THEN LEFT(SettingKey, CHARINDEX(''_'', SettingKey) - 1) ELSE N''SYSTEM'' END),
           ValueType = COALESCE(ValueType, CASE WHEN TRY_CONVERT(DECIMAL(38, 10), Value) IS NOT NULL THEN N''NUMBER'' ELSE N''STRING'' END),
           DefaultValue = COALESCE(DefaultValue, Value),
           IsActive = COALESCE(IsActive, 1)');
GO

-- 5.4 dbo.OtpToken
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
        SendCount INT NULL CONSTRAINT DF_OtpToken_SendCount DEFAULT 0,
        SendCountDate DATETIME NULL,
        CONSTRAINT FK_OtpToken_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
    );
END;
IF COL_LENGTH(N'dbo.OtpToken', N'LockedUntil') IS NULL ALTER TABLE dbo.OtpToken ADD LockedUntil DATETIME2 NULL;
IF COL_LENGTH(N'dbo.OtpToken', N'SendCount') IS NULL ALTER TABLE dbo.OtpToken ADD SendCount INT NULL DEFAULT 0;
IF COL_LENGTH(N'dbo.OtpToken', N'SendCountDate') IS NULL ALTER TABLE dbo.OtpToken ADD SendCountDate DATETIME NULL;
GO

-- 5.5 dbo.LoginAttempt
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
IF COL_LENGTH(N'dbo.LoginAttempt', N'AuthMethod') IS NULL ALTER TABLE dbo.LoginAttempt ADD AuthMethod VARCHAR(30) NULL;
GO

-- 5.6 dbo.OtpAttempt
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
GO

-- 5.7 dbo.IpRateLimit
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
GO

-- 5.8 dbo.OAuthAccount
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
   6. CHECK CONSTRAINTS, FOREIGN KEYS & INDEXES
   ================================================================ */

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE [name] = N'CK_OtpToken_Purpose' AND parent_object_id = OBJECT_ID(N'dbo.OtpToken'))
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OtpToken WHERE Purpose NOT IN (N'ACTIVATION', N'RESET_PASSWORD'))
        ALTER TABLE dbo.OtpToken WITH NOCHECK ADD CONSTRAINT CK_OtpToken_Purpose CHECK (Purpose IN (N'ACTIVATION', N'RESET_PASSWORD'));
    ELSE
        ALTER TABLE dbo.OtpToken WITH CHECK ADD CONSTRAINT CK_OtpToken_Purpose CHECK (Purpose IN (N'ACTIVATION', N'RESET_PASSWORD'));
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE [name] = N'CK_OAuthAccount_Provider' AND parent_object_id = OBJECT_ID(N'dbo.OAuthAccount'))
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OAuthAccount WHERE Provider <> 'GOOGLE')
        ALTER TABLE dbo.OAuthAccount WITH NOCHECK ADD CONSTRAINT CK_OAuthAccount_Provider CHECK (Provider = 'GOOGLE');
    ELSE
        ALTER TABLE dbo.OAuthAccount WITH CHECK ADD CONSTRAINT CK_OAuthAccount_Provider CHECK (Provider = 'GOOGLE');
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE [name] = N'FK_OtpAttempt_OtpToken' AND parent_object_id = OBJECT_ID(N'dbo.OtpAttempt'))
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.OtpAttempt x WHERE x.OtpTokenID IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.OtpToken t WHERE t.OtpTokenID = x.OtpTokenID))
        ALTER TABLE dbo.OtpAttempt WITH NOCHECK ADD CONSTRAINT FK_OtpAttempt_OtpToken FOREIGN KEY (OtpTokenID) REFERENCES dbo.OtpToken(OtpTokenID);
    ELSE
        ALTER TABLE dbo.OtpAttempt WITH CHECK ADD CONSTRAINT FK_OtpAttempt_OtpToken FOREIGN KEY (OtpTokenID) REFERENCES dbo.OtpToken(OtpTokenID);
END;
GO

-- High performance lookup indexes
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
   7. SEED ESSENTIAL SYSTEM CONFIGURATION, ROLES, RANKS & DEMO SEATS
   ================================================================ */

-- Seed System Settings
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

-- Seed Seat Types if missing
IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeCode = N'STD')
    INSERT dbo.SeatType (TypeCode, TypeName, [Description], PriceMultiplier, ColorCode, [Status])
    VALUES (N'STD', N'Standard', N'Ghế tiêu chuẩn', 1.00, N'#3B82F6', N'Active');
IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeCode = N'VIP')
    INSERT dbo.SeatType (TypeCode, TypeName, [Description], PriceMultiplier, ColorCode, [Status])
    VALUES (N'VIP', N'VIP', N'Ghế VIP', 1.25, N'#F59E0B', N'Active');
IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeCode = N'COUPLE')
    INSERT dbo.SeatType (TypeCode, TypeName, [Description], PriceMultiplier, ColorCode, [Status])
    VALUES (N'COUPLE', N'Couple', N'Ghế đôi', 1.80, N'#EC4899', N'Active');
GO

-- Seed Rooms & Seats if missing
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

PRINT '>>> [DONE] Complete Schema Synchronization completed successfully!';
GO
