-- =====================================================================
-- Cinema Management System - SWP391
-- Screen 60: Ticket Pricing Management (Iteration 2 / UC-31)
-- Database Migration Script for SeatType, TicketPricing, and Showtime Pricing Rules
-- Target DBMS: Microsoft SQL Server 2019+
-- Business Rules: BR-50 (Ticket price calculation = BaseTicketPrice * PriceMultiplier, rounded to nearest 1,000 VND)
--                 BR-51 (Price snapshot at booking time)
-- =====================================================================

-- 1. Create SeatType Table if not exists
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SeatType')
BEGIN
    CREATE TABLE SeatType (
        SeatTypeID INT IDENTITY(1,1) PRIMARY KEY,
        TypeCode NVARCHAR(50) NOT NULL,            -- e.g. 'STD', 'VIP', 'COUPLE', 'FRONT'
        TypeName NVARCHAR(100) NOT NULL,           -- e.g. 'Standard', 'VIP', 'Couple', 'Front'
        Description NVARCHAR(255) NULL,
        PriceMultiplier DECIMAL(5,2) NOT NULL DEFAULT 1.00, -- e.g. 1.00, 1.25, 1.50
        ColorCode NVARCHAR(20) NULL DEFAULT '#3B82F6',       -- Badge display color
        Status NVARCHAR(20) NOT NULL CONSTRAINT DF_SeatType_Status DEFAULT 'Active', -- 'Active', 'Inactive'
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_SeatType_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_SeatType_TypeCode UNIQUE (TypeCode),
        CONSTRAINT UQ_SeatType_TypeName UNIQUE (TypeName),
        CONSTRAINT CK_SeatType_PriceMultiplier CHECK (PriceMultiplier >= 0.50 AND PriceMultiplier <= 3.00)
    );
END;
GO

-- 2. Seed default SeatTypes as documented in SRS
IF NOT EXISTS (SELECT 1 FROM SeatType WHERE TypeCode = 'STD')
BEGIN
    INSERT INTO SeatType (TypeCode, TypeName, Description, PriceMultiplier, ColorCode, Status)
    VALUES ('STD', N'Ghế Tiêu Chuẩn (Standard)', N'Ghế ngồi tiêu chuẩn tầm nhìn bao quát toàn màn hình.', 1.00, '#3B82F6', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM SeatType WHERE TypeCode = 'VIP')
BEGIN
    INSERT INTO SeatType (TypeCode, TypeName, Description, PriceMultiplier, ColorCode, Status)
    VALUES ('VIP', N'Ghế VIP Royal Amber', N'Ghế đệm cao cấp tại các hàng trung tâm, góc nhìn tối ưu nhất.', 1.25, '#F59E0B', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM SeatType WHERE TypeCode = 'COUPLE')
BEGIN
    INSERT INTO SeatType (TypeCode, TypeName, Description, PriceMultiplier, ColorCode, Status)
    VALUES ('COUPLE', N'Ghế Đôi Sweetbox (Couple)', N'Ghế đôi kèm vách ngăn riêng tư dành cho 2 người ở hàng cuối.', 1.80, '#EC4899', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM SeatType WHERE TypeCode = 'FRONT')
BEGIN
    INSERT INTO SeatType (TypeCode, TypeName, Description, PriceMultiplier, ColorCode, Status)
    VALUES ('FRONT', N'Ghế Hàng Đầu (Front)', N'Ghế hàng đầu sát màn hình với giá ưu đãi tiết kiệm.', 0.90, '#10B981', 'Active');
END;
GO

-- 3. Create TicketPricing Matrix Table
-- Represents time-slot base pricing and format configurations (Screen 60 / SRS Section 4.6)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'TicketPricing')
BEGIN
    CREATE TABLE TicketPricing (
        PricingID INT IDENTITY(1,1) PRIMARY KEY,
        PricingCode NVARCHAR(50) NOT NULL,          -- e.g. 'PRICE-WEEKDAY-STD', 'PRICE-WEEKEND-PEAK'
        RuleName NVARCHAR(150) NOT NULL,            -- e.g. 'Giá Tiêu Chuẩn Ngày Thường'
        DayType NVARCHAR(50) NOT NULL,              -- 'WEEKDAY' (Mon-Fri), 'WEEKEND' (Sat-Sun), 'HOLIDAY'
        TimeSlot NVARCHAR(50) NOT NULL,             -- 'MORNING' (<12:00), 'STANDARD' (12:00-17:00), 'GOLDEN_HOUR' (>17:00)
        BasePrice DECIMAL(12,2) NOT NULL,           -- Base 2D ticket price (VND)
        FormatSurcharge DECIMAL(12,2) NOT NULL DEFAULT 0.0, -- e.g. 0 for 2D, 20000 for 3D, 60000 for IMAX
        ExperienceType NVARCHAR(50) NOT NULL DEFAULT '2D Digital', -- '2D Digital', '3D RealD', 'Dolby Atmos', 'IMAX Laser'
        Status NVARCHAR(20) NOT NULL CONSTRAINT DF_TicketPricing_Status DEFAULT 'Active', -- 'Active', 'Inactive'
        Description NVARCHAR(255) NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_TicketPricing_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_TicketPricing_Code UNIQUE (PricingCode),
        CONSTRAINT CK_TicketPricing_BasePrice CHECK (BasePrice >= 0),
        CONSTRAINT CK_TicketPricing_FormatSurcharge CHECK (FormatSurcharge >= 0)
    );
END;
GO

-- 4. Seed Standard Ticket Pricing Matrix rules
IF NOT EXISTS (SELECT 1 FROM TicketPricing WHERE PricingCode = 'PRICE-WD-STD')
BEGIN
    INSERT INTO TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, Status, Description)
    VALUES ('PRICE-WD-STD', N'Ngày thường - Khung giờ tiêu chuẩn (12:00 - 17:00)', 'WEEKDAY', 'STANDARD', 85000.00, 0.0, '2D Digital', 'Active', N'Giá vé cơ bản 2D ngày trong tuần từ thứ 2 đến thứ 6.');
END;

IF NOT EXISTS (SELECT 1 FROM TicketPricing WHERE PricingCode = 'PRICE-WD-MORN')
BEGIN
    INSERT INTO TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, Status, Description)
    VALUES ('PRICE-WD-MORN', N'Ngày thường - Buổi sáng sớm (< 12:00)', 'WEEKDAY', 'MORNING', 65000.00, 0.0, '2D Digital', 'Active', N'Suất chiếu sáng ưu đãi học sinh, sinh viên và người cao tuổi.');
END;

IF NOT EXISTS (SELECT 1 FROM TicketPricing WHERE PricingCode = 'PRICE-WD-GOLD')
BEGIN
    INSERT INTO TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, Status, Description)
    VALUES ('PRICE-WD-GOLD', N'Ngày thường - Giờ vàng tối (> 17:00)', 'WEEKDAY', 'GOLDEN_HOUR', 95000.00, 0.0, '2D Digital', 'Active', N'Suất chiếu tối giờ cao điểm từ thứ 2 đến thứ 6.');
END;

IF NOT EXISTS (SELECT 1 FROM TicketPricing WHERE PricingCode = 'PRICE-WE-ALL')
BEGIN
    INSERT INTO TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, Status, Description)
    VALUES ('PRICE-WE-ALL', N'Cuối tuần & Ngày lễ (Thứ 7, CN)', 'WEEKEND', 'ALL_DAY', 110000.00, 0.0, '2D Digital', 'Active', N'Giá vé cơ bản cuối tuần và các dịp lễ tết.');
END;

IF NOT EXISTS (SELECT 1 FROM TicketPricing WHERE PricingCode = 'PRICE-SUR-3D')
BEGIN
    INSERT INTO TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, Status, Description)
    VALUES ('PRICE-SUR-3D', N'Phụ thu định dạng 3D RealD', 'WEEKDAY', 'STANDARD', 85000.00, 20000.00, '3D RealD', 'Active', N'Phụ thu kính và máy chiếu 3D RealD.');
END;

IF NOT EXISTS (SELECT 1 FROM TicketPricing WHERE PricingCode = 'PRICE-SUR-IMAX')
BEGIN
    INSERT INTO TicketPricing (PricingCode, RuleName, DayType, TimeSlot, BasePrice, FormatSurcharge, ExperienceType, Status, Description)
    VALUES ('PRICE-SUR-IMAX', N'Phụ thu định dạng IMAX Laser', 'WEEKDAY', 'STANDARD', 85000.00, 60000.00, 'IMAX Laser', 'Active', N'Phụ thu phòng chiếu màn hình cực đại IMAX Laser.');
END;
GO
