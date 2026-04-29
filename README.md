# SLMS Backend

Smart Logistics Management System (SLMS) backend service. It provides REST APIs for auth, product catalog, cart, orders, shipments, inventory, reporting, and batch jobs.

## Highlights
- JWT authentication and role-based access (CUSTOMER, STAFF, ADMIN)
- Product catalog, cart, and order lifecycle
- Shipment tracking and updates
- Inventory adjustments
- Reporting

## Tech Stack
- Java 17
- Spring Boot
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL (primary database)
- Redis (optional cache)

## Prerequisites
- JDK 17
- PostgreSQL running locally
- Redis (optional)

## Configuration
Update database credentials in:
- [src/main/resources/application-local.properties](src/main/resources/application-local.properties)

Default port is 8080 (see [src/main/resources/application.properties](src/main/resources/application.properties)).

## Database Setup
1. Create database "slms" in Postgres.
2. Initialize schema:
   - Run [src/main/resources/db/migration/V1__init_schema.sql](src/main/resources/db/migration/V1__init_schema.sql)
3. Seed products and accounts:
   - Run [src/main/resources/data.sql](src/main/resources/data.sql)

## Run
From the project root, run:

```powershell
mvn spring-boot:run
```

API base URL:

```text
http://localhost:8080/api
```