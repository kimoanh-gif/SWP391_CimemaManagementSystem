-- =====================================================================
-- Cinema Management System - SWP391
-- Screen 54: Movie Management (Iteration 2)
-- Database Migration Script for Movie and MovieGenre Tables
-- Target DBMS: Microsoft SQL Server 2019+
-- =====================================================================

-- 1. Ensure Movie table exists with all SRS required fields
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Movie')
BEGIN
    CREATE TABLE Movie (
        MovieID INT IDENTITY(1,1) PRIMARY KEY,
        Title NVARCHAR(150) NOT NULL,
        OriginalTitle NVARCHAR(150) NULL,
        Duration INT NOT NULL DEFAULT 120,
        ReleaseDate DATE NULL,
        EndDate DATE NULL,
        [Language] NVARCHAR(50) NULL,
        Director NVARCHAR(100) NULL,
        Actors NVARCHAR(500) NULL,
        AgeRating NVARCHAR(10) NOT NULL CONSTRAINT DF_Movie_AgeRating DEFAULT 'P',
        [Description] NVARCHAR(1000) NULL,
        Poster NVARCHAR(500) NULL,
        Trailer NVARCHAR(500) NULL,
        [Status] NVARCHAR(50) NOT NULL CONSTRAINT DF_Movie_Status DEFAULT 'Upcoming', -- 'Upcoming', 'Now Showing', 'Ended', 'Archived'
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Movie_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL
    );
END
ELSE
BEGIN
    -- Alter existing table to add any missing columns safely
    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'OriginalTitle')
        ALTER TABLE Movie ADD OriginalTitle NVARCHAR(150) NULL;

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'EndDate')
        ALTER TABLE Movie ADD EndDate DATE NULL;

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'Language')
        ALTER TABLE Movie ADD [Language] NVARCHAR(50) NULL;

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'Director')
        ALTER TABLE Movie ADD Director NVARCHAR(100) NULL;

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'Actors')
        ALTER TABLE Movie ADD Actors NVARCHAR(500) NULL;

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'AgeRating')
        ALTER TABLE Movie ADD AgeRating NVARCHAR(10) NOT NULL CONSTRAINT DF_Movie_AgeRating DEFAULT 'P';

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'Description')
        ALTER TABLE Movie ADD [Description] NVARCHAR(1000) NULL;

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'Poster')
        ALTER TABLE Movie ADD Poster NVARCHAR(500) NULL;

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Movie' AND COLUMN_NAME = 'Trailer')
        ALTER TABLE Movie ADD Trailer NVARCHAR(500) NULL;

    IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'UpdatedAt')
        ALTER TABLE Movie ADD UpdatedAt DATETIME2 NULL;
END;
GO

-- 2. Ensure MovieGenre table exists with composite PK (BR-44 uniqueness)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'MovieGenre')
BEGIN
    CREATE TABLE MovieGenre (
        MovieID INT NOT NULL,
        GenreID INT NOT NULL,
        AssignedAt DATETIME2 NOT NULL CONSTRAINT DF_MovieGenre_AssignedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_MovieGenre PRIMARY KEY (MovieID, GenreID),
        CONSTRAINT FK_MovieGenre_Movie FOREIGN KEY (MovieID) REFERENCES Movie(MovieID) ON DELETE CASCADE,
        CONSTRAINT FK_MovieGenre_Genre FOREIGN KEY (GenreID) REFERENCES Genre(GenreID) ON DELETE NO ACTION
    );
END;
GO

-- 3. Ensure Showtime table exists for BR-07 and relationship integrity check
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Showtime')
BEGIN
    CREATE TABLE Showtime (
        ShowtimeID INT IDENTITY(1,1) PRIMARY KEY,
        MovieID INT NOT NULL,
        RoomID INT NULL,
        StartTime DATETIME2 NOT NULL,
        EndTime DATETIME2 NOT NULL,
        BaseTicketPrice DECIMAL(18,2) NOT NULL DEFAULT 80000,
        [Status] NVARCHAR(50) NOT NULL DEFAULT 'Scheduled', -- 'Scheduled', 'Completed', 'Cancelled'
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_Showtime_Movie FOREIGN KEY (MovieID) REFERENCES Movie(MovieID) ON DELETE NO ACTION
    );
END;
GO
