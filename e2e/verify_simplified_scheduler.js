const { chromium } = require('playwright');
const path = require('path');

(async () => {
    const browser = await chromium.launch({ channel: 'msedge', headless: true });
    const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
    const page = await context.newPage();

    try {
        // 1. Login
        console.log('Navigating to login...');
        await page.goto('http://localhost:8080/login');
        await page.fill('#identifier', 'manager@cinema.com');
        await page.fill('#password', 'Manager123');
        await Promise.all([
            page.waitForURL('**/manager/**'),
            page.click('#submitBtn')
        ]);
        console.log('Logged in successfully!');

        // 2. Scheduler
        await page.goto('http://localhost:8080/manager/showtimes/scheduler?roomId=1');
        await page.waitForLoadState('networkidle');

        // Check room header text
        const roomMetaText = await page.locator('.room-meta-group').textContent();
        console.log('Room meta text:', roomMetaText.replace(/\s+/g, ' ').trim());

        // Check trailer is not present in meta
        const hasTrailerInMeta = roomMetaText.toLowerCase().includes('trailer');
        console.log('Trailer in room meta:', hasTrailerInMeta ? 'FOUND (ERROR)' : 'NOT FOUND (CORRECT)');

        // Select Movie and enter Start Time: 17:00
        await page.selectOption('#drawerMovieSelect', { index: 1 });
        await page.fill('#drawerStartTime', '17:00');
        await page.waitForTimeout(500);

        const filmEnd = await page.locator('#calcFilmEnd').textContent();
        const roomAvail = await page.locator('#calcRoomAvailable').textContent();
        const calcDuration = await page.locator('#calcDuration').textContent();
        console.log(`Auto Calculation -> Duration: ${calcDuration}, Film End: ${filmEnd}, Room Available: ${roomAvail}`);

        // Table headers count
        const thElements = await page.locator('.scheduler-table thead th').allTextContents();
        console.log('Table headers count:', thElements.length, 'Headers:', thElements.map(t => t.trim()));

        // Add slot to table and verify
        await page.click('#btnAddSlot');
        await page.waitForTimeout(500);

        await page.screenshot({
            path: 'C:/Users/hoang/.gemini/antigravity/brain/5d0636ff-cfcb-496a-a383-9acde29618db/browser_screenshots/scheduler_simplified_formula.png',
            fullPage: true
        });

        // 3. Check Seat types modal
        await page.goto('http://localhost:8080/manager/seat-types');
        await page.waitForLoadState('networkidle');
        await page.click('button:has-text("Add seat type")');
        await page.waitForTimeout(500);
        await page.screenshot({
            path: 'C:/Users/hoang/.gemini/antigravity/brain/5d0636ff-cfcb-496a-a383-9acde29618db/browser_screenshots/seat_type_modal_fixed.png'
        });

        console.log('Verification completed successfully!');
    } catch (e) {
        console.error('Test error:', e);
    } finally {
        await browser.close();
    }
})();
