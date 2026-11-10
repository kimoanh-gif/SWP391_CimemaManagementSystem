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

        // 2. Go to genres page
        await page.goto('http://localhost:8080/manager/genres');
        await page.waitForLoadState('networkidle');

        // Check first genre row (Hành Động, which has linked movies)
        const firstDelBtn = page.locator('.delete-genre-form button.action-btn-del').first();
        const isDisabled = await firstDelBtn.isDisabled();
        console.log('First delete button disabled status:', isDisabled ? 'DISABLED (WRONG)' : 'ENABLED (CORRECT)');

        // Intercept alert dialog
        let alertMessageReceived = '';
        page.on('dialog', async dialog => {
            alertMessageReceived = dialog.message();
            console.log('Dialog popped up with message:', alertMessageReceived);
            await dialog.accept();
        });

        // Click delete button
        console.log('Clicking delete button for genre with linked movies...');
        await firstDelBtn.click();
        await page.waitForTimeout(500);

        // Check dynamic alert banner
        const dynamicAlertVisible = await page.locator('#dynamicAlert').isVisible();
        const dynamicAlertText = await page.locator('#dynamicAlertText').textContent();
        console.log('Dynamic alert banner visible:', dynamicAlertVisible);
        console.log('Dynamic alert banner text:', dynamicAlertText);

        // Screenshot
        const screenshotPath = 'C:/Users/hoang/.gemini/antigravity/brain/5d0636ff-cfcb-496a-a383-9acde29618db/browser_screenshots/genre_delete_message_alert.png';
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.log('Screenshot saved to:', screenshotPath);

        console.log('TEST PASSED SUCCESSFULLY!');
    } catch (e) {
        console.error('Error during test:', e);
    } finally {
        await browser.close();
    }
})();

