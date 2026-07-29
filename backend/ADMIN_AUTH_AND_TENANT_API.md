# OmnichannelPOS Admin Backend

Backend này chỉ dành cho **platform owner**. Nó cung cấp:

- Đăng nhập quản trị cao nhất bằng opaque session.
- Cookie phiên `HttpOnly`, `SameSite`, có thể bật `Secure`.
- CSRF double-submit cookie cho các request thay đổi dữ liệu.
- Argon2id cho mật khẩu.
- Giới hạn đăng nhập theo IP và khóa tài khoản tạm thời.
- Tạo tenant, gói thuê, tenant manager, credential, role và PII policy trong một transaction.
- Audit đăng nhập, đăng xuất và cấp tenant.

## Cấu trúc source code

```text
src/main/java/com/admin/
├── config/       # Spring Security, OpenAPI, properties và bootstrap owner
├── controller/   # HTTP controller; không chứa SQL hoặc business logic
├── dto/          # Request/response API
├── entity/       # Domain record được repository và service sử dụng
├── exception/    # Business exception và RFC 9457 ProblemDetail handler
├── repository/   # Toàn bộ truy vấn JdbcTemplate/SQL
├── security/     # Session filter, rate limit, password/token utility
├── service/      # Interface nghiệp vụ
└── service/impl/ # Hiện thực transaction và business rule
```

Quy tắc phụ thuộc:

```text
Controller → Service interface → Service implementation → Repository → MySQL
                       │
                       └── Security/domain utilities
```

SQL không được đặt trong `controller` hoặc `service/impl`.

## 1. Chuẩn bị cấu hình local

Backend local đọc trực tiếp cấu hình từ:

```text
src/main/resources/application.properties
```

Thay giá trị sau bằng mật khẩu MySQL local:

```properties
spring.datasource.password=CHANGE_ME_MYSQL_PASSWORD
```

Kiểm tra mật khẩu trước khi chạy:

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p
```

Tài khoản platform owner local được tạo từ các thuộc tính:

```properties
app.admin.bootstrap.enabled=true
app.admin.bootstrap.email=admin@omnichannel.local
app.admin.bootstrap.password=Local-Admin-ChangeMe-2026!
app.admin.bootstrap.display-name=Platform Owner
```

Đổi mật khẩu bootstrap trước lần chạy thành công đầu tiên.

Khởi động:

```bash
./gradlew bootRun
```

Sau lần bootstrap thành công, sửa:

```properties
app.admin.bootstrap.enabled=false
```

Khi triển khai HTTPS, `application-prod.properties` sẽ bật cookie `Secure`.

## 2. API

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

### Lấy CSRF token

```http
GET /api/admin/auth/csrf
```

Response:

```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "csrf-token"
}
```

Trình duyệt phải giữ cookie `XSRF-TOKEN` và gửi token trong header
`X-XSRF-TOKEN` cho các request POST/PUT/PATCH/DELETE được bảo vệ.

### Đăng nhập platform owner

```http
POST /api/admin/auth/login
Content-Type: application/json
X-XSRF-TOKEN: csrf-token
Cookie: XSRF-TOKEN=...

{
  "email": "your-email@example.com",
  "password": "A-strong-owner-password-2026!"
}
```

Token session không xuất hiện trong JSON. Server gửi cookie
`omni_admin_session` với thuộc tính `HttpOnly`.

### Xem tài khoản hiện tại

```http
GET /api/admin/auth/me
Cookie: omni_admin_session=...
```

### Tạo tài khoản thuê

```http
POST /api/admin/tenants
Content-Type: application/json
X-XSRF-TOKEN: csrf-token
Cookie: XSRF-TOKEN=...; omni_admin_session=...

{
  "tenantCode": "SHOP_001",
  "tenantName": "Cửa hàng số 1",
  "legalName": "Công ty TNHH Cửa hàng số 1",
  "contactEmail": "contact@shop1.example",
  "timezoneName": "Asia/Ho_Chi_Minh",
  "defaultCurrency": "VND",
  "subscriptionPlanCode": "DEMO",
  "trialDays": 14,
  "ownerEmail": "manager@shop1.example",
  "ownerDisplayName": "Quản lý cửa hàng"
}
```

API tạo đồng thời:

1. `tenants`
2. `tenant_subscriptions`
3. `tenant_users`
4. `tenant_user_credentials`
5. `tenant_user_roles`
6. Tám bản ghi `data_protection_policies`
7. `security_audit_logs`

`temporaryPassword` chỉ được trả về trong response tạo tenant. Không log và
không lưu plaintext. Tenant user bị buộc đổi mật khẩu ở lần đăng nhập đầu.

### Quản lý gói dịch vụ

Tất cả API dưới đây yêu cầu cookie `omni_admin_session`. Các request thay đổi
dữ liệu còn phải gửi cookie và header CSRF.

Tạo plan:

```http
POST /api/admin/plans
Content-Type: application/json
X-XSRF-TOKEN: csrf-token

{
  "planCode": "STARTER",
  "planName": "Gói Starter",
  "billingPeriod": "MONTHLY",
  "priceAmount": 99000,
  "currency": "VND",
  "limits": {
    "marketplaceAccounts": 2,
    "tenantUsers": 5
  },
  "features": {
    "aiSale": true,
    "analytics": true
  },
  "status": "ACTIVE"
}
```

Danh sách có tìm kiếm, trạng thái và phân trang:

```http
GET /api/admin/plans?search=starter&status=ACTIVE&page=0&size=20
```

Xem chi tiết:

```http
GET /api/admin/plans/{planId}
```

Cập nhật toàn bộ cấu hình:

```http
PUT /api/admin/plans/{planId}
Content-Type: application/json
X-XSRF-TOKEN: csrf-token
```

Body của `PUT` giống `POST`.

Đổi trạng thái:

```http
PATCH /api/admin/plans/{planId}/status
Content-Type: application/json
X-XSRF-TOKEN: csrf-token

{
  "status": "ARCHIVED"
}
```

Giá trị hợp lệ:

- `billingPeriod`: `MONTHLY`, `QUARTERLY`, `YEARLY`, `CUSTOM`.
- `status`: `ACTIVE`, `INACTIVE`, `ARCHIVED`.

Không xóa vật lý plan vì `tenant_subscriptions` có thể đang tham chiếu. Dùng
`ARCHIVED` để loại plan khỏi danh sách gói được phép cấp mới.

## 3. Frontend Axios

```ts
const api = axios.create({
  baseURL: "http://localhost:8080",
  withCredentials: true,
});

const csrf = await api.get("/api/admin/auth/csrf");

await api.post("/api/admin/auth/login", {
  email,
  password,
}, {
  headers: {
    "X-XSRF-TOKEN": csrf.data.token,
  },
});

await api.post("/api/admin/tenants", tenantPayload, {
  headers: {
    "X-XSRF-TOKEN": csrf.data.token,
  },
});
```

Không lưu `omni_admin_session` vào `localStorage`, `sessionStorage`, Redux hoặc
JavaScript state.
