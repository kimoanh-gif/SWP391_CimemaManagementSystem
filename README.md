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

## Local database configuration

Do not commit database credentials. Each member creates their own local file:

1. Copy `src/main/resources/application-local-example.properties`.
2. Rename the copied file to `application-local.properties`.
3. Fill in the local SQL Server values:

   ```properties
   DB_URL=jdbc:sqlserver://localhost:1433;databaseName=CinemaManagementDB;encrypt=true;trustServerCertificate=true
   DB_USERNAME=your_sql_server_username
   DB_PASSWORD=your_sql_server_password
   ```

`application-local.properties` is ignored by Git, so it must not be staged or committed.
