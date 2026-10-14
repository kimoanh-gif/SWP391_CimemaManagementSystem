USE CinemaManagementDB;
GO

/* Optional development data for Iteration 2 screens.
   The script is idempotent and does not delete existing data. */

UPDATE dbo.Movie
SET Title = N'Light Speedway', [Language] = N'English',
    [Description] = N'A young driver begins a journey to push beyond personal limits.'
WHERE Title = N'Đường Đua Ánh Sáng';

UPDATE dbo.Movie
SET Title = N'Blue Planet', [Language] = N'English',
    [Description] = N'A family adventure exploring a wonderful world of nature.'
WHERE Title = N'Hành Tinh Xanh';

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Light Speedway')
BEGIN
    INSERT INTO dbo.Movie
        (Title, Duration, ReleaseDate, [Language], Director, AgeRating,
         [Description], [Status], PosterURL)
    VALUES
        (N'Light Speedway', 118, CAST(GETDATE() AS DATE), N'English',
         N'Nguyễn Minh', 'T13', N'A young driver begins a journey to push beyond personal limits.',
         'NOW_SHOWING', NULL);
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Blue Planet')
BEGIN
    INSERT INTO dbo.Movie
        (Title, Duration, ReleaseDate, [Language], Director, AgeRating,
         [Description], [Status], PosterURL)
    VALUES
        (N'Blue Planet', 105, CAST(GETDATE() AS DATE), N'English',
         N'Alex Morgan', 'P', N'A family adventure exploring a wonderful world of nature.',
         'NOW_SHOWING', NULL);
END;

DECLARE @ActionGenreID INT = (SELECT GenreID FROM dbo.Genre WHERE [Name] = 'Action');
DECLARE @AdventureGenreID INT = (SELECT GenreID FROM dbo.Genre WHERE [Name] = 'Adventure');
DECLARE @MovieOneID INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Light Speedway');
DECLARE @MovieTwoID INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Blue Planet');

IF @ActionGenreID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.MovieGenre WHERE MovieID = @MovieOneID AND GenreID = @ActionGenreID)
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES (@MovieOneID, @ActionGenreID);

IF @AdventureGenreID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.MovieGenre WHERE MovieID = @MovieTwoID AND GenreID = @AdventureGenreID)
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES (@MovieTwoID, @AdventureGenreID);

IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE RoomName = N'Cinema 01')
    INSERT INTO dbo.Room (RoomName, RoomType, Capacity, [Status])
    VALUES (N'Cinema 01', '2D', 40, 'ACTIVE');

DECLARE @RoomID INT = (SELECT RoomID FROM dbo.Room WHERE RoomName = N'Cinema 01');
DECLARE @StandardTypeID INT = (SELECT SeatTypeID FROM dbo.SeatType WHERE TypeName = 'STANDARD');
DECLARE @VipTypeID INT = (SELECT SeatTypeID FROM dbo.SeatType WHERE TypeName = 'VIP');
DECLARE @CoupleTypeID INT = (SELECT SeatTypeID FROM dbo.SeatType WHERE TypeName = 'COUPLE');

IF @RoomID IS NOT NULL
   AND @StandardTypeID IS NOT NULL
   AND @VipTypeID IS NOT NULL
   AND @CoupleTypeID IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM dbo.Seat WHERE RoomID = @RoomID)
BEGIN
    WITH SeatRows AS (
        SELECT * FROM (VALUES
            ('A', 1, @StandardTypeID),
            ('B', 2, @StandardTypeID),
            ('C', 3, @VipTypeID),
            ('D', 4, @VipTypeID),
            ('E', 5, @CoupleTypeID)
        ) value(RowLabel, GridRow, SeatTypeID)
    ),
    SeatNumbers AS (
        SELECT * FROM (VALUES (1), (2), (3), (4), (5), (6), (7), (8)) value(SeatNumber)
    )
    INSERT INTO dbo.Seat
        (RoomID, SeatTypeID, RowLabel, SeatNumber, GridRow, GridColumn, [Status])
    SELECT @RoomID, SeatRows.SeatTypeID, SeatRows.RowLabel, SeatNumbers.SeatNumber,
           SeatRows.GridRow, SeatNumbers.SeatNumber, 'ACTIVE'
    FROM SeatRows CROSS JOIN SeatNumbers;
END;

DECLARE @Tomorrow DATE = DATEADD(DAY, 1, CAST(GETDATE() AS DATE));
DECLARE @DayAfterTomorrow DATE = DATEADD(DAY, 2, CAST(GETDATE() AS DATE));
DECLARE @StartOne DATETIME2 = DATEADD(MINUTE, 10 * 60, CAST(@Tomorrow AS DATETIME2));
DECLARE @StartTwo DATETIME2 = DATEADD(MINUTE, 15 * 60, CAST(@Tomorrow AS DATETIME2));
DECLARE @StartThree DATETIME2 = DATEADD(MINUTE, 19 * 60, CAST(@Tomorrow AS DATETIME2));
DECLARE @StartFour DATETIME2 = DATEADD(MINUTE, 18 * 60 + 30, CAST(@DayAfterTomorrow AS DATETIME2));

IF NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @MovieOneID AND RoomID = @RoomID AND StartTime = @StartOne)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@MovieOneID, @RoomID, @StartOne, DATEADD(MINUTE, 118, @StartOne), 85000, 'SCHEDULED');

IF NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @MovieTwoID AND RoomID = @RoomID AND StartTime = @StartTwo)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@MovieTwoID, @RoomID, @StartTwo, DATEADD(MINUTE, 105, @StartTwo), 80000, 'SCHEDULED');

IF NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @MovieOneID AND RoomID = @RoomID AND StartTime = @StartThree)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@MovieOneID, @RoomID, @StartThree, DATEADD(MINUTE, 118, @StartThree), 95000, 'SCHEDULED');

IF NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @MovieTwoID AND RoomID = @RoomID AND StartTime = @StartFour)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@MovieTwoID, @RoomID, @StartFour, DATEADD(MINUTE, 105, @StartFour), 90000, 'SCHEDULED');

UPDATE dbo.FoodBeverageItem
SET [Name] = N'Large Buttered Popcorn',
    [Description] = N'A large portion of buttered popcorn for two.'
WHERE [Name] = N'Bắp rang bơ lớn';

UPDATE dbo.FoodBeverageItem
SET [Name] = N'Large Soft Drink',
    [Description] = N'Choose from Coca-Cola, Sprite, or Fanta.'
WHERE [Name] = N'Nước ngọt lớn';

UPDATE dbo.FoodBeverageItem
SET [Description] = N'A medium popcorn and a medium soft drink.'
WHERE [Name] = N'Combo Solo';

UPDATE dbo.FoodBeverageItem
SET [Description] = N'A large popcorn and two medium soft drinks.'
WHERE [Name] = N'Combo Couple';

IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE [Name] = N'Large Buttered Popcorn')
    INSERT INTO dbo.FoodBeverageItem ([Name], [Description], ItemType, Price, ImageURL, [Status])
    VALUES (N'Large Buttered Popcorn', N'A large portion of buttered popcorn for two.', 'FOOD', 65000, NULL, 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE [Name] = N'Large Soft Drink')
    INSERT INTO dbo.FoodBeverageItem ([Name], [Description], ItemType, Price, ImageURL, [Status])
    VALUES (N'Large Soft Drink', N'Choose from Coca-Cola, Sprite, or Fanta.', 'DRINK', 45000, NULL, 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE [Name] = N'Combo Solo')
    INSERT INTO dbo.FoodBeverageItem ([Name], [Description], ItemType, Price, ImageURL, [Status])
    VALUES (N'Combo Solo', N'A medium popcorn and a medium soft drink.', 'COMBO', 89000, NULL, 'ACTIVE');

IF NOT EXISTS (SELECT 1 FROM dbo.FoodBeverageItem WHERE [Name] = N'Combo Couple')
    INSERT INTO dbo.FoodBeverageItem ([Name], [Description], ItemType, Price, ImageURL, [Status])
    VALUES (N'Combo Couple', N'A large popcorn and two medium soft drinks.', 'COMBO', 139000, NULL, 'ACTIVE');
GO
