const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const page = await browser.newPage();
  
  await page.goto('http://localhost:8081/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([
    page.waitForURL('**/manager/**'),
    page.click('#submitBtn')
  ]);

  await page.goto('http://localhost:8081/manager/rooms');
  await page.waitForLoadState('networkidle');
  const roomCards = await page.locator('.room-card').count();
  console.log('Room cards count:', roomCards);

  await page.goto('http://localhost:8081/manager/fb');
  await page.waitForLoadState('networkidle');
  const fbCards = await page.locator('.fb-card').count();
  console.log('F&B cards count:', fbCards);

  await browser.close();
})();

