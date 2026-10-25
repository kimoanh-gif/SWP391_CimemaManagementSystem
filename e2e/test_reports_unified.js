const { chromium } = require('./node_modules/playwright');

(async () => {
    console.log('=== START REPORTS & ANALYTICS UNIFIED MODULE E2E VERIFICATION ===');
    const browser = await chromium.launch({ headless: true, channel: 'msedge' });
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
    const page = await context.newPage();

    console.log('--- 1. Login as Manager ---');
    await page.goto('http://localhost:8082/login');
    await page.fill('#identifier', 'manager@cinema.com');
    await page.fill('#password', 'Manager123');
    await Promise.all([
        page.waitForURL('http://localhost:8082/**'),
        page.press('#password', 'Enter')
    ]);
    console.log('Current URL after login:', page.url());

    console.log('--- 2. Navigate to Reports & Analytics ---');
    await page.goto('http://localhost:8082/manager/reports', { waitUntil: 'networkidle' });

    // Verify Title
    const title = await page.locator('h1').first().innerText();
    console.log('Page Title:', title);
    if (!title.includes('Reports & Analytics')) {
        throw new Error('Expected title Reports & Analytics, got: ' + title);
    }

    // Verify Shared KPI Cards
    const pageText = await page.content();
    console.log('Checking KPI values on page...');
    const hasTotalRev = pageText.includes('816,000') || pageText.includes('816.000');
    console.log('Has 816,000 VND in KPI / Ledger:', hasTotalRev);
    if (!hasTotalRev) {
        throw new Error('Expected 816,000 VND in report KPI or table!');
    }

    // Check Tab 1: Revenue Overview is active
    const activeTab1 = await page.locator('a.tab-link:has-text("Revenue Overview")').getAttribute('class');
    console.log('Tab 1 active styling has border-cinema-red:', activeTab1.includes('border-cinema-red'));

    // Capture screenshot of Tab 1
    await page.screenshot({ path: 'e2e/reports_unified_tab1.png', fullPage: true });
    console.log('Saved Tab 1 screenshot: e2e/reports_unified_tab1.png');

    console.log('--- 3. Click Tab 2: Movie Performance ---');
    await Promise.all([
        page.waitForNavigation({ waitUntil: 'networkidle' }),
        page.click('a.tab-link:has-text("Movie Performance")')
    ]);
    console.log('Current URL on Tab 2:', page.url());
    if (!page.url().includes('tab=movies')) {
        throw new Error('URL should include tab=movies, got: ' + page.url());
    }

    // Check Movie Table content
    const movieText = await page.content();
    const hasAvatar = movieText.includes('Avatar: The Way of Water') || movieText.includes('Avatar');
    console.log('Has Avatar in movie table:', hasAvatar);

    // Verify Avatar Total Revenue is 816,000 (and NOT 1,632,000!)
    const has1632 = movieText.includes('1,632,000') || movieText.includes('1.632.000');
    console.log('Check duplicate join bug (1,632,000 VND present?):', has1632);
    if (has1632) {
        throw new Error('FAILURE: Cartesian duplicate join bug detected! 1,632,000 VND found in movie performance table.');
    }

    // Capture screenshot of Tab 2
    await page.screenshot({ path: 'e2e/reports_unified_tab2.png', fullPage: true });
    console.log('Saved Tab 2 screenshot: e2e/reports_unified_tab2.png');

    console.log('--- 4. Click Tab 3: Transaction Ledger ---');
    await Promise.all([
        page.waitForNavigation({ waitUntil: 'networkidle' }),
        page.click('a.tab-link:has-text("Transaction Ledger")')
    ]);
    console.log('Current URL on Tab 3:', page.url());
    if (!page.url().includes('tab=transactions')) {
        throw new Error('URL should include tab=transactions, got: ' + page.url());
    }

    // Check Transactions Table content
    const txText = await page.content();
    const hasBK = txText.includes('BK261021110343E937F2') || txText.includes('BK261021');
    console.log('Has Booking codes in transaction ledger:', hasBK);

    // Capture screenshot of Tab 3
    await page.screenshot({ path: 'e2e/reports_unified_tab3.png', fullPage: true });
    console.log('Saved Tab 3 screenshot: e2e/reports_unified_tab3.png');

    console.log('--- 5. Test Period Filter: Click "This Month" or "Today" ---');
    const todayBtn = page.locator('button.preset-btn[data-preset="today"]');
    if (await todayBtn.count() > 0) {
        await Promise.all([
            page.waitForNavigation({ waitUntil: 'networkidle' }),
            todayBtn.click()
        ]);
        console.log('URL after clicking Today:', page.url());
        if (!page.url().includes('tab=transactions')) {
            throw new Error('Active tab should be preserved as transactions after clicking preset!');
        }
        if (!page.url().includes('preset=today')) {
            throw new Error('Preset should be today in URL!');
        }
        console.log('SUCCESS: Preset filter successfully preserved active tab!');
    }

    console.log('--- 6. Test Compatibility Redirects ---');
    await page.goto('http://localhost:8082/manager/reports/movies?preset=thismonth', { waitUntil: 'networkidle' });
    console.log('Redirected URL from /manager/reports/movies:', page.url());
    if (!page.url().includes('/manager/reports?tab=movies')) {
        throw new Error('Compatibility redirect failed! Expected /manager/reports?tab=movies, got: ' + page.url());
    }

    await page.goto('http://localhost:8082/manager/reports/transactions?preset=thismonth', { waitUntil: 'networkidle' });
    console.log('Redirected URL from /manager/reports/transactions:', page.url());
    if (!page.url().includes('/manager/reports?tab=transactions')) {
        throw new Error('Compatibility redirect failed! Expected /manager/reports?tab=transactions, got: ' + page.url());
    }

    console.log('=== ALL E2E BROWSER VALIDATIONS PASSED ===');
    await browser.close();
})().catch(err => {
    console.error('E2E TEST ERROR:', err);
    process.exit(1);
});
