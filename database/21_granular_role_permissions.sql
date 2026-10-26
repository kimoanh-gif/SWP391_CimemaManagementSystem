-- Optional manual upgrade. The application performs the same upgrade at startup.
-- Grants defaults ONLY for catalog entries introduced by this run.
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @Introduced TABLE (PermissionID INT, Name NVARCHAR(100));
INSERT INTO dbo.Permission (Name, Description)
OUTPUT inserted.PermissionID, inserted.Name INTO @Introduced
SELECT v.Name, v.Description
FROM (VALUES
    (N'booking:read', N'Xem danh sách và chi tiết đặt vé'),
    (N'news:manage', N'Soạn và quản lý bài viết'),
    (N'news:approve', N'Duyệt và xuất bản bài viết'),
    (N'movie:manage', N'Quản lý phim'),
    (N'genre:manage', N'Quản lý thể loại phim'),
    (N'room:manage', N'Quản lý phòng chiếu'),
    (N'seat:manage', N'Quản lý loại ghế và sơ đồ ghế'),
    (N'pricing:manage', N'Quản lý giá vé'),
    (N'fb:manage', N'Quản lý đồ ăn và thức uống'),
    (N'voucher:manage', N'Quản lý voucher'),
    (N'promotion:manage', N'Quản lý khuyến mãi')
) v(Name, Description)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Permission WITH (UPDLOCK, HOLDLOCK) p WHERE p.Name = v.Name);

INSERT INTO dbo.RolePermission (RoleID, PermissionID)
SELECT r.RoleID, p.PermissionID
FROM dbo.[Role] r JOIN @Introduced p ON
    UPPER(r.Name) = 'ADMIN'
    OR (UPPER(r.Name) = 'STAFF' AND p.Name IN (N'movie:read', N'booking:create', N'ticket:checkin', N'booking:read', N'news:manage'))
    OR (UPPER(r.Name) = 'MANAGER' AND p.Name IN (N'movie:read', N'showtime:manage', N'report:view', N'booking:read', N'news:approve', N'movie:manage', N'genre:manage', N'room:manage', N'seat:manage', N'pricing:manage', N'fb:manage', N'voucher:manage', N'promotion:manage'))
WHERE NOT EXISTS (SELECT 1 FROM dbo.RolePermission rp WHERE rp.RoleID = r.RoleID AND rp.PermissionID = p.PermissionID);
COMMIT TRANSACTION;
