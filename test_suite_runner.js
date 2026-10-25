const fs = require('fs');

async function login(username, password) {
    const loginRes = await fetch('http://localhost:8082/login');
    const html = await loginRes.text();
    const cookie = loginRes.headers.get('set-cookie')?.split(';')[0];
    const csrfMatch = html.match(/name="_csrf" value="([^"]+)"/);
    const csrf = csrfMatch ? csrfMatch[1] : '';

    const params = new URLSearchParams();
    params.append('username', username);
    params.append('password', password);
    if (csrf) params.append('_csrf', csrf);

    const authRes = await fetch('http://localhost:8082/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Cookie': cookie || ''
        },
        body: params.toString(),
        redirect: 'manual'
    });

    const sessionCookie = authRes.headers.get('set-cookie')?.split(';')[0] || cookie;
    const location = authRes.headers.get('location');
    return { sessionCookie, location, status: authRes.status };
}

async function runAuditTests() {
    const results = {
        security: [],
        routes: [],
        financial: [],
        admin: []
    };

    console.log('=== PHASE C: SECURITY & AUTHORIZATION TESTS ===');

    // 1. Unauthenticated Manager Access
    const unauthManager = await fetch('http://localhost:8082/manager', { redirect: 'manual' });
    const unauthReports = await fetch('http://localhost:8082/manager/reports', { redirect: 'manual' });
    const unauthReportsApi = await fetch('http://localhost:8082/api/manager/reports/data', { redirect: 'manual' });

    results.security.push({
        test: 'SEC-01.1: Unauthenticated GET /manager',
        status: unauthManager.status,
        vulnerable: unauthManager.status === 200,
        detail: 'Expected 302 redirect to /login or 401/403, actual: ' + unauthManager.status
    });

    results.security.push({
        test: 'SEC-01.2: Unauthenticated GET /manager/reports',
        status: unauthReports.status,
        vulnerable: unauthReports.status === 200,
        detail: 'Expected 302 redirect to /login or 401/403, actual: ' + unauthReports.status
    });

    results.security.push({
        test: 'SEC-01.3: Unauthenticated GET /api/manager/reports/data',
        status: unauthReportsApi.status,
        vulnerable: unauthReportsApi.status === 200,
        detail: 'Expected 401/403 Unauthorized, actual: ' + unauthReportsApi.status
    });

    // 2. Unauthenticated Staff & Admin Access
    const unauthStaff = await fetch('http://localhost:8082/staff', { redirect: 'manual' });
    const unauthAdmin = await fetch('http://localhost:8082/admin/dashboard', { redirect: 'manual' });

    results.security.push({
        test: 'SEC-01.4: Unauthenticated GET /staff',
        status: unauthStaff.status,
        vulnerable: unauthStaff.status === 200,
        detail: 'Status: ' + unauthStaff.status + ', Location: ' + unauthStaff.headers.get('location')
    });

    results.security.push({
        test: 'SEC-01.5: Unauthenticated GET /admin/dashboard',
        status: unauthAdmin.status,
        vulnerable: unauthAdmin.status === 200,
        detail: 'Status: ' + unauthAdmin.status + ', Location: ' + unauthAdmin.headers.get('location')
    });

    // 3. Customer Role Privilege Escalation Test
    console.log('\nLogging in as customer@cinema.com...');
    const customerLogin = await login('customer@cinema.com', 'Customer@123');
    const customerManagerAccess = await fetch('http://localhost:8082/manager/reports', {
        headers: { 'Cookie': customerLogin.sessionCookie },
        redirect: 'manual'
    });

    results.security.push({
        test: 'SEC-01.6: Customer accessing /manager/reports',
        status: customerManagerAccess.status,
        vulnerable: customerManagerAccess.status === 200,
        detail: 'Customer role status when hitting /manager/reports: ' + customerManagerAccess.status
    });

    // 4. Admin Portal Route Tests
    console.log('\nLogging in as admin@cinema.com...');
    const adminLogin = await login('admin@cinema.com', 'Admin@123');
    const adminRoot = await fetch('http://localhost:8082/admin', {
        headers: { 'Cookie': adminLogin.sessionCookie },
        redirect: 'manual'
    });
    const adminDash = await fetch('http://localhost:8082/admin/dashboard', {
        headers: { 'Cookie': adminLogin.sessionCookie },
        redirect: 'manual'
    });
    const adminStaffs = await fetch('http://localhost:8082/admin/staffs', {
        headers: { 'Cookie': adminLogin.sessionCookie },
        redirect: 'manual'
    });
    const adminCustomers = await fetch('http://localhost:8082/admin/customers', {
        headers: { 'Cookie': adminLogin.sessionCookie },
        redirect: 'manual'
    });
    const adminRoles = await fetch('http://localhost:8082/admin/roles', {
        headers: { 'Cookie': adminLogin.sessionCookie },
        redirect: 'manual'
    });
    const adminConfig = await fetch('http://localhost:8082/admin/config', {
        headers: { 'Cookie': adminLogin.sessionCookie },
        redirect: 'manual'
    });

    results.admin.push({ route: '/admin', status: adminRoot.status });
    results.admin.push({ route: '/admin/dashboard', status: adminDash.status });
    results.admin.push({ route: '/admin/staffs', status: adminStaffs.status });
    results.admin.push({ route: '/admin/customers', status: adminCustomers.status });
    results.admin.push({ route: '/admin/roles', status: adminRoles.status });
    results.admin.push({ route: '/admin/config', status: adminConfig.status });

    // 5. Staff Portal Route Tests
    console.log('\nLogging in as staff@cinema.com...');
    const staffLogin = await login('staff@cinema.com', 'Staff@123');
    const staffDash = await fetch('http://localhost:8082/staff', {
        headers: { 'Cookie': staffLogin.sessionCookie },
        redirect: 'manual'
    });
    const staffShowtimes = await fetch('http://localhost:8082/staff/sales/showtimes', {
        headers: { 'Cookie': staffLogin.sessionCookie },
        redirect: 'manual'
    });
    const staffBookings = await fetch('http://localhost:8082/staff/bookings', {
        headers: { 'Cookie': staffLogin.sessionCookie },
        redirect: 'manual'
    });
    const staffCheckin = await fetch('http://localhost:8082/staff/check-in', {
        headers: { 'Cookie': staffLogin.sessionCookie },
        redirect: 'manual'
    });

    results.routes.push({ portal: 'Staff', route: '/staff', status: staffDash.status });
    results.routes.push({ portal: 'Staff', route: '/staff/sales/showtimes', status: staffShowtimes.status });
    results.routes.push({ portal: 'Staff', route: '/staff/bookings', status: staffBookings.status });
    results.routes.push({ portal: 'Staff', route: '/staff/check-in', status: staffCheckin.status });

    // 6. Public & Customer Screens
    const pubHome = await fetch('http://localhost:8082/');
    const pubMovies = await fetch('http://localhost:8082/movies');
    const pubShowtimes = await fetch('http://localhost:8082/showtimes');
    const pubPromotions = await fetch('http://localhost:8082/promotions-news');
    const cusProfile = await fetch('http://localhost:8082/customer/profile', {
        headers: { 'Cookie': customerLogin.sessionCookie }
    });

    results.routes.push({ portal: 'Public', route: '/', status: pubHome.status });
    results.routes.push({ portal: 'Public', route: '/movies', status: pubMovies.status });
    results.routes.push({ portal: 'Public', route: '/showtimes', status: pubShowtimes.status });
    results.routes.push({ portal: 'Public', route: '/promotions-news', status: pubPromotions.status });
    results.routes.push({ portal: 'Customer', route: '/customer/profile', status: cusProfile.status });

    // 7. Financial Reconciliation Check
    console.log('\nVerifying Financial API reconciliation...');
    const finRes = await fetch('http://localhost:8082/api/manager/reports/data?preset=alltime');
    const finJson = await finRes.json();

    results.financial = {
        status: finRes.status,
        success: finJson.success,
        summary: finJson.data?.summary,
        dailyCount: finJson.data?.dailyRevenue?.length || 0,
        moviesCount: finJson.data?.movieRevenue?.length || 0,
        txCount: finJson.data?.transactions?.length || 0
    };

    console.log('\n=== TEST RUN RESULTS SUMMARY ===');
    console.log(JSON.stringify(results, null, 2));

    fs.writeFileSync('audit_test_results.json', JSON.stringify(results, null, 2));
}

runAuditTests().catch(console.error);
