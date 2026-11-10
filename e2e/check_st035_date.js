const { chromium } = require('playwright');
const { querySql } = require('./db');

async function checkDate0711() {
  console.log('=== VERIFYING ST_035 ON DATE 07/11/2026 ===');

  // 1. Direct DB Query
  const sql = `
    SELECT m.MovieID, m.Title,
           COUNT(DISTINCT CASE WHEN CAST(st.StartTime AS DATE) BETWEEN '2026-11-07' AND '2026-11-07' THEN st.ShowtimeID END) AS ShowtimesNov7,
           COUNT(DISTINCT st.ShowtimeID) AS TotalLifetimeShowtimes
    FROM dbo.Movie m
    LEFT JOIN dbo.Showtime st ON m.MovieID = st.MovieID
    GROUP BY m.MovieID, m.Title
    ORDER BY m.MovieID;
  `;
  const dbResult = querySql(sql);
  console.log('SQL Server Results:');
  console.log(dbResult);

  // 2. Query Bambi specifically
  const bambiSql = `
    SELECT m.MovieID, m.Title,
           COUNT(DISTINCT CASE WHEN CAST(st.StartTime AS DATE) BETWEEN '2026-11-07' AND '2026-11-07' THEN st.ShowtimeID END) AS BambiShowtimesNov7,
           COUNT(DISTINCT st.ShowtimeID) AS BambiAllLifetime
    FROM dbo.Movie m
    LEFT JOIN dbo.Showtime st ON m.MovieID = st.MovieID
    WHERE m.Title LIKE '%Bambi%'
    GROUP BY m.MovieID, m.Title;
  `;
  const bambiResult = querySql(bambiSql);
  console.log('\nBambi Specific DB Result:');
  console.log(bambiResult || '(Bambi not found in current database)');

  // 3. Login and Fetch CSV for 07/11/2026
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext();
  const page = await context.newPage();

  await page.goto('http://localhost:8081/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([
    page.waitForURL('http://localhost:8081/**'),
    page.press('#password', 'Enter')
  ]);

  const csvRes = await context.request.get('http://localhost:8081/api/manager/reports/export?preset=custom&startDate=2026-11-07&endDate=2026-11-07&reportType=MOVIE');
  const csvText = await csvRes.text();
  console.log('\nCSV Export for 07/11/2026:');
  console.log(csvText);

  // 4. Also check UI table on 07/11/2026
  await page.goto('http://localhost:8081/manager/reports?preset=custom&startDate=2026-11-07&endDate=2026-11-07&reportType=MOVIE');
  await page.waitForLoadState('networkidle');
  const rows = await page.$$eval('#reportTable tbody tr', trs => trs.map(tr => Array.from(tr.querySelectorAll('td')).map(td => td.textContent.trim())));
  console.log('\nUI Table Rows for 07/11/2026:');
  rows.forEach((r, idx) => console.log(`  Row ${idx + 1}:`, r.join(' | ')));

  await browser.close();
}

checkDate0711().catch(console.error);
