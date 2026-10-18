# 04 — API & Endpoint Inventory

This inventory documents all **84 HTTP endpoints** (36 MVC Web Endpoints + 48 REST API Endpoints) discovered across the application controllers.

---

## 1. MVC View Controllers (36 Endpoints)

| # | Method | Path | Auth / Role | Params / Body | Constraints | Response | Related Service |
|---|---|---|---|---|---|---|---|
| 1 | `GET` | `/` | None | None | - | HTML `index` | - |
| 2 | `GET` | `/customer/profile` | None (Unsafe) | `accountId` (int) | Required | HTML `customer/profile` | `CustomerProfileService` |
| 3 | `GET` | `/customer/profile/edit` | None (Unsafe) | `accountId` (int) | Required | HTML `customer/profile-edit` | `CustomerProfileService` |
| 4 | `POST` | `/customer/profile/edit` | None (Unsafe) | `accountId`, `ProfileUpdateForm` | Phone, address validation | Redirect `/customer/profile` | `CustomerProfileService` |
| 5 | `GET` | `/manager` | `MANAGER` / `ADMIN` | None | - | HTML `manager/dashboard` | `ManagerDashboardService` |
| 6 | `GET` | `/manager/dashboard` | `MANAGER` / `ADMIN` | None | - | HTML `manager/dashboard` | `ManagerDashboardService` |
| 7 | `GET` | `/manager/movies` | `MANAGER` / `ADMIN` | `keyword`, `genreId`, `ageRating`, `status` | Optional filters | HTML `manager/movies` | `MovieService` |
| 8 | `POST` | `/manager/movies/save` | `MANAGER` / `ADMIN` | Form `MovieForm` | Duration > 0, Title not blank | Redirect `/manager/movies` | `MovieService` |
| 9 | `POST` | `/manager/movies/status/{id}` | `MANAGER` / `ADMIN` | `id` (path), `status` (param) | Valid status | Redirect `/manager/movies` | `MovieService` |
| 10 | `POST` | `/manager/movies/delete/{id}` | `MANAGER` / `ADMIN` | `id` (path) | BR-37 (No active showtimes) | Redirect `/manager/movies` | `MovieService` |
| 11 | `GET` | `/manager/genres` | `MANAGER` / `ADMIN` | `keyword`, `status` | Optional filters | HTML `manager/genres` | `GenreService` |
| 12 | `POST` | `/manager/genres/save` | `MANAGER` / `ADMIN` | Form `GenreForm` | Name unique, not empty | Redirect `/manager/genres` | `GenreService` |
| 13 | `POST` | `/manager/genres/status/{id}` | `MANAGER` / `ADMIN` | `id` (path), `status` (param) | `ACTIVE` or `INACTIVE` | Redirect `/manager/genres` | `GenreService` |
| 14 | `POST` | `/manager/genres/delete/{id}` | `MANAGER` / `ADMIN` | `id` (path) | No linked movies | Redirect `/manager/genres` | `GenreService` |
| 15 | `GET` | `/manager/rooms` | `MANAGER` / `ADMIN` | `keyword`, `status` | Optional filters | HTML `manager/rooms` | `RoomService` |
| 16 | `POST` | `/manager/rooms/save` | `MANAGER` / `ADMIN` | Form `RoomForm` | Capacity > 0, Rows > 0 | Redirect `/manager/rooms` | `RoomService` |
| 17 | `POST` | `/manager/rooms/deactivate/{id}` | `MANAGER` / `ADMIN` | `id` (path) | BR-52 (No future showtimes) | Redirect `/manager/rooms` | `RoomService` |
| 18 | `POST` | `/manager/rooms/delete/{id}` | `MANAGER` / `ADMIN` | `id` (path) | Room must exist | Redirect `/manager/rooms` | `RoomService` |
| 19 | `GET` | `/manager/seat-types` | `MANAGER` / `ADMIN` | `status` (param) | Optional filter | HTML `manager/seat-types` | `TicketPricingService` |
| 20 | `POST` | `/manager/seat-types/save` | `MANAGER` / `ADMIN` | Form `SeatTypeForm` | Code unique, Mult `[0.50, 3.00]` | Redirect `/manager/seat-types` | `TicketPricingService` |
| 21 | `POST` | `/manager/seat-types/multiplier` | `MANAGER` / `ADMIN` | `seatTypeId`, `multiplier` | `0.50 <= mult <= 3.00` | Redirect `/manager/seat-types` | `TicketPricingService` |
| 22 | `POST` | `/manager/seat-types/update/{id}` | `MANAGER` / `ADMIN` | `id` (path), Form `SeatTypeForm` | Multiplier & code checks | Redirect `/manager/seat-types` | `TicketPricingService` |
| 23 | `POST` | `/manager/seat-types/status/{id}` | `MANAGER` / `ADMIN` | `id` (path), `status` | `Active` or `Inactive` | Redirect `/manager/seat-types` | `TicketPricingService` |
| 24 | `GET` | `/manager/seats` | `MANAGER` / `ADMIN` | `roomId` (optional) | Valid Room ID | HTML `manager/seats` | `SeatService` |
| 25 | `POST` | `/manager/seats/generate-default`| `MANAGER` / `ADMIN` | `roomId` | Capacity constraint | Redirect `/manager/seats` | `SeatService` |
| 26 | `POST` | `/manager/seats/update-seat` | `MANAGER` / `ADMIN` | `seatId`, `roomId`, `seatTypeName`, `status` | Active, Maintenance, Unavailable | Redirect `/manager/seats` | `SeatService` |
| 27 | `GET` | `/manager/showtimes` | `MANAGER` / `ADMIN` | `date`, `roomId`, `movieId`, `status` | Optional filters | HTML `manager/showtimes` | `ShowtimeService` |
| 28 | `POST` | `/manager/showtimes/save` | `MANAGER` / `ADMIN` | Form `ShowtimeForm` | BR-07, BR-08, BR-09, BR-55 | Redirect `/manager/showtimes` | `ShowtimeService` |
| 29 | `POST` | `/manager/showtimes/status/{id}` | `MANAGER` / `ADMIN` | `id` (path), `status` | Scheduled, Completed, Cancelled | Redirect `/manager/showtimes` | `ShowtimeService` |
| 30 | `GET` | `/manager/pricing` | `MANAGER` / `ADMIN` | `dayType`, `status` | Optional filters | HTML `manager/pricing` | `TicketPricingService` |
| 31 | `POST` | `/manager/pricing/save` | `MANAGER` / `ADMIN` | Form `TicketPricingForm` | BasePrice >= 0, Surcharge >= 0 | Redirect `/manager/pricing` | `TicketPricingService` |
| 32 | `POST` | `/manager/pricing/status/{id}` | `MANAGER` / `ADMIN` | `id` (path), `status` | Active, Inactive | Redirect `/manager/pricing` | `TicketPricingService` |
| 33 | `POST` | `/manager/pricing/seat-multiplier` | `MANAGER` / `ADMIN` | `seatTypeId`, `multiplier` | `0.50 <= mult <= 3.00` | Redirect `/manager/pricing` | `TicketPricingService` |
| 34 | `GET` | `/manager/fb` | `MANAGER` / `ADMIN` | `keyword`, `category`, `status` | Optional filters | HTML `manager/fb` | `FBItemService` |
| 35 | `POST` | `/manager/fb/save` | `MANAGER` / `ADMIN` | Form `FBItemForm` | ItemCode unique, Price >= 0 | Redirect `/manager/fb` | `FBItemService` |
| 36 | `POST` | `/manager/fb/status/{id}` | `MANAGER` / `ADMIN` | `id` (path), `status` | Active, Inactive | Redirect `/manager/fb` | `FBItemService` |

---

## 2. REST API Controllers (48 Endpoints)

All REST API endpoints produce and consume JSON (`application/json`) encapsulated in `com.cinema.model.ApiResponse<T>`.

### A. Manager Dashboard API
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 37 | `GET` | `/api/manager/dashboard/stats` | `MANAGER` / `ADMIN` | None | `200 OK` + `ManagerDashboardStats` | `ManagerDashboardService` |

### B. Movies API (`/api/manager/movies`)
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 38 | `GET` | `/api/manager/movies` | `MANAGER` / `ADMIN` | `keyword`, `genreId`, `ageRating`, `status` | `200 OK` + `List<Movie>` | `MovieService` |
| 39 | `GET` | `/api/manager/movies/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` + `Movie` (or `404`) | `MovieService` |
| 40 | `POST` | `/api/manager/movies` | `MANAGER` / `ADMIN` | JSON `MovieForm` | `201 Created` + created `Movie` | `MovieService` |
| 41 | `PUT` | `/api/manager/movies/{id}` | `MANAGER` / `ADMIN` | `id` (int), JSON `MovieForm` | `200 OK` + updated `Movie` | `MovieService` |
| 42 | `PATCH` | `/api/manager/movies/{id}/status` | `MANAGER` / `ADMIN` | `id` (int), `status` (query) | `200 OK` + updated `Movie` | `MovieService` |
| 43 | `DELETE`| `/api/manager/movies/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` (or `409 Conflict`) | `MovieService` |

### C. Genres API (`/api/manager/genres`)
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 44 | `GET` | `/api/manager/genres` | `MANAGER` / `ADMIN` | `keyword`, `status` | `200 OK` + `List<Genre>` | `GenreService` |
| 45 | `GET` | `/api/manager/genres/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` + `Genre` | `GenreService` |
| 46 | `POST` | `/api/manager/genres` | `MANAGER` / `ADMIN` | JSON `GenreForm` | `201 Created` + `Genre` | `GenreService` |
| 47 | `PUT` | `/api/manager/genres/{id}` | `MANAGER` / `ADMIN` | `id` (int), JSON `GenreForm` | `200 OK` + `Genre` | `GenreService` |
| 48 | `PATCH` | `/api/manager/genres/{id}/status` | `MANAGER` / `ADMIN` | `id` (int), `status` (query) | `200 OK` + `Genre` | `GenreService` |
| 49 | `DELETE`| `/api/manager/genres/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` (or `409 Conflict`) | `GenreService` |

### D. Cinema Rooms API (`/api/manager/rooms`)
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 50 | `GET` | `/api/manager/rooms` | `MANAGER` / `ADMIN` | `keyword`, `status` | `200 OK` + `List<Room>` | `RoomService` |
| 51 | `GET` | `/api/manager/rooms/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` + `Room` | `RoomService` |
| 52 | `POST` | `/api/manager/rooms` | `MANAGER` / `ADMIN` | JSON `RoomForm` | `201 Created` + `Room` | `RoomService` |
| 53 | `PUT` | `/api/manager/rooms/{id}` | `MANAGER` / `ADMIN` | `id` (int), JSON `RoomForm` | `200 OK` + `Room` | `RoomService` |
| 54 | `PATCH` | `/api/manager/rooms/{id}/status` | `MANAGER` / `ADMIN` | `id` (int), `status` (query) | `200 OK` + `Room` | `RoomService` |
| 55 | `DELETE`| `/api/manager/rooms/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` (or `409 Conflict`) | `RoomService` |

### E. Seat Types API (`/api/manager/seat-types`)
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 56 | `GET` | `/api/manager/seat-types` | `MANAGER` / `ADMIN` | `status` (query) | `200 OK` + `List<SeatType>` | `TicketPricingService` |
| 57 | `GET` | `/api/manager/seat-types/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` + `SeatType` | `TicketPricingService` |
| 58 | `POST` | `/api/manager/seat-types` | `MANAGER` / `ADMIN` | JSON `SeatTypeForm` | `201 Created` + `SeatType` | `TicketPricingService` |
| 59 | `PUT` | `/api/manager/seat-types/{id}/multiplier` | `MANAGER` / `ADMIN` | `id`, `multiplier` (param) | `200 OK` + `SeatType` | `TicketPricingService` |
| 60 | `PUT` | `/api/manager/seat-types/{id}` | `MANAGER` / `ADMIN` | `id`, JSON `SeatTypeForm` | `200 OK` + `SeatType` | `TicketPricingService` |
| 61 | `PUT` | `/api/manager/seat-types/{id}/status` | `MANAGER` / `ADMIN` | `id`, `status` (param) | `200 OK` + `SeatType` | `TicketPricingService` |

### F. Seat Layout API (`/api/manager/seats`)
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 62 | `GET` | `/api/manager/seats/room/{roomId}` | `MANAGER` / `ADMIN` | `roomId` (int) | `200 OK` + `List<Seat>` | `SeatService` |
| 63 | `GET` | `/api/manager/seats/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` + `Seat` | `SeatService` |
| 64 | `PUT` | `/api/manager/seats/room/{roomId}/layout` | `MANAGER` / `ADMIN` | `roomId`, JSON `List<Seat>` | `200 OK` + `List<Seat>` | `SeatService` |
| 65 | `POST` | `/api/manager/seats/room/{roomId}/generate-default` | `MANAGER` / `ADMIN` | `roomId` (int) | `200 OK` + `List<Seat>` | `SeatService` |
| 66 | `PATCH` | `/api/manager/seats/{id}` | `MANAGER` / `ADMIN` | `id`, `seatTypeName`, `status` | `200 OK` + updated `Seat` | `SeatService` |

### G. Showtimes API (`/api/manager/showtimes`)
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 67 | `GET` | `/api/manager/showtimes` | `MANAGER` / `ADMIN` | `date`, `roomId`, `movieId`, `status` | `200 OK` + `List<Showtime>` | `ShowtimeService` |
| 68 | `GET` | `/api/manager/showtimes/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` + `Showtime` | `ShowtimeService` |
| 69 | `POST` | `/api/manager/showtimes` | `MANAGER` / `ADMIN` | JSON `ShowtimeForm` | `201 Created` + `Showtime` | `ShowtimeService` |
| 70 | `PUT` | `/api/manager/showtimes/{id}` | `MANAGER` / `ADMIN` | `id`, JSON `ShowtimeForm` | `200 OK` + `Showtime` | `ShowtimeService` |
| 71 | `PATCH` | `/api/manager/showtimes/{id}/status` | `MANAGER` / `ADMIN` | `id`, `status` (query) | `200 OK` + `Showtime` | `ShowtimeService` |

### H. Ticket Pricing API (`/api/manager/pricing`)
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 72 | `GET` | `/api/manager/pricing` | `MANAGER` / `ADMIN` | `dayType`, `status` | `200 OK` + `List<TicketPricing>` | `TicketPricingService` |
| 73 | `GET` | `/api/manager/pricing/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` + `TicketPricing` | `TicketPricingService` |
| 74 | `POST` | `/api/manager/pricing` | `MANAGER` / `ADMIN` | JSON `TicketPricingForm` | `201 Created` + `TicketPricing` | `TicketPricingService` |
| 75 | `PUT` | `/api/manager/pricing/{id}` | `MANAGER` / `ADMIN` | `id`, JSON `TicketPricingForm` | `200 OK` + `TicketPricing` | `TicketPricingService` |
| 76 | `PATCH` | `/api/manager/pricing/{id}/base-price` | `MANAGER` / `ADMIN` | `id`, `basePrice` (param) | `200 OK` + `TicketPricing` | `TicketPricingService` |
| 77 | `PATCH` | `/api/manager/pricing/{id}/status` | `MANAGER` / `ADMIN` | `id`, `status` (param) | `200 OK` + `TicketPricing` | `TicketPricingService` |
| 78 | `GET` | `/api/manager/pricing/matrix` | `MANAGER` / `ADMIN` | None | `200 OK` + `Map<PricingID, Map<SeatTypeID, Price>>` | `TicketPricingService` |

### I. Food & Beverage API (`/api/manager/fb`)
| # | Method | Path | Auth / Role | Request / Params | Expected Response | Related Service |
|---|---|---|---|---|---|---|
| 79 | `GET` | `/api/manager/fb` | `MANAGER` / `ADMIN` | `keyword`, `category`, `status` | `200 OK` + `List<FBItem>` | `FBItemService` |
| 80 | `GET` | `/api/manager/fb/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` + `FBItem` | `FBItemService` |
| 81 | `POST` | `/api/manager/fb` | `MANAGER` / `ADMIN` | JSON `FBItemForm` | `201 Created` + `FBItem` | `FBItemService` |
| 82 | `PUT` | `/api/manager/fb/{id}` | `MANAGER` / `ADMIN` | `id`, JSON `FBItemForm` | `200 OK` + `FBItem` | `FBItemService` |
| 83 | `PATCH` | `/api/manager/fb/{id}/status` | `MANAGER` / `ADMIN` | `id`, `status` (param) | `200 OK` + `FBItem` | `FBItemService` |
| 84 | `DELETE`| `/api/manager/fb/{id}` | `MANAGER` / `ADMIN` | `id` (int) | `200 OK` (or `409 Conflict`) | `FBItemService` |

---

## 3. Discrepancies & Frontend Integration Gaps

1. **Headless REST APIs vs Server-Side Forms:**
   - The Thymeleaf UI currently performs data mutations (create, update, toggle status) through standard HTML form POSTs to MVC paths (`/manager/.../save`, `/manager/.../status`).
   - The dedicated REST API endpoints (`/api/manager/...`) are fully implemented and tested with unit tests, but are not actively called by the current Thymeleaf templates (except for potential asynchronous SPA or mobile consumers).
2. **Missing Customer API Endpoints:**
   - There are no `/api/customer/...` REST endpoints for profile retrieval or update. Only MVC HTML routes exist.
3. **No Authentication Endpoint:**
   - There are zero `/api/auth/login`, `/api/auth/logout`, or `/auth/login` endpoints across the entire application.
