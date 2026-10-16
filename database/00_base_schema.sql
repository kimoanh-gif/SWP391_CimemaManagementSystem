/* Base schema exported from CinemaManagementDB on 2026-10-17.
   Schema and required lookup data only; no accounts or business transactions. */
IF DB_ID(N'CinemaManagementDB') IS NULL CREATE DATABASE CinemaManagementDB;
GO
USE CinemaManagementDB;
GO

IF OBJECT_ID('dbo.Role', 'U') IS NULL CREATE TABLE dbo.[Role] (
    RoleID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Role PRIMARY KEY,
    [Name] nvarchar(50) NOT NULL CONSTRAINT UQ_Role_Name UNIQUE,
    [Description] nvarchar(255) NULL
);
IF OBJECT_ID('dbo.Permission', 'U') IS NULL CREATE TABLE dbo.Permission (
    PermissionID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Permission PRIMARY KEY,
    [Name] nvarchar(100) NOT NULL CONSTRAINT UQ_Permission_Name UNIQUE,
    [Description] nvarchar(255) NULL
);
IF OBJECT_ID('dbo.MembershipRank', 'U') IS NULL CREATE TABLE dbo.MembershipRank (
    RankID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_MembershipRank PRIMARY KEY,
    [Name] nvarchar(50) NOT NULL CONSTRAINT UQ_MembershipRank_Name UNIQUE,
    MinTierPoints int NOT NULL, BenefitDescription nvarchar(500) NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_MembershipRank_Status DEFAULT 'ACTIVE'
);
IF OBJECT_ID('dbo.Genre', 'U') IS NULL CREATE TABLE dbo.Genre (
    GenreID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Genre PRIMARY KEY,
    [Name] nvarchar(100) NOT NULL CONSTRAINT UQ_Genre_Name UNIQUE
);
IF OBJECT_ID('dbo.SeatType', 'U') IS NULL CREATE TABLE dbo.SeatType (
    SeatTypeID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_SeatType PRIMARY KEY,
    TypeName nvarchar(50) NOT NULL CONSTRAINT UQ_SeatType_TypeName UNIQUE,
    [Description] nvarchar(255) NULL,
    PriceMultiplier decimal(5,2) NOT NULL CONSTRAINT DF_SeatType_Multiplier DEFAULT 1.00,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_SeatType_Status DEFAULT 'ACTIVE'
);
GO

IF OBJECT_ID('dbo.Account', 'U') IS NULL CREATE TABLE dbo.Account (
    AccountID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Account PRIMARY KEY,
    RoleID int NOT NULL, Username varchar(50) NOT NULL CONSTRAINT UQ_Account_Username UNIQUE,
    Email varchar(255) NOT NULL CONSTRAINT UQ_Account_Email UNIQUE,
    PasswordHash varchar(255) NULL, FullName nvarchar(100) NOT NULL, Phone varchar(15) NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_Account_Status DEFAULT 'ACTIVE',
    EmailVerifiedAt datetime2 NULL, CreatedAt datetime2 NOT NULL CONSTRAINT DF_Account_CreatedAt DEFAULT GETDATE(),
    LastLoginAt datetime2 NULL,
    CONSTRAINT FK_Account_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID)
);
IF OBJECT_ID('dbo.RolePermission', 'U') IS NULL CREATE TABLE dbo.RolePermission (
    RoleID int NOT NULL, PermissionID int NOT NULL,
    CONSTRAINT PK_RolePermission PRIMARY KEY (RoleID, PermissionID),
    CONSTRAINT FK_RolePermission_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID),
    CONSTRAINT FK_RolePermission_Permission FOREIGN KEY (PermissionID) REFERENCES dbo.Permission(PermissionID)
);
IF OBJECT_ID('dbo.Staff', 'U') IS NULL CREATE TABLE dbo.Staff (
    StaffID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Staff PRIMARY KEY,
    AccountID int NOT NULL CONSTRAINT UQ_Staff_Account UNIQUE, Position nvarchar(100) NOT NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_Staff_Status DEFAULT 'ACTIVE',
    CONSTRAINT FK_Staff_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
);
IF OBJECT_ID('dbo.Customer', 'U') IS NULL CREATE TABLE dbo.Customer (
    CustomerID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Customer PRIMARY KEY,
    AccountID int NOT NULL CONSTRAINT UQ_Customer_Account UNIQUE, RankID int NULL,
    DateOfBirth date NULL, [Address] nvarchar(255) NULL,
    AvailablePoints int NOT NULL CONSTRAINT DF_Customer_AvailablePoints DEFAULT 0,
    TierPoints int NOT NULL CONSTRAINT DF_Customer_TierPoints DEFAULT 0,
    ReceivePromotionEmail bit NOT NULL CONSTRAINT DF_Customer_PromotionEmail DEFAULT 0,
    CONSTRAINT FK_Customer_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID),
    CONSTRAINT FK_Customer_MembershipRank FOREIGN KEY (RankID) REFERENCES dbo.MembershipRank(RankID)
);
GO

IF OBJECT_ID('dbo.Movie', 'U') IS NULL CREATE TABLE dbo.Movie (
    MovieID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Movie PRIMARY KEY,
    Title nvarchar(255) NOT NULL, Duration int NOT NULL, ReleaseDate date NOT NULL,
    [Language] nvarchar(100) NULL, Director nvarchar(255) NULL, AgeRating varchar(20) NULL,
    [Description] nvarchar(max) NULL, [Status] varchar(30) NOT NULL,
    PosterURL varchar(500) NULL, TrailerURL varchar(500) NULL
);
IF OBJECT_ID('dbo.MovieGenre', 'U') IS NULL CREATE TABLE dbo.MovieGenre (
    MovieID int NOT NULL, GenreID int NOT NULL,
    CONSTRAINT PK_MovieGenre PRIMARY KEY (MovieID, GenreID),
    CONSTRAINT FK_MovieGenre_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID),
    CONSTRAINT FK_MovieGenre_Genre FOREIGN KEY (GenreID) REFERENCES dbo.Genre(GenreID)
);
IF OBJECT_ID('dbo.Room', 'U') IS NULL CREATE TABLE dbo.Room (
    RoomID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Room PRIMARY KEY,
    RoomName nvarchar(100) NOT NULL CONSTRAINT UQ_Room_RoomName UNIQUE,
    RoomType varchar(50) NOT NULL, Capacity int NOT NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_Room_Status DEFAULT 'ACTIVE'
);
IF OBJECT_ID('dbo.Seat', 'U') IS NULL CREATE TABLE dbo.Seat (
    SeatID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Seat PRIMARY KEY,
    RoomID int NOT NULL, SeatTypeID int NOT NULL, RowLabel varchar(10) NOT NULL,
    SeatNumber int NOT NULL, GridRow int NOT NULL, GridColumn int NOT NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_Seat_Status DEFAULT 'ACTIVE',
    CONSTRAINT UQ_Seat_Room_Position UNIQUE (RoomID, RowLabel, SeatNumber),
    CONSTRAINT UQ_Seat_Room_Grid UNIQUE (RoomID, GridRow, GridColumn),
    CONSTRAINT FK_Seat_Room FOREIGN KEY (RoomID) REFERENCES dbo.Room(RoomID),
    CONSTRAINT FK_Seat_SeatType FOREIGN KEY (SeatTypeID) REFERENCES dbo.SeatType(SeatTypeID)
);
IF OBJECT_ID('dbo.Showtime', 'U') IS NULL CREATE TABLE dbo.Showtime (
    ShowtimeID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Showtime PRIMARY KEY,
    MovieID int NOT NULL, RoomID int NOT NULL, StartTime datetime2 NOT NULL,
    EndTime datetime2 NOT NULL, BasePrice decimal(18,2) NOT NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_Showtime_Status DEFAULT 'SCHEDULED',
    CONSTRAINT FK_Showtime_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID),
    CONSTRAINT FK_Showtime_Room FOREIGN KEY (RoomID) REFERENCES dbo.Room(RoomID)
);
IF OBJECT_ID('dbo.FoodBeverageItem', 'U') IS NULL CREATE TABLE dbo.FoodBeverageItem (
    FoodBeverageItemID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_FoodBeverageItem PRIMARY KEY,
    [Name] nvarchar(150) NOT NULL, [Description] nvarchar(500) NULL,
    ItemType varchar(50) NOT NULL, Price decimal(18,2) NOT NULL, ImageURL varchar(500) NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_FoodBeverageItem_Status DEFAULT 'ACTIVE'
);
GO

IF OBJECT_ID('dbo.Promotion', 'U') IS NULL CREATE TABLE dbo.Promotion (
    PromotionID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Promotion PRIMARY KEY,
    [Name] nvarchar(150) NOT NULL, [Description] nvarchar(500) NULL,
    StartDate datetime2 NOT NULL, EndDate datetime2 NOT NULL, [Status] varchar(30) NOT NULL
);
IF OBJECT_ID('dbo.Voucher', 'U') IS NULL CREATE TABLE dbo.Voucher (
    VoucherID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Voucher PRIMARY KEY,
    PromotionID int NOT NULL, RequiredRankID int NULL, Code varchar(50) NOT NULL CONSTRAINT UQ_Voucher_Code UNIQUE,
    DiscountType varchar(30) NOT NULL, DiscountValue decimal(18,2) NOT NULL,
    MaxDiscountAmount decimal(18,2) NULL, MinBookingAmount decimal(18,2) NULL,
    ValidFrom datetime2 NOT NULL, ValidTo datetime2 NOT NULL,
    TotalUsageLimit int NULL, PerAccountLimit int NULL, [Status] varchar(30) NOT NULL,
    CONSTRAINT FK_Voucher_Promotion FOREIGN KEY (PromotionID) REFERENCES dbo.Promotion(PromotionID),
    CONSTRAINT FK_Voucher_MembershipRank FOREIGN KEY (RequiredRankID) REFERENCES dbo.MembershipRank(RankID)
);
IF OBJECT_ID('dbo.Booking', 'U') IS NULL CREATE TABLE dbo.Booking (
    BookingID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Booking PRIMARY KEY,
    CustomerID int NULL, StaffID int NULL, ShowtimeID int NOT NULL, VoucherID int NULL,
    BookingCode varchar(30) NOT NULL CONSTRAINT UQ_Booking_BookingCode UNIQUE,
    BookingType varchar(30) NOT NULL, Subtotal decimal(18,2) NOT NULL,
    DiscountAmount decimal(18,2) NOT NULL CONSTRAINT DF_Booking_Discount DEFAULT 0,
    TotalAmount decimal(18,2) NOT NULL,
    BookingTime datetime2 NOT NULL CONSTRAINT DF_Booking_Time DEFAULT SYSDATETIME(),
    [Status] varchar(30) NOT NULL CONSTRAINT DF_Booking_Status DEFAULT 'PENDING',
    CONSTRAINT FK_Booking_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customer(CustomerID),
    CONSTRAINT FK_Booking_Staff FOREIGN KEY (StaffID) REFERENCES dbo.Staff(StaffID),
    CONSTRAINT FK_Booking_Showtime FOREIGN KEY (ShowtimeID) REFERENCES dbo.Showtime(ShowtimeID),
    CONSTRAINT FK_Booking_Voucher FOREIGN KEY (VoucherID) REFERENCES dbo.Voucher(VoucherID)
);
IF OBJECT_ID('dbo.BookingSeat', 'U') IS NULL CREATE TABLE dbo.BookingSeat (
    BookingSeatID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookingSeat PRIMARY KEY,
    BookingID int NOT NULL, SeatID int NOT NULL, ShowtimeID int NOT NULL,
    UnitPrice decimal(18,2) NOT NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_BookingSeat_Status DEFAULT 'HELD',
    HeldUntil datetime2 NULL,
    CONSTRAINT UQ_BookingSeat_Showtime_Seat UNIQUE (ShowtimeID, SeatID),
    CONSTRAINT FK_BookingSeat_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID),
    CONSTRAINT FK_BookingSeat_Seat FOREIGN KEY (SeatID) REFERENCES dbo.Seat(SeatID),
    CONSTRAINT FK_BookingSeat_Showtime FOREIGN KEY (ShowtimeID) REFERENCES dbo.Showtime(ShowtimeID)
);
IF OBJECT_ID('dbo.BookingFoodBeverage', 'U') IS NULL CREATE TABLE dbo.BookingFoodBeverage (
    BookingFoodBeverageID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_BookingFoodBeverage PRIMARY KEY,
    BookingID int NOT NULL, FoodBeverageItemID int NOT NULL, Quantity int NOT NULL,
    UnitPrice decimal(18,2) NOT NULL, Subtotal decimal(18,2) NULL,
    CONSTRAINT UQ_BookingFoodBeverage UNIQUE (BookingID, FoodBeverageItemID),
    CONSTRAINT FK_BookingFoodBeverage_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID),
    CONSTRAINT FK_BookingFoodBeverage_Item FOREIGN KEY (FoodBeverageItemID) REFERENCES dbo.FoodBeverageItem(FoodBeverageItemID)
);
GO

IF OBJECT_ID('dbo.Payment', 'U') IS NULL CREATE TABLE dbo.Payment (
    PaymentID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Payment PRIMARY KEY,
    BookingID int NOT NULL, Method varchar(30) NOT NULL, Amount decimal(18,2) NOT NULL,
    TransactionRef varchar(100) NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_Payment_Status DEFAULT 'PENDING',
    CreatedAt datetime2 NOT NULL CONSTRAINT DF_Payment_CreatedAt DEFAULT SYSDATETIME(), PaidAt datetime2 NULL,
    CONSTRAINT FK_Payment_Booking FOREIGN KEY (BookingID) REFERENCES dbo.Booking(BookingID)
);
IF OBJECT_ID('dbo.Ticket', 'U') IS NULL CREATE TABLE dbo.Ticket (
    TicketID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Ticket PRIMARY KEY,
    BookingSeatID int NOT NULL CONSTRAINT UQ_Ticket_BookingSeat UNIQUE,
    QRCode varchar(500) NOT NULL CONSTRAINT UQ_Ticket_QRCode UNIQUE,
    TicketStatus varchar(30) NOT NULL CONSTRAINT DF_Ticket_Status DEFAULT 'VALID', CheckedInAt datetime2 NULL,
    CONSTRAINT FK_Ticket_BookingSeat FOREIGN KEY (BookingSeatID) REFERENCES dbo.BookingSeat(BookingSeatID)
);
IF OBJECT_ID('dbo.MovieReview', 'U') IS NULL CREATE TABLE dbo.MovieReview (
    ReviewID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_MovieReview PRIMARY KEY,
    CustomerID int NOT NULL, MovieID int NOT NULL, Rating int NOT NULL,
    Content nvarchar(1000) NULL, [Status] varchar(30) NOT NULL CONSTRAINT DF_MovieReview_Status DEFAULT 'ACTIVE',
    CreatedAt datetime2 NOT NULL CONSTRAINT DF_MovieReview_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT UQ_MovieReview_Customer_Movie UNIQUE (CustomerID, MovieID),
    CONSTRAINT FK_MovieReview_Customer FOREIGN KEY (CustomerID) REFERENCES dbo.Customer(CustomerID),
    CONSTRAINT FK_MovieReview_Movie FOREIGN KEY (MovieID) REFERENCES dbo.Movie(MovieID)
);
IF OBJECT_ID('dbo.News', 'U') IS NULL CREATE TABLE dbo.News (
    NewsID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_News PRIMARY KEY,
    CreatedByStaffID int NOT NULL, ReviewedByAccountID int NULL, Title nvarchar(255) NOT NULL,
    Content nvarchar(max) NOT NULL, ThumbnailURL varchar(500) NULL,
    [Status] varchar(30) NOT NULL CONSTRAINT DF_News_Status DEFAULT 'DRAFT',
    CreatedAt datetime2 NOT NULL CONSTRAINT DF_News_CreatedAt DEFAULT SYSDATETIME(), UpdatedAt datetime2 NULL,
    SubmittedAt datetime2 NULL, ReviewedAt datetime2 NULL, PublishedAt datetime2 NULL,
    RejectionReason nvarchar(500) NULL,
    CONSTRAINT FK_News_CreatedByStaff FOREIGN KEY (CreatedByStaffID) REFERENCES dbo.Staff(StaffID),
    CONSTRAINT FK_News_ReviewedByAccount FOREIGN KEY (ReviewedByAccountID) REFERENCES dbo.Account(AccountID)
);
IF OBJECT_ID('dbo.SystemSetting', 'U') IS NULL CREATE TABLE dbo.SystemSetting (
    SettingID int IDENTITY(1,1) NOT NULL CONSTRAINT PK_SystemSetting PRIMARY KEY,
    SettingKey varchar(100) NOT NULL CONSTRAINT UQ_SystemSetting_Key UNIQUE,
    SettingValue nvarchar(max) NULL, [Description] nvarchar(500) NULL, UpdatedByAccountID int NULL,
    UpdatedAt datetime2 NOT NULL CONSTRAINT DF_SystemSetting_UpdatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_SystemSetting_UpdatedByAccount FOREIGN KEY (UpdatedByAccountID) REFERENCES dbo.Account(AccountID)
);
GO

-- Authentication support tables are also created here so a clean database is runnable immediately.
IF OBJECT_ID('dbo.OtpToken', 'U') IS NULL CREATE TABLE dbo.OtpToken (
    OtpTokenID bigint IDENTITY(1,1) NOT NULL CONSTRAINT PK_OtpToken PRIMARY KEY,
    AccountID int NULL, Email nvarchar(255) NOT NULL, CodeHash nvarchar(255) NOT NULL,
    Purpose nvarchar(30) NOT NULL, ExpiresAt datetime2 NOT NULL, ConsumedAt datetime2 NULL,
    AttemptCount int NOT NULL CONSTRAINT DF_OtpToken_AttemptCount DEFAULT 0,
    CreatedAt datetime2 NOT NULL CONSTRAINT DF_OtpToken_CreatedAt DEFAULT SYSDATETIME(), LastSentAt datetime2 NULL,
    CONSTRAINT FK_OtpToken_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
);
IF OBJECT_ID('dbo.LoginAttempt', 'U') IS NULL CREATE TABLE dbo.LoginAttempt (
    AttemptID bigint IDENTITY(1,1) NOT NULL CONSTRAINT PK_LoginAttempt PRIMARY KEY,
    Identifier nvarchar(255) NOT NULL, IPAddress nvarchar(45) NOT NULL, UserAgent nvarchar(500) NULL,
    Success bit NOT NULL, FailureReason nvarchar(50) NULL, AuthMethod varchar(30) NULL,
    AttemptedAt datetime2 NOT NULL CONSTRAINT DF_LoginAttempt_AttemptedAt DEFAULT SYSDATETIME()
);
IF OBJECT_ID('dbo.OAuthAccount', 'U') IS NULL CREATE TABLE dbo.OAuthAccount (
    OAuthAccountID bigint IDENTITY(1,1) NOT NULL CONSTRAINT PK_OAuthAccount PRIMARY KEY,
    AccountID int NOT NULL, Provider varchar(20) NOT NULL, ProviderSubject varchar(255) NOT NULL,
    ProviderEmail varchar(255) NULL,
    EmailVerifiedByProvider bit NOT NULL CONSTRAINT DF_OAuthAccount_Verified DEFAULT 0,
    LinkedAt datetime2 NOT NULL CONSTRAINT DF_OAuthAccount_LinkedAt DEFAULT SYSDATETIME(), LastLoginAt datetime2 NULL,
    CONSTRAINT UQ_OAuthAccount_Provider_Subject UNIQUE (Provider, ProviderSubject),
    CONSTRAINT FK_OAuthAccount_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.MembershipRank WHERE [Name] = N'STANDARD')
    INSERT dbo.MembershipRank ([Name], MinTierPoints, BenefitDescription, [Status])
    VALUES (N'STANDARD', 0, N'Default membership rank', 'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = N'Action') INSERT dbo.Genre ([Name]) VALUES (N'Action');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = N'Adventure') INSERT dbo.Genre ([Name]) VALUES (N'Adventure');
IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeName = N'STANDARD')
    INSERT dbo.SeatType (TypeName, [Description], PriceMultiplier, [Status]) VALUES (N'STANDARD', N'Standard seat', 1.00, 'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeName = N'VIP')
    INSERT dbo.SeatType (TypeName, [Description], PriceMultiplier, [Status]) VALUES (N'VIP', N'VIP seat', 1.25, 'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM dbo.SeatType WHERE TypeName = N'COUPLE')
    INSERT dbo.SeatType (TypeName, [Description], PriceMultiplier, [Status]) VALUES (N'COUPLE', N'Couple seat', 2.00, 'ACTIVE');
GO
