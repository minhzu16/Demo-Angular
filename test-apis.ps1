# Tiki E-Commerce API Testing Script
# Comprehensive testing for all microservices

# Configuration
$BASE_URL = "http://localhost:8080/api/v1"
$AUTH_URL = "http://localhost:8082/api/v1"
$PRODUCT_URL = "http://localhost:8081/api/v1"
$ORDER_URL = "http://localhost:8083/api/v1"
$CART_URL = "http://localhost:8084/api/v1"

# Colors for output
function Write-Success {
    param($message)
    Write-Host "✅ $message" -ForegroundColor Green
}

function Write-Error-Custom {
    param($message)
    Write-Host "❌ $message" -ForegroundColor Red
}

function Write-Info {
    param($message)
    Write-Host "ℹ️  $message" -ForegroundColor Cyan
}

function Write-Test {
    param($message)
    Write-Host "`n🧪 $message" -ForegroundColor Yellow
}

# Variables to store test data
$global:accessToken = ""
$global:userId = 0
$global:productId = 0
$global:orderId = 0

Write-Host "`n╔══════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║   Tiki E-Commerce API Testing Suite                     ║" -ForegroundColor Magenta
Write-Host "╚══════════════════════════════════════════════════════════╝`n" -ForegroundColor Magenta

##############################################################################
# 1. AUTHENTICATION SERVICE TESTS
##############################################################################

Write-Host "`n═══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  AUTH SERVICE TESTS" -ForegroundColor Magenta
Write-Host "═══════════════════════════════════════════`n" -ForegroundColor Magenta

# Test 1.1: Register User
Write-Test "Testing User Registration"
try {
    $registerBody = @{
        username = "testuser_$(Get-Random -Maximum 10000)"
        email = "test$(Get-Random -Maximum 10000)@example.com"
        password = "Test123456"
        phoneNumber = "0123456789"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$AUTH_URL/auth/register" `
        -Method Post `
        -Body $registerBody `
        -ContentType "application/json" `
        -ErrorAction Stop

    if ($response.accessToken) {
        $global:accessToken = $response.accessToken
        $global:userId = $response.user.id
        Write-Success "User registered successfully - User ID: $global:userId"
    } else {
        Write-Error-Custom "Registration failed - No token received"
    }
} catch {
    Write-Error-Custom "Registration failed: $_"
}

# Test 1.2: Login
Write-Test "Testing User Login"
try {
    $loginBody = @{
        usernameOrEmail = "testuser"
        password = "Test123456"
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$AUTH_URL/auth/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/json" `
        -ErrorAction Stop

    if ($response.accessToken) {
        $global:accessToken = $response.accessToken
        Write-Success "Login successful"
    }
} catch {
    Write-Info "Login test skipped (use registered user for actual test)"
}

# Test 1.3: Get Current User
Write-Test "Testing Get Current User"
try {
    $headers = @{
        "Authorization" = "Bearer $global:accessToken"
    }

    $response = Invoke-RestMethod -Uri "$AUTH_URL/auth/me" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop

    Write-Success "Get current user: $($response.username)"
} catch {
    Write-Error-Custom "Get current user failed: $_"
}

##############################################################################
# 2. PRODUCT SERVICE TESTS
##############################################################################

Write-Host "`n═══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  PRODUCT SERVICE TESTS" -ForegroundColor Magenta
Write-Host "═══════════════════════════════════════════`n" -ForegroundColor Magenta

# Test 2.1: List Products
Write-Test "Testing List Products (Pagination)"
try {
    $response = Invoke-RestMethod -Uri "$PRODUCT_URL/products?page=0&size=10" `
        -Method Get `
        -ErrorAction Stop

    Write-Success "Listed products: $($response.totalElements) total, Page: $($response.page)"
    
    if ($response.content -and $response.content.Count -gt 0) {
        $global:productId = $response.content[0].id
        Write-Info "Sample Product ID: $global:productId"
    }
} catch {
    Write-Error-Custom "List products failed: $_"
}

# Test 2.2: Search Products
Write-Test "Testing Product Search"
try {
    $response = Invoke-RestMethod -Uri "$PRODUCT_URL/products/search?keyword=laptop&page=0&size=5" `
        -Method Get `
        -ErrorAction Stop

    Write-Success "Search results: $($response.totalElements) products found"
} catch {
    Write-Error-Custom "Product search failed: $_"
}

# Test 2.3: Get Product Detail
Write-Test "Testing Get Product Detail"
try {
    if ($global:productId -gt 0) {
        $response = Invoke-RestMethod -Uri "$PRODUCT_URL/products/$global:productId" `
            -Method Get `
            -ErrorAction Stop

        Write-Success "Product detail: $($response.name) - Price: $($response.price)"
    } else {
        Write-Info "Product detail test skipped (no product ID)"
    }
} catch {
    Write-Error-Custom "Get product detail failed: $_"
}

# Test 2.4: Filter Products by Category and Price
Write-Test "Testing Product Filtering"
try {
    $response = Invoke-RestMethod -Uri "$PRODUCT_URL/products?category=1&minPrice=100000&maxPrice=5000000sortprice_asc" `
        -Method Get `
        -ErrorAction Stop

    Write-Success "Filtered products: $($response.totalElements) results"
} catch {
    Write-Error-Custom "Product filtering failed: $_"
}

##############################################################################
# 3. CART SERVICE TESTS
##############################################################################

Write-Host "`n═══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  CART SERVICE TESTS" -ForegroundColor Magenta
Write-Host "═══════════════════════════════════════════`n" -ForegroundColor Magenta

# Test 3.1: Get Cart
Write-Test "Testing Get Cart"
try {
    $response = Invoke-RestMethod -Uri "$CART_URL/cart?userId=$global:userId" `
        -Method Get `
        -ErrorAction Stop

    Write-Success "Cart retrieved: $($response.totalItems) items, Total: $($response.totalAmount)"
} catch {
    Write-Info "Cart not found (this is normal for new users)"
}

# Test 3.2: Add Item to Cart
Write-Test "Testing Add Item to Cart"
try {
    if ($global:productId -gt 0) {
        $addItemBody = @{
            productId = $global:productId
            quantity = 2
            userId = $global:userId
        } | ConvertTo-Json

        $headers = @{
            "X-User-Id" = $global:userId.ToString()
            "Content-Type" = "application/json"
        }

        $response = Invoke-RestMethod -Uri "$CART_URL/cart" `
            -Method Post `
            -Headers $headers `
            -Body $addItemBody `
            -ErrorAction Stop

        Write-Success "Item added to cart: Total items = $($response.totalItems)"
    } else {
        Write-Info "Add to cart test skipped (no product ID)"
    }
} catch {
    Write-Error-Custom "Add to cart failed: $_"
}

# Test 3.3: Get Cart Summary
Write-Test "Testing Cart Summary"
try {
    $headers = @{
        "X-User-Id" = $global:userId.ToString()
    }

    $response = Invoke-RestMethod -Uri "$CART_URL/cart/summary" `
        -Method Get `
        -Headers $headers `
        -ErrorAction Stop

    Write-Success "Cart summary: $($response.totalItems) items, Amount: $($response.totalAmount)"
} catch {
    Write-Error-Custom "Cart summary failed: $_"
}

# Test 3.4: Update Cart Quantity
Write-Test "Testing Update Cart Quantity"
try {
    if ($global:productId -gt 0) {
        $updateBody = @{
            quantity = 3
        } | ConvertTo-Json

        $response = Invoke-RestMethod -Uri "$CART_URL/cart/items/${global:productId}?userId=$global:userId" `
            -Method Put `
            -Body $updateBody `
            -ContentType "application/json" `
            -ErrorAction Stop

        Write-Success "Cart quantity updated: Total items = $($response.totalItems)"
    }
} catch {
    Write-Error-Custom "Update cart failed: $_"
}

##############################################################################
# 4. ORDER SERVICE TESTS
##############################################################################

Write-Host "`n═══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  ORDER SERVICE TESTS" -ForegroundColor Magenta
Write-Host "═══════════════════════════════════════════`n" -ForegroundColor Magenta

# Test 4.1: Create Order
Write-Test "Testing Create Order"
try {
    if ($global:userId -gt 0 -and $global:productId -gt 0) {
        $orderBody = @{
            userId = $global:userId
            shippingAddress = "123 Test Street, Test City"
            paymentMethod = "COD"
            items = @(
                @{
                    productId = $global:productId
                    quantity = 2
                    price = 100000
                }
            )
        } | ConvertTo-Json

        $headers = @{
            "X-User-Id" = $global:userId.ToString()
            "Authorization" = "Bearer $global:accessToken"
            "Content-Type" = "application/json"
        }

        $response = Invoke-RestMethod -Uri "$ORDER_URL/orders" `
            -Method Post `
            -Headers $headers `
            -Body $orderBody `
            -ErrorAction Stop

        $global:orderId = $response.id
        Write-Success "Order created successfully - Order ID: $global:orderId"
    } else {
        Write-Info "Create order test skipped (missing user/product)"
    }
} catch {
    Write-Error-Custom "Create order failed: $_"
}

# Test 4.2: Get My Orders
Write-Test "Testing Get My Orders"
try {
    if ($global:userId -gt 0) {
        $headers = @{
            "X-User-Id" = $global:userId.ToString()
            "Authorization" = "Bearer $global:accessToken"
        }

        $response = Invoke-RestMethod -Uri "$ORDER_URL/orders/my-orders?page=0&size=10" `
            -Method Get `
            -Headers $headers `
            -ErrorAction Stop

        Write-Success "My orders: $($response.totalElements) total orders"
    }
} catch {
    Write-Error-Custom "Get my orders failed: $_"
}

# Test 4.3: Get Order Detail
Write-Test "Testing Get Order Detail"
try {
    if ($global:orderId -gt 0) {
        $headers = @{
            "Authorization" = "Bearer $global:accessToken"
        }

        $response = Invoke-RestMethod -Uri "$ORDER_URL/orders/$global:orderId" `
            -Method Get `
            -Headers $headers `
            -ErrorAction Stop

        Write-Success "Order detail: Status = $($response.status), Total = $($response.totalAmount)"
    }
} catch {
    Write-Error-Custom "Get order detail failed: $_"
}

# Test 4.4: Get Order Statistics
Write-Test "Testing Order Statistics"
try {
    if ($global:userId -gt 0) {
        $headers = @{
            "X-User-Id" = $global:userId.ToString()
            "Authorization" = "Bearer $global:accessToken"
        }

        $response = Invoke-RestMethod -Uri "$ORDER_URL/orders/stats" `
            -Method Get `
            -Headers $headers `
            -ErrorAction Stop

        Write-Success "Order stats: Total=$($response.totalOrders), Completed=$($response.completedOrders)"
    }
} catch {
    Write-Error-Custom "Get order stats failed: $_"
}

# Test 4.5: Cancel Order
Write-Test "Testing Cancel Order"
try {
    if ($global:orderId -gt 0) {
        $headers = @{
            "X-User-Id" = $global:userId.ToString()
            "Authorization" = "Bearer $global:accessToken"
        }

        $response = Invoke-RestMethod -Uri "$ORDER_URL/orders/$global:orderId/cancel" `
            -Method Post `
            -Headers $headers `
            -ErrorAction Stop

        Write-Success "Order cancelled: Status = $($response.status)"
    }
} catch {
    Write-Info "Cancel order test skipped or order cannot be cancelled"
}

##############################################################################
# SUMMARY
##############################################################################

Write-Host "`n╔══════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║   Testing Complete!                                      ║" -ForegroundColor Magenta
Write-Host "╚══════════════════════════════════════════════════════════╝`n" -ForegroundColor Magenta

Write-Host "Test Data Summary:" -ForegroundColor Cyan
Write-Host "  User ID: $global:userId" -ForegroundColor White
Write-Host "  Access Token: $($global:accessToken.Substring(0, [Math]::Min(20, $global:accessToken.Length)))..." -ForegroundColor White
Write-Host "  Product ID: $global:productId" -ForegroundColor White
Write-Host "  Order ID: $global:orderId`n" -ForegroundColor White

Write-Info "Note: Some tests may fail if services are not running or data doesn't exist yet."
Write-Info "Ensure all microservices are running before executing this script.`n"
