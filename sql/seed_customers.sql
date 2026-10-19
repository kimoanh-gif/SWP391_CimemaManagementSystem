-- Xóa 10 account test cũ
DELETE FROM dbo.Customer WHERE AccountID IN (
    SELECT AccountID FROM dbo.Account WHERE Username LIKE 'testuser%'
);
DELETE FROM dbo.Account WHERE Username LIKE 'testuser%';

-- Tạo lại 10 account test với tiếng Việt đúng encoding
DECLARE @RoleID_Customer INT;
SELECT @RoleID_Customer = RoleID FROM dbo.[Role] WHERE UPPER(LTRIM(RTRIM(Name))) = 'CUSTOMER';

DECLARE @i INT = 1;
WHILE @i <= 10
BEGIN
    INSERT INTO dbo.Account (RoleID, Username, Email, PasswordHash, FullName, Phone, Status)
    VALUES (
        @RoleID_Customer,
        'testuser' + CAST(@i AS VARCHAR(10)),
        'test' + CAST(@i AS VARCHAR(10)) + '@mailinator.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBqvq0v9m9vq8z5x5j5k5j5k5j5k5',
        N'Người dùng Test ' + CAST(@i AS VARCHAR(10)),
        '09' + RIGHT('000000000' + CAST(@i AS VARCHAR(10)), 9),
        'ACTIVE'
    );
    
    DECLARE @AccountID INT;
    SELECT @AccountID = SCOPE_IDENTITY();
    
    INSERT INTO dbo.Customer (AccountID, AvailablePoints, TierPoints, ReceivePromotionEmail)
    VALUES (@AccountID, 0, 0, 0);
    
    SET @i = @i + 1;
END;
