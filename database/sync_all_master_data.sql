-- =====================================================================
-- Cinema Management System - SWP391
-- Full Data Synchronization Script Across All Manager Screens
-- Target DBMS: Microsoft SQL Server (localhost\SQLEXPRESS - CinemaManagementDB)
-- =====================================================================

USE CinemaManagementDB;
GO

-- 1. CLEAN UP AND STANDARDIZE GENRES
-- Remap any MovieGenre references from duplicate English genres to canonical Vietnamese IDs
UPDATE MovieGenre SET GenreID = 1 WHERE GenreID = 24; -- action -> Hành Động
UPDATE MovieGenre SET GenreID = 2 WHERE GenreID = 17; -- Adventure -> Phiêu Lưu
UPDATE MovieGenre SET GenreID = 3 WHERE GenreID = 18; -- Animation -> Hoạt Hình
UPDATE MovieGenre SET GenreID = 4 WHERE GenreID = 19; -- Comedy -> Hài Hước
UPDATE MovieGenre SET GenreID = 5 WHERE GenreID = 23; -- Drama -> Tâm Lý Xã Hội
UPDATE MovieGenre SET GenreID = 6 WHERE GenreID = 21; -- Horror -> Kinh Dị
UPDATE MovieGenre SET GenreID = 7 WHERE GenreID = 22; -- Romance -> Lãng Mạn
UPDATE MovieGenre SET GenreID = 8 WHERE GenreID = 20; -- Sci-Fi -> Khoa Học Viễn Tưởng

-- Delete duplicate English genre records
DELETE FROM Genre WHERE GenreID IN (17, 18, 19, 20, 21, 22, 23, 24);

-- Update all standard genres with proper UTF-8 Vietnamese names, descriptions and ACTIVE status
UPDATE Genre SET Name = N'Hành Động', Description = N'Hành động kịch tính, rượt đuổi và chiến đấu mãn nhãn', Status = 'ACTIVE' WHERE GenreID = 1;
UPDATE Genre SET Name = N'Phiêu Lưu', Description = N'Phiêu lưu mạo hiểm, khám phá các vùng đất mới', Status = 'ACTIVE' WHERE GenreID = 2;
UPDATE Genre SET Name = N'Hoạt Hình', Description = N'Hoạt hình đồ họa 3D/2D dành cho mọi lứa tuổi', Status = 'ACTIVE' WHERE GenreID = 3;
UPDATE Genre SET Name = N'Hài Hước', Description = N'Hài hước, dí dỏm mang lại tiếng cười sảng khoái', Status = 'ACTIVE' WHERE GenreID = 4;
UPDATE Genre SET Name = N'Tâm Lý Xã Hội', Description = N'Tâm lý xã hội sâu sắc, câu chuyện cảm động', Status = 'ACTIVE' WHERE GenreID = 5;
UPDATE Genre SET Name = N'Kinh Dị', Description = N'Kinh dị, rùng rợn, giật gân kích thích nỗi sợ', Status = 'ACTIVE' WHERE GenreID = 6;
UPDATE Genre SET Name = N'Lãng Mạn', Description = N'Tình cảm lãng mạn, câu chuyện tình yêu ngọt ngào', Status = 'ACTIVE' WHERE GenreID = 7;
UPDATE Genre SET Name = N'Khoa Học Viễn Tưởng', Description = N'Khoa học viễn tưởng, công nghệ tương lai và vũ trụ', Status = 'ACTIVE' WHERE GenreID = 8;
UPDATE Genre SET Name = N'Hồi Hộp Gay Cấn', Description = N'Hồi hộp, bí ẩn và căng thẳng nghẹt thở', Status = 'ACTIVE' WHERE GenreID = 9;
UPDATE Genre SET Name = N'Tài Liệu', Description = N'Tài liệu thực tế, khám phá tự nhiên và nhân vật lịch sử', Status = 'ACTIVE' WHERE GenreID = 10;
UPDATE Genre SET Name = N'Kỳ Ảo', Description = N'Kỳ ảo, phép thuật và thế giới huyền bí', Status = 'ACTIVE' WHERE GenreID = 11;
UPDATE Genre SET Name = N'Kinh Điển', Description = N'Phim kinh điển đã ngừng chiếu thương mại', Status = 'ACTIVE' WHERE GenreID = 12;
UPDATE Genre SET Name = N'Ca Nhạc', Description = N'Phim ca nhạc và biểu diễn nghệ thuật', Status = 'ACTIVE' WHERE GenreID = 13;
GO

-- 2. STANDARDIZE MOVIES
-- Update Movie 1: Avatar: The Way of Water (Now Showing)
UPDATE Movie 
SET Title = N'Avatar: The Way of Water',
    OriginalTitle = N'Avatar: The Way of Water',
    Duration = 192,
    ReleaseDate = '2026-10-01',
    EndDate = '2026-11-30',
    [Language] = N'Tiếng Anh - Phụ đề Tiếng Việt',
    Director = N'James Cameron',
    Actors = N'Sam Worthington, Zoe Saldana, Sigourney Weaver, Stephen Lang',
    AgeRating = 'T13',
    [Description] = N'Jake Sully sống cùng gia đình mới trên hành tinh Pandora. Khi một mối đe dọa quen thuộc quay trở lại, Jake phải hợp tác với Neytiri và quân đội Na''vi để bảo vệ quê hương.',
    Poster = 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80',
    Trailer = 'https://www.youtube.com/watch?v=d9MyW72ELq0',
    [Status] = 'Now Showing',
    UpdatedAt = SYSUTCDATETIME()
WHERE MovieID = 1;

-- Update Movie 2: Oppenheimer (Now Showing)
UPDATE Movie 
SET Title = N'Oppenheimer',
    OriginalTitle = N'Oppenheimer',
    Duration = 180,
    ReleaseDate = '2026-10-05',
    EndDate = '2026-11-20',
    [Language] = N'Tiếng Anh - Phụ đề Tiếng Việt',
    Director = N'Christopher Nolan',
    Actors = N'Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.',
    AgeRating = 'T18',
    [Description] = N'Câu chuyện về nhà vật lý lý thuyết người Mỹ J. Robert Oppenheimer, người đứng đầu Dự án Manhattan phát triển vũ khí hạt nhân đầu tiên trong Thế chiến thứ hai.',
    Poster = 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?auto=format&fit=crop&w=600&q=80',
    Trailer = 'https://www.youtube.com/watch?v=uYPbbksJxIg',
    [Status] = 'Now Showing',
    UpdatedAt = SYSUTCDATETIME()
WHERE MovieID = 2;

-- Update Movie 3: Dune: Part Two (Now Showing)
UPDATE Movie 
SET Title = N'Dune: Part Two',
    OriginalTitle = N'Dune: Part Two',
    Duration = 166,
    ReleaseDate = '2026-10-10',
    EndDate = '2026-12-15',
    [Language] = N'Tiếng Anh - Phụ đề Tiếng Việt',
    Director = N'Denis Villeneuve',
    Actors = N'Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem',
    AgeRating = 'T16',
    [Description] = N'Paul Atreides hợp lực cùng Chani và người Fremen để trả thù những kẻ đã hủy hoại gia đình mình, đồng thời nỗ lực ngăn chặn viễn cảnh đen tối về tương lai vũ trụ.',
    Poster = 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80',
    Trailer = 'https://www.youtube.com/watch?v=Way9Dexny3w',
    [Status] = 'Now Showing',
    UpdatedAt = SYSUTCDATETIME()
WHERE MovieID = 3;

-- Update Movie 4: The Conjuring: Last Rites (Upcoming)
UPDATE Movie 
SET Title = N'The Conjuring: Last Rites',
    OriginalTitle = N'The Conjuring: Last Rites',
    Duration = 115,
    ReleaseDate = '2026-11-15',
    EndDate = '2026-12-31',
    [Language] = N'Tiếng Anh - Phụ đề Tiếng Việt',
    Director = N'Michael Chaves',
    Actors = N'Patrick Wilson, Vera Farmiga',
    AgeRating = 'T18',
    [Description] = N'Hai nhà điều tra hiện tượng siêu nhiên Ed và Lorraine Warren tiếp tục đối mặt với một vụ án trừ tà kinh hoàng và nguy hiểm nhất trong sự nghiệp của họ.',
    Poster = 'https://images.unsplash.com/photo-1509248961158-e54f6934749c?auto=format&fit=crop&w=600&q=80',
    Trailer = 'https://www.youtube.com/watch?v=h9Q4zZS2v1k',
    [Status] = 'Upcoming',
    UpdatedAt = SYSUTCDATETIME()
WHERE MovieID = 4;

-- Update Movie 5: Godzilla x Kong: The New Empire (3D) (Now Showing)
UPDATE Movie 
SET Title = N'Godzilla x Kong: The New Empire (3D)',
    OriginalTitle = N'Godzilla x Kong: The New Empire',
    Duration = 120,
    ReleaseDate = '2026-10-12',
    EndDate = '2026-11-25',
    [Language] = N'Tiếng Anh - Phụ đề Tiếng Việt',
    Director = N'Adam Wingard',
    Actors = N'Rebecca Hall, Brian Tyree Henry, Dan Stevens',
    AgeRating = 'T13',
    [Description] = N'Hai sinh vật khổng lồ Godzilla và Kong phải gạt bỏ mối thù truyền kiếp để hợp sức chống lại một mối đe dọa tiềm ẩn khổng lồ đe dọa sự tồn vong của loài người.',
    Poster = 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=600&q=80',
    Trailer = 'https://www.youtube.com/watch?v=qqrpMRDuPfc',
    [Status] = 'Now Showing',
    UpdatedAt = SYSUTCDATETIME()
WHERE MovieID = 5;

-- Update Movie 6: Kung Fu Panda 4 (Upcoming)
UPDATE Movie 
SET Title = N'Kung Fu Panda 4',
    OriginalTitle = N'Kung Fu Panda 4',
    Duration = 94,
    ReleaseDate = '2026-11-01',
    EndDate = '2026-12-20',
    [Language] = N'Tiếng Anh - Lồng tiếng Tiếng Việt',
    Director = N'Mike Mitchell',
    Actors = N'Jack Black, Awkwafina, Viola Davis, Dustin Hoffman',
    AgeRating = 'P',
    [Description] = N'Po chuẩn bị trở thành Thủ lĩnh tinh thần của Thung lũng Bình Yên và cần tìm kiếm, huấn luyện một Chiến binh Rồng mới trước khi đối mặt kẻ thù Tắc Kè Hoa.',
    Poster = 'https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=600&q=80',
    Trailer = 'https://www.youtube.com/watch?v=_inKs4eeHiI',
    [Status] = 'Upcoming',
    UpdatedAt = SYSUTCDATETIME()
WHERE MovieID = 6;

-- Insert Movie 7: Mai (Ended) if not exists
IF NOT EXISTS (SELECT 1 FROM Movie WHERE Title = N'Mai' OR MovieID = 7)
BEGIN
    SET IDENTITY_INSERT Movie ON;
    INSERT INTO Movie (MovieID, Title, OriginalTitle, Duration, ReleaseDate, EndDate, [Language], Director, Actors, AgeRating, [Description], Poster, Trailer, [Status])
    VALUES (7, N'Mai', N'Mai', 131, '2026-02-10', '2026-04-10', N'Tiếng Việt', N'Trấn Thành', N'Phương Anh Đào, Tuấn Trần, Trấn Thành, Hồng Đào', 'T18',
            N'Câu chuyện cuộc đời của Mai, một người phụ nữ làm nghề massage trị liệu và mối tình nhiều sóng gió với chàng nhạc công lãng tử Dương.',
            'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&q=80',
            'https://www.youtube.com/watch?v=0X2l_oO7Z8k',
            'Ended');
    SET IDENTITY_INSERT Movie OFF;
END
ELSE
BEGIN
    UPDATE Movie 
    SET Title = N'Mai',
        OriginalTitle = N'Mai',
        Duration = 131,
        ReleaseDate = '2026-02-10',
        EndDate = '2026-04-10',
        [Language] = N'Tiếng Việt',
        Director = N'Trấn Thành',
        Actors = N'Phương Anh Đào, Tuấn Trần, Trấn Thành, Hồng Đào',
        AgeRating = 'T18',
        [Description] = N'Câu chuyện cuộc đời của Mai, một người phụ nữ làm nghề massage trị liệu và mối tình nhiều sóng gió với chàng nhạc công lãng tử Dương.',
        Poster = 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&q=80',
        Trailer = 'https://www.youtube.com/watch?v=0X2l_oO7Z8k',
        [Status] = 'Ended',
        UpdatedAt = SYSUTCDATETIME()
    WHERE MovieID = 7;
END;
GO

-- 3. SYNCHRONIZE MOVIE-GENRE MAPPINGS
DELETE FROM MovieGenre;

-- Movie 1: Avatar -> Hành Động (1), Phiêu Lưu (2), Khoa Học Viễn Tưởng (8)
INSERT INTO MovieGenre (MovieID, GenreID) VALUES (1, 1), (1, 2), (1, 8);

-- Movie 2: Oppenheimer -> Tâm Lý Xã Hội (5), Hồi Hộp Gay Cấn (9)
INSERT INTO MovieGenre (MovieID, GenreID) VALUES (2, 5), (2, 9);

-- Movie 3: Dune: Part Two -> Hành Động (1), Phiêu Lưu (2), Khoa Học Viễn Tưởng (8)
INSERT INTO MovieGenre (MovieID, GenreID) VALUES (3, 1), (3, 2), (3, 8);

-- Movie 4: The Conjuring: Last Rites -> Kinh Dị (6), Hồi Hộp Gay Cấn (9)
INSERT INTO MovieGenre (MovieID, GenreID) VALUES (4, 6), (4, 9);

-- Movie 5: Godzilla x Kong -> Hành Động (1), Phiêu Lưu (2), Khoa Học Viễn Tưởng (8)
INSERT INTO MovieGenre (MovieID, GenreID) VALUES (5, 1), (5, 2), (5, 8);

-- Movie 6: Kung Fu Panda 4 -> Hoạt Hình (3), Hài Hước (4), Phiêu Lưu (2)
INSERT INTO MovieGenre (MovieID, GenreID) VALUES (6, 3), (6, 4), (6, 2);

-- Movie 7: Mai -> Tâm Lý Xã Hội (5), Lãng Mạn (7)
INSERT INTO MovieGenre (MovieID, GenreID) VALUES (7, 5), (7, 7);
GO

-- 4. SYNCHRONIZE SHOWTIMES FOR TODAY AND UPCOMING DAYS
-- Reset existing showtimes and seed active showtimes
DELETE FROM Showtime;

-- Today's date calculations
DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @Tomorrow DATE = DATEADD(DAY, 1, @Today);
DECLARE @DayAfter DATE = DATEADD(DAY, 2, @Today);

-- Today Showtimes:
-- 1. Avatar (Movie 1) in Room 1 (Standard) at 09:30
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status])
VALUES (1, 1, 
        DATEADD(MINUTE, 9 * 60 + 30, CAST(@Today AS DATETIME2)), 
        DATEADD(MINUTE, 12 * 60 + 42, CAST(@Today AS DATETIME2)), 
        85000, 'Scheduled');

-- 2. Oppenheimer (Movie 2) in Room 2 (Dolby Atmos) at 13:00
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status])
VALUES (2, 2, 
        DATEADD(MINUTE, 13 * 60, CAST(@Today AS DATETIME2)), 
        DATEADD(MINUTE, 16 * 60, CAST(@Today AS DATETIME2)), 
        110000, 'Scheduled');

-- 3. Dune: Part Two (Movie 3) in Room 3 (IMAX Laser) at 16:30
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status])
VALUES (3, 3, 
        DATEADD(MINUTE, 16 * 60 + 30, CAST(@Today AS DATETIME2)), 
        DATEADD(MINUTE, 19 * 60 + 16, CAST(@Today AS DATETIME2)), 
        150000, 'Scheduled');

-- 4. Godzilla x Kong (Movie 5) in Room 1 (Standard) at 19:30
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status])
VALUES (5, 1, 
        DATEADD(MINUTE, 19 * 60 + 30, CAST(@Today AS DATETIME2)), 
        DATEADD(MINUTE, 21 * 60 + 30, CAST(@Today AS DATETIME2)), 
        95000, 'Scheduled');

-- 5. Avatar (Movie 1) in Room 4 (Gold Class VIP) at 20:00
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status])
VALUES (1, 4, 
        DATEADD(MINUTE, 20 * 60, CAST(@Today AS DATETIME2)), 
        DATEADD(MINUTE, 23 * 60 + 12, CAST(@Today AS DATETIME2)), 
        180000, 'Scheduled');

-- Tomorrow Showtimes:
-- 6. Dune: Part Two in Room 2 at 10:00
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status])
VALUES (3, 2, 
        DATEADD(MINUTE, 10 * 60, CAST(@Tomorrow AS DATETIME2)), 
        DATEADD(MINUTE, 12 * 60 + 46, CAST(@Tomorrow AS DATETIME2)), 
        110000, 'Scheduled');

-- 7. Oppenheimer in Room 3 at 14:00
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status])
VALUES (2, 3, 
        DATEADD(MINUTE, 14 * 60, CAST(@Tomorrow AS DATETIME2)), 
        DATEADD(MINUTE, 17 * 60, CAST(@Tomorrow AS DATETIME2)), 
        150000, 'Scheduled');

-- 8. Godzilla x Kong in Room 4 at 18:00
INSERT INTO Showtime (MovieID, RoomID, StartTime, EndTime, BaseTicketPrice, [Status])
VALUES (5, 4, 
        DATEADD(MINUTE, 18 * 60, CAST(@Tomorrow AS DATETIME2)), 
        DATEADD(MINUTE, 20 * 60, CAST(@Tomorrow AS DATETIME2)), 
        180000, 'Scheduled');
GO
