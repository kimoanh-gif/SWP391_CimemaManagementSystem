SET NOCOUNT ON;
SET XACT_ABORT ON;

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE UPPER(LTRIM(RTRIM([Status]))) = N'NOW SHOWING')
    THROW 50001, 'No Now Showing movies are available for showtime seeding.', 1;

IF NOT EXISTS (
    SELECT 1 FROM dbo.Room
    WHERE UPPER(LTRIM(RTRIM([Status]))) IN (N'READY', N'SCREENING', N'ACTIVE')
      AND (COL_LENGTH(N'dbo.Room', N'IsDeleted') IS NULL OR IsDeleted = 0)
)
    THROW 50002, 'No active rooms are available for showtime seeding.', 1;

DECLARE @SeedDate DATE, @MovieID INT, @Duration INT, @RoomID INT;
DECLARE @StartTime DATETIME2, @EndTime DATETIME2, @BaseStart DATETIME2;
DECLARE @DayOffset INT = 0;

BEGIN TRANSACTION;

WHILE @DayOffset <= 4
BEGIN
    SET @SeedDate = DATEADD(DAY, @DayOffset, CAST(GETDATE() AS DATE));
    -- Today's slots start at the next full hour so the public query can show them immediately.
    SET @BaseStart = CASE WHEN @DayOffset = 0
        THEN DATEADD(HOUR, DATEDIFF(HOUR, 0, SYSDATETIME()) + 1, 0)
        ELSE DATEADD(HOUR, 9, CAST(@SeedDate AS DATETIME2)) END;

    DECLARE movie_cursor CURSOR LOCAL FAST_FORWARD FOR
        SELECT MovieID, Duration
        FROM dbo.Movie
        WHERE UPPER(LTRIM(RTRIM([Status]))) = N'NOW SHOWING'
          AND NOT EXISTS (
              SELECT 1 FROM dbo.Showtime st
              WHERE st.MovieID = dbo.Movie.MovieID
                AND CAST(st.StartTime AS DATE) = @SeedDate
                AND UPPER(st.[Status]) IN (N'SCHEDULED', N'ACTIVE')
                AND (@DayOffset > 0 OR st.StartTime > SYSDATETIME())
          )
        ORDER BY MovieID;

    OPEN movie_cursor;
    FETCH NEXT FROM movie_cursor INTO @MovieID, @Duration;
    WHILE @@FETCH_STATUS = 0
    BEGIN
        -- Append to the room that is available first and preserve the 15-minute cleaning buffer.
        SELECT TOP (1) @RoomID = room.RoomID, @StartTime = availability.NextStart
        FROM dbo.Room room
        CROSS APPLY (
            SELECT CASE
                WHEN MAX(st.EndTime) IS NULL OR DATEADD(MINUTE, 15, MAX(st.EndTime)) < @BaseStart
                    THEN @BaseStart
                ELSE DATEADD(MINUTE, 15, MAX(st.EndTime))
            END AS NextStart
            FROM dbo.Showtime st
            WHERE st.RoomID = room.RoomID
              AND CAST(st.StartTime AS DATE) = @SeedDate
              AND UPPER(st.[Status]) NOT IN (N'CANCELLED', N'STOPPED')
        ) availability
        WHERE UPPER(LTRIM(RTRIM(room.[Status]))) IN (N'READY', N'SCREENING', N'ACTIVE')
          AND (COL_LENGTH(N'dbo.Room', N'IsDeleted') IS NULL OR room.IsDeleted = 0)
        ORDER BY availability.NextStart, room.RoomID;

        SET @EndTime = DATEADD(MINUTE, @Duration, @StartTime);
        INSERT dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
        VALUES (@MovieID, @RoomID, @StartTime, @EndTime, 95000, N'Scheduled');

        FETCH NEXT FROM movie_cursor INTO @MovieID, @Duration;
    END;
    CLOSE movie_cursor;
    DEALLOCATE movie_cursor;
    SET @DayOffset += 1;
END;

COMMIT TRANSACTION;

SELECT CAST(st.StartTime AS DATE) AS ShowDate, m.Title, r.RoomName,
       st.StartTime, st.EndTime, st.BasePrice, st.[Status]
FROM dbo.Showtime st
JOIN dbo.Movie m ON m.MovieID = st.MovieID
JOIN dbo.Room r ON r.RoomID = st.RoomID
WHERE st.StartTime >= CAST(GETDATE() AS DATE)
  AND st.StartTime < DATEADD(DAY, 5, CAST(GETDATE() AS DATE))
  AND UPPER(m.[Status]) = N'NOW SHOWING'
ORDER BY ShowDate, m.Title, st.StartTime;
