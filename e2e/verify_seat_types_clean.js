const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });

    await page.goto('http://localhost:8080/login');
    await page.fill('#identifier', 'manager@cinema.com');
    await page.fill('#password', 'Manager123');
    await Promise.all([
        page.waitForURL('**/manager/**'),
        page.click('#submitBtn')
    ]);

    await page.goto('http://localhost:8080/manager/seat-types');
    await page.waitForLoadState('networkidle');

    const editMultCount = await page.locator('.btn-edit-multiplier').count();
    console.log('Edit multiplier buttons count:', editMultCount);

    const multBadges = await page.locator('.multiplier-badge').allTextContents();
    console.log('Multiplier badges found:', multBadges);

    await page.screenshot({
        path: 'C:/Users/hoang/.gemini/antigravity/brain/5d0636ff-cfcb-496a-a383-9acde29618db/browser_screenshots/seat_types_clean_multiplier.png',
        fullPage: true
    });

    console.log('Verification completed successfully!');
    await browser.close();
})();

