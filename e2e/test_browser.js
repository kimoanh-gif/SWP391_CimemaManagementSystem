const { chromium } = require('playwright');

(async () => {
  console.log('Testing Playwright browser launch with system browser...');
  const browser = await chromium.launch({
    headless: true,
    channel: 'msedge' // or chrome
  });
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto('http://localhost:8082/');
  console.log('Page Title:', await page.title());
  await browser.close();
  console.log('Browser test successful!');
})();
