# ⚠️ DEPRECATED - Controllers không nên ở Common Module

## Vấn đề

Controllers trong common module gây ra vấn đề:
- Bean name conflicts khi scan packages
- Business logic không nên ở shared library
- Vi phạm nguyên tắc Single Responsibility

## Giải pháp

Mỗi service nên có controllers riêng:

```
auth-service/
└── controller/
    ├── AuthController.java
    └── SellerApplicationController.java

product-service/
└── controller/
    └── ProductController.java

order-service/
└── controller/
    └── OrderController.java
```

## Trạng thái hiện tại

❌ Các controllers trong thư mục này **KHÔNG được sử dụng**
✅ Services không scan `com.tiki.common` cho components
✅ Không ảnh hưởng đến runtime

## Action Items

- [ ] Di chuyển logic vào services tương ứng
- [ ] Xóa thư mục này sau khi verify không còn dependency
