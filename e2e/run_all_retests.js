const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { querySql } = require('./db');

async function main() {
  console.log('=== STARTING CINEORA LIVE RETEST SUITE ===');

  // Check SQL connection
  console.log('\n--- 1. Querying DB for Movies & Showtimes ---');
  const movies = querySql("SELECT MovieID, Title, Status, CONVERT(varchar, ReleaseDate, 23) AS ReleaseDate, CONVERT(varchar, EndDate, 23) AS EndDate FROM dbo.Movie ORDER BY MovieID ASC;");
  console.log('Movies in DB:');
  console.log(movies);

  // Check ST_035 data: Bambi showtimes
  console.log('\n--- 2. ST_035: Checking Bambi showtimes on 07/11/2026 vs all dates ---');
  const bambiAll = querySql("SELECT COUNT(*) AS AllShowtimes FROM dbo.Showtime st JOIN dbo.Movie m ON st.MovieID = m.MovieID WHERE m.Title LIKE '%Bambi%';");
  console.log('Bambi Total Lifetime Showtimes:', bambiAll);

  const bambiNov7 = querySql("SELECT COUNT(*) AS Nov7Showtimes FROM dbo.Showtime st JOIN dbo.Movie m ON st.MovieID = m.MovieID WHERE m.Title LIKE '%Bambi%' AND CAST(st.StartTime AS DATE) = '2026-11-07';");
  console.log('Bambi Showtimes on 2026-11-07:', bambiNov7);

  const bambiNov7List = querySql("SELECT st.ShowtimeID, CONVERT(varchar, st.StartTime, 120) AS StartTime, CONVERT(varchar, st.EndTime, 120) AS EndTime, r.RoomName FROM dbo.Showtime st JOIN dbo.Movie m ON st.MovieID = m.MovieID JOIN dbo.Room r ON st.RoomID = r.RoomID WHERE m.Title LIKE '%Bambi%' AND CAST(st.StartTime AS DATE) = '2026-11-07';");
  console.log('Bambi Showtimes Detail on 2026-11-07:');
  console.log(bambiNov7List);

  // Check ST_040 data: Bookings and seats
  console.log('\n--- 3. ST_040: Checking Booking & BookingSeat status ---');
  const bookings = querySql("SELECT TOP 5 BookingID, Status, BookingTime, TotalAmount FROM dbo.Booking ORDER BY BookingID DESC;");
  console.log('Recent Bookings:');
  console.log(bookings);

  const heldSeats = querySql("SELECT COUNT(*) AS HeldCount FROM dbo.BookingSeat WHERE Status = 'HELD';");
  console.log('Current Held Seats:', heldSeats);

  const expiredSeats = querySql("SELECT COUNT(*) AS ExpiredHeldCount FROM dbo.BookingSeat WHERE Status = 'HELD' AND HeldUntil < SYSDATETIME();");
  console.log('Expired Held Seats in DB:', expiredSeats);
}

main().catch(err => {
  console.error('Error running test:', err);
  process.exit(1);
});
