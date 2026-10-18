-- =====================================================================
-- Cinema Management System - SWP391
-- Screen 55: Genre Management (Iteration 2)
-- Database Migration Script for Genre and MovieGenre Tables
-- Target DBMS: Microsoft SQL Server 2019+
-- =====================================================================

-- 1. Create Movie Table if not already created (for referential integrity)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Movie')
BEGIN
    CREATE TABLE Movie (
        MovieID INT IDENTITY(1,1) PRIMARY KEY,
        Title NVARCHAR(250) NOT NULL,
        Duration INT NOT NULL DEFAULT 120,
        ReleaseDate DATE NULL,
        [Language] NVARCHAR(50) NULL,
        Director NVARCHAR(150) NULL,
        AgeRating NVARCHAR(10) NOT NULL DEFAULT 'P',
        [Description] NVARCHAR(1000) NULL,
        Poster NVARCHAR(500) NULL,
        Trailer NVARCHAR(500) NULL,
        [Status] NVARCHAR(50) NOT NULL DEFAULT 'Now Showing', -- 'Upcoming', 'Now Showing', 'Ended', 'Archived'
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );
END;
GO

-- 2. Create Genre Table
-- BR-Identity: Genre name must be unique (case-insensitive in SQL Server collation)
-- Lifecycle: Active / Inactive (managed administratively by Manager)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Genre')
BEGIN
    CREATE TABLE Genre (
        GenreID INT IDENTITY(1,1) PRIMARY KEY,
        [Name] NVARCHAR(100) NOT NULL,
        [Description] NVARCHAR(500) NULL,
        [Status] NVARCHAR(20) NOT NULL CONSTRAINT DF_Genre_Status DEFAULT 'ACTIVE', -- 'ACTIVE' or 'INACTIVE'
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Genre_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_Genre_Name UNIQUE ([Name])
    );
END;
GO

-- 3. Create MovieGenre Table (Many-to-Many relationship between Movie and Genre)
-- BR-44: MovieGenre uniqueness - The same Genre must not be assigned to the same Movie more than once.
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

-- 4. Seed initial standard cinema genres if table is empty
IF NOT EXISTS (SELECT 1 FROM Genre)
BEGIN
    INSERT INTO Genre ([Name], [Description], [Status]) VALUES
    (N'Action', N'Fast-paced, physical feats, fights, chases, and battles', 'ACTIVE'),
    (N'Adventure', N'Exciting undertakings, quests, journeys, and discoveries', 'ACTIVE'),
    (N'Animation', N'Animated storylines created via CGI or traditional animation', 'ACTIVE'),
    (N'Comedy', N'Humorous, lighthearted entertainment designed to amuse', 'ACTIVE'),
    (N'Drama', N'Narrative fiction focused on emotional themes and realistic character depth', 'ACTIVE'),
    (N'Horror', N'Suspenseful, frightening events aimed at evoking fear and thrill', 'ACTIVE'),
    (N'Romance', N'Romantic relationships, passion, and emotional bonds', 'ACTIVE'),
    (N'Sci-Fi', N'Science fiction dealing with futuristic technology, space, or science concepts', 'ACTIVE'),
    (N'Thriller', N'High suspense, tension, uncertainty, and excitement', 'ACTIVE'),
    (N'Documentary', N'Non-fictional motion-picture intended to document reality', 'ACTIVE'),
    (N'Fantasy', N'Magical, supernatural elements and fantastical worlds', 'ACTIVE'),
    (N'Mystery', N'Plot centered on solving a mysterious crime, puzzle, or event', 'ACTIVE'),
    (N'Musical', N'Songs and musical performances interwoven into the narrative', 'ACTIVE'),
    (N'Family', N'Wholesome entertainment suitable for all age brackets', 'ACTIVE');
END;
GO

-- 5. Seed sample movies and movie-genre mappings if empty (for testing)
IF NOT EXISTS (SELECT 1 FROM Movie)
BEGIN
    INSERT INTO Movie (Title, Duration, AgeRating, [Status]) VALUES
    (N'Avatar: The Way of Water', 192, 'T13', 'Now Showing'),
    (N'Oppenheimer', 180, 'T18', 'Now Showing'),
    (N'Spider-Man: Across the Spider-Verse', 140, 'P', 'Now Showing'),
    (N'Dune: Part Two', 166, 'T16', 'Upcoming'),
    (N'The Conjuring: Last Rites', 115, 'T18', 'Upcoming');

    -- Link sample MovieGenre
    -- Avatar -> Action (1), Adventure (2), Sci-Fi (8)
    INSERT INTO MovieGenre (MovieID, GenreID) VALUES 
    (1, 1), (1, 2), (1, 8),
    -- Oppenheimer -> Drama (5)
    (2, 5),
    -- Spider-Man -> Animation (3), Action (1), Adventure (2)
    (3, 3), (3, 1), (3, 2),
    -- Dune: Part Two -> Sci-Fi (8), Adventure (2)
    (4, 8), (4, 2),
    -- The Conjuring -> Horror (6), Mystery (12)
    (5, 6), (5, 12);
END;
GO
