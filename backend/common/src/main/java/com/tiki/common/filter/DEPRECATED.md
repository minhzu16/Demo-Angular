# ⚠️ DEPRECATED - Filters không nên ở Common Module

## Vấn đề

Filters/Components trong common module:
- Tự động được scan và register nếu service scan `com.tiki.common`
- Gây side effects không mong muốn
- Mỗi service nên tự quản lý filters của mình

## Giải pháp

Filters nên implement ở từng service:

```java
// Trong auth-service
@Component
public class AuthLoggingFilter extends OncePerRequestFilter {
    // Auth-specific logging
}

// Trong product-service  
@Component
public class ProductLoggingFilter extends OncePerRequestFilter {
    // Product-specific logging
}
```

## Trạng thái hiện tại

❌ Filter trong thư mục này **KHÔNG được sử dụng**
✅ Services không scan `com.tiki.common` cho components
✅ Không ảnh hưởng đến runtime

## Action Items

- [ ] Xóa thư mục này sau khi verify không còn dependency
