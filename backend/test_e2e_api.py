import requests
import json
import time
import random

BASE_URL = "http://localhost:8080/api/v1"

# Colors for terminal output
GREEN = '\033[92m'
RED = '\033[91m'
YELLOW = '\033[93m'
RESET = '\033[0m'

def print_step(name):
    print(f"\n{YELLOW}--- {name} ---{RESET}")

def assert_status(response, expected, msg):
    if response.status_code == expected or (type(expected) == list and response.status_code in expected):
        print(f"{GREEN}[OK]{RESET} {msg} (Status: {response.status_code})")
    else:
        print(f"{RED}[FAIL]{RESET} {msg} - Expected {expected}, got {response.status_code}")
        print(f"Response: {response.text}")
        exit(1)

def run_tests():
    # 1. Register a new user
    print_step("Auth Flow")
    username = f"testuser_{int(time.time())}"
    email = f"{username}@example.com"
    
    register_payload = {
        "username": username,
        "email": email,
        "password": "Password123!",
        "fullName": "Test User",
        "phone": "0987654321"
    }
    
    r = requests.post(f"{BASE_URL}/auth/register", json=register_payload)
    assert_status(r, [200, 201], "Register new user")
    
    # 2. Login
    login_payload = {
        "usernameOrEmail": username,
        "password": "Password123!"
    }
    r = requests.post(f"{BASE_URL}/auth/login", json=login_payload)
    assert_status(r, 200, "Login user")
    
    token = r.json().get("accessToken")
    headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
    
    # 3. Product Catalog
    print_step("Product Catalog")
    r = requests.get(f"{BASE_URL}/products?page=0&size=10")
    assert_status(r, 200, "Get products list")
    
    products = r.json().get("content", [])
    if not products:
        print(f"{YELLOW}[WARN]{RESET} No products found to test checkout flow.")
        return
        
    product_id = products[0]["id"]
    r = requests.get(f"{BASE_URL}/products/{product_id}")
    assert_status(r, 200, f"Get product details (ID: {product_id})")
    
    # 4. Cart Flow
    print_step("Cart Flow")
    add_cart_payload = {
        "productId": product_id,
        "quantity": 1
    }
    r = requests.post(f"{BASE_URL}/cart/items", json=add_cart_payload, headers=headers)
    assert_status(r, 200, "Add item to cart")
    
    r = requests.get(f"{BASE_URL}/cart", headers=headers)
    assert_status(r, 200, "Get cart items")
    
    # 5. Order Flow
    print_step("Order Flow")
    order_payload = {
        "shippingAddress": {
            "fullName": "Test User",
            "phoneNumber": "0987654321",
            "province": "Ho Chi Minh",
            "district": "District 1",
            "street": "123 Le Loi"
        },
        "paymentMethod": "COD",
        "items": [
            {
                "productId": product_id,
                "quantity": 1,
                "productName": products[0]["name"],
                "unitPrice": products[0]["price"]
            }
        ]
    }
    r = requests.post(f"{BASE_URL}/orders", json=order_payload, headers=headers)
    assert_status(r, [200, 201], "Create order")
    
    if r.status_code in [200, 201]:
        order_id = r.json().get("id")
        r = requests.get(f"{BASE_URL}/orders/{order_id}", headers=headers)
        assert_status(r, 200, "Get order details")

    print(f"\n{GREEN}All basic E2E flows completed successfully!{RESET}")

if __name__ == "__main__":
    try:
        run_tests()
    except requests.exceptions.ConnectionError:
        print(f"{RED}[ERROR]{RESET} Could not connect to API Gateway at {BASE_URL}")
