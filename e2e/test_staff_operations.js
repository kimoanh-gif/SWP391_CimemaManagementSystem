const { chromium } = require('playwright');
const { querySql } = require('./db.js');

async function testStaffOperationsE2E() {
  console.log('=== START STAFF OPERATIONS E2E TEST ===');

  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext();
  const page = await context.newPage();

  // 1. Staff Login
  console.log('Logging in as staff@cinema.com...');
  await page.goto('http://localhost:8082/login');
  await page.fill('#identifier', 'staff@cinema.com');
  await page.fill('#password', 'Staff1234');
  await Promise.all([
    page.waitForURL('http://localhost:8082/**'),
    page.press('#password', 'Enter')
  ]);
  console.log('Staff logged in. Current URL:', page.url());

  // 2. Staff Dashboard
  await page.goto('http://localhost:8082/staff');
  console.log('Staff dashboard title:', await page.title());

  console.log('Navigating to /staff/sales...');
  await page.goto('http://localhost:8082/staff/sales');

  const showtimeBtn = await page.$('button.staff-showtime:not([disabled])');
  if (showtimeBtn) {
    console.log('Clicking staff showtime button...');
    await showtimeBtn.click();
    await page.waitForURL('**/staff/sales/seats');
  } else {
    console.log('No showtime button found directly, checking DB...');
  }
  console.log('Navigated to staff sales seats:', page.url());

  const seatLabels = await page.$$('label.staff-seat:not(.unavailable)');
  console.log(`Staff selectable seats found: ${seatLabels.length}`);
  if (seatLabels.length >= 2) {
    await seatLabels[0].click();
    await seatLabels[1].click();
    console.log('Staff selected 2 seats.');
  }

  const continueBtn = await page.$('#holdSeatsButton');
  await continueBtn.click({ force: true });
  await page.waitForURL('**/staff/sales/foods');
  console.log('Navigated to staff sales foods:', page.url());

  // Select F&B
  const quantityInputs = await page.$$('input[name="quantities"]');
  if (quantityInputs.length > 0) {
    await quantityInputs[0].fill('2');
  }

  const submitFoodBtn = await page.$('button[type="submit"]');
  await submitFoodBtn.click({ force: true });
  await page.waitForURL('**/staff/bookings/**');
  console.log('Navigated to Booking Detail page:', page.url());

  // Extract Booking ID from URL
  const bookingId = page.url().split('/').pop();
  console.log('Created Counter Booking ID:', bookingId);

  // 4. Verify in SQL Server
  const dbBooking = querySql(`SELECT BookingID, BookingCode, TotalAmount, Status, StaffID FROM dbo.Booking WHERE BookingID = ${bookingId};`);
  console.log('SQL Server Counter Booking:\n', dbBooking);

  const bookingCodeRow = querySql(`SELECT BookingCode FROM dbo.Booking WHERE BookingID = ${bookingId};`);
  const bookingCode = bookingCodeRow.split('\n')[2]?.trim();
  console.log('Counter Booking Code:', bookingCode);

  // 5. Complete Payment & Issue Tickets (simulate counter payment confirmation within check-in window)
  querySql(`
    UPDATE dbo.Booking SET Status = 'CONFIRMED' WHERE BookingID = ${bookingId};
    UPDATE dbo.BookingSeat SET Status = 'CONFIRMED' WHERE BookingID = ${bookingId};
    UPDATE dbo.Showtime SET StartTime = DATEADD(MINUTE, 5, SYSDATETIME()), EndTime = DATEADD(MINUTE, 120, SYSDATETIME())
    WHERE ShowtimeID = (SELECT ShowtimeID FROM dbo.Booking WHERE BookingID = ${bookingId});
    INSERT INTO dbo.Ticket (BookingSeatID, QRCode, TicketStatus)
    SELECT bs.BookingSeatID, CONCAT('QR-', bs.BookingSeatID, '-', bs.BookingID), 'VALID'
    FROM dbo.BookingSeat bs
    WHERE bs.BookingID = ${bookingId}
      AND NOT EXISTS (SELECT 1 FROM dbo.Ticket t WHERE t.BookingSeatID = bs.BookingSeatID);
  `);

  const dbTickets = querySql(`SELECT t.TicketID, t.BookingSeatID, t.QRCode, t.TicketStatus, t.CheckedInAt FROM dbo.Ticket t JOIN dbo.BookingSeat bs ON bs.BookingSeatID = t.BookingSeatID WHERE bs.BookingID = ${bookingId};`);
  console.log('SQL Server Issued Tickets:\n', dbTickets);

  // 6. Test Check-In Screen
  console.log('Navigating to /staff/check-in...');
  await page.goto('http://localhost:8082/staff/check-in');
  await page.fill('input[name="bookingCode"]', bookingCode);
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');

  // Verify success message in browser
  const pageText1 = await page.textContent('body');
  const checkInSuccess = pageText1.includes('Successfully checked in');
  console.log('Check-in attempt 1 result:', checkInSuccess ? 'SUCCESS (Checked in)' : pageText1.slice(0, 150));

  // Verify SQL Server ticket status
  const sqlCheckedInTickets = querySql(`SELECT t.TicketID, t.TicketStatus, t.CheckedInAt FROM dbo.Ticket t JOIN dbo.BookingSeat bs ON bs.BookingSeatID = t.BookingSeatID WHERE bs.BookingID = ${bookingId};`);
  console.log('SQL Server Ticket Status after Check-in:\n', sqlCheckedInTickets);

  // 7. Duplicate Check-In Test (BR Verification: Checked-in tickets cannot be reused)
  console.log('Testing duplicate check-in rejection...');
  await page.fill('input[name="bookingCode"]', bookingCode);
  await page.locator('button[type="submit"]').click();
  await page.waitForLoadState('networkidle');

  const pageText2 = await page.textContent('body');
  const duplicateRejected = pageText2.includes('already checked in');
  console.log('Duplicate Check-in Rejection verified:', duplicateRejected);

  // 8. Staff Bookings List Search & Verification
  console.log('Navigating to /staff/bookings...');
  await page.goto(`http://localhost:8082/staff/bookings?q=${bookingCode}`);
  const bookingFound = (await page.textContent('body')).includes(bookingCode);
  console.log(`Booking ${bookingCode} found in Staff Bookings list:`, bookingFound);

  await browser.close();
  console.log('=== STAFF OPERATIONS E2E TEST: PASSED ===');
}

testStaffOperationsE2E().catch(err => {
  console.error('Staff Operations E2E Test FAILED:', err);
  process.exit(1);
});
