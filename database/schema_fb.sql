-- =====================================================================
-- Cinema Management System - SWP391
-- Screen 61: Food & Beverage Management (Iteration 2 / UC-32)
-- Database Migration Script for FBItem and ComboItem Tables
-- Target DBMS: Microsoft SQL Server 2019+
-- Business Rules: BR-15 (Optional F&B), BR-51 (Snapshot pricing)
-- =====================================================================

-- 1. Create FBItem Table (Food, Drink, Combo)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'FBItem')
BEGIN
    CREATE TABLE FBItem (
        ItemID INT IDENTITY(1,1) PRIMARY KEY,
        ItemCode NVARCHAR(50) NOT NULL,            -- e.g. 'CB-ROYAL', 'PC-CARAMEL', 'DR-COKE'
        ItemName NVARCHAR(150) NOT NULL,
        Category NVARCHAR(50) NOT NULL,            -- 'Combo', 'Food', 'Drink', 'Snack'
        Description NVARCHAR(500) NULL,
        SellingPrice DECIMAL(12,2) NOT NULL,       -- Retail selling price (VND)
        CostPrice DECIMAL(12,2) NOT NULL DEFAULT 0.0, -- COGS (Cost of goods sold)
        Image NVARCHAR(255) NULL,
        Status NVARCHAR(20) NOT NULL CONSTRAINT DF_FBItem_Status DEFAULT 'Active', -- 'Active', 'Inactive'
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_FBItem_CreatedAt DEFAULT SYSUTCDATETIME(),
        UpdatedAt DATETIME2 NULL,
        CONSTRAINT UQ_FBItem_ItemCode UNIQUE (ItemCode),
        CONSTRAINT CK_FBItem_SellingPrice CHECK (SellingPrice >= 0),
        CONSTRAINT CK_FBItem_CostPrice CHECK (CostPrice >= 0)
    );
END;
GO

-- 2. Create ComboItem Table for combo components (relationship between combo and its food/drink items)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'ComboItem')
BEGIN
    CREATE TABLE ComboItem (
        ComboItemID INT IDENTITY(1,1) PRIMARY KEY,
        ComboID INT NOT NULL,
        ComponentItemID INT NOT NULL,
        Quantity INT NOT NULL DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_ComboItem_CreatedAt DEFAULT SYSUTCDATETIME(),
        CONSTRAINT FK_ComboItem_Combo FOREIGN KEY (ComboID) REFERENCES FBItem(ItemID) ON DELETE CASCADE,
        CONSTRAINT FK_ComboItem_Component FOREIGN KEY (ComponentItemID) REFERENCES FBItem(ItemID) ON DELETE NO ACTION,
        CONSTRAINT CK_ComboItem_Quantity CHECK (Quantity > 0)
    );
END;
GO

-- 3. Seed Standard F&B Products
IF NOT EXISTS (SELECT 1 FROM FBItem WHERE ItemCode = 'CB-COUPLE')
BEGIN
    INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status)
    VALUES ('CB-COUPLE', N'Combo Couple Sweetbox', 'Combo', N'1 Bắp rang bơ cỡ lớn (Phô mai/Caramel) + 2 Nước ngọt có ga lớn 32oz', 119000.00, 35000.00, 'https://images.unsplash.com/photo-1585647347483-22b66260dfff?w=400', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM FBItem WHERE ItemCode = 'CB-SOLO')
BEGIN
    INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status)
    VALUES ('CB-SOLO', N'Combo Solo Cine', 'Combo', N'1 Bắp rang bơ vị ngọt vừa 24oz + 1 Nước ngọt có ga 22oz', 79000.00, 22000.00, 'https://images.unsplash.com/photo-1572177191856-3cde618dee1f?w=400', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM FBItem WHERE ItemCode = 'CB-PARTY')
BEGIN
    INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status)
    VALUES ('CB-PARTY', N'Combo Siêu Tiệc Party 4 Người', 'Combo', N'2 Bắp rang bơ Jumbo đặc biệt + 4 Nước ngọt lớn + 1 Khoai tây chiên giòn', 229000.00, 68000.00, 'https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=400', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM FBItem WHERE ItemCode = 'PC-CARAMEL')
BEGIN
    INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status)
    VALUES ('PC-CARAMEL', N'Bắp Rang Bơ Vị Caramel Thượng Hạng', 'Food', N'Bắp nổ hảo hạng tẩm sốt caramel béo ngậy giòn tan.', 55000.00, 15000.00, 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=400', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM FBItem WHERE ItemCode = 'PC-CHEESE')
BEGIN
    INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status)
    VALUES ('PC-CHEESE', N'Bắp Rang Bơ Vị Phô Mai Lắc', 'Food', N'Bắp nổ lắc cùng bột phô mai Cheddar đậm đà thơm lừng.', 55000.00, 16000.00, 'https://images.unsplash.com/photo-1512058564366-18510be2db19?w=400', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM FBItem WHERE ItemCode = 'DR-COKE')
BEGIN
    INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status)
    VALUES ('DR-COKE', N'Coca-Cola Tươi 32oz (Size Lớn)', 'Drink', N'Nước ngọt có ga sảng khoái mát lạnh.', 32000.00, 8000.00, 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM FBItem WHERE ItemCode = 'DR-SPRITE')
BEGIN
    INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status)
    VALUES ('DR-SPRITE', N'Sprite Chanh Tươi 32oz (Size Lớn)', 'Drink', N'Hương chanh thanh mát đã khát.', 32000.00, 8000.00, 'https://images.unsplash.com/photo-1625772299848-391b6a87d7b3?w=400', 'Active');
END;

IF NOT EXISTS (SELECT 1 FROM FBItem WHERE ItemCode = 'SN-HOTDOG')
BEGIN
    INSERT INTO FBItem (ItemCode, ItemName, Category, Description, SellingPrice, CostPrice, Image, Status)
    VALUES ('SN-HOTDOG', N'Bánh Mì Kẹp Xúc Xích Phô Mai Nóng', 'Snack', N'Xúc xích Đức nướng xốt mù tạt vàng và sốt cà chua.', 45000.00, 18000.00, 'https://images.unsplash.com/photo-1619740455993-9e612b1af08a?w=400', 'Active');
END;
GO
