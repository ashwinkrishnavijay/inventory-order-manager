# Inventory & Order Management API Documentation

## Overview
A production-ready Spring Boot REST API for managing products, categories, inventory levels, and customer orders with automated stock updates and transactional integrity.

- **Base URL**: `http://localhost:8080/api`
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI v3 JSON**: `http://localhost:8080/v3/api-docs`
- **H2 Console (Dev)**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:inventorydb`, User: `sa`, Password: blank)

---

## Authentication Endpoints (`/api/auth`)

### 1. Register User / Admin
`POST /api/auth/register`
```json
{
  "name": "Jane Admin",
  "email": "jane@store.com",
  "password": "secretPassword",
  "role": "ADMIN"
}
```
*(Roles: `ADMIN`, `USER` - defaults to `USER` if omitted)*

### 2. Login
`POST /api/auth/login`
```json
{
  "email": "jane@store.com",
  "password": "secretPassword"
}
```
Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsIn...",
  "type": "Bearer",
  "id": 1,
  "name": "Jane Admin",
  "email": "jane@store.com",
  "role": "ADMIN"
}
```

---

## Category Management Endpoints (`/api/categories`)

- `POST /api/categories` - Create category *(ADMIN only)*
- `GET /api/categories` - View all categories
- `GET /api/categories/{id}` - View category by ID
- `PUT /api/categories/{id}` - Update category *(ADMIN only)*
- `DELETE /api/categories/{id}` - Delete category *(ADMIN only)*

---

## Product & Inventory Management Endpoints (`/api/products`)

- `POST /api/products` - Create product with categories and initial stock *(ADMIN only)*
- `PUT /api/products/{id}` - Update product details *(ADMIN only)*
- `PATCH /api/products/{id}/stock` - Update stock quantity directly *(ADMIN only)*
- `PATCH /api/products/{id}/status?status=ACTIVE|INACTIVE` - Activate / Deactivate product *(ADMIN only)*
- `DELETE /api/products/{id}` - Delete product *(ADMIN only)*
- `GET /api/products/{id}` - Get product details
- `GET /api/products` - Search and filter products:
  - Query parameters:
    - `keyword` (search by name)
    - `categoryId` (filter by category)
    - `status` (`ACTIVE` / `INACTIVE`)
    - `maxStock` (filter by max stock)
    - `sortBy` (e.g. `price`, `name`, `createdAt`)
    - `direction` (`asc` / `desc`)
    - `page`, `size`
- `GET /api/products/low-stock?threshold=5` - View low-stock products *(ADMIN only)*

---

## Order Management Endpoints (`/api/orders`)

- `POST /api/orders` - Place order *(USER / ADMIN)*
  - Validates stock availability.
  - Automatically reduces product stock quantity in the database.
  - Rejects with `400 Bad Request` (`InsufficientStockException`) if quantity exceeds stock.
  - Request body:
    ```json
    {
      "items": [
        {
          "productId": 1,
          "quantity": 2
        }
      ]
    }
    ```
- `GET /api/orders` - View order history (User views their own orders; Admin views all orders). Paginated.
- `GET /api/orders/{id}` - View single order details with item breakdown.
- `POST /api/orders/{id}/cancel` - Cancel order.
  - Restores stock quantity for all ordered items automatically using transactional consistency.
  - Sets order status to `CANCELLED`.
