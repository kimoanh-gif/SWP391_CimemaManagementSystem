DECLARE @Movie1 INT = (SELECT TOP 1 MovieID FROM dbo.Movie WHERE [Status] = 'Now Showing');
DECLARE @Movie2 INT = (SELECT TOP 1 MovieID FROM dbo.Movie WHERE [Status] = 'Now Showing' AND MovieID <> @Movie1);
DECLARE @Room1 INT = (SELECT TOP 1 RoomID FROM dbo.Room WHERE [Status] = 'Ready');
DECLARE @Room2 INT = (SELECT TOP 1 RoomID FROM dbo.Room WHERE [Status] = 'Ready' AND RoomID <> @Room1);
DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @Tomorrow DATE = DATEADD(DAY, 1, @Today);
INSERT dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status]) VALUES
  (@Movie1, @Room1, DATEADD(HOUR, 14, CAST(@Today AS DATETIME2)), DATEADD(MINUTE, 130, DATEADD(HOUR, 14, CAST(@Today AS DATETIME2))), 85000, N'Scheduled'),
  (@Movie2, @Room2, DATEADD(HOUR, 16, CAST(@Today AS DATETIME2)), DATEADD(MINUTE, 150, DATEADD(HOUR, 16, CAST(@Today AS DATETIME2))), 95000, N'Scheduled'),
  (@Movie1, @Room2, DATEADD(HOUR, 19, CAST(@Today AS DATETIME2)), DATEADD(MINUTE, 130, DATEADD(HOUR, 19, CAST(@Today AS DATETIME2))), 110000, N'Scheduled'),
  (@Movie2, @Room1, DATEADD(HOUR, 10, CAST(@Tomorrow AS DATETIME2)), DATEADD(MINUTE, 150, DATEADD(HOUR, 10, CAST(@Tomorrow AS DATETIME2))), 85000, N'Scheduled');
SELECT s.ShowtimeID, m.Title, s.StartTime, s.BasePrice, s.Status FROM dbo.Showtime s JOIN dbo.Movie m ON s.MovieID = m.MovieID WHERE s.StartTime >= CAST(GETDATE() AS DATE);
