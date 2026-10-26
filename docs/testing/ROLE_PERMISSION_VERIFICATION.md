# Role and permission verification

The predefined boundaries follow the database seed and SRS UC-36/38, GB-08/11.

| Role | Allowed permissions | Required permissions |
| --- | --- | --- |
| CUSTOMER | movie:read, booking:create | movie:read |
| STAFF | movie:read, booking:create, ticket:checkin, booking:read, news:manage | movie:read, ticket:checkin |
| MANAGER | movie:read, showtime:manage, report:view, booking:read, movie:manage, genre:manage, room:manage, seat:manage, pricing:manage, fb:manage, voucher:manage, promotion:manage, news:approve | movie:read, report:view |
| ADMIN | all 17 predefined permissions | movie:read, account:manage |

movie:read is a baseline: movies/showtimes are public, so revoking it cannot restrict public browsing. Personal profile/password changes and access to existing bookings are not configurable management permissions. Existing ownership and payment validations remain in their controllers/services.

On application startup, invalid grants on known roles are removed and required grants restored in one transaction. Optional grants are preserved. The 11 additional catalog entries are created if absent and granted to their allowed roles only on their first introduction. Restarting never regrants an existing optional permission. Fresh databases include them in 00_full_database.sql; an optional manual upgrade is provided in 21_granular_role_permissions.sql. Database failures abort startup rather than reporting a successful repair. The existing SQL Server integration tests also start the application and run this repair. Restart the running web application to load the updated authorization code.

Role boundaries AND assigned permissions are checked for account management, revenue dashboards/reports, showtime management, check-in, counter sales/member lookup and new customer bookings. Each manager module also requires its specific management permission; booking lists and staff news are checked separately. Writing staff news (news:manage) never grants approval/publication (news:approve). Session authorization is refreshed from Account/Role/RolePermission on every authenticated request, including role changes and inactive accounts. It adds database reads per request. Manager headers/query parameters no longer establish authentication.

## Manual checks (use test data)

1. Restart the updated app. Open Roles & Permissions as ADMIN. CUSTOMER must show only movie:read and booking:create; movie:read cannot be removed. No internal permissions appear as available.
2. Try assigning account:manage to CUSTOMER via POST /api/admin/roles/{id}/permissions/add. Expect HTTP 400 and INVALID_ROLE_CONFIGURATION; verify no database change.
3. Keep a CUSTOMER session open in another browser. Remove booking:create as ADMIN. On the next request, GET /customer/booking and POST /customer/booking/checkout must return 403. Profile/password, history and existing payment/ticket routes remain subject to their normal authentication/ownership checks.
4. Add booking:create again. The same CUSTOMER session can enter booking without logging in again. CUSTOMER still receives 403 for /admin/customers and /api/admin/customers.
5. Remove showtime:manage from MANAGER; both /manager/showtimes and /api/manager/showtimes must be denied. Restore it and both work again.
6. Required permissions cannot be removed by UI or direct API. Staff counter sales depend on booking:create; check-in depends on ticket:checkin and STAFF/ADMIN role.
7. Without a session, /manager and /api/manager routes must not authorize based on X-User-Role or role query parameters. Public movie browsing remains accessible.

## Automated checks

RolePermissionServiceTest covers role boundaries, required permissions, before/after differences, UI metadata and cleanup. AuthorizationSnapshotServiceTest covers same-session revocation, invalid grants and suspended accounts. RolePermissionAccessTest uses the actual Spring Security filter chain with stub endpoints to check page/API authorization, role boundaries, public access and booking revocation. Service DAO dependencies are mocked; these tests do not validate a live SQL Server connection.

## Detailed permissions

| Permission | Allowed roles | Page/API |
| --- | --- | --- |
| booking:read | STAFF, MANAGER, ADMIN | /staff/bookings/** |
| news:manage | STAFF, ADMIN | /staff/news/**, /api/staff/news/** |
| news:approve | MANAGER, ADMIN | /manager/news/** |
| movie:manage | MANAGER, ADMIN | /manager/movies/**, /api/manager/movies/** |
| genre:manage | MANAGER, ADMIN | /manager/genres/**, /api/manager/genres/** |
| room:manage | MANAGER, ADMIN | /manager/rooms/**, /api/manager/rooms/** |
| seat:manage | MANAGER, ADMIN | /manager/seats/**, /manager/seat-types/** and corresponding APIs |
| pricing:manage | MANAGER, ADMIN | /manager/pricing/**, /api/manager/pricing/** |
| fb:manage | MANAGER, ADMIN | /manager/fb/**, /api/manager/fb/** |
| voucher:manage | MANAGER, ADMIN | /manager/vouchers/** |
| promotion:manage | MANAGER, ADMIN | /manager/promotions/** |

All 11 new permissions are optional. Check-in, report viewing and account management retain their previous required permissions. When counter sales are allowed but booking:read is removed, completing the order returns to sales with a success message instead of redirecting to a forbidden booking-detail page. Menu links and dashboard shortcuts respect the effective permissions. Permission control is per functional module, not separate create/edit/delete actions or individual accounts.

To verify: keep STAFF or MANAGER signed in in a second browser; remove the module permission as ADMIN, reload the page (403), then add it back (allowed without signing in again). Direct API writes must also be forbidden without the permission. CUSTOMER must never receive any of these internal permissions. New parameterized tests exercise each module's page/API, both STAFF/MANAGER and ADMIN access, CUSTOMER rejection, and API POST authorization.
