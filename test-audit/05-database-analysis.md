# 05 — Database & Persistence Analysis

This document details the database schema, primary/foreign key relationships, constraints, transaction semantics, and data consistency risks discovered in the project SQL scripts and DAO implementations.

---

## 1. Schema Entity Matrix

| Table Name | Primary Key | Foreign Keys | Migration Script | Description |
|:---|:---|:---|:---|:---|
| `Movie` | `MovieID` (Identity) | None | `schema_movie.sql` | Film catalog, metadata, runtime, age certification, and release status |
| `Genre` | `GenreID` (Identity) | None | `schema_genre.sql` | Film genre classifications (Action, Drama, Sci-Fi, etc.) |
| `MovieGenre` | `(MovieID, GenreID)` (Composite) | `MovieID` -> `Movie(MovieID)` [CASCADE]<br>`GenreID` -> `Genre(GenreID)` [NO ACTION] | `schema_genre.sql` | Many-to-Many association mapping movies to genres |
| `Room` | `RoomID` (Identity) | None | `schema_room.sql` | Physical screening halls (Laser, IMAX, Dolby Atmos) and dimensions |
| `Seat` | `SeatID` (Identity) | `RoomID` -> `Room(RoomID)` [CASCADE] | `schema_room.sql` | Individual seats with row/column matrix, type and maintenance status |
| `SeatType` | `SeatTypeID` (Identity) | None | `schema_pricing.sql` | Seat tiers (Standard, VIP, Couple) and price multipliers (0.50 - 3.00) |
| `TicketPricing` | `PricingID` (Identity) | None | `schema_pricing.sql` | Time-slot base pricing and premium experience format surcharges |
| `Showtime` | `ShowtimeID` (Identity) | `MovieID` -> `Movie(MovieID)` [NO ACTION]<br>`RoomID` -> `Room(RoomID)` [NO ACTION] | `schema_movie.sql`, `schema_room.sql` | Scheduled screening sessions with start/end timestamps and base price |
| `FBItem` | `ItemID` (Identity) | None | `schema_fb.sql` | Concessions catalog (popcorn, beverages, hot snacks, combos) |
| `ComboItem` | `ComboItemID` (Identity) | `ComboID` -> `FBItem(ItemID)` [CASCADE]<br>`ComponentItemID` -> `FBItem(ItemID)` [NO ACTION] | `schema_fb.sql` | Breakdown of items contained inside a combo package |

---

## 2. Table Constraints & Business Rules in DDL

```sql
-- 1. Uniqueness Constraints
CONSTRAINT UQ_Genre_Name UNIQUE ([Name])
CONSTRAINT UQ_Room_RoomCode UNIQUE (RoomCode)
CONSTRAINT UQ_Room_RoomName UNIQUE (RoomName)
CONSTRAINT UQ_Seat_Room_Position UNIQUE (RoomID, RowLabel, SeatNumber)
CONSTRAINT UQ_SeatType_TypeCode UNIQUE (TypeCode)
CONSTRAINT UQ_SeatType_TypeName UNIQUE (TypeName)
CONSTRAINT UQ_TicketPricing_Code UNIQUE (PricingCode)
CONSTRAINT UQ_FBItem_ItemCode UNIQUE (ItemCode)

-- 2. Check Constraints
CONSTRAINT CK_Room_Capacity CHECK (Capacity > 0)
CONSTRAINT CK_Room_TotalRows CHECK (TotalRows > 0)
CONSTRAINT CK_Room_SeatsPerRow CHECK (SeatsPerRow > 0)
CONSTRAINT CK_SeatType_PriceMultiplier CHECK (PriceMultiplier >= 0.50 AND PriceMultiplier <= 3.00)
CONSTRAINT CK_TicketPricing_BasePrice CHECK (BasePrice >= 0)
CONSTRAINT CK_TicketPricing_FormatSurcharge CHECK (FormatSurcharge >= 0)
CONSTRAINT CK_FBItem_SellingPrice CHECK (SellingPrice >= 0)
CONSTRAINT CK_FBItem_CostPrice CHECK (CostPrice >= 0)
CONSTRAINT CK_ComboItem_Quantity CHECK (Quantity > 0)
```

---

## 3. Transaction Handling Audit

Spring's `@Transactional` annotation is used across state-altering service methods:
- `SeatService.saveRoomLayout`: Annotated with `@Transactional`. Deletes existing seats in the room and batch inserts the new layout atomically.
- `SeatService.generateDefaultLayout`: Annotated with `@Transactional`. Generates and persists grid in a single transaction.
- `ShowtimeService.createShowtime` / `updateShowtime`: Annotated with `@Transactional`. Overlap checks and insertions run in a single transaction boundary.
- `FBItemService.createItem` / `updateItem`: Annotated with `@Transactional`. Manages parent `FBItem` and child `ComboItem` rows in a single unit of work.
- `CustomerProfileService.updateProfile`: Annotated with `@Transactional`.

---

## 4. Referential Integrity & Data Consistency Risks

### Risk 1: Seat to SeatType Soft Relationship (No Physical Foreign Key)
- In `Seat` table:
  `SeatTypeName NVARCHAR(50) NOT NULL DEFAULT 'Standard'`
- **Issue:** `Seat` does **not** declare a foreign key to `SeatType(SeatTypeID)`. It stores the string name.
- **Risk:** If a SeatType's name is renamed or deleted directly in the database, existing `Seat` records will retain orphaned strings, potentially breaking pricing calculations or visual rendering.

### Risk 2: Seat Layout Overwrite Cascading Deletes
- `SeatService.saveRoomLayout` executes `DELETE FROM Seat WHERE RoomID = ?` before inserting new layout seats.
- **Risk:** Once customer bookings (`Ticket` referencing `SeatID`) are introduced, executing a layout update will trigger foreign key violations or purge historical booking seat references.

### Risk 3: Missing DDL Scripts for Customer & Booking Tables
- `CustomerProfileDao` queries `Account`, `Customer`, and `MembershipRank`.
- `ShowtimeDao.hasActiveBookings()` queries `Booking`.
- **Issue:** None of these four tables (`Account`, `Customer`, `MembershipRank`, `Booking`) are defined in any of the `.sql` scripts in the `database/` folder.
- **Risk:** A fresh deployment executing only the scripts in `database/` will encounter `Invalid object name 'Account'` runtime SQL exceptions when accessing customer profiles or checking booking locks.

---

## 5. Required Baseline Test Data

To conduct comprehensive functional testing, the database must contain:
1. **Cinema Rooms:** At least 2 rooms with distinct formats (e.g., 1 Standard Laser with 96 seats, 1 IMAX with 180 seats).
2. **Seat Types:** Active `STD` (1.00), `VIP` (1.25), `COUPLE` (1.80), `FRONT` (0.90).
3. **Seats:** Full grid generated for test rooms.
4. **Genres:** Standard genres (Action, Drama, Animation, Sci-Fi, Horror).
5. **Movies:**
   - At least 1 `Now Showing` movie.
   - At least 1 `Upcoming` movie.
   - At least 1 `Ended` movie (to test BR-07 rejection).
6. **Ticket Pricing:** Weekday Standard (`PRICE-WD-STD`), Weekend Golden Hour (`PRICE-WE-GOLD`).
7. **F&B Items:** Popcorn (`Food`), Soda (`Drink`), and at least 1 `Combo` containing both.
