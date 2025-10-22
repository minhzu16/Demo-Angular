# 🛒 Tiki E-Commerce System

A complete, production-ready e-commerce platform built with Spring Boot microservices and Angular frontend.

## 🚀 System Overview

This e-commerce system provides a full-featured online shopping platform with:
- **Customer Interface**: Product browsing, cart, checkout, reviews
- **Admin Dashboard**: Complete management for products, orders, inventory
- **Payment Processing**: Stripe integration with sandbox testing
- **Inventory Management**: Real-time stock tracking with reservation logic

## 🏗️ Architecture

### Backend (Spring Boot Microservices)
- **Auth Service**: User authentication, JWT tokens, refresh tokens
- **Product Service**: Product catalog, reviews, ratings, moderation
- **Order Service**: Order processing, payment integration
- **Warehouse Service**: Inventory management, stock tracking
- **Gateway Service**: API routing, load balancing

### Frontend (Angular)
- **Customer Portal**: Shopping experience, product discovery
- **Admin Portal**: Management dashboard, analytics
- **Responsive Design**: Mobile and desktop optimized

## ✨ Key Features

### Customer Features
- 🔍 **Product Discovery**: Search, filter, and browse products
- 🛒 **Shopping Cart**: Add items, manage quantities
- 💳 **Checkout**: Secure payment processing with Stripe
- ⭐ **Reviews & Ratings**: Leave feedback on products
- 📦 **Order Tracking**: Monitor order status and history
- 👤 **User Profile**: Manage account and shipping addresses

### Admin Features
- 📊 **Dashboard**: Overview of sales, inventory, orders
- 🛍️ **Product Management**: CRUD operations, image uploads
- 📋 **Order Management**: Process orders, update status
- 📦 **Inventory Control**: Stock monitoring, adjustments
- 👥 **User Management**: Customer accounts and roles

### Technical Features
- 🔐 **Security**: JWT authentication, role-based access
- 💰 **Payments**: Stripe integration with sandbox testing
- 📈 **Analytics**: Sales reports, inventory insights
- 🔄 **Real-time**: Live inventory updates, order tracking
- 📱 **Responsive**: Mobile-first design

## 🛠️ Technology Stack

### Backend
- **Java 17** with Spring Boot 3.x
- **Spring Security** for authentication
- **Spring Data JPA** for data persistence
- **PostgreSQL** database
- **Stripe API** for payments
- **Maven** for dependency management

### Frontend
- **Angular 17** with standalone components
- **Bootstrap 5** for styling
- **RxJS** for reactive programming
- **TypeScript** for type safety
- **Ant Design** icons

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 13+
- Maven 3.8+

### Backend Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd demo_tt/backend
```

2. **Configure database**
```bash
# Update application.yml files with your PostgreSQL credentials
# Database: tiki_ecommerce
# Username: your_username
# Password: your_password
```

3. **Install dependencies and run**
```bash
mvn clean install
mvn spring-boot:run -pl gateway
```

### Frontend Setup

1. **Navigate to frontend**
```bash
cd ../frontend
```

2. **Install dependencies**
```bash
npm install
```

3. **Start development server**
```bash
npm start
```

4. **Access the application**
- Customer Portal: http://localhost:4200
- Admin Portal: http://localhost:4200/admin

## 🔧 Configuration

### Database Setup
```sql
CREATE DATABASE tiki_ecommerce;
CREATE USER tiki_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE tiki_ecommerce TO tiki_user;
```

### Stripe Configuration
1. Get your Stripe API keys from https://stripe.com
2. Update `application.yml` files:
```yaml
stripe:
  secret-key: sk_test_your_secret_key
  publishable-key: pk_test_your_publishable_key
  webhook-secret: whsec_your_webhook_secret
```

### Environment Variables
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tiki_ecommerce
DB_USERNAME=tiki_user
DB_PASSWORD=your_password

# Stripe
STRIPE_SECRET_KEY=sk_test_your_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_key
```

## 📊 API Documentation

### Authentication Endpoints
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Refresh token
- `GET /api/v1/auth/me` - Get current user

### Product Endpoints
- `GET /api/v1/products` - List products
- `GET /api/v1/products/{id}` - Get product details
- `POST /api/v1/admin/products` - Create product (Admin)
- `PUT /api/v1/admin/products/{id}` - Update product (Admin)
- `DELETE /api/v1/admin/products/{id}` - Delete product (Admin)

### Order Endpoints
- `POST /api/v1/orders` - Create order
- `GET /api/v1/orders` - List user orders
- `GET /api/v1/orders/{id}` - Get order details
- `PUT /api/v1/admin/orders/{id}` - Update order status (Admin)

### Payment Endpoints
- `POST /api/v1/payments/create-intent` - Create payment intent
- `POST /api/v1/payments/confirm` - Confirm payment
- `POST /api/v1/payments/cancel` - Cancel payment

## 🧪 Testing

### Backend Tests
```bash
mvn test
```

### Frontend Tests
```bash
npm test
```

### E2E Tests
```bash
npm run e2e
```

## 🚀 Deployment

### Docker Deployment
```bash
# Build and run with Docker Compose
docker-compose up -d
```

### Production Deployment
1. **Configure production database**
2. **Set up Stripe production keys**
3. **Configure environment variables**
4. **Deploy to cloud platform**
5. **Set up SSL certificates**
6. **Configure monitoring and logging**

## 📈 Monitoring

### Health Checks
- Backend: http://localhost:8080/actuator/health
- Database connectivity
- External service status

### Logging
- Application logs in `logs/` directory
- Error tracking and monitoring
- Performance metrics

## 🔒 Security

### Authentication
- JWT tokens with refresh mechanism
- Role-based access control
- Password encryption

### Data Protection
- Input validation and sanitization
- SQL injection prevention
- XSS protection

### Payment Security
- PCI DSS compliance through Stripe
- Secure token handling
- Webhook signature verification

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📝 License

This project is licensed under the MIT License.

## 🆘 Support

For support and questions:
- Create an issue in the repository
- Check the documentation
- Review the API endpoints

## 🎯 Roadmap

### Phase 1 (Completed)
- ✅ Basic e-commerce functionality
- ✅ User authentication
- ✅ Product management
- ✅ Order processing
- ✅ Payment integration

### Phase 2 (Future)
- 🔄 Advanced analytics
- 🔄 Recommendation engine
- 🔄 Multi-language support
- 🔄 Mobile app
- 🔄 Advanced reporting

---

**Built with ❤️ using Spring Boot and Angular**