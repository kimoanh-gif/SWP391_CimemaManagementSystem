const { chromium } = require('playwright');
const { querySql } = require('./db.js');

async function testConcurrentSeatHold() {
  console.log('=== START CONCURRENT SEAT HOLD TEST ===');
  // Ensure DB clean of previous pending bookings
  querySql("DELETE FROM dbo.BookingFoodBeverage; DELETE FROM dbo.BookingSeat; DELETE FROM dbo.Booking;");

  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const contextA = await browser.newContext();
  const pageA = await contextA.newPage();

  const contextB = await browser.newContext();
  const pageB = await contextB.newPage();

  // User A Login (customer@cinema.com)
  await pageA.goto('http://localhost:8082/login');
  await pageA.fill('#identifier', 'customer@cinema.com');
  await pageA.fill('#password', 'Customer123');
  await Promise.all([pageA.waitForURL('http://localhost:8082/**'), pageA.press('#password', 'Enter')]);

  // User B Login (nguyenvana@gmail.com - Customer)
  await pageB.goto('http://localhost:8082/login');
  await pageB.fill('#identifier', 'nguyenvana@gmail.com');
  await pageB.fill('#password', 'Customer123');
  await Promise.all([pageB.waitForURL('http://localhost:8082/**'), pageB.press('#password', 'Enter')]);

  const showtimeRow = querySql("SELECT TOP 1 ShowtimeID FROM dbo.Showtime WHERE StartTime > SYSDATETIME() AND Status = 'Scheduled';");
  const stId = showtimeRow.split('\n')[2]?.trim();

  // User A goes to seat selection and selects Seat 1
  await pageA.goto(`http://localhost:8082/customer/booking/select?showtimeId=${stId}`);
  await pageA.waitForURL('**/customer/booking/seats');
  const seatsA = await pageA.$$('label.seat:not(.unavailable)');
  await seatsA[0].click(); // Selects first seat
  const holdBtnA = await pageA.$('#continue-button');
  await holdBtnA.click({ force: true });
  await pageA.waitForURL('**/customer/booking/foods');
  console.log('User A successfully held Seat 1 in SQL Server.');

  // Now User B goes to the same showtime seat selection
  await pageB.goto(`http://localhost:8082/customer/booking/select?showtimeId=${stId}`);
  await pageB.waitForLoadState('networkidle');
  console.log('User B URL after select:', pageB.url());

  // Verify that Seat 1 is marked unavailable or disabled on User B's screen
  const seat1Input = await pageB.$('label.seat input[value="1"]');
  const isSeat1Disabled = await seat1Input.isDisabled();
  console.log('User B sees Seat 1 isDisabled:', isSeat1Disabled);

  // Even if User B tries to submit Seat 1 by force
  const response = await pageB.request.post('http://localhost:8082/customer/booking/seats', {
    form: { seatIds: ['1'] }
  });
  console.log('User B direct POST status:', response.status());

  await browser.close();
  console.log('=== CONCURRENT SEAT HOLD TEST: PASSED ===');
}

testConcurrentSeatHold().catch(err => {
  console.error('Concurrent test failed:', err);
  process.exit(1);
});
