# Ecommerce Order Service

Microservice for order management and processing in the Ecommerce Platform.

## Overview

The Order Service handles the complete order lifecycle including:
- Order creation from cart
- Order status management
- Order tracking
- Payment status updates
- Order history
- Integration with cart and payment services

## Features

- ✅ **Order Management**: Full order lifecycle management
- ✅ **Cart Integration**: Create orders from shopping cart
- ✅ **Order Tracking**: Track order status and delivery
- ✅ **Payment Integration**: Payment status updates via Kafka
- ✅ **Order History**: User order history with pagination
- ✅ **Kafka Integration**: Event-driven architecture
- ✅ **Service Discovery**: Eureka client integration
- ✅ **OpenFeign**: Service-to-service communication
- ✅ **MySQL Database**: Persistent order storage
- ✅ **RESTful API**: Comprehensive REST endpoints

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- MySQL 8.0+ (Port 3308)
- Kafka (Port 9092)
- Eureka Server (Port 8761)
- Cart Service (for checkout)
- Payment Service (for payment processing)

### Database Setup

1. **Create MySQL Database**:
   ```sql
   CREATE DATABASE order_service_db;
   ```

### Running Locally

1. **Build the project**:
   ```bash
   mvn clean package
   ```

2. **Run the application**:
   ```bash
   java -jar target/order-service-1.0.0.jar
   ```

3. **Or use Maven**:
   ```bash
   mvn spring-boot:run
   ```

The service will start on `http://localhost:8084`

## API Endpoints

### Order Endpoints

#### Checkout - Create Order from Cart
```http
POST /api/orders/checkout/{userId}
Content-Type: application/json

{
  "shippingAddress": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "paymentMethod": "CREDIT_CARD",
  "paymentGatewayId": 1
}
```

#### Get Order by ID
```http
GET /api/orders/{id}
```

#### Get Order by Order Number
```http
GET /api/orders/number/{orderNumber}
```

#### Get User Orders
```http
GET /api/orders/user/{userId}?page=0&size=10
```

#### Update Order Status (Admin)
```http
PUT /api/orders/{id}/status?status=PROCESSING
```

#### Update Payment Status (Internal)
```http
PUT /api/orders/{id}/payment-status?paymentStatus=PAID
```

#### Track Order by Order Number (Public)
```http
GET /api/orders/track/{orderNumber}
```

#### Track Order by ID
```http
GET /api/orders/{orderId}/track
```

#### Update Tracking Number (Admin)
```http
PUT /api/orders/{orderId}/tracking?trackingNumber=TRACK123456
```

## Configuration

### Application Configuration (application.yml)

```yaml
server:
  port: 8084

spring:
  application:
    name: order-service
  datasource:
    url: jdbc:mysql://localhost:3308/order_service_db
    username: root
    password: password
  kafka:
    bootstrap-servers: localhost:9092
```

### Order Statuses

- `PENDING` - Order placed, awaiting confirmation
- `CONFIRMED` - Order confirmed
- `PROCESSING` - Order being processed
- `SHIPPED` - Order shipped
- `DELIVERED` - Order delivered
- `CANCELLED` - Order cancelled
- `REFUNDED` - Order refunded

### Payment Statuses

- `PENDING` - Payment pending
- `PAID` - Payment completed
- `FAILED` - Payment failed
- `REFUNDED` - Payment refunded

## Usage Examples

### Create Order from Cart

```bash
curl -X POST http://localhost:8084/api/orders/checkout/1 \
  -H "Content-Type: application/json" \
  -d '{
    "shippingAddress": {
      "street": "123 Main St",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    },
    "paymentMethod": "CREDIT_CARD",
    "paymentGatewayId": 1
  }'
```

### Get Order by ID

```bash
curl http://localhost:8084/api/orders/1
```

### Get User Orders

```bash
curl "http://localhost:8084/api/orders/user/1?page=0&size=10"
```

### Track Order

```bash
curl http://localhost:8084/api/orders/track/ORD-20240101-001
```

### Update Order Status

```bash
curl -X PUT "http://localhost:8084/api/orders/1/status?status=SHIPPED"
```

## Architecture

### Order Creation Flow

1. **Checkout Request** → Order Service receives checkout request
2. **Cart Validation** → Fetch cart via OpenFeign from Cart Service
3. **Order Creation** → Create order with items from cart
4. **Payment Event** → Publish payment request event to Kafka
5. **Order Persistence** → Save order to MySQL
6. **Cart Clear** → Clear cart after successful order creation
7. **Notification Event** → Publish order created event

### Payment Status Update Flow

1. **Payment Event** → Receive payment status from Kafka
2. **Order Update** → Update order payment status
3. **Status Update** → Update order status based on payment
4. **Notification Event** → Publish order status update event

### Components

1. **OrderController**: REST endpoints
2. **OrderService**: Business logic
3. **OrderRepository**: Database operations
4. **CartServiceClient**: OpenFeign client for cart service
5. **PaymentEventConsumer**: Kafka consumer for payment events

## Kafka Integration

### Topics Consumed

- `payment-events`: Payment status updates

### Topics Produced

- `order-events`: Order creation and status updates
- `payment-requests`: Payment processing requests

### Event Types

- `ORDER_CREATED` → Order created event
- `ORDER_STATUS_UPDATED` → Order status change event
- `PAYMENT_CONFIRMED` → Payment confirmed event
- `PAYMENT_FAILED` → Payment failed event

## Database Schema

### Order Entity

- `id` - Primary key
- `orderNumber` - Unique order number (e.g., ORD-20240101-001)
- `userId` - User ID
- `status` - Order status
- `paymentStatus` - Payment status
- `totalAmount` - Total order amount
- `shippingAddress` - Shipping address JSON
- `trackingNumber` - Shipping tracking number
- `estimatedDeliveryDate` - Estimated delivery date
- `createdAt` - Creation timestamp
- `updatedAt` - Update timestamp

### OrderItem Entity

- `id` - Primary key
- `orderId` - Foreign key to Order
- `productId` - Product ID
- `quantity` - Item quantity
- `price` - Item price at time of order
- `subtotal` - Item subtotal

## Testing

### Run Tests

```bash
mvn test
```

### Manual Testing

1. Start MySQL, Kafka, Eureka
2. Start Cart Service, Payment Service
3. Start Order Service
4. Create cart with items
5. Create order from cart
6. Verify order in database
7. Test order tracking

## Deployment

### Docker

```bash
# Build image
docker build -t ecommerce/order-service .

# Run container
docker run -p 8084:8084 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-order:3306/order_service_db \
  ecommerce/order-service
```

### Production Considerations

1. **Database**:
   - Use connection pooling
   - Set up read replicas for order history
   - Archive old orders
   - Set up proper indexing
2. **Kafka**:
   - Configure proper partitions
   - Set up consumer groups
   - Handle message failures
3. **Order Number Generation**:
   - Ensure uniqueness
   - Use distributed ID generation
4. **Tracking**:
   - Integrate with shipping providers
   - Update tracking numbers automatically

## Troubleshooting

### Common Issues

1. **Cart Service Unavailable**:
   - Verify Cart Service is running
   - Check Eureka service registration
   - Verify OpenFeign configuration

2. **Payment Event Not Received**:
   - Verify Kafka connection
   - Check consumer group configuration
   - Verify topic exists

3. **Order Creation Fails**:
   - Check cart validation
   - Verify product availability
   - Check database connection

4. **Order Number Duplication**:
   - Verify order number generation logic
   - Check database constraints

### Logs

```bash
# Enable debug logging
java -jar target/order-service-1.0.0.jar \
  --logging.level.org.example.order=DEBUG \
  --logging.level.org.apache.kafka=DEBUG
```

## Dependencies

- Spring Boot 3.2.0
- Spring Data JPA (MySQL)
- Spring Kafka
- Spring Cloud OpenFeign
- Spring Cloud Netflix Eureka Client
- MySQL Connector
- Validation API

## Project Structure

```
src/
├── main/
│   ├── java/org/example/order/
│   │   ├── OrderServiceApplication.java
│   │   ├── controller/
│   │   │   └── OrderController.java
│   │   ├── service/
│   │   │   └── OrderService.java
│   │   ├── entity/
│   │   │   ├── Order.java
│   │   │   └── OrderItem.java
│   │   ├── repository/
│   │   │   └── OrderRepository.java
│   │   ├── dto/
│   │   │   ├── CheckoutRequest.java
│   │   │   ├── CartDto.java
│   │   │   └── CartItemDto.java
│   │   ├── client/
│   │   │   └── CartServiceClient.java
│   │   └── consumer/
│   │       └── PaymentEventConsumer.java
│   └── resources/
│       └── application.yml
└── test/
```

## Contributing

1. Follow Spring Boot best practices
2. Write comprehensive tests
3. Handle order state transitions properly
4. Document order status flow
5. Handle errors gracefully

## License

This project is part of the Ecommerce Microservices Platform.
