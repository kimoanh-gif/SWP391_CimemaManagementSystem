USE CinemaManagementDB;
GO

/* =====================================================================
   iter2-home-movie-promotions-data.sql
   Development seed data for Iteration 2:
   - Screen 21: Home Page
   - Screen 22: Movie List
   - Screen 23: Movie Details
   - Screen 24: Showtime List
   - Screen 25: Promotions & News

   Author: Nguyễn Đức Anh (Iter2)
   Script is idempotent and safe to re-run.
   ===================================================================== */

-- 1. Thể loại phim (Genres)
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = 'Action')
    INSERT INTO dbo.Genre ([Name]) VALUES ('Action');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = 'Adventure')
    INSERT INTO dbo.Genre ([Name]) VALUES ('Adventure');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = 'Animation')
    INSERT INTO dbo.Genre ([Name]) VALUES ('Animation');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = 'Comedy')
    INSERT INTO dbo.Genre ([Name]) VALUES ('Comedy');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = 'Sci-Fi')
    INSERT INTO dbo.Genre ([Name]) VALUES ('Sci-Fi');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = 'Horror')
    INSERT INTO dbo.Genre ([Name]) VALUES ('Horror');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = 'Romance')
    INSERT INTO dbo.Genre ([Name]) VALUES ('Romance');
IF NOT EXISTS (SELECT 1 FROM dbo.Genre WHERE [Name] = 'Drama')
    INSERT INTO dbo.Genre ([Name]) VALUES ('Drama');
GO

-- 2. Danh sách phim (Movies)
-- Phim 1: Light Speedway (Đang chiếu)
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Light Speedway')
BEGIN
    INSERT INTO dbo.Movie (Title, Duration, ReleaseDate, [Language], Director, AgeRating, [Description], [Status], PosterURL, TrailerURL)
    VALUES (N'Light Speedway', 118, CAST(GETDATE() AS DATE), N'Tiếng Anh - Phụ đề Tiếng Việt',
            N'Nguyễn Minh', 'T13',
            N'Theo chân một tay đua trẻ tuổi mang khát vọng vượt qua ranh giới tốc độ và đối đầu với những thử thách khắc nghiệt nhất trên đường đua ánh sáng tương lai.',
            'NOW_SHOWING', 'https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?auto=format&fit=crop&w=600&q=80',
            'https://www.youtube.com/watch?v=dQw4w9WgXcQ');
END;

-- Phim 2: Blue Planet (Đang chiếu)
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Blue Planet')
BEGIN
    INSERT INTO dbo.Movie (Title, Duration, ReleaseDate, [Language], Director, AgeRating, [Description], [Status], PosterURL, TrailerURL)
    VALUES (N'Blue Planet', 105, CAST(GETDATE() AS DATE), N'Tiếng Anh - Lồng tiếng Tiếng Việt',
            N'Alex Morgan', 'P',
            N'Hành trình phiêu lưu kỳ thú của một gia đình nhỏ khám phá những vùng biển sâu thẳm và kỳ quan đại dương đầy màu sắc của Trái Đất.',
            'NOW_SHOWING', 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80',
            'https://www.youtube.com/watch?v=dQw4w9WgXcQ');
END;

-- Phim 3: Cyber Horizon: 2099 (Đang chiếu)
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Cyber Horizon: 2099')
BEGIN
    INSERT INTO dbo.Movie (Title, Duration, ReleaseDate, [Language], Director, AgeRating, [Description], [Status], PosterURL, TrailerURL)
    VALUES (N'Cyber Horizon: 2099', 135, DATEADD(DAY, -2, CAST(GETDATE() AS DATE)), N'Tiếng Anh - Phụ đề Tiếng Việt',
            N'James Chen', 'T16',
            N'Trong thế giới tương lai nơi công nghệ AI thống trị, một nhóm thám tử công nghệ phải giải mã bí ẩn vụ rò rỉ mã nguồn đe dọa sự tồn vong của loài người.',
            'NOW_SHOWING', 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=600&q=80',
            'https://www.youtube.com/watch?v=dQw4w9WgXcQ');
END;

-- Phim 4: Ngôi Nhà Bí Ẩn (Đang chiếu)
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Ngôi Nhà Bí Ẩn')
BEGIN
    INSERT INTO dbo.Movie (Title, Duration, ReleaseDate, [Language], Director, AgeRating, [Description], [Status], PosterURL, TrailerURL)
    VALUES (N'Ngôi Nhà Bí Ẩn', 110, DATEADD(DAY, -5, CAST(GETDATE() AS DATE)), N'Tiếng Hàn - Phụ đề Tiếng Việt',
            N'Park Chan', 'T18',
            N'Một biệt thự cổ bị bỏ hoang chứa đựng bí mật kinh hoàng của gia tộc danh giá từng biến mất không dấu vết vào 30 năm trước.',
            'NOW_SHOWING', 'https://images.unsplash.com/photo-1509248961158-e54f6934749c?auto=format&fit=crop&w=600&q=80',
            'https://www.youtube.com/watch?v=dQw4w9WgXcQ');
END;

-- Phim 5: Vùng Đất Kỳ Diệu (Sắp chiếu)
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Vùng Đất Kỳ Diệu')
BEGIN
    INSERT INTO dbo.Movie (Title, Duration, ReleaseDate, [Language], Director, AgeRating, [Description], [Status], PosterURL, TrailerURL)
    VALUES (N'Vùng Đất Kỳ Diệu', 98, DATEADD(DAY, 7, CAST(GETDATE() AS DATE)), N'Tiếng Anh - Lồng tiếng Tiếng Việt',
            N'Sarah Jenkins', 'P',
            N'Câu chuyện cổ tích hiện đại đầy cảm động về cô bé Clara và chú cáo tuyết dũng cảm trên con đường tìm kiếm chiếc chìa khóa khôi phục ánh sáng mùa xuân.',
            'COMING_SOON', 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=80',
            'https://www.youtube.com/watch?v=dQw4w9WgXcQ');
END;

-- Phim 6: Đấu Trường Vô Tận (Sắp chiếu)
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE Title = N'Đấu Trường Vô Tận')
BEGIN
    INSERT INTO dbo.Movie (Title, Duration, ReleaseDate, [Language], Director, AgeRating, [Description], [Status], PosterURL, TrailerURL)
    VALUES (N'Đấu Trường Vô Tận', 142, DATEADD(DAY, 14, CAST(GETDATE() AS DATE)), N'Tiếng Anh - Phụ đề Tiếng Việt',
            N'Christopher Lee', 'T16',
            N'Siêu phẩm hành động không gian hội tụ những chiến binh dũng mãnh nhất ngân hà trong giải đấu sinh tử bảo vệ hành tinh mẹ.',
            'COMING_SOON', 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80',
            'https://www.youtube.com/watch?v=dQw4w9WgXcQ');
END;
GO

-- 3. Gắn thể loại cho phim (MovieGenre)
DECLARE @m1 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Light Speedway');
DECLARE @m2 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Blue Planet');
DECLARE @m3 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Cyber Horizon: 2099');
DECLARE @m4 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Ngôi Nhà Bí Ẩn');
DECLARE @m5 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Vùng Đất Kỳ Diệu');
DECLARE @m6 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Đấu Trường Vô Tận');

DECLARE @gAction INT = (SELECT GenreID FROM dbo.Genre WHERE [Name] = 'Action');
DECLARE @gAdv INT = (SELECT GenreID FROM dbo.Genre WHERE [Name] = 'Adventure');
DECLARE @gAnim INT = (SELECT GenreID FROM dbo.Genre WHERE [Name] = 'Animation');
DECLARE @gSciFi INT = (SELECT GenreID FROM dbo.Genre WHERE [Name] = 'Sci-Fi');
IF @gSciFi IS NULL SET @gSciFi = (SELECT GenreID FROM dbo.Genre WHERE [Name] = 'Action');
DECLARE @gHorror INT = (SELECT GenreID FROM dbo.Genre WHERE [Name] = 'Horror');

IF @m1 IS NOT NULL AND @gAction IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.MovieGenre WHERE MovieID = @m1 AND GenreID = @gAction)
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES (@m1, @gAction);
IF @m1 IS NOT NULL AND @gAdv IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.MovieGenre WHERE MovieID = @m1 AND GenreID = @gAdv)
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES (@m1, @gAdv);

IF @m2 IS NOT NULL AND @gAnim IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.MovieGenre WHERE MovieID = @m2 AND GenreID = @gAnim)
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES (@m2, @gAnim);
IF @m2 IS NOT NULL AND @gAdv IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.MovieGenre WHERE MovieID = @m2 AND GenreID = @gAdv)
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES (@m2, @gAdv);

IF @m3 IS NOT NULL AND @gSciFi IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.MovieGenre WHERE MovieID = @m3 AND GenreID = @gSciFi)
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES (@m3, @gSciFi);

IF @m4 IS NOT NULL AND @gHorror IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.MovieGenre WHERE MovieID = @m4 AND GenreID = @gHorror)
    INSERT INTO dbo.MovieGenre (MovieID, GenreID) VALUES (@m4, @gHorror);
GO

-- 4. Phòng chiếu (Rooms)
IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE RoomName = N'Cinema 01')
    INSERT INTO dbo.Room (RoomName, RoomType, Capacity, [Status]) VALUES (N'Cinema 01', '2D', 40, 'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE RoomName = N'Cinema 02')
    INSERT INTO dbo.Room (RoomName, RoomType, Capacity, [Status]) VALUES (N'Cinema 02', '3D', 40, 'ACTIVE');
IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE RoomName = N'Cinema 03 (VIP)')
    INSERT INTO dbo.Room (RoomName, RoomType, Capacity, [Status]) VALUES (N'Cinema 03 (VIP)', 'VIP', 24, 'ACTIVE');
GO

-- 5. Suất chiếu mẫu (Showtimes) trải dài cho hôm nay và các ngày tới
DECLARE @r1 INT = (SELECT RoomID FROM dbo.Room WHERE RoomName = N'Cinema 01');
DECLARE @r2 INT = (SELECT RoomID FROM dbo.Room WHERE RoomName = N'Cinema 02');
DECLARE @r3 INT = (SELECT RoomID FROM dbo.Room WHERE RoomName = N'Cinema 03 (VIP)');

DECLARE @mov1 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Light Speedway');
DECLARE @mov2 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Blue Planet');
DECLARE @mov3 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Cyber Horizon: 2099');
DECLARE @mov4 INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Ngôi Nhà Bí Ẩn');

DECLARE @Today DATE = CAST(GETDATE() AS DATE);
DECLARE @Tomorrow DATE = DATEADD(DAY, 1, @Today);
DECLARE @Day3 DATE = DATEADD(DAY, 2, @Today);

-- Suất chiếu hôm nay (sau thời điểm hiện tại)
DECLARE @SlotToday1 DATETIME2 = DATEADD(MINUTE, 30, SYSDATETIME());
DECLARE @SlotToday2 DATETIME2 = DATEADD(MINUTE, 180, SYSDATETIME());
DECLARE @SlotToday3 DATETIME2 = DATEADD(MINUTE, 360, SYSDATETIME());

IF @mov1 IS NOT NULL AND @r1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @mov1 AND RoomID = @r1 AND CAST(StartTime AS DATE) = @Today)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@mov1, @r1, @SlotToday1, DATEADD(MINUTE, 118, @SlotToday1), 85000, 'SCHEDULED');

IF @mov2 IS NOT NULL AND @r2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @mov2 AND RoomID = @r2 AND CAST(StartTime AS DATE) = @Today)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@mov2, @r2, @SlotToday2, DATEADD(MINUTE, 105, @SlotToday2), 95000, 'SCHEDULED');

-- Suất chiếu ngày mai
DECLARE @SlotTmr1 DATETIME2 = DATEADD(HOUR, 10, CAST(@Tomorrow AS DATETIME2));
DECLARE @SlotTmr2 DATETIME2 = DATEADD(HOUR, 14, CAST(@Tomorrow AS DATETIME2));
DECLARE @SlotTmr3 DATETIME2 = DATEADD(HOUR, 19, CAST(@Tomorrow AS DATETIME2));
DECLARE @SlotTmr4 DATETIME2 = DATEADD(HOUR, 21, CAST(@Tomorrow AS DATETIME2));

IF @mov1 IS NOT NULL AND @r1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @mov1 AND StartTime = @SlotTmr1)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@mov1, @r1, @SlotTmr1, DATEADD(MINUTE, 118, @SlotTmr1), 85000, 'SCHEDULED');

IF @mov2 IS NOT NULL AND @r2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @mov2 AND StartTime = @SlotTmr2)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@mov2, @r2, @SlotTmr2, DATEADD(MINUTE, 105, @SlotTmr2), 90000, 'SCHEDULED');

IF @mov3 IS NOT NULL AND @r3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @mov3 AND StartTime = @SlotTmr3)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@mov3, @r3, @SlotTmr3, DATEADD(MINUTE, 135, @SlotTmr3), 120000, 'SCHEDULED');

IF @mov4 IS NOT NULL AND @r1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE MovieID = @mov4 AND StartTime = @SlotTmr4)
    INSERT INTO dbo.Showtime (MovieID, RoomID, StartTime, EndTime, BasePrice, [Status])
    VALUES (@mov4, @r1, @SlotTmr4, DATEADD(MINUTE, 110, @SlotTmr4), 95000, 'SCHEDULED');
GO

-- 6. Khuyến mãi & Vouchers (Promotions & Vouchers - Screen 25)
IF NOT EXISTS (SELECT 1 FROM dbo.Promotion WHERE [Name] = N'Chào Bạn Mới - Giảm 20% Vé Đầu Tiên')
BEGIN
    INSERT INTO dbo.Promotion ([Name], [Description], StartDate, EndDate, [Status])
    VALUES (N'Chào Bạn Mới - Giảm 20% Vé Đầu Tiên',
            N'Chào đón thành viên mới gia nhập đại gia đình CineVerse! Nhận ngay mã giảm giá 20% cho giao dịch đặt vé trực tuyến đầu tiên áp dụng cho tất cả các suất chiếu và cụm rạp.',
            SYSDATETIME(), DATEADD(MONTH, 3, SYSDATETIME()), 'ACTIVE');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Promotion WHERE [Name] = N'Thứ Tư Vui Vẻ - Đồng Giá Vé 45.000đ')
BEGIN
    INSERT INTO dbo.Promotion ([Name], [Description], StartDate, EndDate, [Status])
    VALUES (N'Thứ Tư Vui Vẻ - Đồng Giá Vé 45.000đ',
            N'Chương trình Happy Wednesday đặc quyền mỗi thứ 4 hàng tuần: Đồng giá vé xem phim chỉ 45.000đ cho tất cả phim 2D tiêu chuẩn.',
            SYSDATETIME(), DATEADD(MONTH, 6, SYSDATETIME()), 'ACTIVE');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Promotion WHERE [Name] = N'Combo Đôi Lãng Mạn - Tiết Kiệm 30.000đ')
BEGIN
    INSERT INTO dbo.Promotion ([Name], [Description], StartDate, EndDate, [Status])
    VALUES (N'Combo Đôi Lãng Mạn - Tiết Kiệm 30.000đ',
            N'Tận hưởng buổi hẹn hò tuyệt vời với ưu đãi giảm 30.000đ khi đặt từ 2 vé xem phim kèm combo bắp nước lớn.',
            SYSDATETIME(), DATEADD(MONTH, 2, SYSDATETIME()), 'ACTIVE');
END;

IF NOT EXISTS (SELECT 1 FROM dbo.Promotion WHERE [Name] = N'Ưu Đãi Học Sinh - Sinh Viên')
BEGIN
    INSERT INTO dbo.Promotion ([Name], [Description], StartDate, EndDate, [Status])
    VALUES (N'Ưu Đãi Học Sinh - Sinh Viên',
            N'Chương trình trợ giá dành riêng cho các bạn học sinh - sinh viên với mức giảm 15% tất cả các ngày trong tuần.',
            SYSDATETIME(), DATEADD(MONTH, 12, SYSDATETIME()), 'ACTIVE');
END;
GO

-- Vouchers gắn với Promotions
DECLARE @p1 INT = (SELECT PromotionID FROM dbo.Promotion WHERE [Name] = N'Chào Bạn Mới - Giảm 20% Vé Đầu Tiên');
DECLARE @p2 INT = (SELECT PromotionID FROM dbo.Promotion WHERE [Name] = N'Thứ Tư Vui Vẻ - Đồng Giá Vé 45.000đ');
DECLARE @p3 INT = (SELECT PromotionID FROM dbo.Promotion WHERE [Name] = N'Combo Đôi Lãng Mạn - Tiết Kiệm 30.000đ');
DECLARE @p4 INT = (SELECT PromotionID FROM dbo.Promotion WHERE [Name] = N'Ưu Đãi Học Sinh - Sinh Viên');

IF @p1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Voucher WHERE Code = 'WELCOME20')
    INSERT INTO dbo.Voucher (PromotionID, Code, DiscountType, DiscountValue, ValidFrom, ValidTo, [Status])
    VALUES (@p1, 'WELCOME20', 'PERCENTAGE', 20, SYSDATETIME(), DATEADD(MONTH, 3, SYSDATETIME()), 'ACTIVE');

IF @p2 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Voucher WHERE Code = 'HAPPYWED')
    INSERT INTO dbo.Voucher (PromotionID, Code, DiscountType, DiscountValue, ValidFrom, ValidTo, [Status])
    VALUES (@p2, 'HAPPYWED', 'FIXED_AMOUNT', 45000, SYSDATETIME(), DATEADD(MONTH, 6, SYSDATETIME()), 'ACTIVE');

IF @p3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Voucher WHERE Code = 'COUPLE30')
    INSERT INTO dbo.Voucher (PromotionID, Code, DiscountType, DiscountValue, ValidFrom, ValidTo, [Status])
    VALUES (@p3, 'COUPLE30', 'FIXED_AMOUNT', 30000, SYSDATETIME(), DATEADD(MONTH, 2, SYSDATETIME()), 'ACTIVE');

IF @p4 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.Voucher WHERE Code = 'STUDENT50')
    INSERT INTO dbo.Voucher (PromotionID, Code, DiscountType, DiscountValue, ValidFrom, ValidTo, [Status])
    VALUES (@p4, 'STUDENT50', 'PERCENTAGE', 15, SYSDATETIME(), DATEADD(MONTH, 12, SYSDATETIME()), 'ACTIVE');
GO

-- 7. Tin tức điện ảnh & sự kiện (News - Screen 25)
DECLARE @StaffId INT = (SELECT TOP 1 StaffID FROM dbo.Staff);

IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'CineVerse Khai Trương Cụm Rạp Chuẩn Quốc Tế Với Phòng Chiếu Dolby Atmos')
BEGIN
    INSERT INTO dbo.News (CreatedByStaffID, Title, Content, ThumbnailURL, [Status], CreatedAt, PublishedAt)
    VALUES (
        @StaffId,
        N'CineVerse Khai Trương Cụm Rạp Chuẩn Quốc Tế Với Phòng Chiếu Dolby Atmos',
        N'Cụm rạp CineVerse chính thức khai trương với hệ thống phòng chiếu hiện đại bậc nhất, mang đến chất lượng âm thanh vòm Dolby Atmos đỉnh cao cùng màn hình siêu nét chuẩn DCI-P3. Khách hàng tới tham quan và thưởng thức phim trong tuần lễ khai trương sẽ được nhận bắp rang bơ miễn phí và cơ hội bốc thăm trúng thưởng vé xem phim 1 năm.',
        'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=600&q=80',
        'PUBLISHED',
        SYSDATETIME(),
        SYSDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'Top 5 Siêu Phẩm Điện Ảnh Không Thể Bỏ Lỡ Trong Tháng Này')
BEGIN
    INSERT INTO dbo.News (CreatedByStaffID, Title, Content, ThumbnailURL, [Status], CreatedAt, PublishedAt)
    VALUES (
        @StaffId,
        N'Top 5 Siêu Phẩm Điện Ảnh Không Thể Bỏ Lỡ Trong Tháng Này',
        N'Tháng này đánh dấu sự đổ bộ của hàng loạt bom tấn từ hành động kịch tính như Light Speedway, Cyber Horizon 2099 đến phim gia đình ấm áp Blue Planet. Đừng quên đặt vé sớm để chọn được những hàng ghế đẹp nhất tại CineVerse!',
        'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=600&q=80',
        'PUBLISHED',
        SYSDATETIME(),
        SYSDATETIME()
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.News WHERE Title = N'Chương Trình Thành Viên CineVerse: Tích Điểm Thả Ga, Đổi Vé Cực Đã')
BEGIN
    INSERT INTO dbo.News (CreatedByStaffID, Title, Content, ThumbnailURL, [Status], CreatedAt, PublishedAt)
    VALUES (
        @StaffId,
        N'Chương Trình Thành Viên CineVerse: Tích Điểm Thả Ga, Đổi Vé Cực Đã',
        N'Với mỗi giao dịch mua vé xem phim và combo bắp nước online, thành viên CineVerse sẽ được tích lũy 5% - 10% điểm thưởng để quy đổi thành vé xem phim miễn phí cùng nhiều phần quà độc quyền.',
        'https://images.unsplash.com/photo-1524985069026-dd778a71c7b4?auto=format&fit=crop&w=600&q=80',
        'PUBLISHED',
        SYSDATETIME(),
        SYSDATETIME()
    );
END;
GO

-- 8. Đánh giá phim mẫu (MovieReviews - Screen 23)
DECLARE @cust INT = (SELECT TOP 1 CustomerID FROM dbo.Customer);
DECLARE @mReview INT = (SELECT MovieID FROM dbo.Movie WHERE Title = N'Light Speedway');

IF @cust IS NOT NULL AND @mReview IS NOT NULL AND NOT EXISTS (SELECT 1 FROM dbo.MovieReview WHERE CustomerID = @cust AND MovieID = @mReview)
BEGIN
    INSERT INTO dbo.MovieReview (CustomerID, MovieID, Rating, Content, [Status], CreatedAt)
    VALUES (@cust, @mReview, 5, N'Phim xem cực kỳ mãn nhãn, âm thanh sống động tại phòng chiếu CineVerse, diễn xuất của dàn diễn viên rất tròn vai!', 'ACTIVE', SYSDATETIME());
END;
GO

PRINT N'Idempotent seed data for Iteration 2 (Screens 21-25) completed successfully.';
GO
