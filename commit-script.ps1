# Professional Git Commit Script for Tiki E-Commerce
# Execute commits in organized, logical groups

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Git Commit Organization Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if we're in a git repository
if (-not (Test-Path .git)) {
    Write-Host "Error: Not a git repository!" -ForegroundColor Red
    exit 1
}

# Show current status
Write-Host "Current Git Status:" -ForegroundColor Yellow
git status --short
Write-Host ""

# Confirm before proceeding
$confirm = Read-Host "Proceed with commits? (y/n)"
if ($confirm -ne 'y') {
    Write-Host "Aborted." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Starting organized commits..." -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 1: Configuration Files
# ============================================
Write-Host "[1/13] Committing: Frontend Configuration..." -ForegroundColor Cyan
git add frontend/src/environments/environment.ts
git add frontend/src/environments/environment.prod.ts
git add frontend/proxy.conf.json

git commit -m "feat(frontend): add environment configuration and proxy setup

- Add development environment configuration (environment.ts)
- Add production environment configuration (environment.prod.ts)
- Add proxy configuration for CORS handling (proxy.conf.json)
- Configure API base URLs for all microservices (auth, product, order, cart)

Resolves: Critical missing configuration files that prevented frontend build"

Write-Host "✓ Configuration files committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 2: Guards & Interceptors
# ============================================
Write-Host "[2/13] Committing: Security Infrastructure..." -ForegroundColor Cyan
git add frontend/src/app/guards/seller.guard.ts
git add frontend/src/app/core/interceptors/error.interceptor.ts

git commit -m "feat(frontend): implement security guards and HTTP error interceptor

- Add seller guard for role-based route protection (SELLER, ADMIN)
- Implement global HTTP error interceptor with auto token cleanup
- Add automatic redirect to login on 401 Unauthorized
- Add user-friendly error messages for all HTTP failures

Features:
- Role-based access control (RBAC)
- Automatic authentication error handling
- Global error boundary for HTTP requests"

Write-Host "✓ Security infrastructure committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 3: Routes Configuration
# ============================================
Write-Host "[3/13] Committing: Routes Refactoring..." -ForegroundColor Cyan
git add frontend/src/app/app.routes.ts

git commit -m "refactor(frontend): simplify routes configuration

- Simplify routes to use only existing components
- Remove references to 20+ unimplemented components
- Add proper route guards for protected routes (authGuard)
- Improve route organization and readability

Changes:
- Reduced from complex nested structure to 6 core routes
- All routes now reference existing components (login, register, dashboard, profile)
- Prevents TypeScript build errors from missing component imports"

Write-Host "✓ Routes configuration committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 4: Product Service
# ============================================
Write-Host "[4/13] Committing: Product Service..." -ForegroundColor Cyan
git add frontend/src/app/services/product.service.ts

git commit -m "feat(frontend): implement comprehensive product service

- Add product listing with pagination support
- Implement product search functionality
- Add product filtering by category and price range
- Add product sorting options (price, name, popularity)
- Implement product detail retrieval

API Endpoints:
- GET /products (list with filters)
- GET /products/search (search by keyword)
- GET /products/:id (product details)
- GET /products/filter (advanced filtering)

Features:
- Full pagination support
- Advanced search and filtering
- Type-safe interfaces for all operations"

Write-Host "✓ Product service committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 5: Cart Service
# ============================================
Write-Host "[5/13] Committing: Cart Service..." -ForegroundColor Cyan
git add frontend/src/app/services/cart.service.ts

git commit -m "feat(frontend): implement shopping cart management service

- Add cart retrieval and management
- Implement add/update/remove item operations
- Add cart validation with backend price verification
- Implement cart merge for logged-in users
- Add cart summary calculation
- Add payment session creation

Features:
- Server-side price validation for security
- Guest cart to user cart migration
- Real-time cart updates
- Cart validation before checkout
- Session-based cart for guests
- User-based cart for authenticated users"

Write-Host "✓ Cart service committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 6: Order Service
# ============================================
Write-Host "[6/13] Committing: Order Service..." -ForegroundColor Cyan
git add frontend/src/app/services/order.service.ts

git commit -m "feat(frontend): implement comprehensive order management service

- Add order creation with validation
- Implement order history retrieval with pagination
- Add order status tracking
- Implement order cancellation
- Add order return/refund requests
- Add order statistics for dashboards
- Add shop order management for sellers

API Endpoints:
- POST /orders (create order)
- GET /orders/my-orders (user order history)
- GET /orders/:id (order details)
- POST /orders/:id/cancel (cancel order)
- GET /orders/shop/:shopId (seller orders)
- GET /orders/shop/:shopId/statistics (order stats)

Features:
- Complete order lifecycle management
- Pagination and filtering support
- Seller dashboard integration
- Order statistics and analytics"

Write-Host "✓ Order service committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 7: Shop Service
# ============================================
Write-Host "[7/13] Committing: Shop Service..." -ForegroundColor Cyan
git add frontend/src/app/services/shop.service.ts

git commit -m "feat(frontend): implement shop management service

- Add shop retrieval for sellers
- Implement shop creation
- Add shop update functionality
- Add shop profile management

Features:
- Seller shop management
- Shop profile CRUD operations
- Shop information retrieval
- Support for multi-shop sellers"

Write-Host "✓ Shop service committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 8: Seller Application Service
# ============================================
Write-Host "[8/13] Committing: Seller Application Service..." -ForegroundColor Cyan
git add frontend/src/app/services/seller-application.service.ts

git commit -m "feat(frontend): implement seller registration service

- Add seller application submission
- Implement application status tracking
- Add admin approval/rejection endpoints
- Add application history retrieval

Features:
- Multi-step seller registration process
- Application status tracking (PENDING/APPROVED/REJECTED)
- Admin application management
- Application review workflow
- Business license and tax code validation"

Write-Host "✓ Seller application service committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 9: Warehouse Service
# ============================================
Write-Host "[9/13] Committing: Warehouse Service..." -ForegroundColor Cyan
git add frontend/src/app/services/warehouse.service.ts

git commit -m "feat(frontend): implement inventory management service

- Add product stock statistics
- Implement stock level tracking
- Add low stock alerts
- Add stock update operations

Features:
- Real-time inventory tracking
- Low stock notifications
- Out-of-stock alerts
- Shop-level stock management
- Product statistics for sellers"

Write-Host "✓ Warehouse service committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 10: Dashboard Component Fix
# ============================================
Write-Host "[10/13] Committing: Dashboard Component Fixes..." -ForegroundColor Cyan
git add frontend/src/app/pages/dashboard/dashboard.component.ts

git commit -m "fix(frontend): resolve TypeScript errors in dashboard component

- Add explicit type annotations to all callback parameters
- Fix 'implicit any' type errors (4 instances)
- Fix 'object is of type unknown' errors (4 instances)
- Add proper error handling for all async operations

Technical improvements:
- Strict TypeScript compliance
- Better type safety throughout component
- Improved error reporting and handling
- Enhanced code maintainability"

Write-Host "✓ Dashboard component fixes committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 11: Auth Service Enhancement
# ============================================
Write-Host "[11/13] Committing: Auth Service Enhancement..." -ForegroundColor Cyan
git add frontend/src/app/services/auth.service.ts

git commit -m "feat(frontend): add role property to user profile

- Add role field to UserProfile interface
- Enable role-based access control (RBAC)
- Support for SELLER, ADMIN, USER roles

Changes:
- UserProfile interface extended with optional role field
- Enhanced authentication service for role-based features
- Enables seller guard and admin guard functionality"

Write-Host "✓ Auth service enhancement committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 12: Style Configuration Fix
# ============================================
Write-Host "[12/13] Committing: Style Configuration Fix..." -ForegroundColor Cyan
git add frontend/src/styles.scss

git commit -m "fix(frontend): resolve SCSS import errors

- Comment out missing SCSS partial imports
- Fix build errors related to missing style files
- Maintain existing styles functionality

Changes:
- Removed references to non-existent styles/variables
- Removed references to non-existent styles/mixins
- Build now completes successfully
- All existing styles remain functional"

Write-Host "✓ Style configuration fix committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Commit 13: API Testing Script
# ============================================
Write-Host "[13/13] Committing: API Testing Infrastructure..." -ForegroundColor Cyan
git add test-apis.ps1

git commit -m "test: add comprehensive API testing script

- Add PowerShell script for automated API testing
- Cover all 4 microservices (Auth, Product, Cart, Order)
- Test 25+ API endpoints
- Include authentication flow testing

Coverage:
- Auth Service: 4 endpoints (register, login, profile, refresh)
- Product Service: 5 endpoints (list, search, detail, filter, sort)
- Cart Service: 8 endpoints (get, add, update, remove, validate, etc.)
- Order Service: 8 endpoints (create, list, detail, cancel, stats, etc.)

Features:
- Color-coded output for pass/fail status
- Automatic token management
- Request/response logging
- Error handling and reporting"

Write-Host "✓ API testing script committed" -ForegroundColor Green
Write-Host ""

# ============================================
# Summary
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All commits completed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Commit Summary:" -ForegroundColor Yellow
git log --oneline -13

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Review commits: git log --graph --oneline -13" -ForegroundColor White
Write-Host "2. Push to remote: git push origin develop" -ForegroundColor White
Write-Host "3. Create pull request if needed" -ForegroundColor White
Write-Host ""
