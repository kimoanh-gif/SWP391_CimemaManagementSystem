-- =====================================================================
-- Cinema Management System - SWP391
-- Migration: P1-02 Seat Type Relationship Normalization
-- Target DBMS: Microsoft SQL Server 2019+
-- Purpose:
--   1. Transition Seat.SeatTypeName (NVARCHAR) to Seat.SeatTypeID (FK).
--   2. Preserve existing Seat.SeatTypeName for backward compatibility.
--   3. Safely backfill SeatTypeID for 100% of existing seats.
--   4. Add Foreign Key constraint FK_Seat_SeatType.
--   5. Idempotent & Non-destructive (safe to run multiple times).
-- =====================================================================

PRINT N'Starting P1-02 Seat Type Normalization migration...';

-- Step 1: Add SeatTypeID column if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Seat') AND name = 'SeatTypeID'
)
BEGIN
    PRINT N'Adding SeatTypeID column to Seat table...';
    ALTER TABLE Seat ADD SeatTypeID INT NULL;
    PRINT N'Column SeatTypeID added successfully.';
END
ELSE
BEGIN
    PRINT N'Column SeatTypeID already exists in Seat table.';
END
GO

-- Step 2: Backfill SeatTypeID based on existing SeatTypeName
PRINT N'Backfilling SeatTypeID from existing SeatTypeName values...';

-- 2.1 Map Standard / STD
UPDATE Seat 
SET SeatTypeID = (SELECT TOP 1 SeatTypeID FROM SeatType WHERE TypeCode = 'STD')
WHERE SeatTypeID IS NULL AND (SeatTypeName = 'Standard' OR SeatTypeName = 'STD' OR SeatTypeName LIKE N'%Tiêu Chuẩn%');

-- 2.2 Map VIP
UPDATE Seat 
SET SeatTypeID = (SELECT TOP 1 SeatTypeID FROM SeatType WHERE TypeCode = 'VIP')
WHERE SeatTypeID IS NULL AND (SeatTypeName = 'VIP' OR SeatTypeName LIKE N'%Royal Amber%');

-- 2.3 Map Couple / Sweetbox
UPDATE Seat 
SET SeatTypeID = (SELECT TOP 1 SeatTypeID FROM SeatType WHERE TypeCode = 'COUPLE')
WHERE SeatTypeID IS NULL AND (SeatTypeName = 'Couple' OR SeatTypeName = 'COUPLE' OR SeatTypeName LIKE N'%Sweetbox%');

-- 2.4 Map Front
UPDATE Seat 
SET SeatTypeID = (SELECT TOP 1 SeatTypeID FROM SeatType WHERE TypeCode = 'FRONT')
WHERE SeatTypeID IS NULL AND (SeatTypeName = 'Front' OR SeatTypeName = 'FRONT' OR SeatTypeName LIKE N'%Hàng Đầu%');

-- 2.5 Fallback for any unmatched active seat types: default to STD (ID 1)
UPDATE Seat 
SET SeatTypeID = (SELECT TOP 1 SeatTypeID FROM SeatType WHERE TypeCode = 'STD')
WHERE SeatTypeID IS NULL;

PRINT N'Backfill completed.';
GO

-- Step 3: Add Foreign Key Constraint FK_Seat_SeatType if not exists
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'FK_Seat_SeatType' AND parent_object_id = OBJECT_ID('Seat')
)
BEGIN
    PRINT N'Creating Foreign Key constraint FK_Seat_SeatType...';
    ALTER TABLE Seat
    ADD CONSTRAINT FK_Seat_SeatType FOREIGN KEY (SeatTypeID) 
        REFERENCES SeatType(SeatTypeID) ON DELETE NO ACTION;
    PRINT N'FK_Seat_SeatType constraint created successfully.';
END
ELSE
BEGIN
    PRINT N'Foreign Key FK_Seat_SeatType already exists.';
END
GO

-- Step 4: Add Index for fast FK joins
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_Seat_SeatTypeID' AND object_id = OBJECT_ID('Seat')
)
BEGIN
    PRINT N'Creating Index IX_Seat_SeatTypeID...';
    CREATE NONCLUSTERED INDEX IX_Seat_SeatTypeID ON Seat(SeatTypeID);
    PRINT N'Index IX_Seat_SeatTypeID created successfully.';
END
GO

-- Step 5: Verification
SELECT 
    COUNT(*) AS TotalSeats,
    COUNT(SeatTypeID) AS PopulatedSeatTypeIDs,
    SUM(CASE WHEN SeatTypeID IS NULL THEN 1 ELSE 0 END) AS UnmappedSeats
FROM Seat;

SELECT 
    st.SeatTypeID,
    st.TypeCode,
    st.TypeName,
    COUNT(s.SeatID) AS SeatCount
FROM SeatType st
LEFT JOIN Seat s ON st.SeatTypeID = s.SeatTypeID
GROUP BY st.SeatTypeID, st.TypeCode, st.TypeName
ORDER BY st.SeatTypeID;

PRINT N'P1-02 Migration finished successfully.';
GO
