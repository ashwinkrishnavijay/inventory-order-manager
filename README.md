# Inventory & Order Management API

A secure, scalable Spring Boot 3 REST API for managing products, categories, inventory levels, and customer orders. The system automatically manages inventory stock levels across order placement, fulfillment, and cancellations with ACID transactional guarantees.

---

## Features

1. **Authentication & Role-Based Access Control (RBAC)**
   - JWT-based authentication with BCrypt password hashing.
   - Roles: `ADMIN` and `USER`.
   - Access control rules enforced via Spring Security `@PreAuthorize`:
     - **ADMIN**: Full control over products, categories, and inventory.
     - **USER**: Browse products, place orders, view order history, and cancel orders.

2. **Category Management (Admin)**
   - Full CRUD operations on categories.
   - Many-to-many relationship: products can belong to multiple categories.

3. **Product & Inventory Management (Admin)**
   - Create, update, and delete products with name, description, price, stock, and categories.
   - Activate / Deactivate product status (`ACTIVE` / `INACTIVE`).
   - Direct stock adjustments and low-stock threshold queries.

4. **Order Management & Stock Automation (Transactional)**
   - Pre-order stock verification.
   - Atomic stock reduction upon order confirmation.
   - Prevention of orders exceeding available stock with custom `InsufficientStockException`.
   - Automatic stock restoration upon order cancellation.
   - Order statuses: `CREATED`, `CONFIRMED`, `CANCELLED`.

5. **Advanced Querying & Pagination**
   - Filter products by category, status, and maximum stock.
   - Search products by name (case-insensitive substring).
   - Dynamic sorting (e.g. by price ascending or descending).
   - Paginated responses for products and order history.

6. **Interactive Documentation & API Client**
   - OpenAPI 3 / Swagger UI embedded at `/swagger-ui.html`.
   - Complete Postman collection included (`inventory-order-manager.postman_collection.json`).

---

## Tech Stack

- **Java**: 17
- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security 6, JJWT 0.12.6, BCrypt
- **Persistence**: Spring Data JPA & Hibernate
- **Database**: H2 (in-memory default), PostgreSQL, MySQL
- **Documentation**: SpringDoc OpenAPI 2.6.0 (Swagger 3)
- **Containerization**: Dockerfile & Docker Compose

---

## Getting Started

### Local Execution
```bash
cd /Users/b/.gemini/antigravity/scratch/inventory-order-manager
mvn clean test
mvn spring-boot:run
```
The API starts on port `8080`.

### Docker Execution
```bash
docker compose up --build
```

---

## Endpoints Summary

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
- **Detailed Specifications**: See [API_DOCUMENTATION.md](API_DOCUMENTATION.md).
