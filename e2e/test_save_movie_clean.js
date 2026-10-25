const { chromium } = require('./node_modules/playwright');
(async () => {
  const browser = await chromium.launch({ headless: true, channel: 'msedge' });
  const context = await browser.newContext();
  const page = await context.newPage();
  page.on('response', res => {
    if (res.url().includes('movies') || res.url().includes('login') || res.url().includes('save')) {
      console.log('RESPONSE:', res.status(), res.url(), '-> Location:', res.headers()['location']);
    }
  });
  await page.goto('http://localhost:8080/login');
  await page.fill('#identifier', 'manager@cinema.com');
  await page.fill('#password', 'Manager123');
  await Promise.all([page.waitForURL('**/manager/**'), page.press('#password', 'Enter')]);
  await page.goto('http://localhost:8080/manager/movies');
  
  await page.evaluate(() => {
    document.getElementById('movieTitle').value = 'Avatar 3: Fire and Ash';
    document.getElementById('movieDuration').value = '180';
    document.getElementById('movieReleaseDate').value = '2026-11-01';
    document.getElementById('movieEndDate').value = '2026-12-01';
    document.getElementById('moviePoster').value = 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800';
    document.getElementById('movieTrailer').value = 'https://youtube.com/watch?v=dQw4w9WgXcQ';
    document.getElementById('movieAgeRating').value = 'P';
    document.getElementById('movieStatus').value = 'Upcoming';
    const cb = document.querySelector('.genre-checkbox-item input');
    if (cb) cb.checked = true;
    document.getElementById('movieModalForm').submit();
  });

  await page.waitForNavigation({ waitUntil: 'networkidle' });
  console.log('FINAL URL:', page.url());

  const content = await page.content();
  if (content.includes('Avatar 3: Fire and Ash')) {
    console.log('SUCCESS: Movie found on page!');
  } else {
    console.log('NOT FOUND. Checking alerts...');
    const alert = await page.$('.alert');
    if (alert) {
      console.log('ALERT TEXT:', await alert.innerText());
    }
  }

  await browser.close();
})();
