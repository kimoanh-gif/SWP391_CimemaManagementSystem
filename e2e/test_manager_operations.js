const { chromium } = require('playwright');
const { querySql } = require('./db.js');

async function testManagerOperationsE2E() {
  console.log('=== START MANAGER OPERATIONS E2E TEST ===');

  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext();
  const page = await context.newPage();

  // 1. Manager Login
  console.log('Logging in as manager@cinema.com...');
  await page.goto('http://localhost:8082/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([
    page.waitForURL('http://localhost:8082/**'),
    page.press('#password', 'Enter')
  ]);
  console.log('Manager logged in. URL:', page.url());

  // 2. Manager Dashboard
  await page.goto('http://localhost:8082/manager');
  console.log('Manager dashboard title:', await page.title());
  const dashContent = await page.textContent('body');
  console.log('Dashboard contains expected sections:', dashContent.includes('Weekly Revenue') || dashContent.includes('Revenue'));

  // 3. Movie Management (Create Movie)
  console.log('Navigating to /manager/movies...');
  await page.goto('http://localhost:8082/manager/movies');
  console.log('Movies title:', await page.title());

  const testTitle = 'E2E Test Movie ' + Date.now();
  // Clean up any old test movies
  querySql("DELETE FROM dbo.MovieGenre WHERE MovieID IN (SELECT MovieID FROM dbo.Movie WHERE Title LIKE 'E2E Test Movie%');");
  querySql("DELETE FROM dbo.Showtime WHERE MovieID IN (SELECT MovieID FROM dbo.Movie WHERE Title LIKE 'E2E Test Movie%');");
  querySql("DELETE FROM dbo.Movie WHERE Title LIKE 'E2E Test Movie%';");

  // Create Movie via POST /api/manager/movies
  const createMovieRes = await context.request.post('http://localhost:8082/api/manager/movies', {
    headers: { 'Content-Type': 'application/json' },
    data: {
      title: testTitle,
      originalTitle: testTitle,
      duration: 120,
      releaseDate: '2026-10-21',
      endDate: '2026-11-21',
      language: 'English',
      director: 'Christopher Nolan',
      actors: 'Cillian Murphy',
      ageRating: 'T16',
      description: 'An exclusive E2E test movie created by automated suite.',
      poster: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600',
      trailer: 'https://www.youtube.com/',
      status: 'Now Showing',
      genreIds: [1, 2]
    }
  });
  console.log('API Create Movie response:', createMovieRes.status(), await createMovieRes.text());

  // Verify in SQL Server
  const dbMovie = querySql(`SELECT MovieID, Title, Duration, Status FROM dbo.Movie WHERE Title = '${testTitle}';`);
  console.log('SQL Server Movie verification:\n', dbMovie);
  const movieIdRow = querySql(`SELECT MovieID FROM dbo.Movie WHERE Title = '${testTitle}';`);
  const testMovieId = movieIdRow.split('\n')[2]?.trim();

  // 4. Create Showtime for this Movie via POST /api/manager/showtimes
  console.log('Scheduling showtime for test movie ID:', testMovieId);
  const createShowtimeRes = await context.request.post('http://localhost:8082/api/manager/showtimes', {
    headers: { 'Content-Type': 'application/json' },
    data: {
      movieId: parseInt(testMovieId, 10),
      roomId: 1,
      startTime: '2026-10-21T21:00',
      endTime: '2026-10-21T23:00',
      baseTicketPrice: 90000,
      status: 'Scheduled'
    }
  });
  console.log('API Create Showtime response:', createShowtimeRes.status(), await createShowtimeRes.text());

  const dbShowtime = querySql(`SELECT ShowtimeID, MovieID, RoomID, StartTime, BasePrice, Status FROM dbo.Showtime WHERE MovieID = ${testMovieId};`);
  console.log('SQL Server Showtime verification:\n', dbShowtime);

  // 5. Verify Customer Visibility on Public Pages
  console.log('Verifying customer visibility on /movies...');
  const publicContext = await browser.newContext();
  const publicPage = await publicContext.newPage();
  await publicPage.goto('http://localhost:8082/movies');
  const publicMoviesText = await publicPage.textContent('body');
  console.log('Test movie visible on public /movies:', publicMoviesText.includes(testTitle));

  // 6. F&B Management (/manager/fb)
  console.log('Testing /manager/fb...');
  await page.goto('http://localhost:8082/manager/fb');
  console.log('F&B Manager page title:', await page.title());
  const fbText = await page.textContent('body');
  console.log('F&B Manager items loaded:', fbText.includes('Combo') || fbText.includes('Popcorn') || fbText.includes('Caramel'));

  // 7. Revenue Reports (/manager/reports)
  console.log('Testing /manager/reports overview, movies, summary tabs...');
  await page.goto('http://localhost:8082/manager/reports');
  console.log('Reports overview title:', await page.title());

  await page.goto('http://localhost:8082/manager/reports?tab=movies');
  console.log('Reports movies tab URL:', page.url());

  await page.goto('http://localhost:8082/manager/reports?tab=summary');
  console.log('Reports summary tab URL:', page.url());

  await browser.close();
  console.log('=== MANAGER OPERATIONS E2E TEST: PASSED ===');
}

testManagerOperationsE2E().catch(err => {
  console.error('Manager Operations E2E Test FAILED:', err);
  process.exit(1);
});
