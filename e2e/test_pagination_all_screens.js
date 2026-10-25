const { chromium } = require('./node_modules/playwright');

(async () => {
    console.log('=== START PAGINATION E2E VERIFICATION FOR ALL 5 SCREENS ===');
    const browser = await chromium.launch({ headless: true, channel: 'msedge' });
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const page = await context.newPage();

    // 1. Login as Manager
    console.log('--- 1. Login as Manager ---');
    await page.goto('http://localhost:8082/login');
    await page.fill('#identifier', 'manager@cinema.com');
    await page.fill('#password', 'Manager123');
    await Promise.all([
        page.waitForURL('http://localhost:8082/**'),
        page.press('#password', 'Enter')
    ]);
    console.log('Logged in. URL:', page.url());

    // 2. Screen 1: Film Genres (/manager/genres)
    console.log('--- 2. Testing Screen 1: Film Genres ---');
    await page.goto('http://localhost:8082/manager/genres', { waitUntil: 'networkidle' });
    const genrePagination = page.locator('#genrePagination');
    await genrePagination.waitFor({ state: 'visible', timeout: 5000 });
    const genreText = await genrePagination.innerText();
    console.log('Genres Pagination Text:', genreText.replace(/\s+/g, ' '));
    if (!genreText.includes('Showing 1 – 8 of')) {
        throw new Error('Expected "Showing 1 – 8 of" on genres screen, got: ' + genreText);
    }
    // Click page 2
    await genrePagination.locator('button[data-page="2"]').click();
    await page.waitForTimeout(300);
    const genrePage2Text = await genrePagination.innerText();
    console.log('Genres Page 2 Text:', genrePage2Text.replace(/\s+/g, ' '));
    if (!genrePage2Text.includes('Showing 9 –')) {
        throw new Error('Expected "Showing 9 –" on genres page 2, got: ' + genrePage2Text);
    }
    await page.screenshot({ path: 'e2e/pagination_genres.png', fullPage: true });
    console.log('Saved: e2e/pagination_genres.png');

    // 3. Screen 2: Screening Rooms (/manager/rooms)
    console.log('--- 3. Testing Screen 2: Screening Rooms ---');
    await page.goto('http://localhost:8082/manager/rooms', { waitUntil: 'networkidle' });
    const roomPagination = page.locator('#roomPagination');
    await roomPagination.waitFor({ state: 'visible', timeout: 5000 });
    const roomText = await roomPagination.innerText();
    console.log('Rooms Pagination Text:', roomText.replace(/\s+/g, ' '));
    if (!roomText.includes('Showing 1 – 4 of 5')) {
        throw new Error('Expected "Showing 1 – 4 of 5" on rooms screen, got: ' + roomText);
    }
    // Click page 2
    await roomPagination.locator('button[data-page="2"]').click();
    await page.waitForTimeout(300);
    const roomPage2Text = await roomPagination.innerText();
    console.log('Rooms Page 2 Text:', roomPage2Text.replace(/\s+/g, ' '));
    if (!roomPage2Text.includes('Showing 5 – 5 of 5')) {
        throw new Error('Expected "Showing 5 – 5 of 5" on rooms page 2, got: ' + roomPage2Text);
    }
    await page.screenshot({ path: 'e2e/pagination_rooms.png', fullPage: true });
    console.log('Saved: e2e/pagination_rooms.png');

    // 4. Screen 3: Seat Types Management (/manager/seat-types)
    console.log('--- 4. Testing Screen 3: Seat Types ---');
    await page.goto('http://localhost:8082/manager/seat-types', { waitUntil: 'networkidle' });
    const seatPagination = page.locator('#seatTypePagination');
    await seatPagination.waitFor({ state: 'visible', timeout: 5000 });
    const seatText = await seatPagination.innerText();
    console.log('Seat Types Pagination Text:', seatText.replace(/\s+/g, ' '));
    if (!seatText.includes('Showing 1 – 4 of 5')) {
        throw new Error('Expected "Showing 1 – 4 of 5" on seat types screen, got: ' + seatText);
    }
    // Click page 2
    await seatPagination.locator('button[data-page="2"]').click();
    await page.waitForTimeout(300);
    const seatPage2Text = await seatPagination.innerText();
    console.log('Seat Types Page 2 Text:', seatPage2Text.replace(/\s+/g, ' '));
    if (!seatPage2Text.includes('Showing 5 – 5 of 5')) {
        throw new Error('Expected "Showing 5 – 5 of 5" on seat types page 2, got: ' + seatPage2Text);
    }
    await page.screenshot({ path: 'e2e/pagination_seat_types.png', fullPage: true });
    console.log('Saved: e2e/pagination_seat_types.png');

    // 5. Screen 4: F&B Concessions (/manager/fb)
    console.log('--- 5. Testing Screen 4: F&B Concessions ---');
    await page.goto('http://localhost:8082/manager/fb', { waitUntil: 'networkidle' });
    const fbPagination = page.locator('#fbPagination');
    await fbPagination.waitFor({ state: 'visible', timeout: 5000 });
    const fbText = await fbPagination.innerText();
    console.log('F&B Pagination Text:', fbText.replace(/\s+/g, ' '));
    if (!fbText.includes('Showing 1 – 4 of 8')) {
        throw new Error('Expected "Showing 1 – 4 of 8" on F&B screen, got: ' + fbText);
    }
    // Click page 2
    await fbPagination.locator('button[data-page="2"]').click();
    await page.waitForTimeout(300);
    const fbPage2Text = await fbPagination.innerText();
    console.log('F&B Page 2 Text:', fbPage2Text.replace(/\s+/g, ' '));
    if (!fbPage2Text.includes('Showing 5 – 8 of 8')) {
        throw new Error('Expected "Showing 5 – 8 of 8" on F&B page 2, got: ' + fbPage2Text);
    }
    await page.screenshot({ path: 'e2e/pagination_fb.png', fullPage: true });
    console.log('Saved: e2e/pagination_fb.png');

    // 6. Screen 5: Reports & Analytics (/manager/reports)
    console.log('--- 6. Testing Screen 5: Reports & Analytics ---');
    await page.goto('http://localhost:8082/manager/reports', { waitUntil: 'networkidle' });
    const dailyPagination = page.locator('#dailyPagination');
    await dailyPagination.waitFor({ state: 'visible', timeout: 5000 });
    const dailyText = await dailyPagination.innerText();
    console.log('Daily Reports Pagination Text:', dailyText.replace(/\s+/g, ' '));
    await page.screenshot({ path: 'e2e/pagination_reports.png', fullPage: true });
    console.log('Saved: e2e/pagination_reports.png');

    console.log('=== ALL 5 SCREENS PASSED PAGINATION VERIFICATION! ===');
    await browser.close();
})().catch(err => {
    console.error('PAGINATION TEST FAILED:', err);
    process.exit(1);
});
