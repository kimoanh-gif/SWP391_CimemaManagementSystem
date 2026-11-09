const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:8080/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([
    page.waitForURL('**/manager/**'),
    page.click('#submitBtn')
  ]);

  await page.goto('http://localhost:8080/manager/showtimes/scheduler');
  await page.waitForLoadState('networkidle');
  await page.selectOption('#roomSelect', { index: 0 });
  await page.fill('#dateSelect', '2026-11-13');
  await page.dispatchEvent('#dateSelect', 'change');
  await page.waitForTimeout(1500);

  const savedBlocks = await page.locator('.timeline-slot-block.is-saved').count();
  console.log('Saved blocks on timeline with .timeline-slot-block.is-saved:', savedBlocks);

  const cleaningBlocks = await page.locator('.timeline-cleaning-block').count();
  console.log('Cleaning buffer blocks with .timeline-cleaning-block:', cleaningBlocks);

  const tableRows = await page.locator('#scheduleSlotsBody tr').count();
  console.log('Schedule Slots table rows:', tableRows);

  await browser.close();
})();

