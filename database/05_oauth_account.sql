-- =====================================================================
-- 05_oauth_account.sql
-- Mapping between local Account rows and external identity providers
-- (Google, …). One local account can be linked to multiple providers.
-- Run after 04_account_columns.sql.
-- =====================================================================
USE CinemaManagementDB;
GO

IF OBJECT_ID('dbo.OAuthAccount', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.OAuthAccount (
        OAuthAccountID         BIGINT IDENTITY(1,1) NOT NULL,
        AccountID              INT          NOT NULL,
        Provider               VARCHAR(20)  NOT NULL,             -- GOOGLE, …
        ProviderSubject        VARCHAR(255) NOT NULL,             -- OIDC `sub`
        ProviderEmail          VARCHAR(255) NULL,
        EmailVerifiedByProvider BIT         NOT NULL
            CONSTRAINT DF_OAuthAccount_EmailVerified DEFAULT (0),
        LinkedAt               DATETIME2    NOT NULL
            CONSTRAINT DF_OAuthAccount_LinkedAt DEFAULT (SYSDATETIME()),
        LastLoginAt            DATETIME2    NULL,
        CONSTRAINT PK_OAuthAccount PRIMARY KEY (OAuthAccountID),
        CONSTRAINT UQ_OAuthAccount_Provider_Subject UNIQUE (Provider, ProviderSubject),
        CONSTRAINT FK_OAuthAccount_Account FOREIGN KEY (AccountID)
            REFERENCES dbo.Account(AccountID),
        CONSTRAINT CK_OAuthAccount_Provider CHECK (Provider IN ('GOOGLE'))
    );
END
GO

CREATE INDEX IX_OAuthAccount_Account ON dbo.OAuthAccount(AccountID);
GO
