-- =====================================================================
-- Cinema Management System - SWP391
-- Fix UTF-8 Vietnamese Encoding for all Master Data Tables
-- Target DBMS: Microsoft SQL Server (CinemaManagementDB)
-- =====================================================================

USE CinemaManagementDB;
GO

-- 1. Fix FBItem (Food & Beverage)
UPDATE FBItem 
SET ItemName = N'Combo Couple Sweetbox', 
    Description = N'1 Bắp rang bơ cỡ lớn (Phô mai/Caramel) + 2 Nước ngọt có ga lớn 32oz'
WHERE ItemCode = 'CB-COUPLE';

UPDATE FBItem 
SET ItemName = N'Combo Solo Cine', 
    Description = N'1 Bắp rang bơ vị ngọt vừa 24oz + 1 Nước ngọt có ga 22oz'
WHERE ItemCode = 'CB-SOLO';

UPDATE FBItem 
SET ItemName = N'Combo Siêu Tiệc Party 4 Người', 
    Description = N'2 Bắp rang bơ Jumbo đặc biệt + 4 Nước ngọt lớn + 1 Khoai tây chiên giòn'
WHERE ItemCode = 'CB-PARTY';

UPDATE FBItem 
SET ItemName = N'Bắp Rang Bơ Vị Caramel Thượng Hạng', 
    Description = N'Bắp nở hảo hạng tẩm sốt caramel béo ngậy giòn tan.'
WHERE ItemCode = 'PC-CARAMEL';

UPDATE FBItem 
SET ItemName = N'Bắp Rang Bơ Vị Phô Mai Lắc', 
    Description = N'Bắp nở lắc cùng bột phô mai Cheddar đậm đà thơm lừng.'
WHERE ItemCode = 'PC-CHEESE';

UPDATE FBItem 
SET ItemName = N'Coca-Cola Tươi 32oz (Size Lớn)', 
    Description = N'Nước ngọt có ga sảng khoái mát lạnh.'
WHERE ItemCode = 'DR-COKE';

UPDATE FBItem 
SET ItemName = N'Sprite Chanh Tươi 32oz (Size Lớn)', 
    Description = N'Hương chanh thanh mát đã khát.'
WHERE ItemCode = 'DR-SPRITE';

UPDATE FBItem 
SET ItemName = N'Bánh Mì Kẹp Xúc Xích Phô Mai Nóng', 
    Description = N'Xúc xích Đức nướng xốt mù tạt vàng và sốt cà chua.'
WHERE ItemCode = 'SN-HOTDOG';
GO

-- 2. Fix Room (Phòng Chiếu)
UPDATE Room 
SET RoomName = N'Phòng Chiếu 01 - Standard Laser',
    RoomType = N'Standard Laser',
    Projector = N'Barco SP4K-15 Laser',
    SoundSystem = N'Dolby Surround 7.1',
    Description = N'Phòng chiếu tiêu chuẩn Laser độ phân giải 4K sắc nét.'
WHERE RoomCode = 'ROOM-01';

UPDATE Room 
SET RoomName = N'Phòng Chiếu 02 - Dolby Atmos Hall',
    RoomType = N'Dolby Atmos',
    Projector = N'Christie CP4435-RGB Dual Laser',
    SoundSystem = N'Dolby Atmos 12.1 vòm 360 độ',
    Description = N'Phòng chiếu âm thanh đa chiều Dolby Atmos cao cấp.'
WHERE RoomCode = 'ROOM-02';

UPDATE Room 
SET RoomName = N'Phòng Chiếu IMAX Laser 01',
    RoomType = N'IMAX Laser',
    Projector = N'IMAX Commercial Laser 4K Dual',
    SoundSystem = N'IMAX 12.0 Channel Immersive Sound',
    Description = N'Màn hình cực đại IMAX chuẩn quốc tế cho trải nghiệm chân thực.'
WHERE RoomCode = 'ROOM-IMAX-01';

UPDATE Room 
SET RoomName = N'Phòng Chiếu VIP Gold Class',
    RoomType = N'Gold Class',
    Projector = N'Sony SRX-R515P 4K D-Cinema',
    SoundSystem = N'JBL Premium Theater Sound System',
    Description = N'Ghế sofa da chỉnh điện đơn và đôi, dịch vụ ẩm thực tại chỗ.'
WHERE RoomCode = 'ROOM-GOLD-01';

UPDATE Room 
SET RoomName = N'Phòng Chiếu 03 - Laser Compact',
    RoomType = N'Standard Laser',
    Projector = N'Barco SP2K-11 Laser',
    SoundSystem = N'Dolby 7.1 Studio Audio',
    Description = N'Đang bảo trì định kỳ hệ thống quạt thông gió và máy chiếu.'
WHERE RoomCode = 'ROOM-03';
GO

-- 3. Fix SeatType (Hạng Ghế)
UPDATE SeatType 
SET TypeName = N'Ghế Tiêu Chuẩn (Standard)',
    Description = N'Ghế ngồi tiêu chuẩn tầm nhìn bao quát toàn màn hình.'
WHERE TypeCode = 'STD';

UPDATE SeatType 
SET TypeName = N'Ghế VIP Royal Amber',
    Description = N'Ghế đệm cao cấp tại các hàng trung tâm, góc nhìn tối ưu nhất.'
WHERE TypeCode = 'VIP';

UPDATE SeatType 
SET TypeName = N'Ghế Đôi Sweetbox (Couple)',
    Description = N'Ghế đôi kèm vách ngăn riêng tư dành cho 2 người ở hàng cuối.'
WHERE TypeCode = 'COUPLE';

UPDATE SeatType 
SET TypeName = N'Ghế Hàng Đầu (Front)',
    Description = N'Ghế hàng đầu sát màn hình với giá ưu đãi tiết kiệm.'
WHERE TypeCode = 'FRONT';
GO

-- 4. Fix TicketPricing (Bảng Giá Vé)
UPDATE TicketPricing 
SET RuleName = N'Ngày thường - Khung giờ tiêu chuẩn (12:00 - 17:00)',
    Description = N'Giá vé cơ bản 2D ngày trong tuần từ thứ 2 đến thứ 6.'
WHERE PricingCode = 'PRICE-WD-STD';

UPDATE TicketPricing 
SET RuleName = N'Ngày thường - Buổi sáng sớm (< 12:00)',
    Description = N'Suất chiếu sáng ưu đãi học sinh, sinh viên và người cao tuổi.'
WHERE PricingCode = 'PRICE-WD-MORN';

UPDATE TicketPricing 
SET RuleName = N'Ngày thường - Giờ vàng tối (> 17:00)',
    Description = N'Suất chiếu tối giờ cao điểm từ thứ 2 đến thứ 6.'
WHERE PricingCode = 'PRICE-WD-GOLD';

UPDATE TicketPricing 
SET RuleName = N'Cuối tuần & Ngày lễ (Thứ 7, CN)',
    Description = N'Giá vé cơ bản cuối tuần và các dịp lễ tết.'
WHERE PricingCode = 'PRICE-WE-ALL';

UPDATE TicketPricing 
SET RuleName = N'Phụ thu định dạng 3D RealD',
    Description = N'Phụ thu kính và máy chiếu 3D RealD.'
WHERE PricingCode = 'PRICE-SUR-3D';

UPDATE TicketPricing 
SET RuleName = N'Phụ thu định dạng IMAX Laser',
    Description = N'Phụ thu phòng chiếu màn hình cực đại IMAX Laser.'
WHERE PricingCode = 'PRICE-SUR-IMAX';
GO

-- 5. Fix Genre (Thể Loại Phim)
UPDATE Genre 
SET Name = N'Hành Động',
    Description = N'Hành động kịch tính, rượt đuổi và chiến đấu mãn nhãn'
WHERE GenreID = 1;

UPDATE Genre 
SET Name = N'Phiêu Lưu',
    Description = N'Phiêu lưu mạo hiểm, khám phá các vùng đất mới'
WHERE GenreID = 2;

UPDATE Genre 
SET Name = N'Hoạt Hình',
    Description = N'Hoạt hình đồ họa 3D/2D dành cho mọi lứa tuổi'
WHERE GenreID = 3;

UPDATE Genre 
SET Name = N'Hài Hước',
    Description = N'Hài hước, dí dỏm mang lại tiếng cười sảng khoái'
WHERE GenreID = 4;

UPDATE Genre 
SET Name = N'Tâm Lý Xã Hội',
    Description = N'Tâm lý xã hội sâu sắc, câu chuyện cảm động'
WHERE GenreID = 5;

UPDATE Genre 
SET Name = N'Kinh Dị',
    Description = N'Kinh dị, rùng rợn, giật gân kích thích nỗi sợ'
WHERE GenreID = 6;

UPDATE Genre 
SET Name = N'Lãng Mạn',
    Description = N'Tình cảm lãng mạn, câu chuyện tình yêu ngọt ngào'
WHERE GenreID = 7;

UPDATE Genre 
SET Name = N'Khoa Học Viễn Tưởng',
    Description = N'Khoa học viễn tưởng, công nghệ tương lai và vũ trụ'
WHERE GenreID = 8;

UPDATE Genre 
SET Name = N'Hồi Hộp Gay Cấn',
    Description = N'Hồi hộp, bí ẩn và căng thẳng nghẹt thở'
WHERE GenreID = 9;

UPDATE Genre 
SET Name = N'Tài Liệu',
    Description = N'Tài liệu thực tế, khám phá tự nhiên và nhân vật lịch sử'
WHERE GenreID = 10;

UPDATE Genre 
SET Name = N'Kỳ Ảo',
    Description = N'Kỳ ảo, phép thuật và thế giới huyền bí'
WHERE GenreID = 11;

UPDATE Genre 
SET Name = N'Kinh Điển',
    Description = N'Phim kinh điển đã ngừng chiếu thương mại'
WHERE GenreID = 12;

UPDATE Genre 
SET Name = N'Ca Nhạc',
    Description = N'Phim ca nhạc và biểu diễn nghệ thuật'
WHERE GenreID = 13;
GO

-- 6. Fix Account & Customer
UPDATE Account 
SET FullName = N'Nguyễn Văn A'
WHERE Username = 'customer1';

UPDATE Customer 
SET Address = N'123 Cầu Giấy, Hà Nội'
WHERE CustomerID = 1;
GO
