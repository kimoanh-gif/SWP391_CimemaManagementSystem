UPDATE dbo.Account SET PasswordHash = '$2a$10$gTgnEHa0uGCd/4Ar.J0iPOZg42c2UFDlmshEjBlHB1W0nAx4YQY0W' WHERE Email = 'admin@cinema.com';
UPDATE dbo.Account SET PasswordHash = '$2a$10$zViO0Dbrl/vo/3ZnU8F02eEaFoJpg0qGXUW8Jl99b1WnRsxTPF56y' WHERE Email = 'manager@cinema.com';
UPDATE dbo.Account SET PasswordHash = '$2a$10$WSGGgdTBdoLmreWH70vuMO0WUqBhmK.L62RBfmFkDON9pnHu3Fyie' WHERE Email = 'staff@cinema.com';
UPDATE dbo.Account SET PasswordHash = '$2a$10$.yKwrJWcHpGm.6nH/0uUJee6t/BvdC7Oc6h/vtM78DkD6ctvtWjH.' WHERE Email = 'customer@cinema.com' OR Email = 'nguyenvana@gmail.com';
SELECT Email, PasswordHash FROM dbo.Account;
