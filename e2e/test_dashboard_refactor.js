const { chromium } = require('./node_modules/playwright');

async function testDashboardRefactor() {
  console.log('=== START EXECUTIVE DASHBOARD REFACTOR E2E VERIFICATION ===');

  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
  const page = await context.newPage();

  // 1. Manager Login
  console.log('1. Logging in as manager@cinema.com...');
  await page.goto('http://localhost:8082/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([
    page.waitForURL('http://localhost:8082/**'),
    page.press('#password', 'Enter')
  ]);
  console.log('Logged in. Current URL:', page.url());

  // 2. Open Executive Dashboard
  console.log('2. Opening /manager...');
  await page.goto('http://localhost:8082/manager');
  await page.waitForTimeout(500);

  // Verify Header
  const title = await page.textContent('h1');
  console.log('Page heading:', title.trim());
  if (!title.includes('Executive Management Dashboard')) {
    throw new Error('Heading does not match Executive Management Dashboard');
  }

  // Verify 4 KPI Cards exist, 2 removed cards do NOT exist
  console.log('3. Verifying 4 KPI Cards...');
  const bodyText = await page.textContent('body');
  const hasTodayRevenue = bodyText.includes("Today's Revenue");
  const hasTicketsSold = bodyText.includes('Tickets Sold');
  const hasTodayShowtimes = bodyText.includes("Today's Showtimes");
  const hasActivePromotions = bodyText.includes('Active Promotions');
  const hasTotalMoviesCard = bodyText.includes('Total Movies');
  const hasTotalGenresCard = bodyText.includes('Total Genres');

  console.log(`- Today's Revenue: ${hasTodayRevenue}`);
  console.log(`- Tickets Sold: ${hasTicketsSold}`);
  console.log(`- Today's Showtimes: ${hasTodayShowtimes}`);
  console.log(`- Active Promotions: ${hasActivePromotions}`);
  console.log(`- Total Movies Card present (should be false): ${hasTotalMoviesCard}`);
  console.log(`- Total Genres Card present (should be false): ${hasTotalGenresCard}`);

  if (!hasTodayRevenue || !hasTicketsSold || !hasTodayShowtimes || !hasActivePromotions) {
    throw new Error('One or more of the 4 required KPI cards is missing!');
  }
  if (hasTotalMoviesCard || hasTotalGenresCard) {
    throw new Error('Total Movies or Total Genres card was not removed!');
  }

  // 4. Test Period selector tabs
  console.log('4. Testing Period Filter Tabs...');
  // Click Today tab
  await page.click("a[href*='period=today']");
  await page.waitForTimeout(500);
  console.log('Period Today URL:', page.url());
  const todayBody = await page.textContent('body');
  console.log('Today revenue period displayed:', todayBody.includes('/ today'));

  // Click This Month tab
  await page.click("a[href*='period=thismonth']");
  await page.waitForTimeout(500);
  console.log('Period This Month URL:', page.url());
  const monthBody = await page.textContent('body');
  console.log('This Month revenue period displayed:', monthBody.includes('/ this month'));

  // Click Last 7 Days tab
  await page.click("a[href*='period=last7days']");
  await page.waitForTimeout(500);
  console.log('Period Last 7 Days URL:', page.url());
  const weekBody = await page.textContent('body');
  console.log('7 Days revenue period displayed:', weekBody.includes('/ 7 days'));

  // 5. Test Quick Management Shortcuts
  console.log('5. Verifying Quick Management Shortcuts...');
  const shortcuts = await page.$$eval("div:has(> h3:has-text('Quick Management Shortcuts')) .grid a", els => els.map(e => e.textContent.trim()));
  console.log('Shortcuts count:', shortcuts.length);
  console.log('Shortcuts text:', shortcuts);
  if (shortcuts.length !== 3) {
    throw new Error(`Expected exactly 3 shortcuts, but found ${shortcuts.length}`);
  }
  const hasMovieShortcut = shortcuts.some(s => s.includes('Movie Management'));
  const hasShowtimeShortcut = shortcuts.some(s => s.includes('Showtime Management'));
  const hasReportsShortcut = shortcuts.some(s => s.includes('Reports & Analytics'));
  const hasGenreShortcut = shortcuts.some(s => s.includes('Genre Management'));

  console.log(`- Movie Management: ${hasMovieShortcut}`);
  console.log(`- Showtime Management: ${hasShowtimeShortcut}`);
  console.log(`- Reports & Analytics: ${hasReportsShortcut}`);
  console.log(`- Genre Management present (should be false): ${hasGenreShortcut}`);

  if (!hasMovieShortcut || !hasShowtimeShortcut || !hasReportsShortcut || hasGenreShortcut) {
    throw new Error('Quick Management Shortcuts do not match requirement!');
  }

  // 6. Test Recent Bookings & Transactions Table
  console.log('6. Verifying Recent Bookings & Transactions Table...');
  const hasViewAllTx = await page.isVisible("a:has-text('View all transactions')");
  console.log('View all transactions link visible:', hasViewAllTx);
  if (!hasViewAllTx) {
    throw new Error('View all transactions link is missing!');
  }

  // Capture Screenshot of refactored dashboard
  await page.screenshot({ path: 'e2e/dashboard_refactored.png', fullPage: true });
  console.log('Full dashboard screenshot saved to e2e/dashboard_refactored.png');

  await browser.close();
  console.log('=== EXECUTIVE DASHBOARD REFACTOR: ALL TESTS VERIFIED PASSED ===');
}

testDashboardRefactor().catch(err => {
  console.error('Test FAILED:', err);
  process.exit(1);
});
