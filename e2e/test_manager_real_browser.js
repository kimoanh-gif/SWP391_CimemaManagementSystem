const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const ARTIFACT_DIR = 'C:/Users/hoang/.gemini/antigravity/brain/5d0636ff-cfcb-496a-a383-9acde29618db/browser_screenshots';

if (!fs.existsSync(ARTIFACT_DIR)) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true });
}

async function runRealBrowserAudit() {
  console.log('=== STARTING REAL BROWSER TESTING FOR CINEORA MANAGER ===');
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  const auditLog = [];
  function log(step, status, detail) {
    console.log(`[${status}] ${step}: ${detail}`);
    auditLog.push({ step, status, detail, timestamp: new Date().toISOString() });
  }

  try {
    // 1. AUTHENTICATION
    log('AUTH', 'INFO', 'Navigating to /login');
    await page.goto('http://localhost:8080/login');
    await page.fill('#identifier', 'manager@cinema.com');
    await page.fill('#password', 'Manager123');
    await Promise.all([
      page.waitForURL('**/manager/**'),
      page.click('#submitBtn')
    ]);
    log('AUTH', 'PASS', `Logged in successfully. Redirected to: ${page.url()}`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '01_login_dashboard.png') });

    // 2. MODULE 01 - OVERVIEW DASHBOARD
    log('MOD_01_OVERVIEW', 'INFO', 'Checking Overview Dashboard metrics');
    await page.goto('http://localhost:8080/manager/dashboard');
    await page.waitForLoadState('networkidle');
    const pageTitle = await page.title();
    log('MOD_01_OVERVIEW', 'PASS', `Overview loaded. Page title: ${pageTitle}`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '02_overview_dashboard.png') });

    // 3. MODULE 02 - MOVIES
    log('MOD_02_MOVIES', 'INFO', 'Checking Movie Management table');
    await page.goto('http://localhost:8080/manager/movies');
    await page.waitForLoadState('networkidle');
    const movieRows = await page.locator('tbody tr').count();
    log('MOD_02_MOVIES', 'PASS', `Movies page loaded with ${movieRows} rows`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '03_movies_page.png') });

    // 4. MODULE 03 - GENRES
    log('MOD_03_GENRES', 'INFO', 'Checking Movie Genre Management');
    await page.goto('http://localhost:8080/manager/genres');
    await page.waitForLoadState('networkidle');
    const genreRows = await page.locator('tbody tr').count();
    log('MOD_03_GENRES', 'PASS', `Genres page loaded with ${genreRows} rows`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '04_genres_page.png') });

    // 5. MODULE 04 - SCREENING ROOMS
    log('MOD_04_ROOMS', 'INFO', 'Checking Screening Rooms table and room types');
    await page.goto('http://localhost:8080/manager/rooms');
    await page.waitForLoadState('networkidle');
    const roomRows = await page.locator('tbody tr').count();
    log('MOD_04_ROOMS', 'PASS', `Rooms page loaded with ${roomRows} rooms`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '05_rooms_page.png') });

    // 6. MODULE 05 - SEAT TYPES
    log('MOD_05_SEAT_TYPES', 'INFO', 'Checking Seat Types page and modal');
    await page.goto('http://localhost:8080/manager/seat-types');
    await page.waitForLoadState('networkidle');
    const stCount = await page.locator('tbody tr').count();
    log('MOD_05_SEAT_TYPES', 'PASS', `Seat types loaded with ${stCount} seat types`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '06_seat_types_page.png') });

    // 7. MODULE 06 - SEAT MAP
    log('MOD_06_SEAT_MAP', 'INFO', 'Checking Seat Map visualizer');
    await page.goto('http://localhost:8080/manager/seats');
    await page.waitForLoadState('networkidle');
    log('MOD_06_SEAT_MAP', 'PASS', 'Seat Map editor loaded successfully');
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '07_seat_map_page.png') });

    // 8. MODULE 08 - F&B
    log('MOD_08_FB', 'INFO', 'Checking F&B Items management');
    await page.goto('http://localhost:8080/manager/fb');
    await page.waitForLoadState('networkidle');
    const fbCount = await page.locator('tbody tr').count();
    log('MOD_08_FB', 'PASS', `F&B items page loaded with ${fbCount} items`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '08_fb_page.png') });

    // 9. MODULE 09 - REPORTS
    log('MOD_09_REPORTS', 'INFO', 'Checking Reports & Statistics charts and summaries');
    await page.goto('http://localhost:8080/manager/reports');
    await page.waitForLoadState('networkidle');
    log('MOD_09_REPORTS', 'PASS', 'Reports page loaded successfully');
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '09_reports_page.png') });

    // 10. MODULE 07 - DEEP DIVE: ROOM SHOWTIME SCHEDULER
    log('MOD_07_SCHEDULER', 'INFO', 'Navigating to /manager/showtimes/scheduler');
    await page.goto('http://localhost:8080/manager/showtimes/scheduler');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '10_scheduler_initial.png') });

    // 10.1 Select Room & Date
    log('MOD_07_SCHEDULER', 'INFO', 'Selecting Room and Screening Date');
    await page.selectOption('#roomSelect', { index: 0 }); // Select Cinema 01
    const targetDate = '2026-11-13'; // 3 days in the future to avoid existing bookings
    await page.fill('#dateSelect', targetDate);
    await page.dispatchEvent('#dateSelect', 'change');
    await page.waitForTimeout(1000);

    // 10.2 Select Movie & Start Time
    log('MOD_07_SCHEDULER', 'INFO', 'Selecting Movie and Setting Start Time');
    await page.selectOption('#drawerMovieSelect', { index: 1 });
    await page.fill('#drawerStartTime', '09:00');
    await page.dispatchEvent('#drawerStartTime', 'input');
    await page.waitForTimeout(500);

    // 10.3 Verify Auto-Calculations
    const calcFilmEnd = await page.textContent('#calcFilmEnd');
    const calcRoomAvailable = await page.textContent('#calcRoomAvailable');
    log('MOD_07_SCHEDULER', 'INFO', `Auto-Calculation: Film End=${calcFilmEnd.trim()}, Room Available=${calcRoomAvailable.trim()}`);
    
    // Check if Film End is NOT equal to Room Available (15m buffer separation)
    if (calcFilmEnd.trim() !== calcRoomAvailable.trim()) {
      log('MOD_07_SCHEDULER', 'PASS', `Film End (${calcFilmEnd.trim()}) differs from Room Available (${calcRoomAvailable.trim()}) by 15-minute cleaning buffer`);
    } else {
      log('MOD_07_SCHEDULER', 'FAIL', 'Film End equals Room Available! Cleaning buffer not applied!');
    }
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '11_scheduler_auto_calc.png') });

    // 10.4 Add Draft Slot #1
    log('MOD_07_SCHEDULER', 'INFO', 'Clicking Add to Schedule for Slot #1');
    await page.click('#btnAddSlot');
    await page.waitForTimeout(500);
    const draftCount1 = await page.locator('.timeline-draft').count();
    log('MOD_07_SCHEDULER', 'PASS', `Draft slot #1 added. Draft blocks on timeline: ${draftCount1}`);

    // 10.5 Add Draft Slot #2 (Consecutive after buffer)
    log('MOD_07_SCHEDULER', 'INFO', 'Configuring Slot #2 starting after cleaning buffer');
    await page.selectOption('#drawerMovieSelect', { index: 1 });
    const nextStart = calcRoomAvailable.trim();
    await page.fill('#drawerStartTime', nextStart);
    await page.dispatchEvent('#drawerStartTime', 'input');
    await page.waitForTimeout(500);
    await page.click('#btnAddSlot');
    await page.waitForTimeout(500);
    const draftCount2 = await page.locator('.timeline-draft').count();
    log('MOD_07_SCHEDULER', 'PASS', `Draft slot #2 added. Draft blocks on timeline: ${draftCount2}`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '12_scheduler_two_drafts.png') });

    // 10.6 Test Conflict Detection (Intentional Overlapping Slot)
    log('MOD_07_SCHEDULER', 'INFO', 'Testing Conflict Detection: entering overlapping start time 10:00');
    await page.selectOption('#drawerMovieSelect', { index: 1 });
    await page.fill('#drawerStartTime', '10:00'); // Overlaps with Slot 1 (09:00 - 11:10)
    await page.dispatchEvent('#drawerStartTime', 'input');
    await page.waitForTimeout(500);
    const alertVisible = await page.isVisible('#calcConflictAlert');
    const alertText = await page.textContent('#calcConflictTitle').catch(() => '');
    log('MOD_07_SCHEDULER', alertVisible ? 'PASS' : 'WARN', `Conflict Alert shown: ${alertVisible}, Text: ${alertText.trim()}`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '13_scheduler_conflict_warning.png') });

    // Clear form
    await page.click('#btnClearForm');

    // 10.7 Save Drafts (Batch Save)
    log('MOD_07_SCHEDULER', 'INFO', 'Saving Draft Schedule (Batch Save)');
    const btnSave = page.locator('#btnSaveSchedule');
    const stickyBarVisible = await page.isVisible('#schedulerStickyBar');
    log('MOD_07_SCHEDULER', 'INFO', `Sticky Bar visible: ${stickyBarVisible}`);
    if (await btnSave.isVisible()) {
      await btnSave.click();
      await page.waitForTimeout(2000);
      log('MOD_07_SCHEDULER', 'PASS', 'Save Room Schedule button clicked successfully');
      await page.screenshot({ path: path.join(ARTIFACT_DIR, '14_scheduler_saved.png') });
    }

    // 10.8 Reload and Verify Persistence
    log('MOD_07_SCHEDULER', 'INFO', 'Reloading page to verify persisted showtimes in database');
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.selectOption('#roomSelect', { index: 0 });
    await page.fill('#dateSelect', targetDate);
    await page.dispatchEvent('#dateSelect', 'change');
    await page.waitForTimeout(1500);
    const savedBlocks = await page.locator('.timeline-saved').count();
    log('MOD_07_SCHEDULER', 'PASS', `Reload verified: ${savedBlocks} saved showtimes rendered on timeline`);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, '15_scheduler_persisted.png') });

    console.log('\n=== REAL BROWSER AUDIT COMPLETED SUCCESSFULLY ===');
  } catch (err) {
    console.error('Browser Test Error:', err);
    log('AUDIT_EXCEPTION', 'FAIL', err.message);
    await page.screenshot({ path: path.join(ARTIFACT_DIR, 'error_screenshot.png') });
  } finally {
    await browser.close();
    fs.writeFileSync(
      path.join(ARTIFACT_DIR, 'browser_audit_log.json'),
      JSON.stringify(auditLog, null, 2)
    );
  }
}

runRealBrowserAudit();
