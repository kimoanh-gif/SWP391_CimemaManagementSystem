/* Iteration 3 - Manager News Management (#63).
   Adds an audit log for news review actions and a few sample articles in every
   review status so the Manager screen can be tested. Idempotent; safe to rerun.
   Run after 00_base_schema.sql and 99_test_accounts.sql. */
USE CinemaManagementDB;
GO
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID('dbo.NewsReviewLog', 'U') IS NULL
CREATE TABLE dbo.NewsReviewLog (
    NewsReviewLogID bigint IDENTITY(1,1) NOT NULL CONSTRAINT PK_NewsReviewLog PRIMARY KEY,
    NewsID int NOT NULL,
    AccountID int NOT NULL,
    [Action] varchar(30) NOT NULL,          -- APPROVE, PUBLISH, REJECT, UNPUBLISH, REOPEN
    FromStatus varchar(30) NOT NULL,
    ToStatus varchar(30) NOT NULL,
    Note nvarchar(500) NULL,
    CreatedAt datetime2 NOT NULL CONSTRAINT DF_NewsReviewLog_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_NewsReviewLog_News FOREIGN KEY (NewsID) REFERENCES dbo.News(NewsID),
    CONSTRAINT FK_NewsReviewLog_Account FOREIGN KEY (AccountID) REFERENCES dbo.Account(AccountID)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.NewsReviewLog') AND [name] = 'IX_NewsReviewLog_CreatedAt')
    CREATE INDEX IX_NewsReviewLog_CreatedAt ON dbo.NewsReviewLog(CreatedAt DESC);

IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE object_id = OBJECT_ID('dbo.News') AND [name] = 'IX_News_Status_CreatedAt')
    CREATE INDEX IX_News_Status_CreatedAt ON dbo.News([Status], CreatedAt DESC);
GO

/* ------------------------------------------------------------------
   Sample articles for the review workflow (needs at least one Staff row).
   Status values used by the Manager screen:
   DRAFT -> PENDING -> APPROVED -> PUBLISHED, PENDING -> REJECTED
   ------------------------------------------------------------------ */
DECLARE @StaffId INT = (SELECT TOP 1 StaffID FROM dbo.Staff ORDER BY StaffID);

IF @StaffId IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'Autumn Film Festival — Free Sweet Popcorn with VIP Suites Tickets')
        INSERT INTO dbo.News (CreatedByStaffID, Title, Content, ThumbnailURL, [Status], CreatedAt, SubmittedAt)
        VALUES (@StaffId,
                N'Autumn Film Festival — Free Sweet Popcorn with VIP Suites Tickets',
                N'Get 01 cup of honey butter popcorn with any showtime in the VIP Suites rooms.

Applied period: from the 15th of this month until the first 2,000 gifts run out.
Show your e-ticket at the F&B bar 15 minutes before the showtime to receive the gift.
The program cannot be combined with partner discount vouchers.',
                'https://images.unsplash.com/photo-1585647347483-22b66260dfff?auto=format&fit=crop&w=800&q=80',
                'PENDING', DATEADD(HOUR, -5, SYSDATETIME()), DATEADD(HOUR, -4, SYSDATETIME()));

    IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'Scheduled Maintenance Notice for the Dolby Atmos Sound System')
        INSERT INTO dbo.News (CreatedByStaffID, Title, Content, ThumbnailURL, [Status], CreatedAt, SubmittedAt)
        VALUES (@StaffId,
                N'Scheduled Maintenance Notice for the Dolby Atmos Sound System',
                N'One screening room will be closed next Tuesday from 10:00 to 14:00 for sound system maintenance.
Showtimes in other rooms are not affected.',
                'https://images.unsplash.com/photo-1598899134739-24c46f58b8c0?auto=format&fit=crop&w=800&q=80',
                'PENDING', DATEADD(HOUR, -9, SYSDATETIME()), DATEADD(HOUR, -8, SYSDATETIME()));

    IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'Movie Review Creative Contest — Win Free Movie Tickets for 1 Year')
        INSERT INTO dbo.News (CreatedByStaffID, Title, Content, ThumbnailURL, [Status], CreatedAt, SubmittedAt, ReviewedAt, RejectionReason)
        VALUES (@StaffId,
                N'Movie Review Creative Contest — Win Free Movie Tickets for 1 Year',
                N'Write a review of any movie you watched at our cinema this month and win free tickets for a whole year.',
                'https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=800&q=80',
                'REJECTED', DATEADD(DAY, -3, SYSDATETIME()), DATEADD(DAY, -3, SYSDATETIME()), DATEADD(DAY, -2, SYSDATETIME()),
                N'Missing detailed terms and conditions.');

    IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'Launch of the First VIP Motorized Recliner Seat Class')
        INSERT INTO dbo.News (CreatedByStaffID, Title, Content, ThumbnailURL, [Status], CreatedAt, SubmittedAt, ReviewedAt)
        VALUES (@StaffId,
                N'Launch of the First VIP Motorized Recliner Seat Class',
                N'Our new VIP seats recline 160 degrees and come with wireless charging and a personal swivel table.',
                'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=800&q=80',
                'APPROVED', DATEADD(DAY, -4, SYSDATETIME()), DATEADD(DAY, -4, SYSDATETIME()), DATEADD(DAY, -4, SYSDATETIME()));

    IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'Halloween Cheese Popcorn Combo (draft)')
        INSERT INTO dbo.News (CreatedByStaffID, Title, Content, [Status], CreatedAt)
        VALUES (@StaffId,
                N'Halloween Cheese Popcorn Combo (draft)',
                N'Draft content - not submitted for review yet.',
                'DRAFT', DATEADD(DAY, -1, SYSDATETIME()));
END
GO
