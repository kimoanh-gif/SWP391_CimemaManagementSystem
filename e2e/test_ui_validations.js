const { chromium } = require('playwright');

async function testUIValidations() {
  console.log('=== START UI VALIDATIONS TEST ===');
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Login
  await page.goto('http://localhost:8080/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([
    page.waitForURL('http://localhost:8080/**'),
    page.press('#password', 'Enter')
  ]);
  console.log('Logged in. Current URL:', page.url());

  // 1. ST_017A Subtest 1: Multiplier 0.40
  console.log('\n--- Testing ST_017A Subtest 1: Multiplier 0.40 ---');
  await page.goto('http://localhost:8080/manager/seat-types');
  await page.evaluate(() => {
    const form = document.querySelector('#seatTypeForm');
    form.noValidate = true;
    document.querySelector('#typeCode').value = 'TEST_LOW';
    document.querySelector('#typeName').value = 'Test Low Multiplier';
    document.querySelector('#priceMultiplier').value = '0.40';
    document.querySelector('#colorCode').value = '#3B82F6';
    form.submit();
  });
  await page.waitForLoadState('networkidle');
  const alert17A1 = await page.textContent('.alert-error .alert-body').catch(() => null);
  console.log('ST_017A.1 Alert Message:', alert17A1 ? alert17A1.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017a_multiplier_low.png', fullPage: true });

  // 2. ST_017A Subtest 2: Multiplier 3.50
  console.log('\n--- Testing ST_017A Subtest 2: Multiplier 3.50 ---');
  await page.goto('http://localhost:8080/manager/seat-types');
  await page.evaluate(() => {
    const form = document.querySelector('#seatTypeForm');
    form.noValidate = true;
    document.querySelector('#typeCode').value = 'TEST_HIGH';
    document.querySelector('#typeName').value = 'Test High Multiplier';
    document.querySelector('#priceMultiplier').value = '3.50';
    document.querySelector('#colorCode').value = '#3B82F6';
    form.submit();
  });
  await page.waitForLoadState('networkidle');
  const alert17A2 = await page.textContent('.alert-error .alert-body').catch(() => null);
  console.log('ST_017A.2 Alert Message:', alert17A2 ? alert17A2.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017a_multiplier_high.png', fullPage: true });

  // 3. ST_017A Subtest 3: Color "blue"
  console.log('\n--- Testing ST_017A Subtest 3: Color "blue" ---');
  await page.goto('http://localhost:8080/manager/seat-types');
  await page.evaluate(() => {
    const form = document.querySelector('#seatTypeForm');
    form.noValidate = true;
    document.querySelector('#typeCode').value = 'TEST_BLUE';
    document.querySelector('#typeName').value = 'Test Invalid Color';
    document.querySelector('#priceMultiplier').value = '1.20';
    // Change input type to text to set non-hex value 'blue'
    const colorInput = document.querySelector('#colorCode');
    colorInput.type = 'text';
    colorInput.value = 'blue';
    form.submit();
  });
  await page.waitForLoadState('networkidle');
  const alert17A3 = await page.textContent('.alert-error .alert-body').catch(() => null);
  console.log('ST_017A.3 Alert Message:', alert17A3 ? alert17A3.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017a_color_invalid.png', fullPage: true });

  // 4. ST_017B: Base Price = -1
  console.log('\n--- Testing ST_017B: Base Price = -1 ---');
  await page.goto('http://localhost:8080/manager/pricing');
  await page.evaluate(() => {
    const form = document.querySelector('#pricingModal form');
    form.noValidate = true;
    document.querySelector('#formPricingCode').value = 'TEST_NEG_BASE';
    document.querySelector('#formRuleName').value = 'Test Negative Base Price';
    document.querySelector('#formBasePrice').value = '-1';
    document.querySelector('#formFormatSurcharge').value = '0';
    form.submit();
  });
  await page.waitForLoadState('networkidle');
  const alert17B = await page.textContent('.alert-error .alert-body').catch(() => null);
  console.log('ST_017B Alert Message:', alert17B ? alert17B.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017b_negative_base_price.png', fullPage: true });

  // 5. ST_017C: Cost Price > Selling Price
  console.log('\n--- Testing ST_017C: Cost Price 60,000 > Selling Price 50,000 ---');
  await page.goto('http://localhost:8080/manager/fb');
  await page.evaluate(() => {
    const form = document.querySelector('#fbModal form, #fbForm, form[action*="/manager/fb/save"]');
    form.noValidate = true;
    const code = form.querySelector('[name="itemCode"]') || document.querySelector('#itemCode');
    if (code) code.value = 'TEST_FB_COST';
    const name = form.querySelector('[name="itemName"]') || document.querySelector('#itemName');
    if (name) name.value = 'Test Popcorn High Cost';
    const sell = form.querySelector('[name="sellingPrice"]') || document.querySelector('#sellingPrice');
    if (sell) sell.value = '50000';
    const cost = form.querySelector('[name="costPrice"]') || document.querySelector('#costPrice');
    if (cost) cost.value = '60000';
    form.submit();
  });
  await page.waitForLoadState('networkidle');
  const alert17C = await page.textContent('.alert-error .alert-body').catch(() => null);
  console.log('ST_017C Alert Message:', alert17C ? alert17C.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st017c_cost_exceeds_selling.png', fullPage: true });

  // 6. ST_025: Showtime scheduling before movie release date
  console.log('\n--- Testing ST_025: Showtime before movie release date ---');
  await page.goto('http://localhost:8080/manager/showtimes?date=2026-11-09');
  await page.evaluate(() => {
    const form = document.querySelector('#showtimeModal form, form[action*="/manager/showtimes/save"]');
    form.noValidate = true;
    const movieSel = form.querySelector('[name="movieId"]') || document.querySelector('#formMovieId');
    if (movieSel) movieSel.value = '15'; // Movie 'hay' (ReleaseDate = 2026-11-11)
    const roomSel = form.querySelector('[name="roomId"]') || document.querySelector('#formRoomId');
    if (roomSel) roomSel.value = '1';
    const start = form.querySelector('[name="startTime"]') || document.querySelector('#formStartTime');
    if (start) start.value = '2026-11-09T10:00';
    const price = form.querySelector('[name="baseTicketPrice"]') || document.querySelector('#formBaseTicketPrice');
    if (price) price.value = '85000';
    form.submit();
  });
  await page.waitForLoadState('networkidle');
  const alert25 = await page.textContent('.alert-error .alert-body').catch(() => null);
  console.log('ST_025 Alert Message:', alert25 ? alert25.trim() : 'NONE');
  await page.screenshot({ path: 'e2e/st025_showtime_before_release.png', fullPage: true });

  // 7. ST_035: Movie Report Date Range 07/11/2026
  console.log('\n--- Testing ST_035: Movie Report on 07/11/2026 ---');
  await page.goto('http://localhost:8080/manager/reports?reportType=MOVIE&startDate=2026-11-07&endDate=2026-11-07');
  await page.waitForLoadState('networkidle');
  await page.screenshot({ path: 'e2e/st035_movie_report_20261107.png', fullPage: true });

  // 8. ST_038: CSV Export via API
  console.log('\n--- Testing ST_038: CSV Export ---');
  const csvRes = await context.request.get('http://localhost:8080/api/manager/reports/export?preset=custom&startDate=2026-10-01&endDate=2026-11-07&reportType=MOVIE');
  const csvContent = await csvRes.text();
  const fs = require('fs');
  fs.writeFileSync('e2e/st038_exported_report.csv', csvContent, 'utf-8');
  console.log('CSV downloaded successfully! Lines:');
  csvContent.split('\n').slice(0, 6).forEach((line, i) => console.log(`  Line ${i + 1}: ${line.trim()}`));

  await browser.close();
  console.log('\n=== ALL UI VALIDATIONS COMPLETED ===');
}

testUIValidations().catch(err => {
  console.error('Error in UI validation tests:', err);
  process.exit(1);
});
