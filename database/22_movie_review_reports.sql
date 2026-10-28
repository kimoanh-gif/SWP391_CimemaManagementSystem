USE CinemaManagementDB;
GO
IF OBJECT_ID(N'dbo.MovieReviewReport', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.MovieReviewReport (
        ReportID INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_MovieReviewReport PRIMARY KEY,
        ReviewID INT NOT NULL,
        ReporterAccountID INT NOT NULL,
        Reason VARCHAR(40) NOT NULL,
        Details NVARCHAR(500) NULL,
        [Status] VARCHAR(20) NOT NULL CONSTRAINT DF_MovieReviewReport_Status DEFAULT 'PENDING',
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_MovieReviewReport_CreatedAt DEFAULT SYSDATETIME(),
        ResolvedAt DATETIME2 NULL,
        ResolvedByAccountID INT NULL,
        CONSTRAINT FK_MovieReviewReport_Review FOREIGN KEY (ReviewID) REFERENCES dbo.MovieReview(ReviewID),
        CONSTRAINT FK_MovieReviewReport_Reporter FOREIGN KEY (ReporterAccountID) REFERENCES dbo.Account(AccountID),
        CONSTRAINT UQ_MovieReviewReport_Review_Reporter UNIQUE (ReviewID, ReporterAccountID)
    );
END;
GO
