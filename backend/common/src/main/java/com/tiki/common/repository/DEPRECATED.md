# ⚠️ DEPRECATED - Repositories đã bị disabled

## Vấn đề

Repositories trong common module gây conflict:
- Bean name conflicts (e.g. UserRepository trong cả auth và common)
- Spring Data JPA không biết repository nào để dùng
- Mỗi service nên có repositories riêng cho entities của mình

## Giải pháp đã áp dụng

1. **Repositories đã bị comment out**
   - UserRepository → disabled
   - AddressRepository → disabled
   - ComplaintRepository → disabled
   - SellerRepository → disabled
   - SellerApplicationRepository → **di chuyển vào auth.repository**

2. **Services không scan common.repository**
   ```java
   // Auth Service
   @EnableJpaRepositories(basePackages = "com.tiki.auth.repository")
   // Chỉ scan auth repositories, KHÔNG scan common.repository
   ```

## Trạng thái hiện tại

✅ Tất cả repositories đã bị disabled (commented out)
✅ Auth service có SellerApplicationRepository riêng
✅ Không còn bean conflicts

## Action Items

- [ ] Xóa thư mục này vì tất cả repositories đã disabled
- [ ] Hoặc giữ lại để tham khảo schema
