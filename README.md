# ShopCore API

ShopCore API là REST API thương mại điện tử, xây dựng bằng Java 21 và Spring Boot theo kiến trúc modular monolith.

Dự án đang được phát triển. Phần xác thực, phân quyền và danh mục sản phẩm đã hoàn thành. Tồn kho, giỏ hàng, đơn hàng và thanh toán sẽ được làm tiếp.

## Tính năng

### Xác thực

- Đăng ký và đăng nhập, mật khẩu được băm bằng BCrypt
- Access token JWT không trạng thái (HS256, thời hạn 15 phút)
- Xoay vòng refresh token, mỗi phiên đăng nhập có thời hạn cố định 7 ngày
- Refresh token chỉ được lưu dưới dạng mã băm SHA-256
- Phát hiện dùng lại token: khi một refresh token đã thay thế bị dùng lại, toàn bộ token family bị thu hồi
- Pessimistic lock trên bản ghi user, để các request refresh và logout của cùng một user chạy lần lượt

### Phân quyền

- User, role và permission được lưu trong database
- Kiểm tra quyền theo URL cho `/api/v1/admin/**` và theo method bằng `@PreAuthorize` cho các thao tác ghi dữ liệu catalog
- Lỗi `401` và `403` trả về cùng một định dạng JSON

### Danh mục sản phẩm

- API công khai để xem danh mục và sản phẩm
- API admin để tạo, sửa, xóa danh mục và sản phẩm
- Tìm sản phẩm theo từ khóa, danh mục và khoảng giá, có phân trang và sắp xếp
- Xóa mềm sản phẩm
- Optimistic lock: cập nhật sản phẩm với `version` cũ sẽ trả về `409 Conflict`

### Nền tảng

- Quản lý phiên bản schema PostgreSQL bằng Flyway, Hibernate kiểm tra schema khi khởi động
- Validate request và xử lý exception tập trung
- Tài liệu API bằng OpenAPI và Swagger UI
- Endpoint kiểm tra trạng thái bằng Spring Boot Actuator

## Công nghệ

| Nhóm | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.1, Spring Web MVC |
| Bảo mật | Spring Security, OAuth2 Resource Server (JWT) |
| Dữ liệu | Spring Data JPA, Hibernate, PostgreSQL 17, Flyway |
| Tài liệu API | springdoc-openapi, Swagger UI |
| Công cụ | Maven Wrapper, Docker Compose, Testcontainers, Lombok |

## Cấu trúc dự án

```text
shopcore/
└── backend/
    ├── compose.yaml                 PostgreSQL cho môi trường local
    └── src/main/
        ├── java/.../shopcore/
        │   ├── auth/                Đăng nhập, token, cấu hình bảo mật
        │   ├── rbac/                Role và permission
        │   ├── user/                Tài khoản người dùng
        │   ├── category/            API danh mục
        │   ├── product/             API sản phẩm và tìm kiếm
        │   └── common/              Auditing, xử lý lỗi, kiểu response
        └── resources/db/migration/  Flyway migration
```

Mỗi module được chia thành các lớp controller, service và repository.

## Chạy trên máy local

### Yêu cầu

- JDK 21
- Docker Desktop

Không cần cài Maven vì repository đã có Maven Wrapper.

### Khởi động ứng dụng

1. Clone repository:

   ```bash
   git clone https://github.com/datnguyen2810/shopcore.git
   cd shopcore/backend
   ```

2. Tạo volume cho PostgreSQL. Bước này chỉ cần làm một lần, vì `compose.yaml` dùng external volume:

   ```bash
   docker volume create backend_shopcore-postgres-data
   ```

3. Chạy ứng dụng. Spring Boot sẽ khởi động PostgreSQL qua Docker Compose, và Flyway sẽ tạo schema.

   ```bash
   # macOS hoặc Linux
   ./mvnw spring-boot:run

   # Windows PowerShell
   .\mvnw.cmd spring-boot:run
   ```

4. Mở ứng dụng:

   | Trang | URL |
   |---|---|
   | Swagger UI | http://localhost:8080/swagger-ui/index.html |
   | Kiểm tra trạng thái | http://localhost:8080/actuator/health |

### Biến môi trường

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `SERVER_PORT` | `8080` | Cổng của ứng dụng |
| `POSTGRES_PASSWORD` | `shopcore_dev_password` | Mật khẩu database local |

### Dừng ứng dụng

Nhấn `Ctrl+C`, sau đó chạy lệnh này trong thư mục `backend/`:

```bash
docker compose down
```

Dữ liệu vẫn được giữ trong Docker volume.

## Dùng thử API

Các ví dụ dưới đây dùng `curl` trong Bash, ví dụ Git Bash trên Windows. Swagger UI chưa có nút **Authorize**, nên hãy dùng `curl` hoặc Postman để gọi các endpoint cần đăng nhập.

### Đăng ký và đăng nhập

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_admin","email":"demo@example.com","password":"demo123456","fullName":"Demo Admin"}'

curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_admin","password":"demo123456"}'
```

Sao chép `accessToken` trong response đăng nhập, rồi gọi endpoint cần xác thực:

```bash
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>"
```

### Cấp quyền admin

User mới có role `CUSTOMER`, và hệ thống không tạo sẵn tài khoản admin. Để cấp quyền admin cho user demo, chạy:

```bash
docker exec shopcore-postgres psql -U shopcore -d shopcore -c "INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u CROSS JOIN roles r WHERE u.username = 'demo_admin' AND r.code = 'ADMIN';"
```

Đăng nhập lại để lấy token có role mới, rồi tạo danh mục:

```bash
curl -X POST http://localhost:8080/api/v1/admin/categories \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Phones","slug":"phones"}'
```

Sản phẩm mới có trạng thái `DRAFT`. Sản phẩm chỉ hiện ở API công khai sau khi chuyển sang `ACTIVE`.

## Tổng quan API

Xem chi tiết request và response trong Swagger UI.

### Xác thực và người dùng

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Công khai | Tạo tài khoản khách hàng |
| POST | `/api/v1/auth/login` | Công khai | Lấy access token và refresh token |
| POST | `/api/v1/auth/refresh` | Công khai | Đổi refresh token lấy cặp token mới |
| POST | `/api/v1/auth/logout` | User | Thu hồi token family của refresh token |
| GET | `/api/v1/users/me` | User | Lấy thông tin user hiện tại |

### Danh mục và sản phẩm

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| GET | `/api/v1/categories` | Công khai | Lấy danh sách danh mục |
| GET | `/api/v1/categories/{id}` | Công khai | Lấy một danh mục |
| GET | `/api/v1/products` | Công khai | Tìm kiếm sản phẩm |
| GET | `/api/v1/products/{id}` | Công khai | Lấy một sản phẩm |
| POST | `/api/v1/admin/categories` | Admin | Tạo danh mục |
| PUT | `/api/v1/admin/categories/{id}` | Admin | Cập nhật danh mục |
| DELETE | `/api/v1/admin/categories/{id}` | Admin | Xóa danh mục |
| POST | `/api/v1/admin/products` | Admin | Tạo sản phẩm |
| PUT | `/api/v1/admin/products/{id}` | Admin | Cập nhật sản phẩm |
| PATCH | `/api/v1/admin/products/{id}/status` | Admin | Đổi trạng thái sản phẩm |
| DELETE | `/api/v1/admin/products/{id}` | Admin | Xóa mềm sản phẩm |

Ví dụ tìm kiếm sản phẩm:

```text
GET /api/v1/products?keyword=iphone&minPrice=100&maxPrice=2000&currentPage=0&pageSize=10&sortBy=price:asc
```

### Định dạng response

Response thành công:

```json
{
  "status": 200,
  "message": "Login successfully",
  "data": {}
}
```

Response lỗi:

```json
{
  "errorCode": "INVALID_REFRESH_TOKEN",
  "message": "Refresh token is invalid or expired",
  "path": "/api/v1/auth/refresh",
  "status": 401,
  "timestamp": "2026-11-06T17:15:35.821Z"
}
```

## Các quyết định thiết kế chính

**Xoay vòng refresh token.** Mỗi lần refresh, token cũ được thay bằng token mới trong cùng family. Nếu token đã thay thế bị dùng lại, có thể nó đã bị đánh cắp, nên cả family bị thu hồi và người dùng phải đăng nhập lại.

**Hai chiến lược khóa.** Refresh và logout dùng pessimistic lock vì các thao tác này ngắn và không được chạy xen kẽ nhau. Cập nhật sản phẩm dùng optimistic lock vì xung đột ít xảy ra, và client có thể tải lại sản phẩm rồi thử lại.

**Modular monolith.** Tất cả module chạy trong một ứng dụng và dùng chung một database. Cách này giúp việc triển khai và xử lý transaction đơn giản, trong khi các package vẫn tách riêng từng mảng nghiệp vụ.

## Chạy test

Khởi động Docker Desktop, rồi chạy lệnh này trong thư mục `backend/`:

```bash
./mvnw clean verify
```

Trên Windows PowerShell, dùng `.\mvnw.cmd clean verify`.

Hiện có một test, test này khởi động toàn bộ ứng dụng với PostgreSQL chạy bằng Testcontainers.

## Hạn chế hiện tại

- Chưa có integration test cho API xác thực và catalog.
- Access token vẫn dùng được đến khi hết hạn, tối đa 15 phút sau khi logout.
- JWT secret trong `application.yml` chỉ dùng cho môi trường local, không được dùng khi triển khai.
- Schema đã có ảnh sản phẩm, nhưng chưa có đầy đủ API quản trị ảnh.
- Tìm kiếm từ khóa dùng `LIKE '%keyword%'`, chỉ phù hợp với catalog nhỏ.
- Ứng dụng chưa được triển khai và chưa có CI/CD.

## Lộ trình

- [x] Danh mục và sản phẩm
- [x] Xác thực JWT, RBAC và xoay vòng refresh token
- [ ] Integration test cho xác thực và catalog
- [ ] Tồn kho và xử lý cập nhật đồng thời
- [ ] Giỏ hàng và voucher
- [ ] Xử lý đơn hàng
- [ ] Thanh toán giả lập với webhook idempotent
- [ ] Cache và rate limiting bằng Redis
- [ ] RabbitMQ và Outbox pattern
- [ ] Observability, CI/CD và triển khai

## Tác giả

Nguyễn Xuân Đạt · [GitHub](https://github.com/datnguyen2810)
