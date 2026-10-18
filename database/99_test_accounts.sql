-- =====================================================================
-- Test accounts for all roles — ready to run on CinemaManagementDB.
-- Password format: BCrypt strength-10 hash (Spring Security default).
-- Login with: email / password below.
-- =====================================================================
USE CinemaManagementDB;
GO

-- Remove existing test accounts so we start fresh
-- Order matters: delete child rows first (FK constraints)
DELETE FROM Customer WHERE AccountID IN (SELECT AccountID FROM Account WHERE Email LIKE '%@cinema.com');
DELETE FROM Staff    WHERE AccountID IN (SELECT AccountID FROM Account WHERE Email LIKE '%@cinema.com');
DELETE FROM Account  WHERE Email LIKE '%@cinema.com';
GO

DECLARE @admin_hash  VARCHAR(255) = '$BCRYPT_ADMIN_HASH_PLACEHOLDER$';
DECLARE @manager_hash VARCHAR(255) = '$BCRYPT_MANAGER_HASH_PLACEHOLDER$';
DECLARE @staff_hash  VARCHAR(255) = '$BCRYPT_STAFF_HASH_PLACEHOLDER$';
DECLARE @cust_hash   VARCHAR(255) = '$BCRYPT_CUSTOMER_HASH_PLACEHOLDER$';

DECLARE @admin_rid  INT = (SELECT RoleID FROM [Role] WHERE Name = 'ADMIN');
DECLARE @manager_rid INT = (SELECT RoleID FROM [Role] WHERE Name = 'MANAGER');
DECLARE @staff_rid  INT = (SELECT RoleID FROM [Role] WHERE Name = 'STAFF');
DECLARE @cust_rid   INT = (SELECT RoleID FROM [Role] WHERE Name = 'CUSTOMER');

-- ADMIN
IF NOT EXISTS (SELECT 1 FROM Account WHERE Email = 'admin@cinema.com')
BEGIN
    INSERT INTO Account (RoleID, Username, Email, PasswordHash, FullName, Phone, [Status])
    VALUES (@admin_rid, 'admin', 'admin@cinema.com', @admin_hash, N'Quản trị viên', '0900000001', 'ACTIVE');
    PRINT 'Created: admin@cinema.com / Admin1234';
END

-- MANAGER
IF NOT EXISTS (SELECT 1 FROM Account WHERE Email = 'manager@cinema.com')
BEGIN
    INSERT INTO Account (RoleID, Username, Email, PasswordHash, FullName, Phone, [Status])
    VALUES (@manager_rid, 'manager', 'manager@cinema.com', @manager_hash, N'Người quản lý', '0900000002', 'ACTIVE');
    PRINT 'Created: manager@cinema.com / Manager123';
END

-- STAFF
IF NOT EXISTS (SELECT 1 FROM Account WHERE Email = 'staff@cinema.com')
BEGIN
    INSERT INTO Account (RoleID, Username, Email, PasswordHash, FullName, Phone, [Status])
    VALUES (@staff_rid, 'staff', 'staff@cinema.com', @staff_hash, N'Nhân viên', '0900000003', 'ACTIVE');
    PRINT 'Created: staff@cinema.com / Staff1234';
END

-- CUSTOMER
IF NOT EXISTS (SELECT 1 FROM Account WHERE Email = 'customer@cinema.com')
BEGIN
    INSERT INTO Account (RoleID, Username, Email, PasswordHash, FullName, Phone, [Status])
    VALUES (@cust_rid, 'customer', 'customer@cinema.com', @cust_hash, N'Khách hàng', '0900000004', 'ACTIVE');
    PRINT 'Created: customer@cinema.com / Customer123';
END

-- Link Customer to Customer table
IF NOT EXISTS (SELECT 1 FROM Customer WHERE AccountID = (SELECT AccountID FROM Account WHERE Email = 'customer@cinema.com'))
BEGIN
    INSERT INTO Customer (AccountID, RankID, DateOfBirth, [Address], AvailablePoints, TierPoints, ReceivePromotionEmail)
    VALUES (
        (SELECT AccountID FROM Account WHERE Email = 'customer@cinema.com'),
        (SELECT RankID FROM MembershipRank WHERE Name = 'STANDARD'),
        '2000-01-01',
        N'Ho Chi Minh City',
        0, 0, 1
    );
END

-- Link Staff to Staff table
IF NOT EXISTS (SELECT 1 FROM Staff WHERE AccountID = (SELECT AccountID FROM Account WHERE Email = 'staff@cinema.com'))
BEGIN
    INSERT INTO Staff (AccountID, Position, [Status])
    VALUES (
        (SELECT AccountID FROM Account WHERE Email = 'staff@cinema.com'),
        N'Nhân viên bán vé',
        'ACTIVE'
    );
END

PRINT '';
PRINT '=== Test Accounts Summary ===';
SELECT Username, Email, FullName, r.Name AS Role, a.[Status]
FROM Account a
JOIN [Role] r ON a.RoleID = r.RoleID
WHERE Email LIKE '%@cinema.com'
ORDER BY r.RoleID;
GO
