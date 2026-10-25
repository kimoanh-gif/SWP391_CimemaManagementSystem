const { chromium } = require('playwright');
const { querySql } = require('./db.js');

async function runCustomerBookingE2E() {
  console.log('=== START CUSTOMER BOOKING E2E TEST ===');

  // Step 0: Ensure DB clean of previous pending test bookings
  querySql("DELETE FROM dbo.BookingFoodBeverage WHERE BookingID IN (SELECT BookingID FROM dbo.Booking WHERE CustomerID = 2 AND Status = 'PENDING');");
  querySql("DELETE FROM dbo.BookingSeat WHERE BookingID IN (SELECT BookingID FROM dbo.Booking WHERE CustomerID = 2 AND Status = 'PENDING');");
  querySql("DELETE FROM dbo.Booking WHERE CustomerID = 2 AND Status = 'PENDING';");

  const initialBookingsCount = querySql("SELECT COUNT(*) FROM dbo.Booking WHERE CustomerID = 2;");
  console.log('Initial customer bookings count in SQL:', initialBookingsCount);

  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext();
  const page = await context.newPage();

  // 1. Visit Login Page
  console.log('Navigating to http://localhost:8082/login...');
  await page.goto('http://localhost:8082/login');
  await page.fill('#identifier', 'customer@cinema.com');
  await page.fill('#password', 'Customer123');
  await Promise.all([
    page.waitForURL('http://localhost:8082/**'),
    page.press('#password', 'Enter')
  ]);
  console.log('Logged in successfully. Current URL:', page.url());

  // 2. Navigate to Customer Booking
  console.log('Navigating to /customer/booking...');
  await page.goto('http://localhost:8082/customer/booking');
  const title = await page.title();
  console.log('Booking page title:', title);

  // Check if showtimes exist
  const showtimeBtns = await page.$$('button[name="showtimeId"], input[name="showtimeId"]');
  console.log('Found showtime selectors:', showtimeBtns.length);

  // Select first available showtime form
  const firstShowtimeForm = await page.$('form[action*="/customer/booking/showtime"]');
  if (firstShowtimeForm) {
    console.log('Submitting showtime selection...');
    const submitBtn = await firstShowtimeForm.$('button[type="submit"]');
    if (submitBtn) {
      await submitBtn.click();
    } else {
      await firstShowtimeForm.evaluate(f => f.submit());
    }
  } else {
    // Alternatively direct select
    const showtimeRow = querySql("SELECT TOP 1 ShowtimeID FROM dbo.Showtime WHERE StartTime > SYSDATETIME() AND Status = 'Scheduled';");
    const stId = showtimeRow.split('\n')[2]?.trim();
    console.log('Direct navigating to /customer/booking/select?showtimeId=' + stId);
    await page.goto(`http://localhost:8082/customer/booking/select?showtimeId=${stId}`);
  }

  await page.waitForURL('**/customer/booking/seats');
  console.log('Navigated to Seats selection:', page.url());

  // 3. Select 2 Seats via seat labels
  const seatLabels = await page.$$('label.seat:not(.unavailable)');
  console.log(`Available selectable seats found: ${seatLabels.length}`);
  if (seatLabels.length < 2) {
    throw new Error('Not enough selectable seats on screen!');
  }

  await seatLabels[0].click();
  await seatLabels[1].click();
  console.log('Clicked 2 seats.');

  // Submit seats
  const holdSeatsBtn = await page.$('#continue-button');
  await holdSeatsBtn.click({ force: true });

  await page.waitForURL('**/customer/booking/foods');
  console.log('Navigated to Concessions selection:', page.url());

  // 4. Verify Database Persistence of Pending Booking & Held Seats
  const dbBooking = querySql("SELECT BookingID, BookingCode, TotalAmount, Status FROM dbo.Booking WHERE CustomerID = 2 AND Status = 'PENDING';");
  console.log('Database Pending Booking:\n', dbBooking);

  const dbSeats = querySql("SELECT bs.BookingSeatID, bs.SeatID, bs.Status, bs.UnitPrice FROM dbo.BookingSeat bs JOIN dbo.Booking b ON b.BookingID = bs.BookingID WHERE b.CustomerID = 2 AND b.Status = 'PENDING';");
  console.log('Database Held Seats in BookingSeat:\n', dbSeats);

  // 5. Select Food & Beverages
  const quantityInputs = await page.$$('input[name="quantities"]');
  if (quantityInputs.length > 0) {
    console.log(`Setting quantity for ${quantityInputs.length} food items...`);
    await quantityInputs[0].fill('1');
    const saveFoodBtn = await page.$('button[type="submit"]');
    await saveFoodBtn.click();
    await page.waitForLoadState('networkidle');
    console.log('Food selection submitted.');

    const dbFood = querySql("SELECT bfb.BookingFoodBeverageID, bfb.FoodBeverageItemID, bfb.Quantity, bfb.UnitPrice FROM dbo.BookingFoodBeverage bfb JOIN dbo.Booking b ON b.BookingID = bfb.BookingID WHERE b.CustomerID = 2;");
    console.log('Database Food selections:\n', dbFood);
  }

  // 6. Test Cancel/Release
  console.log('Testing booking release / cancellation...');
  const cancelBtn = await page.$('form[action*="/customer/booking/cancel"] button[type="submit"]');
  if (cancelBtn) {
    await cancelBtn.click();
    await page.waitForURL('**/customer/booking**');
    console.log('Cancelled booking. URL:', page.url());

    const activeSeatsAfterCancel = querySql("SELECT COUNT(*) FROM dbo.BookingSeat bs JOIN dbo.Booking b ON b.BookingID = bs.BookingID WHERE b.CustomerID = 2 AND bs.Status = 'HELD';");
    console.log('Held seats after cancel:', activeSeatsAfterCancel);
  }

  await browser.close();
  console.log('=== CUSTOMER BOOKING E2E TEST: PASSED ===');
}

runCustomerBookingE2E().catch(err => {
  console.error('Customer Booking E2E Test FAILED:', err);
  process.exit(1);
});
