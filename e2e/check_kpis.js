const { querySql } = require('./db.js');

console.log('Bookings:\n', querySql("SELECT BookingID, Status, TotalAmount, BookingTime FROM dbo.Booking;"));
console.log('Valid Tickets Count:\n', querySql("SELECT COUNT(*) AS ValidTickets FROM dbo.Ticket t JOIN dbo.Booking b ON t.BookingID = b.BookingID WHERE UPPER(b.Status) IN ('PAID', 'CONFIRMED', 'COMPLETED');"));
console.log('BookingSeat Count:\n', querySql("SELECT COUNT(*) AS ValidSeats FROM dbo.BookingSeat bs JOIN dbo.Booking b ON bs.BookingID = b.BookingID WHERE UPPER(b.Status) IN ('PAID', 'CONFIRMED', 'COMPLETED');"));
console.log('Active Promotions Count:\n', querySql("SELECT COUNT(*) AS ActivePromos FROM dbo.Promotion WHERE UPPER(Status) IN ('ACTIVE', 'OPEN') AND (EndDate IS NULL OR EndDate >= GETDATE());"));
console.log('Today Showtimes Count:\n', querySql("SELECT COUNT(*) AS TodayShowtimes FROM dbo.Showtime WHERE CAST(StartTime AS DATE) = CAST(GETDATE() AS DATE);"));
console.log('Today Revenue:\n', querySql("SELECT COALESCE(SUM(TotalAmount), 0) AS TodayRev FROM dbo.Booking WHERE UPPER(Status) IN ('PAID', 'CONFIRMED', 'COMPLETED') AND CAST(BookingTime AS DATE) = CAST(GETDATE() AS DATE);"));
console.log('Total Revenue:\n', querySql("SELECT COALESCE(SUM(TotalAmount), 0) AS TotalRev FROM dbo.Booking WHERE UPPER(Status) IN ('PAID', 'CONFIRMED', 'COMPLETED');"));
