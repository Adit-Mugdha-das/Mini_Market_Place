# REST API Documentation

## Overview
This project includes a REST API layer alongside the Thymeleaf web application. The API endpoints follow RESTful principles and return JSON responses. Authentication is handled via session cookies (same as the web app).

## Endpoints

### 1. Products (`/api/products`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/products` | List/Search products | Authenticated |
| `GET` | `/api/products/{id}` | Get product details | Authenticated |
| `POST` | `/api/products` | Create a new product | Seller, Admin |
| `PUT` | `/api/products/{id}` | Update a product | Seller, Admin |
| `DELETE` | `/api/products/{id}` | Delete a product | Seller, Admin |

**Sample Request (POST /api/products):**
```json
{
  "name": "New Product",
  "description": "Description",
  "price": 99.99,
  "quantity": 10,
  "categoryId": 1
}
```

### 2. Orders (`/api/orders`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/orders` | List my orders | Buyer |
| `GET` | `/api/orders/{id}` | Get my order details | Buyer |
| `POST` | `/api/orders` | Place a new order | Buyer |
| `PUT` | `/api/orders/{id}` | Advance status (Seller) | Seller, Admin |
| `DELETE` | `/api/orders/{id}` | Cancel order | Buyer |

**Sample Request (POST /api/orders):**
```json
{
  "productId": 1,
  "quantity": 2,
  "paymentMethod": "CASH_ON_DELIVERY"
}
```

### 3. Reviews (`/api/reviews`)
| Method | Endpoint | Description | Role |
|---|---|---|---|
| `GET` | `/api/reviews` | List all reviews | Authenticated |
| `GET` | `/api/reviews/{id}` | Get review details | Authenticated |
| `POST` | `/api/reviews` | Submit a review | Buyer |
| `PUT` | `/api/reviews/{id}` | Edit a review | Buyer |
| `DELETE` | `/api/reviews/{id}` | Delete a review | Buyer, Admin |

**Sample Request (POST /api/reviews):**
```json
{
  "productId": 1,
  "rating": 5,
  "comment": "Great product!"
}
```

## Error Handling
The API returns standard JSON error responses:
```json
{
  "timestamp": "2023-10-27T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Error details..."
}
```
