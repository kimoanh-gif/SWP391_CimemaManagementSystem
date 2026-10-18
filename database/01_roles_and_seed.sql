-- =====================================================================
-- 01_roles_and_seed.sql
-- Ensure the four application roles exist and provide a permissions table
-- that maps to the existing Role table in CinemaManagementSystem.sql.
-- Run after CinemaManagementSystem.sql.
-- =====================================================================
USE CinemaManagementDB;
GO

-- ------------------------------------------------------------------ Role
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE Name = 'CUSTOMER')
    INSERT INTO dbo.[Role] (Name, Description) VALUES ('CUSTOMER', N'Khách hàng');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE Name = 'STAFF')
    INSERT INTO dbo.[Role] (Name, Description) VALUES ('STAFF', N'Nhân viên');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE Name = 'MANAGER')
    INSERT INTO dbo.[Role] (Name, Description) VALUES (N'MANAGER', N'Quản lý rạp');
IF NOT EXISTS (SELECT 1 FROM dbo.[Role] WHERE Name = 'ADMIN')
    INSERT INTO dbo.[Role] (Name, Description) VALUES ('ADMIN', N'Quản trị hệ thống');
GO

-- ----------------------------------------------------- Permission (table)
IF OBJECT_ID('dbo.Permission', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Permission (
        PermissionID INT IDENTITY(1,1) NOT NULL,
        Name NVARCHAR(100) NOT NULL,
        Description NVARCHAR(255) NULL,
        CONSTRAINT PK_Permission PRIMARY KEY (PermissionID),
        CONSTRAINT UQ_Permission_Name UNIQUE (Name)
    );
END
GO

-- ----------------------------------------------------- RolePermission (M2M)
IF OBJECT_ID('dbo.RolePermission', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.RolePermission (
        RoleID INT NOT NULL,
        PermissionID INT NOT NULL,
        CONSTRAINT PK_RolePermission PRIMARY KEY (RoleID, PermissionID),
        CONSTRAINT FK_RolePermission_Role FOREIGN KEY (RoleID) REFERENCES dbo.[Role](RoleID),
        CONSTRAINT FK_RolePermission_Permission FOREIGN KEY (PermissionID) REFERENCES dbo.Permission(PermissionID)
    );
END
GO

-- -------------------------------------------------- seed core permissions
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE Name = 'movie:read')
    INSERT INTO dbo.Permission (Name, Description) VALUES ('movie:read', N'Xem phim & suất chiếu');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE Name = 'booking:create')
    INSERT INTO dbo.Permission (Name, Description) VALUES ('booking:create', N'Đặt vé');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE Name = 'ticket:checkin')
    INSERT INTO dbo.Permission (Name, Description) VALUES ('ticket:checkin', N'Check-in vé');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE Name = 'showtime:manage')
    INSERT INTO dbo.Permission (Name, Description) VALUES ('showtime:manage', N'Quản lý suất chiếu');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE Name = 'account:manage')
    INSERT INTO dbo.Permission (Name, Description) VALUES ('account:manage', N'Quản lý người dùng');
IF NOT EXISTS (SELECT 1 FROM dbo.Permission WHERE Name = 'report:view')
    INSERT INTO dbo.Permission (Name, Description) VALUES ('report:view', N'Xem báo cáo / thống kê');
GO
