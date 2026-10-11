# Cinema Management System

SWP391 project.

## Architecture

MVC + Service + DAO

View → Controller → Service → DAO → SQL Server

- **View**: Thymeleaf templates in `src/main/resources/templates`.
- **Controller**: Handles HTTP requests and selects views.
- **Service**: Holds business logic and calls DAOs.
- **DAO**: Performs JDBC database access and maps results to model classes.
- **Model**: Shared data classes used across the layers.

## Technology

- Java 21
- Spring Boot
- Maven
- Thymeleaf
- SQL Server
- JDBC

## Getting started

1. Clone the repository.
2. Configure SQL Server environment variables. In PowerShell for the current session:

   ```powershell
   $env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=CinemaManagementDB;encrypt=true;trustServerCertificate=true"
   $env:DB_USERNAME="your_username"
   $env:DB_PASSWORD="your_password"
   ```

   See `src/main/resources/application-example.properties` for the expected values. Do not put real credentials in source control.
3. Run the application:

   ```powershell
   mvn spring-boot:run
   ```
4. Open http://localhost:8080.

## Detailed team guide

### Prerequisites

Install Java 21, Maven 3.9+, SQL Server, SSMS, Git, and GitHub Desktop.

Verify Java and Maven in PowerShell:

```powershell
java -version
mvn -version
```

Java must be version 21. Maven must be available as the `mvn` command. If `mvn` is not recognized, install Maven or add its `bin` directory to Windows `Path`.

## Database setup

The website connects to a SQL Server database named `CinemaManagementDB`.

1. Obtain the approved team schema script, for example `CinemaManagementSystem.sql`.
2. Open it in SSMS.
3. Execute it once.
4. Refresh `CinemaManagementDB` in Object Explorer.

Expected tables include:

```text
Account, Customer, MembershipRank, Movie, Showtime,
Booking, BookingSeat, Payment, Ticket, FoodBeverageItem
```

Do not re-run a full `CREATE TABLE` script after the tables have been created, because SQL Server will report existing-object errors.

Verify the tables with:

```sql
USE CinemaManagementDB;

SELECT s.name AS SchemaName, t.name AS TableName
FROM sys.tables t
JOIN sys.schemas s ON s.schema_id = t.schema_id
ORDER BY s.name, t.name;
```

## Local database configuration

Credentials must never be committed to Git.

`src/main/resources/application.properties` is the shared configuration. It reads local values from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

Each developer must create a local configuration file:

1. Copy `src/main/resources/application-local-example.properties`.
2. Rename the copy to `src/main/resources/application-local.properties`.
3. Enter local SQL Server credentials:

   ```properties
   DB_URL=jdbc:sqlserver://localhost:1433;databaseName=CinemaManagementDB;encrypt=true;trustServerCertificate=true
   DB_USERNAME=your_sql_server_username
   DB_PASSWORD=your_sql_server_password
   ```

`application-local.properties` is ignored by Git. Do not stage or commit it.

## Run the website

From the repository root:

```powershell
mvn spring-boot:run
```

Successful startup contains:

```text
Tomcat started on port 8080 (http)
Started CinemaManagementApplication
```

Open the website:

```text
http://localhost:8080
```

Stop the local server with `Ctrl + C`.

## Customer Profile

The current feature supports:

- Viewing customer personal and account information.
- Viewing membership rank, available points, and tier points.
- Updating full name, phone number, date of birth, and address.
- Validating input before save.

Authentication is not implemented yet. For development only, the Profile routes use an `accountId` query parameter:

```text
http://localhost:8080/customer/profile?accountId=1
http://localhost:8080/customer/profile/edit?accountId=1
```

Replace `1` with a real `AccountID` linked to a row in `Customer`. This temporary approach must be replaced by the authenticated account/session after Login is implemented.

## Git workflow

`develop` is the integration branch. Do not push feature work directly to `main`.

```text
feature/<feature-name> -> Pull Request -> develop
```

Example:

```text
feature/customer-profile -> develop
```

Workflow:

1. Pull the latest `develop`.
2. Create a feature branch.
3. Implement and test only the assigned feature.
4. Commit relevant files only.
5. Push the feature branch.
6. Create a Pull Request with `develop` as the base branch.
7. Review before merging.

Never commit:

```text
application-local.properties
target/
.env
```

Example commit message:

```text
feat(customer): add profile and edit profile
```

## Folder overview

```text
src/main/java/com/cinema/
  controller/    HTTP controllers
  service/       Business logic
  dao/           JDBC data access
  model/         Java models and forms

src/main/resources/
  templates/     Thymeleaf pages
  static/        CSS, JavaScript, images

database/        Approved schema and seed scripts
docs/            Project documentation
```
