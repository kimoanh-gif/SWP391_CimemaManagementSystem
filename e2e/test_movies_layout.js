const { chromium } = require('./node_modules/playwright');

(async () => {
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext({ viewport: { width: 1366, height: 900 } });
  const page = await context.newPage();
  await page.goto('http://localhost:8082/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([
    page.waitForURL('http://localhost:8082/**'),
    page.press('#password', 'Enter')
  ]);
  await page.goto('http://localhost:8082/manager/movies');
  await page.waitForTimeout(500);

  // Click Standard (7) to trigger switchViewState('normal')
  await page.click("button[data-state-btn='normal']");
  await page.waitForTimeout(500);

  await page.screenshot({ path: 'e2e/manager_movies_fixed2.png', fullPage: true });
  console.log('Full page screenshot saved to e2e/manager_movies_fixed2.png');
  await browser.close();
})();
