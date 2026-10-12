-- =====================================================================
-- Cinema Management System - SWP391
-- Screen 56: Cinema Room Management (Iteration 2 / UC-27)
-- Database Migration Script for Room and Seat Tables
-- Target DBMS: Microsoft SQL Server 2019+
-- Business Rules: BR-08 (Active Room for Showtime), BR-52 (Room Deactivation)
-- =====================================================================

-- 1. Create Room Table
-- Identity: RoomCode and RoomName must be unique within the cinema system
-- Lifecycle: 'Ready', 'Screening', 'Cleaning', 'Maintenance' (or 'Active', 'Inactive')
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Room')
BEGIN
    CREATE TABLE Room (
        RoomID INT IDENTITY(1,1) PRIMARY KEY,
        RoomCode NVARCHAR(50) NOT NULL,
        RoomName NVARCHAR(100) NOT NULL,
        RoomType NVARCHAR(50) NOT NULL DEFAULT 'Standard Laser', -- 'IMAX Laser', 'Dolby Atmos', 'Gold Class', 'Standard Laser', '2D Digital'
        Capacity INT NOT NULL DEFAULT 100,
        TotalRows INT NOT NULL DEFAULT 10,
        SeatsPerRow INT NOT NULL DEFAULT 12,
        Projector NVARCHAR(150) NULL,
        SoundSystem NVARCHAR(150) NULL,
        ScreenSize NVARCHAR(50) NULL,
        [Status] NVARCHAR(50) NOT NULL CONSTRAINT DF_Room_Status DEFAULT 'Ready', -- 'Ready', 'Screening', 'Cleaning', 'Maintenance'
        Description NVARCHAR(500) NULL,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Room_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_Room_RoomCode UNIQUE (RoomCode),
        CONSTRAINT UQ_Room_RoomName UNIQUE (RoomName),
        CONSTRAINT CK_Room_Capacity CHECK (Capacity > 0),
        CONSTRAINT CK_Room_TotalRows CHECK (TotalRows > 0),
        CONSTRAINT CK_Room_SeatsPerRow CHECK (SeatsPerRow > 0)
    );
END;
GO

-- 2. Add Foreign Key between Showtime and Room if not present
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'Showtime')
   AND NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_Showtime_Room')
BEGIN
    ALTER TABLE Showtime
    ADD CONSTRAINT FK_Showtime_Room FOREIGN KEY (RoomID) REFERENCES Room(RoomID) ON DELETE NO ACTION;
END;
GO

-- 3. Create Seat Table to maintain Room-Seat relationship (BR-10, BR-39, BR-53)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Seat')
BEGIN
    CREATE TABLE Seat (
        SeatID INT IDENTITY(1,1) PRIMARY KEY,
        RoomID INT NOT NULL,
        SeatTypeName NVARCHAR(50) NOT NULL DEFAULT 'Standard', -- 'Standard', 'VIP', 'Sweetbox'
        RowLabel NVARCHAR(5) NOT NULL,                         -- 'A', 'B', 'C', ...
        SeatNumber INT NOT NULL,                               -- 1, 2, 3, ...
        GridRow INT NOT NULL,                                  -- Row index (1-based)
        GridColumn INT NOT NULL,                               -- Column index (1-based)
        [Status] NVARCHAR(20) NOT NULL CONSTRAINT DF_Seat_Status DEFAULT 'Active', -- 'Active', 'Maintenance', 'Unavailable'
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Seat_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Seat_Room FOREIGN KEY (RoomID) REFERENCES Room(RoomID) ON DELETE CASCADE,
        CONSTRAINT UQ_Seat_Room_Position UNIQUE (RoomID, RowLabel, SeatNumber)
    );
END;
GO

-- 4. Seed Standard Cinema Rooms
IF NOT EXISTS (SELECT 1 FROM Room WHERE RoomCode = 'ROOM-01')
BEGIN
    INSERT INTO Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, Projector, SoundSystem, ScreenSize, [Status], Description)
    VALUES ('ROOM-01', N'Phòng Chiếu 01 - Standard Laser', 'Standard Laser', 96, 8, 12, N'Barco SP4K-15 Laser', N'Dolby Surround 7.1', N'15m x 8m', 'Ready', N'Phòng chiếu tiêu chuẩn Laser độ phân giải 4K sắc nét.');
END;

IF NOT EXISTS (SELECT 1 FROM Room WHERE RoomCode = 'ROOM-02')
BEGIN
    INSERT INTO Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, Projector, SoundSystem, ScreenSize, [Status], Description)
    VALUES ('ROOM-02', N'Phòng Chiếu 02 - Dolby Atmos Hall', 'Dolby Atmos', 120, 10, 12, N'Christie CP4435-RGB Dual Laser', N'Dolby Atmos 12.1 vòm 360 độ', N'18m x 10m', 'Screening', N'Phòng chiếu âm thanh đa chiều Dolby Atmos cao cấp.');
END;

IF NOT EXISTS (SELECT 1 FROM Room WHERE RoomCode = 'ROOM-IMAX-01')
BEGIN
    INSERT INTO Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, Projector, SoundSystem, ScreenSize, [Status], Description)
    VALUES ('ROOM-IMAX-01', N'Phòng Chiếu IMAX Laser 01', 'IMAX Laser', 180, 12, 15, N'IMAX Commercial Laser 4K Dual', N'IMAX 12.0 Channel Immersive Sound', N'24m x 14m', 'Ready', N'Màn hình cực đại IMAX chuẩn quốc tế cho trải nghiệm chân thực.');
END;

IF NOT EXISTS (SELECT 1 FROM Room WHERE RoomCode = 'ROOM-GOLD-01')
BEGIN
    INSERT INTO Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, Projector, SoundSystem, ScreenSize, [Status], Description)
    VALUES ('ROOM-GOLD-01', N'Phòng Chiếu VIP Gold Class', 'Gold Class', 40, 5, 8, N'Sony SRX-R515P 4K D-Cinema', N'JBL Premium Theater Sound System', N'14m x 7m', 'Cleaning', N'Ghế sofa da chỉnh điện đơn và đôi, dịch vụ ẩm thực tại chỗ.');
END;

IF NOT EXISTS (SELECT 1 FROM Room WHERE RoomCode = 'ROOM-03')
BEGIN
    INSERT INTO Room (RoomCode, RoomName, RoomType, Capacity, TotalRows, SeatsPerRow, Projector, SoundSystem, ScreenSize, [Status], Description)
    VALUES ('ROOM-03', N'Phòng Chiếu 03 - Laser Compact', 'Standard Laser', 80, 8, 10, N'Barco SP2K-11 Laser', N'Dolby 7.1 Studio Audio', N'14m x 7m', 'Maintenance', N'Đang bảo trì định kỳ hệ thống quạt thông gió và máy chiếu.');
END;
GO

-- 5. Seed sample seats for each Room to establish Room-Seat relationship
DECLARE @r1 INT = (SELECT RoomID FROM Room WHERE RoomCode = 'ROOM-01');
IF @r1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Seat WHERE RoomID = @r1)
BEGIN
    -- Insert sample seats for Room 01 (e.g. 96 seats across rows A-H, 12 seats each)
    DECLARE @row INT = 1;
    WHILE @row <= 8
    BEGIN
        DECLARE @rLabel NVARCHAR(1) = CHAR(64 + @row);
        DECLARE @col INT = 1;
        WHILE @col <= 12
        BEGIN
            DECLARE @stType NVARCHAR(50) = CASE WHEN @row >= 6 THEN 'VIP' ELSE 'Standard' END;
            INSERT INTO Seat (RoomID, SeatTypeName, RowLabel, SeatNumber, GridRow, GridColumn, [Status])
            VALUES (@r1, @stType, @rLabel, @col, @row, @col, 'Active');
            SET @col = @col + 1;
        END;
        SET @row = @row + 1;
    END;
END;
GO

-- 6. Seed a sample scheduled showtime in ROOM-02 for Movie 1 (Avatar) to establish Room-Showtime relationship
-- and support BR-52 (Cannot deactivate room with upcoming showtime)
DECLARE @r2 INT = (SELECT RoomID FROM Room WHERE RoomCode = 'ROOM-02');
DECLARE @m1 INT = (SELECT TOP 1 MovieID FROM Movie WHERE Status = 'Now Showing');
IF @r2 IS NOT NULL AND @m1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Showtime WHERE RoomID = @r2)
BEGIN
    INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, Status, CreatedAt)
    VALUES (
        @m1,
        @r2,
        DATEADD(hour, 2, SYSUTCDATETIME()),
        DATEADD(hour, 5, SYSUTCDATETIME()),
        95000.00,
        'Scheduled',
        SYSUTCDATETIME()
    );
END;
GO
