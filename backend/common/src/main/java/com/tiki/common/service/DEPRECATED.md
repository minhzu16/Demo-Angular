# ⚠️ DEPRECATED - Services không nên ở Common Module

## Vấn đề

Services trong common module:
- Chứa business logic không phù hợp với shared library
- Phụ thuộc vào repositories đã bị disabled
- Gây confusion về separation of concerns

## Giải pháp

Business logic nên ở service tương ứng:

```
auth-service/
└── service/
    ├── AuthService.java
    ├── UserService.java
    └── SellerApplicationService.java

product-service/
└── service/
    └── ProductService.java
```

## Trạng thái hiện tại

❌ Các services trong thư mục này **KHÔNG được sử dụng**
✅ Auth service có services riêng của mình
✅ Không ảnh hưởng đến runtime

## Action Items

- [ ] Xóa thư mục này sau khi verify không còn reference
