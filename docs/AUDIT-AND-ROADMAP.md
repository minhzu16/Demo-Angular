# NexMart — Báo cáo kiểm tra lỗi & Lộ trình nâng cấp

> Phạm vi: đọc mã tĩnh + build/test. Build backend (`mvn test`) và frontend (`ng build`) đều pass trước và sau thay đổi.
> **Chưa phải audit toàn diện**: workflow audit 23 phạm vi bị ngắt do chạm giới hạn phiên; các mục dưới đây là những lỗi đã **đọc mã và xác nhận trực tiếp**. Các module chưa được rà (b2b, live, settlement, review, analytics, notification, chat, template, warehouse admin, authz/IDOR của cart/notification) vẫn cần audit tiếp.

## 0. Cập nhật: đã sửa B1–B7, B9 (backend/infra)

- **B1, B2, B5, B6** — `JwtAuthGlobalFilter`: bảng luật BLOCKED/ADMIN/AUTHENTICATED, chuẩn hóa path, token hết hạn trên GET catalog được phục vụ như khách; thêm 4 test (`mvn test` toàn bộ pass).
- **B3** — `compose.yaml` và `compose-full.yaml`: thêm URL Feign cho order/cart/payment/warehouse/product (đã `docker compose config` hợp lệ; chưa chạy thực tế vì Docker không chạy).
- **B4** — `PUT /api/v1/orders/{id}/status` cho SELLER/ADMIN, kiểm tra shop sở hữu đơn và chuỗi chuyển trạng thái hợp lệ.
- **B7** — bỏ lời gọi tới `common-service` không tồn tại. **B9** — nginx bỏ header CORS `*`, proxy WebSocket.
- Còn lại: B8, `defaultValue="1"` của `X-User-Id` trong payment (đã chặn ở gateway nhưng nên bỏ), `compose-full.yaml` công khai cổng của mọi service, **chưa có test cho endpoint seller mới**, và các module chưa audit.

## 0b. Audit tiếp: cart, notification, warehouse, b2b, live (đã sửa, `mvn test` toàn bộ pass)

| Module | Lỗi xác nhận | Đã sửa |
|---|---|---|
| **cart** | IDOR: `userId` trên query/body được tin khi thiếu header → ẩn danh đọc/sửa giỏ người khác; `/user/{id}` không kiểm tra; `/merge` gộp giỏ vào user tuỳ ý; giá fallback `0` khi product-service lỗi | Chỉ dùng `X-User-Id`; `/user/{id}` chỉ của mình; merge cần đăng nhập; giá không xác minh được → 503 (+3 test) |
| **notification** | Ai cũng đọc/đánh dấu thông báo của bất kỳ user (IDOR) | Kiểm tra chủ sở hữu, `markRead` theo owner (+4 test); gateway yêu cầu đăng nhập |
| **warehouse** | Không có xác thực: ai cũng sửa tồn kho, tạo kho, xem tồn kho/thống kê mọi shop | Gateway: ghi kho = SELLER/ADMIN, kho/phân bổ = ADMIN; xem kho shop chỉ chủ shop/ADMIN (tra shop-service) |
| **b2b** | `X-User-Id` mặc định 1 / **admin 999** → ẩn danh tự duyệt công ty (hạn mức 50tr) và đặt bảng giá; PO lấy giá từ client (`defaultPrice`, thậm chí 0); IDOR công ty/PO; VIEWER cũng reject được PO; `convert` mặc định orderId giả 1001 | Header bắt buộc; verify/giá = ADMIN; giá PO chỉ từ bảng giá B2B; kiểm tra thành viên; reject cần APPROVER/ADMIN; bắt buộc `convertedOrderId` (+2 test) |
| **live** | `X-User-Id` mặc định 1 → ẩn danh thành seller 1 (bắt đầu/kết thúc live, ghim sản phẩm) hoặc mua nhanh như user 1; tên chat tuỳ ý & giả loại tin nhắn; chỉnh số người xem tuỳ ý; race vượt hạn mức `soldCount` | Header bắt buộc; tên từ token, loại CHAT; `/viewers` bị chặn; khoá hàng (`PESSIMISTIC_WRITE`) khi mua nhanh |

**Phát hiện quan trọng:** id shop **không** bằng id người dùng (seed: shop 1 thuộc seller user 2). Code cũ so `order.shopId == userId` (kiểm tra hoàn tiền/từ chối trả hàng) nên **chặn mọi seller thật**, còn các endpoint `/orders/shop/{id}` và thống kê shop **không kiểm tra gì** (seller xem đơn + địa chỉ khách của shop khác). Đã thêm `ShopClient` + `ShopOwnershipService` (order, warehouse) tra chủ shop qua shop-service, fail-closed; endpoint đổi trạng thái của seller (B4) dùng cùng cơ chế.

**Đã sửa sau đó:**
- **WebSocket STOMP** (notification + chat): `StompJwtChannelInterceptor` bắt buộc JWT trong frame CONNECT; SUBSCRIBE chỉ `/topic/notifications/{id}` & `/topic/chat/{id}` của chính mình (+ `/topic/flash-sale`), topic lạ bị từ chối; SEND ghi đè `senderId`/`senderName` từ token (trước đây header do client tự khai). Frontend gửi token trong CONNECT, chỉ reconnect khi còn đăng nhập, chat đi qua gateway/nginx. Compose truyền `JWT_SECRET` cho notification và chat (+5 test).
- **shop-service**: tạo shop lấy chủ từ người gọi, chỉ copy trường hồ sơ (bỏ `id`/`status`/`isActive`/`sellerId` trong body), mỗi seller 1 shop; sửa shop chỉ chủ shop/ADMIN; **duyệt/từ chối/liệt kê hồ sơ người bán chỉ ADMIN** (trước đây ai cũng tự duyệt hồ sơ của mình để thành SELLER) ở cả gateway lẫn service; không còn trả stack trace/tên class ra client (+4 test shop, +1 test gateway).

- **Chat theo từng cuộc trò chuyện**: kênh dùng chung `/topic/shop/{shopId}` đã bị bỏ. Mỗi hội thoại (shop, người mua) có kênh riêng `/topic/conv/{shopId}/{buyerId}` (chỉ người mua đó hoặc chủ shop), chủ shop có thêm `/topic/inbox/{shopId}` để thấy mọi hội thoại; SEND chỉ vào hội thoại của mình (`/app/chat/{shopId}/{buyerId}`), chủ shop tra qua shop-service (cache 5 phút, fail-closed). Seller UI có danh sách hội thoại + số tin chưa đọc (+5 test interceptor, +2 test controller).

**Chưa sửa (đã xác nhận):**
- Chat không lưu lịch sử (in-memory): người mua mở lại thì mất hội thoại; seller chỉ thấy tin từ lúc mở inbox.
- live: `quickBuyCheckoutToken` không service nào kiểm → mua nhanh chưa tạo đơn thật; giá live do seller tự nhập. b2b: `creditLimit` không được áp dụng khi tạo PO; hợp đồng `convert` chưa xác thực orderId thật.
- warehouse: seller sửa được tồn kho của sản phẩm bất kỳ (chưa kiểm sản phẩm thuộc shop mình).
- Chưa audit: settlement, review, analytics, chat, template_storage, product (ownership khi sửa sản phẩm), shop, auth/OTP.

## 1. Lỗi đã xác nhận (trạng thái trước khi sửa)

| # | Mức | Lỗi | Bằng chứng | Hướng sửa |
|---|---|---|---|---|
| B1 | **Critical** | Leo thang đặc quyền không cần đăng nhập: `POST /api/v1/users/{id}/role?role=ADMIN` và `/points` | `auth/.../SecurityConfig.java` đặt `permitAll` cho `/api/v1/users/*/role`, `/*/points`; gateway định tuyến `/api/v1/users/**` tới auth và không chặn; `UserService.updateUserRole` không kiểm tra gì | Gateway chặn 403 các endpoint service-to-service này (hoặc đổi sang đường dẫn `/internal/`); lâu dài: token nội bộ ký giữa các service |
| B2 | **Critical** | payment-service cho tạo tiền / đánh dấu đã thanh toán không cần quyền: `POST /store-credit/add`, `/gift-cards/{code}/reload`, `/payments/confirm/{intent}/{status}`, `/payments/order/{id}/refund`, `PUT /payments/order/{id}/status` | Không có `SecurityConfig` trong payment; header `X-User-Id` có `defaultValue = "1"` nên request ẩn danh chạy như user 1 | Gateway: ADMIN-only cho các thao tác trên, bắt buộc đăng nhập cho `/payments/**` (trừ `sepay-webhook`), `/gift-cards/**`, `/store-credit/**`; bỏ `defaultValue="1"` |
| B3 | **High** | Docker: order-service không có URL tới product/payment/warehouse/settlement/auth → Feign gọi `localhost` → checkout hỏng | `compose.yaml` service `order` chỉ có 3 biến env; `application-docker.yml` không có block `services:`; defaults là `http://localhost:808x` | Thêm `PRODUCT_SERVICE_URL`, `PAYMENT_SERVICE_URL`, `WAREHOUSE_SERVICE_URL`, `SETTLEMENT_SERVICE_URL`, `SERVICES_AUTH_URL` cho order; `SERVICES_AUTH_URL` cho cart; `ORDER_SERVICE_URL` cho payment; `NOTIFICATION_SERVICE_URL` cho warehouse; `SERVICES_ORDER/REVIEW/SHOP_URL` cho product (cả `compose-full.yaml`) |
| B4 | **High** | Người bán không thể cập nhật trạng thái đơn: frontend gọi `PUT /orders/{id}/status` nhưng backend chỉ có `PUT /admin/orders/{id}/status/{status}` (ADMIN) | `seller-orders.component.ts:70` → `order.service.ts:240`; `OrderAdminController` | Thêm endpoint cho SELLER (kiểm tra shop sở hữu đơn) |
| B5 | Medium | Gateway trả 401 cho token hết hạn ngay cả trên trang công khai | `JwtAuthGlobalFilter` bắt `JwtException` → 401 | Với GET catalog công khai: bỏ qua token lỗi, coi như khách (frontend đã tự xử lý — xem mục 2) |
| B6 | Medium | `isInternalPath`/`isAdminPath` so khớp `contains("/internal/")` trên path chưa chuẩn hóa → có thể bypass bằng matrix param `;` | `JwtAuthGlobalFilter` | Chuẩn hóa path (bỏ `;…`, gộp `//`) trước khi so khớp |
| B7 | Medium | product-service `ReviewService.createReview` gọi `http://common-service` (không tồn tại) → báo "Invalid user id" cho mọi user | `product/client/UserClient.java` | Trỏ về auth hoặc bỏ bước xác thực (frontend đang dùng review-service nên chưa bộc lộ) |
| B8 | Medium | `GET /admin/orders` trả "mock empty page"; `ShopClient` gọi `/shops/{id}/name` không tồn tại | `OrderAdminController`, `ShopController` | Hiện thực hoặc bỏ |
| B9 | Low | CORS: `allowedOriginPatterns` có `${CORS_ALLOWED_ORIGIN:*}` cùng `allowCredentials: true`; nginx thêm `Access-Control-Allow-Origin: *` chồng lên | `gateway/application.yml`, `frontend/nginx.conf` | Whitelist origin theo môi trường, bỏ header ở nginx; nginx chưa proxy WebSocket `/ws/*` |

Ghi chú: các sửa B1–B2 tôi đã thiết kế (bảng luật Access BLOCKED/ADMIN/AUTHENTICATED trong `JwtAuthGlobalFilter` + chuẩn hóa path) nhưng việc sửa file backend bị hệ thống phân quyền từ chối nên **chưa áp dụng**. Nếu bạn cho phép, tôi sẽ làm kèm test.

## 2. Lỗi frontend đã sửa (đã build kiểm chứng)

- CSS của ngx-toastr **chưa từng được nạp** → toast không có style/vị trí. Đã thêm vào `angular.json` + theme.
- Header `position: fixed` + padding cố định 132px → **che nội dung trên mobile** (header 2 hàng). Đổi sang sticky.
- Wishlist: trang "Yêu thích" thực chất hiển thị sản phẩm nổi bật; nút tim chỉ đổi cờ cục bộ. Đã nối API `/api/v1/wishlist` (service mới), có trạng thái rỗng/lỗi.
- Link tới route không tồn tại: `/flash-sale`, `/notifications` → 404.
- Admin `/admin/**`, `/dashboard`, `/seller/**` chỉ có `authGuard` → thêm `roleGuard`; `authGuard` giữ `returnUrl`.
- Interceptor lỗi: mọi 401 xoá phiên + che thông báo của server (đăng nhập sai hiện "Login failed"); nay giữ message, thử lại như khách với GET công khai khi token cũ.
- Trang chủ: số liệu/giá/đếm ngược **bịa** (10M khách hàng, giá Sony cố định, điểm mặc định 1250, đếm ngược giả 2h) → dùng dữ liệu thật hoặc trạng thái rỗng.
- Trang sản phẩm: ảnh lấy `imageUrl` (API không có) nên không bao giờ hiện gallery; giá gạch `listPrice` không hiện; "Mua ngay" dùng `setTimeout(500)` chạy đua với request; số lượng không giới hạn theo tồn kho.
- Làm tròn sao sai (4.8 → 4 sao); NexPoints ước tính thấp hơn 10 lần so với backend (`/1000`).
- `?q=` từ header/danh mục không được trang Sản phẩm đọc.
- Hai nút chat (shop + trợ lý) chồng đúng một vị trí.
- Màu chữ/badge/alert từ theme tối cũ trượt contrast; `outline:none` mất focus; icon/nút thiếu nhãn ARIA; `localStorage.getItem('user')` parse không bắt lỗi.

## 3. UI/UX — hướng "Sơn mài & Giấy"

Nền giấy ấm, mực đen, đỏ sơn mài làm màu chủ đạo, jade cho thành công, vàng cho điểm thưởng; Fraunces (tiêu đề) + Be Vietnam Pro (UI, thiết kế cho tiếng Việt); dấu triện (hình thoi đỏ) làm điểm nhấn. Token `--nx-*` giữ nguyên tên nên các trang chưa đụng tới vẫn nhận palette mới. Đã làm lại: tokens/typography/components toàn cục, header, footer, trang chủ, đăng nhập/đăng ký, wishlist, chi tiết sản phẩm, admin layout/dashboard, thẻ sản phẩm; hỗ trợ `prefers-reduced-motion`, focus-visible, skip-link.
Chưa làm: checkout/orders/profile/seller/admin-tables theo hướng mới (đang hưởng token mới nhưng cần rà từng trang), dark mode, i18n thật (en/vi JSON mới có 19 dòng).
Lưu ý: font chưa kiểm chứng subset tiếng Việt qua mạng (Be Vietnam Pro chắc chắn có; Fraunces có fallback Georgia).

## 4. Lộ trình nâng cấp

**Giai đoạn 0 — An toàn & ổn định (1–2 tuần)**: sửa B1–B4, B6; token nội bộ giữa service (hoặc mTLS); bỏ `defaultValue` user; test e2e cho luồng checkout trong docker; dọn root (log, `BOOT-INF/`, `com/`, `*.ps1`).
**Giai đoạn 1 — Nền tảng (≈1–2 tháng)**: Flyway thay `ddl-auto=update`; OpenAPI + contract test cho Feign/RabbitMQ (Spring Cloud Contract/Pact); Testcontainers; CI chạy test frontend + Playwright; OpenTelemetry + Prometheus/Grafana; secrets không mặc định; refresh token + RS256/JWKS.
**Giai đoạn 2 — Tính năng nâng cao (≈2–4 tháng)**: tìm kiếm hybrid Elasticsearch (kNN + phân tích tiếng Việt, gợi ý tự động); tích hợp vận chuyển GHN/GHTK + webhook theo dõi; hoá đơn điện tử (NĐ 123/2020); thông báo Zalo ZNS/web push; công cụ người bán (upload hàng loạt, quảng cáo sản phẩm, dự báo tồn kho); B2B RFQ/hạn mức công nợ; PWA + SSR cho trang sản phẩm (SEO).
**Giai đoạn 3 — AI & quy mô (≈4–6+ tháng)**: trợ lý mua sắm bằng Claude có tool-use trên API catalog/đơn hàng (thay bot FAQ; model `claude-sonnet-5-5` cho hội thoại, `claude-haiku-4-5-20251001` cho phân loại/kiểm duyệt); tóm tắt & kiểm duyệt đánh giá; gợi ý cá nhân hoá theo sự kiện hành vi; chấm điểm gian lận ML; CDC (Debezium) cho outbox; Kubernetes + Helm + autoscaling; tuân thủ NĐ 13/2023 (PII).
**Quick wins (≤1 tuần)**: B3 (env compose), bỏ `defaultValue="1"`, Dependabot, thêm `README` chạy local một lệnh, xoá artefact build khỏi repo.
