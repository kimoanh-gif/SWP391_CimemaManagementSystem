USE CinemaManagementDB;
GO

IF COL_LENGTH('dbo.News', 'Summary') IS NULL
BEGIN
    ALTER TABLE dbo.News
        ADD Summary nvarchar(500) NOT NULL
            CONSTRAINT DF_News_Summary DEFAULT N'';
END;
GO
