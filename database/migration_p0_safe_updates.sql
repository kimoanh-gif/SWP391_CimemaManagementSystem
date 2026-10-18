-- =====================================================================
-- Cinema Management System - SWP391
-- Database Migration Script for P0 Critical Fixes
-- Target DBMS: Microsoft SQL Server 2019+
-- Scope:
--   1. Verify/Enforce UQ_Seat_Room_Position on Seat (RoomID, RowLabel, SeatNumber)
--   2. Ensure Room.UpdatedAt exists for Optimistic Concurrency Control
--   3. Non-destructive: Does NOT delete, drop, or alter existing data
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. PRE-CHECK: Check for any existing duplicate seat positions
-- ---------------------------------------------------------------------
PRINT 'Checking for duplicate seat positions before applying unique constraint...';

SELECT RoomID, RowLabel, SeatNumber, COUNT(*) AS DuplicateCount
FROM Seat
GROUP BY RoomID, RowLabel, SeatNumber
HAVING COUNT(*) > 1;

IF @@ROWCOUNT > 0
BEGIN
    RAISERROR('Aborting migration: Duplicate seat positions detected in Seat table. Manual cleanup required before adding unique constraint.', 16, 1);
    RETURN;
END;
PRINT 'Pre-check PASSED: No duplicate seat positions found.';
GO

-- ---------------------------------------------------------------------
-- 2. ENSURE Room.UpdatedAt COLUMN EXISTS
-- ---------------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('Room') AND name = 'UpdatedAt'
)
BEGIN
    PRINT 'Adding UpdatedAt column to Room table...';
    ALTER TABLE Room ADD UpdatedAt DATETIME2 NULL;
    PRINT 'Room.UpdatedAt column added successfully.';
END
ELSE
BEGIN
    PRINT 'Room.UpdatedAt column already exists. Skipping.';
END;
GO

-- ---------------------------------------------------------------------
-- 3. ENSURE UNIQUE CONSTRAINT UQ_Seat_Room_Position EXISTS
-- ---------------------------------------------------------------------
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE object_id = OBJECT_ID('Seat') AND name = 'UQ_Seat_Room_Position'
)
BEGIN
    PRINT 'Adding UQ_Seat_Room_Position unique constraint to Seat table...';
    ALTER TABLE Seat
    ADD CONSTRAINT UQ_Seat_Room_Position UNIQUE (RoomID, RowLabel, SeatNumber);
    PRINT 'UQ_Seat_Room_Position constraint created successfully.';
END
ELSE
BEGIN
    PRINT 'UQ_Seat_Room_Position unique constraint already exists. Skipping.';
END;
GO

PRINT 'P0 Migration completed successfully.';

-- =====================================================================
-- ROLLBACK SCRIPT (Run only if rollback is required)
-- =====================================================================
/*
-- Rollback constraint:
IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('Seat') AND name = 'UQ_Seat_Room_Position')
BEGIN
    ALTER TABLE Seat DROP CONSTRAINT UQ_Seat_Room_Position;
END;

-- Rollback column (optional):
-- ALTER TABLE Room DROP COLUMN UpdatedAt;
*/
