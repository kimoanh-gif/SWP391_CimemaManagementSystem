const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { querySql } = require('./db');

async function run() {
  console.log('=== STARTING COMPLETE RETEST SUITE ===');
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Step 0: Login as Manager
  console.log('\n--- Step 0: Logging in as Manager ---');
  await page.goto('http://localhost:8081/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([
    page.waitForURL('http://localhost:8081/**'),
    page.press('#password', 'Enter')
  ]);
  console.log('Current URL after login:', page.url());

  // -------------------------------------------------------------
  // Test ST_017A: Seat Type Validation (Screen 57)
  // -------------------------------------------------------------
  // Test ST_017A: Seat Type Validation (Screen 57)
  // -------------------------------------------------------------
  console.log('\n--- Retesting ST_017A: Seat Type Validation ---');
  await page.goto('http://localhost:8081/manager/seat-types');
  await page.waitForLoadState('networkidle');

  // Subtest 1: Multiplier 0.40 (min is 0.50)
  console.log('ST_017A Subtest 1: Testing Multiplier = 0.40');
  const res17A1 = await context.request.post('http://localhost:8081/manager/seat-types/save', {
    form: {
      typeCode: 'TEST_LOW',
      typeName: 'Test Seat Type Low',
      priceMultiplier: '0.40',
      colorCode: '#3B82F6',
      status: 'Active',
      description: 'Testing low multiplier'
    }
  });
  await page.goto('http://localhost:8081/manager/seat-types');
  await page.waitForLoadState('networkidle');
  const alertText17A1 = await page.textContent('.alert-error').catch(() => null);
  console.log('ST_017A.1 Alert Message:', alertText17A1 ? alertText17A1.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017a_multiplier_low.png', fullPage: true });

  // Subtest 2: Multiplier 3.50 (max is 3.00)
  console.log('ST_017A Subtest 2: Testing Multiplier = 3.50');
  const res17A2 = await context.request.post('http://localhost:8081/manager/seat-types/save', {
    form: {
      typeCode: 'TEST_HIGH',
      typeName: 'Test Seat Type High',
      priceMultiplier: '3.50',
      colorCode: '#3B82F6',
      status: 'Active',
      description: 'Testing high multiplier'
    }
  });
  await page.goto('http://localhost:8081/manager/seat-types');
  await page.waitForLoadState('networkidle');
  const alertText17A2 = await page.textContent('.alert-error').catch(() => null);
  console.log('ST_017A.2 Alert Message:', alertText17A2 ? alertText17A2.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017a_multiplier_high.png', fullPage: true });

  // Subtest 3: Color "blue" (HEX required)
  console.log('ST_017A Subtest 3: Testing Color = "blue"');
  const res17A3 = await context.request.post('http://localhost:8081/manager/seat-types/save', {
    form: {
      typeCode: 'TEST_BLUE',
      typeName: 'Test Seat Type Blue',
      priceMultiplier: '1.50',
      colorCode: 'blue',
      status: 'Active',
      description: 'Testing invalid color'
    }
  });
  await page.goto('http://localhost:8081/manager/seat-types');
  await page.waitForLoadState('networkidle');
  const alertText17A3 = await page.textContent('.alert-error').catch(() => null);
  console.log('ST_017A.3 Alert Message:', alertText17A3 ? alertText17A3.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017a_color_invalid.png', fullPage: true });

  // Check if inline field error tags exist on seat-types.html
  const inlineErrors17A = await page.$$eval('.field-error, .invalid-feedback, span.error, span.text-danger', els => els.map(e => e.textContent.trim()));
  console.log('ST_017A Inline Error Elements Found:', inlineErrors17A.length, inlineErrors17A);

  // -------------------------------------------------------------
  // Test ST_017B: Ticket Pricing Validation (Screen 60)
  // -------------------------------------------------------------
  console.log('\n--- Retesting ST_017B: Ticket Pricing Validation ---');
  await page.goto('http://localhost:8081/manager/pricing');
  await page.waitForLoadState('networkidle');

  // Submit basePrice = -1
  console.log('ST_017B: Testing Base Price = -1');
  const res17B = await context.request.post('http://localhost:8081/manager/pricing/save', {
    form: {
      pricingCode: 'TEST_NEG_BASE',
      ruleName: 'Test Invalid Rule',
      dayType: 'WEEKDAY',
      timeSlot: 'MORNING',
      basePrice: '-1',
      formatSurcharge: '0',
      experienceType: '2D Digital',
      seatTypeId: '1',
      status: 'Active'
    }
  });
  await page.goto('http://localhost:8081/manager/pricing');
  await page.waitForLoadState('networkidle');
  const alertText17B = await page.textContent('.alert-error').catch(() => null);
  console.log('ST_017B Alert Message:', alertText17B ? alertText17B.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017b_negative_base_price.png', fullPage: true });

  const inlineErrors17B = await page.$$eval('.field-error, .invalid-feedback, span.error, span.text-danger', els => els.map(e => e.textContent.trim()));
  console.log('ST_017B Inline Error Elements Found:', inlineErrors17B.length, inlineErrors17B);

  // -------------------------------------------------------------
  // Test ST_017C: F&B Validation (Screen 61)
  // -------------------------------------------------------------
  console.log('\n--- Retesting ST_017C: F&B Validation ---');
  await page.goto('http://localhost:8081/manager/fb');
  await page.waitForLoadState('networkidle');

  // Submit costPrice = 60000 > sellingPrice = 50000
  console.log('ST_017C: Testing Cost Price (60000) > Selling Price (50000)');
  const res17C = await context.request.post('http://localhost:8081/manager/fb/save', {
    form: {
      itemCode: 'FB_TEST_COST',
      itemName: 'Test Popcorn Deluxe',
      category: 'Food',
      sellingPrice: '50000',
      costPrice: '60000',
      status: 'Active',
      description: 'Testing cost price greater than selling price'
    }
  });
  await page.goto('http://localhost:8081/manager/fb');
  await page.waitForLoadState('networkidle');
  const alertText17C = await page.textContent('.alert-error').catch(() => null);
  console.log('ST_017C Alert Message:', alertText17C ? alertText17C.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017c_cost_exceeds_selling.png', fullPage: true });

  const inlineErrors17C = await page.$$eval('.field-error, .invalid-feedback, span.error, span.text-danger', els => els.map(e => e.textContent.trim()));
  console.log('ST_017C Inline Error Elements Found:', inlineErrors17C.length, inlineErrors17C);

  // -------------------------------------------------------------
  // Test ST_025: Showtime Scheduling vs Movie Release Date
  // -------------------------------------------------------------
  console.log('\n--- Retesting ST_025: Showtime Scheduling vs Movie Release Date ---');
  // Movie 15 is 'hay', ReleaseDate = 2026-11-11
  console.log('Movie 15: "hay", ReleaseDate: 2026-11-11. Attempting showtime on 2026-11-09 10:00:00 (before release date)');
  const res25 = await context.request.post('http://localhost:8081/manager/showtimes/save', {
    form: {
      movieId: '15',
      roomId: '1',
      startTime: '2026-11-09T10:00',
      basePrice: '85000'
    }
  });
  await page.goto('http://localhost:8081/manager/showtimes?date=2026-11-09');
  await page.waitForLoadState('networkidle');
  const alertText25 = await page.textContent('.alert-error').catch(() => null);
  console.log('ST_025 Alert Message:', alertText25 ? alertText25.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st025_showtime_before_release.png', fullPage: true });

  // Also verify DB that no showtime was inserted for movie 15 on 2026-11-09
  const stCount = querySql("SELECT COUNT(*) AS ST_Count FROM dbo.Showtime WHERE MovieID = 15 AND CAST(StartTime AS DATE) = '2026-11-09';");
  console.log('ST_025 DB verification: Showtime count for Movie 15 on 2026-11-09:', stCount.trim());

  // -------------------------------------------------------------
  // Test ST_035: Movie Report Date Range Filtering on 07/11/2026
  // -------------------------------------------------------------
  console.log('\n--- Retesting ST_035: Movie Report Date Range Filtering on 07/11/2026 ---');
  await page.goto('http://localhost:8081/manager/reports?reportType=MOVIE&startDate=2026-11-07&endDate=2026-11-07');
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: 'e2e/st035_movie_report_20261107.png', fullPage: true });

  // Extract table rows from reports page
  const reportRows = await page.$$eval('#reportTable tbody tr', rows => rows.map(r => {
    const cols = Array.from(r.querySelectorAll('td')).map(td => td.textContent.trim());
    return cols;
  }));
  console.log('ST_035 Report Table Rows count on 2026-11-07:', reportRows.length);
  reportRows.forEach((r, idx) => console.log(`  Row ${idx + 1}:`, r.join(' | ')));

  // Query DB with the exact query from RevenueReportDao for 2026-11-07
  const dbMovieReport = querySql(`
    SELECT m.MovieID, m.Title,
           COUNT(DISTINCT CASE WHEN CAST(st.StartTime AS DATE) BETWEEN '2026-11-07' AND '2026-11-07' THEN st.ShowtimeID END) AS ShowtimesCount,
           COUNT(DISTINCT bs.BookingSeatID) AS TicketsSold,
           ISNULL(SUM(bs.Price), 0) AS TicketRevenue
    FROM dbo.Movie m
    LEFT JOIN dbo.Showtime st ON m.MovieID = st.MovieID
    LEFT JOIN dbo.BookingSeat bs ON st.ShowtimeID = bs.ShowtimeID
    LEFT JOIN dbo.Booking b ON bs.BookingID = b.BookingID AND UPPER(b.Status) IN ('PAID', 'CONFIRMED', 'COMPLETED')
    GROUP BY m.MovieID, m.Title
    ORDER BY ShowtimesCount DESC, m.MovieID ASC;
  `);
  console.log('ST_035 DB Query Result for 2026-11-07:');
  console.log(dbMovieReport);

  // -------------------------------------------------------------
  // Test ST_038: CSV Export Branding and Code Format
  // -------------------------------------------------------------
  console.log('\n--- Retesting ST_038: CSV Export Branding & Code Format ---');
  const csvRes = await context.request.get('http://localhost:8081/manager/reports/export?reportType=MOVIE&startDate=2026-10-01&endDate=2026-11-07');
  const csvText = await csvRes.text();
  const csvLines = csvText.split('\n').map(l => l.trim()).filter(l => l.length > 0);
  console.log('CSV Line 1 (Header):', csvLines[0]);
  console.log('CSV Line 2 (Metadata):', csvLines[1]);
  console.log('CSV Line 3 (Columns):', csvLines[2]);
  console.log('CSV Sample Data Lines:');
  csvLines.slice(3, 8).forEach(l => console.log('  ', l));
  fs.writeFileSync('e2e/st038_exported_report.csv', csvText, 'utf-8');

  const hasCineoraHeader = csvLines[0].includes('CINEMA REVENUE REPORT - CINEORA');
  const hasOldBrand = csvText.includes('ROYALLY CINEMA');
  const hasFormattedMovieCode = csvLines.slice(3).some(l => l.startsWith('MOV-'));
  console.log('ST_038 Header is CINEORA:', hasCineoraHeader);
  console.log('ST_038 Contains old ROYALLY CINEMA brand:', hasOldBrand);
  console.log('ST_038 Uses MOV-%d format:', hasFormattedMovieCode);

  // -------------------------------------------------------------
  // Test ST_040: Seat Expiration Background Job (JOB-01) Retest
  // -------------------------------------------------------------
  console.log('\n--- Retesting ST_040: Seat Expiration Background Job ---');
  // Check scheduler / job configuration in code
  const pendingBookings = querySql("SELECT BookingID, Status, BookingTime FROM dbo.Booking WHERE Status = 'PENDING';");
  console.log('ST_040 Pending Bookings in DB:');
  console.log(pendingBookings);

  const heldSeatRows = querySql("SELECT BookingSeatID, BookingID, SeatID, Status, CONVERT(varchar, HeldUntil, 120) AS HeldUntil FROM dbo.BookingSeat WHERE Status = 'HELD';");
  console.log('ST_040 Held Seats in DB:');
  console.log(heldSeatRows);

  await browser.close();
  console.log('\n=== COMPLETE RETEST SUITE FINISHED ===');
}

run().catch(err => {
  console.error('Fatal test error:', err);
  process.exit(1);
});
