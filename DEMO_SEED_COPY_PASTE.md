# CINEORA — Demo Copy-Paste Data

> Keep this file open beside the browser and copy values directly into each form.
> This is **not a SQL file**. Create records in the order below so all relationships are valid.

## 0. Recommended Demo Flow

1. Sign in as Manager.
2. Create genres and movies.
3. Create seat types, screening rooms, and seat layouts.
4. Create ticket pricing rules and showtimes.
5. Create food, drinks, and combos.
6. Create a promotion and vouchers.
7. Sign in as Customer and complete a VNPay Sandbox booking.
8. Sign in as Staff to find the booking and check in the ticket.

---

## 1. Existing Demo Accounts

| Role | Username | Email | Password |
|---|---|---|---|
| Admin | `admin` | `admin@cinema.com` | `Admin1234` |
| Manager | `manager` | `manager@cinema.com` | `Manager123` |
| Staff | `staff` | `staff@cinema.com` | `Staff1234` |
| Customer | `customer` | `customer@cinema.com` | `Customer123` |

---

## 2. Movie Genres

### Action

| Field | Value to copy |
|---|---|
| Name | `Action` |
| Description | `High-energy movies featuring intense combat, exciting missions, and spectacular chase sequences.` |
| Status | `ACTIVE` |

### Animation

| Field | Value to copy |
|---|---|
| Name | `Animation` |
| Description | `Animated movies for families, featuring entertaining stories and memorable characters.` |
| Status | `ACTIVE` |

### Sci-Fi

| Field | Value to copy |
|---|---|
| Name | `Sci-Fi` |
| Description | `Science-fiction stories exploring space, advanced technology, and the future.` |
| Status | `ACTIVE` |

---

## 3. Movies

### Movie 1 — Journey to the Stars

| Field | Value to copy |
|---|---|
| Title | `Journey to the Stars` |
| Original title | `Journey to the Stars` |
| Duration | `105` |
| Release date | `2026-11-10` |
| End date | `2026-12-31` |
| Language | `English with Vietnamese subtitles` |
| Director | `Michael Nguyen` |
| Actors | `Anna Tran, Henry Le, Brian Pham` |
| Age rating | `P` |
| Genre | `Sci-Fi` |
| Status | `Now Showing` |
| Description | `A group of young explorers begins an intergalactic journey to find an energy source that could save Earth.` |
| Poster URL | `https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=800&auto=format&fit=crop&q=85` |
| Banner URL | `https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1600&auto=format&fit=crop&q=85` |
| Trailer URL | `https://www.youtube.com/watch?v=zSWdZVtXT7E` |

### Movie 2 — The Velocity Squad

| Field | Value to copy |
|---|---|
| Title | `The Velocity Squad` |
| Original title | `The Velocity Squad` |
| Duration | `115` |
| Release date | `2026-11-12` |
| End date | `2026-12-31` |
| Language | `English with Vietnamese subtitles` |
| Director | `David Le` |
| Actors | `Victor Hoang, Michelle Tran, Anthony Pham` |
| Age rating | `T13` |
| Genre | `Action` |
| Status | `Now Showing` |
| Description | `An elite response team races against time to stop a conspiracy that threatens the entire city.` |
| Poster URL | `https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=85` |
| Banner URL | `https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1600&auto=format&fit=crop&q=85` |
| Trailer URL | `https://www.youtube.com/watch?v=Way9Dexny3w` |

### Movie 3 — The Wondrous Forest

| Field | Value to copy |
|---|---|
| Title | `The Wondrous Forest` |
| Original title | `The Wondrous Forest` |
| Duration | `95` |
| Release date | `2026-11-14` |
| End date | `2026-12-31` |
| Language | `English with Vietnamese dubbing` |
| Director | `CINEORA Animation Studio` |
| Actors | `Voice cast: Mia Nguyen, Brian Le, Hannah Tran` |
| Age rating | `P` |
| Genre | `Animation` |
| Status | `Now Showing` |
| Description | `Young friends discover courage, friendship, and wonder inside an enchanted forest.` |
| Poster URL | `https://images.unsplash.com/photo-1473448912268-2022ce9509d8?w=800&auto=format&fit=crop&q=85` |
| Banner URL | `https://images.unsplash.com/photo-1473448912268-2022ce9509d8?w=1600&auto=format&fit=crop&q=85` |
| Trailer URL | `https://www.youtube.com/watch?v=LEjhY15eCx0` |

---

## 4. Seat Types

### Standard Seat

| Field | Value to copy |
|---|---|
| Type code | `STD-DEMO` |
| Type name | `Demo Standard Seat` |
| Description | `A comfortable standard cinema seat suitable for every customer.` |
| Price multiplier | `1.00` |
| Color | `#3B82F6` |
| Status | `Active` |

### VIP Seat

| Field | Value to copy |
|---|---|
| Type code | `VIP-DEMO` |
| Type name | `Demo VIP Seat` |
| Description | `A premium cushioned seat located in the optimal viewing area.` |
| Price multiplier | `1.25` |
| Color | `#F59E0B` |
| Status | `Active` |

### Couple Seat

| Field | Value to copy |
|---|---|
| Type code | `COUPLE-DEMO` |
| Type name | `Demo Couple Seat` |
| Description | `A private Sweetbox couple seat located in the back row.` |
| Price multiplier | `1.80` |
| Color | `#EC4899` |
| Status | `Active` |

---

## 5. Screening Rooms

### Standard Room

| Field | Value to copy |
|---|---|
| Room code | `DEMO-01` |
| Room name | `Demo Hall 01` |
| Room type | `Standard Laser` |
| Capacity | `48` |
| Total rows | `6` |
| Seats per row | `8` |
| Projector | `Laser 4K` |
| Sound system | `Dolby 7.1` |
| Screen size | `15m x 8m` |
| Status | `Ready` |
| Description | `A standard digital auditorium prepared for the system demonstration.` |

### VIP Room

| Field | Value to copy |
|---|---|
| Room code | `DEMO-02` |
| Room name | `Demo VIP Hall` |
| Room type | `Gold Class VIP` |
| Capacity | `48` |
| Total rows | `6` |
| Seats per row | `8` |
| Projector | `Laser 4K` |
| Sound system | `Dolby Atmos` |
| Screen size | `18m x 10m` |
| Status | `Ready` |
| Description | `A premium auditorium with Dolby Atmos sound prepared for the system demonstration.` |

### Suggested Seat Layout

| Rows | Seats | Seat type |
|---|---|---|
| A–C | `1` to `8` | `Demo Standard Seat` |
| D–E | `1` to `8` | `Demo VIP Seat` |
| F | `1` to `8` | `Demo Couple Seat` |

---

## 6. Ticket Pricing Rules

### Morning Price

| Field | Value to copy |
|---|---|
| Pricing code | `DEMO-MORNING` |
| Rule name | `Demo Morning Price` |
| Day type | `WEEKDAY` |
| Time slot | `MORNING` |
| Base price | `65000` |
| Format surcharge | `0` |
| Experience type | `2D Digital` |
| Status | `Active` |
| Description | `Discounted ticket price for screenings starting before noon.` |

### Standard Price

| Field | Value to copy |
|---|---|
| Pricing code | `DEMO-STANDARD` |
| Rule name | `Demo Standard Price` |
| Day type | `WEEKDAY` |
| Time slot | `STANDARD` |
| Base price | `85000` |
| Format surcharge | `0` |
| Experience type | `2D Digital` |
| Status | `Active` |
| Description | `Standard ticket price for regular weekday screenings.` |

---

## 7. Morning Showtimes — November 16, 2026

> If a showtime has passed, move it to the nearest available future time. Leave enough cleaning time between screenings in the same room.

| Movie | Room | Start time | End time | Base price | Status |
|---|---|---|---|---:|---|
| `Journey to the Stars` | `Demo Hall 01` | `2026-11-16 08:30` | `2026-11-16 10:15` | `65000` | `Scheduled` |
| `The Velocity Squad` | `Demo VIP Hall` | `2026-11-16 09:00` | `2026-11-16 10:55` | `95000` | `Scheduled` |
| `The Wondrous Forest` | `Demo Hall 01` | `2026-11-16 10:45` | `2026-11-16 12:20` | `75000` | `Scheduled` |
| `Journey to the Stars` | `Demo VIP Hall` | `2026-11-16 11:30` | `2026-11-16 13:15` | `105000` | `Scheduled` |

### Backup Showtimes — November 17, 2026

| Movie | Room | Start time | End time | Base price | Status |
|---|---|---|---|---:|---|
| `Journey to the Stars` | `Demo Hall 01` | `2026-11-17 09:00` | `2026-11-17 10:45` | `65000` | `Scheduled` |
| `The Velocity Squad` | `Demo VIP Hall` | `2026-11-17 10:00` | `2026-11-17 11:55` | `95000` | `Scheduled` |
| `The Wondrous Forest` | `Demo Hall 01` | `2026-11-17 14:00` | `2026-11-17 15:35` | `85000` | `Scheduled` |

---

## 8. Food, Drinks, and Combos

### Popcorn

| Field | Value to copy |
|---|---|
| Item code | `DEMO-POPCORN` |
| Name | `Demo Caramel Popcorn` |
| Item type | `Food` |
| Price | `55000` |
| Cost price | `15000` |
| Description | `Freshly popped large popcorn coated with golden caramel.` |
| Image URL | `/images/fb/caramel-popcorn.png` |
| Status | `Active` |

### Soft Drink

| Field | Value to copy |
|---|---|
| Item code | `DEMO-COLA` |
| Name | `Demo Large Coca-Cola` |
| Item type | `Drink` |
| Price | `32000` |
| Cost price | `8000` |
| Description | `A large serving of chilled Coca-Cola.` |
| Image URL | `/images/fb/large-cola.png` |
| Status | `Active` |

### Combo

| Field | Value to copy |
|---|---|
| Item code | `DEMO-COMBO` |
| Name | `Demo Movie Combo` |
| Item type | `Combo` |
| Price | `79000` |
| Cost price | `22000` |
| Description | `One large caramel popcorn and one large Coca-Cola.` |
| Image URL | `/images/fb/solo-combo.png` |
| Status | `Active` |
| Components | `1 × Demo Caramel Popcorn, 1 × Demo Large Coca-Cola` |

---

## 9. Promotion

| Field | Value to copy |
|---|---|
| Name | `Morning Demo Special` |
| Description | `A promotional campaign prepared for the CINEORA system demonstration.` |
| Banner URL | `https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1400&auto=format&fit=crop&q=88` |
| Start date | `2026-11-15 00:00` |
| End date | `2026-12-31 23:59` |
| Status | `ACTIVE` |

---

## 10. Vouchers

### Recommended Percentage Voucher

| Field | Value to copy |
|---|---|
| Promotion | `Morning Demo Special` |
| Code | `DEMO20` |
| Discount type | `PERCENT` |
| Discount value | `20` |
| Maximum discount | `80000` |
| Minimum booking amount | `100000` |
| Valid from | `2026-11-15 00:00` |
| Valid to | `2026-12-31 23:59` |
| Total usage limit | `1000` |
| Per-account limit | `10` |
| Required rank | `No rank required` |
| Status | `ACTIVE` |

### Fixed-Amount Voucher

| Field | Value to copy |
|---|---|
| Promotion | `Morning Demo Special` |
| Code | `DEMO50K` |
| Discount type | `FIXED` |
| Discount value | `50000` |
| Maximum discount | `Leave blank` |
| Minimum booking amount | `150000` |
| Valid from | `2026-11-15 00:00` |
| Valid to | `2026-12-31 23:59` |
| Total usage limit | `1000` |
| Per-account limit | `10` |
| Required rank | `No rank required` |
| Status | `ACTIVE` |

### Backup 100% Voucher

| Field | Value to copy |
|---|---|
| Promotion | `Morning Demo Special` |
| Code | `FREEMORNING` |
| Discount type | `PERCENT` |
| Discount value | `100` |
| Maximum discount | `500000` |
| Minimum booking amount | `0` |
| Valid from | `2026-11-15 00:00` |
| Valid to | `2026-12-31 23:59` |
| Total usage limit | `1000` |
| Per-account limit | `10` |
| Required rank | `No rank required` |
| Status | `ACTIVE` |

---

## 11. VNPay Sandbox Test Card

> Use this card only on **VNPay Sandbox**, never on a production payment gateway.

| Field | Value to copy |
|---|---|
| Bank | `NCB` |
| Card number | `9704198526191432198` |
| Cardholder name | `NGUYEN VAN A` |
| Issue date | `07/15` |
| OTP | `123456` |

```text
NCB
9704198526191432198
NGUYEN VAN A
07/15
123456
```

---

## 12. New Customer Registration

| Field | Value to copy |
|---|---|
| Username | `democustomer01` |
| Full name | `David Nguyen` |
| Email | `democustomer01@gmail.com` |
| Phone | `0912345678` |
| Password | `Demo@12345` |
| Confirm password | `Demo@12345` |
| Date of birth | `2000-05-11` |
| Address | `Thu Duc City, Ho Chi Minh City` |

> Use an accessible email if verification is required. The code `123456` is only the VNPay Sandbox OTP, not the registration OTP.

---

## 13. Backup Staff and Manager Accounts

### Staff

| Field | Value to copy |
|---|---|
| Username | `staffdemo01` |
| Full name | `Emma Tran` |
| Email | `staffdemo01@cineora.vn` |
| Phone | `0987654321` |
| Role | `STAFF` |
| Position | `Box Office Staff` |
| Status | `ACTIVE` |
| Temporary password | `StaffDemo@123` |

### Manager

| Field | Value to copy |
|---|---|
| Username | `managerdemo01` |
| Full name | `Daniel Le` |
| Email | `managerdemo01@cineora.vn` |
| Phone | `0977777777` |
| Role | `MANAGER` |
| Position | `Cinema Manager` |
| Status | `ACTIVE` |
| Temporary password | `ManagerDemo@123` |

---

## 14. News Articles

### Promotional Article

| Field | Value to copy |
|---|---|
| Title | `CINEORA Launches a New Morning Movie Special` |
| Thumbnail URL | `https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1200&auto=format&fit=crop&q=85` |
| Content | `Start your day at CINEORA with premium screenings at attractive prices. Members can use the DEMO20 voucher to receive a 20% discount on eligible bookings. This promotion is available for a limited time.` |
| Initial status | `DRAFT` |
| Next action | `Submit for review` |
| Publish mode | `NOW` |

### Cinema Experience Article

| Field | Value to copy |
|---|---|
| Title | `Experience Dolby Atmos Sound at CINEORA` |
| Thumbnail URL | `https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1200&auto=format&fit=crop&q=85` |
| Content | `CINEORA's premium auditorium combines Dolby Atmos sound with Laser 4K projection to deliver immersive audio and sharp images. It is an excellent choice for action and science-fiction movies.` |
| Initial status | `DRAFT` |
| Next action | `Submit for review` |
| Publish mode | `NOW` |

### Sample Rejection Reason

```text
Please add the promotion period and voucher eligibility requirements before publication.
```

---

## 15. Complete Customer Booking Scenario

| Step | Recommended selection |
|---|---|
| Customer | `customer@cinema.com` / `Customer123` |
| Movie | `Journey to the Stars` |
| Showtime | Nearest future showtime in `Demo Hall 01` |
| Seats | `D4`, `D5` — VIP seats |
| F&B | `1 × Demo Movie Combo` |
| Voucher | `DEMO20` |
| Payment method | `VNPAY` |
| Bank | `NCB` |

### Sample Price Calculation

Assuming an `85,000 VND` base ticket price:

| Item | Formula | Amount |
|---|---|---:|
| Two VIP seats | `85,000 × 1.25 × 2` | `212,500 VND` |
| One combo | `79,000 × 1` | `79,000 VND` |
| Subtotal | `212,500 + 79,000` | `291,500 VND` |
| `DEMO20` discount | `291,500 × 20%` | `-58,300 VND` |
| Expected total | `291,500 - 58,300` | `233,200 VND` |

> The actual amount may differ if vouchers apply only to tickets or another base price is selected.

---

## 16. Staff Counter-Sale Flow

1. Sign in with `staff@cinema.com` / `Staff1234`.
2. Select a date containing a future showtime.
3. Select `The Velocity Squad`.
4. Select seats `D6` and `D7`.
5. Look up the member with `customer@cinema.com` or `0900000004`.
6. Add `1 × Demo Caramel Popcorn` and `2 × Demo Large Coca-Cola`.
7. Select `CASH` to avoid VNPay dependency during this flow.
8. Enter `500000` as the cash received.
9. Confirm the customer's age if required.
10. Print the ticket and retain its generated code for Check-in.

| Field | Value to copy |
|---|---|
| Payment method | `CASH` |
| Cash received | `500000` |
| Age verified | `Confirmed` |

---

## 17. Ticket Check-in Flow

> Use the dynamically generated ticket or QR code from a successfully paid booking.

1. Open the completed booking details.
2. Copy the `Ticket Code` or QR content.
3. Sign in as Staff and open Check-in.
4. Paste the code into `Ticket code`.
5. Validate the ticket, confirm age if required, and complete Check-in.
6. Submit the same code again to demonstrate duplicate check-in prevention.

| Rejection situation | Value |
|---|---|
| Customer is underage | `UNDERAGE` |
| Customer has no identification | `NO_ID` |

---

## 18. Report Filters

### Daily Report

| Field | Value to copy |
|---|---|
| Start date | `2026-11-16` |
| End date | `2026-11-16` |
| Preset | `TODAY` |

### Custom Report

| Field | Value to copy |
|---|---|
| Start date | `2026-11-01` |
| End date | `2026-11-30` |
| Preset | `CUSTOM` |

> Complete at least one paid booking before opening the report so revenue data is available.

---

## 19. Search and Filter Values

| Screen | Keyword or filter |
|---|---|
| Movie Management | `Journey` |
| Movies by genre | `Sci-Fi` |
| Room Management | `DEMO-01` |
| F&B Management | `DEMO` |
| Voucher Management | `DEMO20` |
| Staff Booking Lookup | `customer@cinema.com` |
| Member phone lookup | `0900000004` |
| Report date | `2026-11-16` |

---

## 20. Quick Copy — Everything to Remember

```text
Manager: manager / Manager123
Staff: staff / Staff1234
Customer: customer / Customer123

Room: DEMO-01
Movie: Journey to the Stars
Voucher: DEMO20
Combo: DEMO-COMBO

VNPay bank: NCB
Card: 9704198526191432198
Cardholder: NGUYEN VAN A
Issue date: 07/15
OTP: 123456
```

---

## 21. Short Presentation Script

1. **Manager:** “First, I create the movie catalog, screening rooms, and seat types.”
2. **Manager:** “Next, I configure ticket prices and schedule showtimes. The system prevents room conflicts.”
3. **Manager:** “I can manage food products, promotional campaigns, and vouchers with usage limits.”
4. **Customer:** “The customer selects a movie, showtime, seats, and a combo, then applies `DEMO20`.”
5. **Payment:** “The customer completes payment on VNPay Sandbox with the NCB test card.”
6. **Staff:** “Staff can look up the booking, print the ticket, and perform ticket check-in.”

---

## 22. Quick Troubleshooting

| Problem | Quick solution |
|---|---|
| Room creation fails | Change the code to `DEMO-03` and ensure capacity equals `rows × seats per row`. |
| Room name already exists | Change the name to `Demo Hall 03`. |
| Showtime creation fails | Select a future time and leave at least 15 minutes after the previous screening. |
| Movie is missing from scheduling | Set it to `Now Showing` and ensure its end date has not passed. |
| Room cannot be selected | Set its status to `Ready`. |
| Voucher is missing | Check `ACTIVE`, its validity period, minimum order, and required rank. |
| Voucher limit is reached | Use another Customer or increase the per-account limit. |
| VNPay does not open | Check `VNPAY_ENABLED`, TMN code, hash secret, and Sandbox return URL. |
| VNPay signature is invalid | Ensure TMN code and hash secret belong to the same Sandbox merchant. |
| No ticket after payment | Wait for VNPay to return successfully to the application return URL. |
| Check-in fails | Booking must be `CONFIRMED`; the ticket must be unused and within the valid time. |
| Revenue report is empty | Complete a paid booking and reload the report. |

---

## 23. Pre-Demo Checklist

- [ ] SQL Server is running and the application connects successfully.
- [ ] The application is available at `http://localhost:8080`.
- [ ] Manager, Customer, and Staff can sign in.
- [ ] At least one movie is `Now Showing`.
- [ ] The room is `Ready` and has a complete seat layout.
- [ ] At least one future showtime is available without a room conflict.
- [ ] `Morning Demo Special` and `DEMO20` are `ACTIVE` and currently valid.
- [ ] VNPay uses valid Sandbox TMN code and hash secret values.
- [ ] Manager, Customer, and Staff are open in separate tabs.
- [ ] Keep this file open at section 20 for quick copying.
- [ ] Keep the completed booking page open for the Check-in demonstration.
