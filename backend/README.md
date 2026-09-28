# ShopCore API

ShopCore API là dự án backend thương mại điện tử được xây dựng theo kiến trúc modular monolith bằng Java và Spring Boot.
Dự án hiện đang được phát triển và sẽ được mở rộng dần theo từng milestone.

## Trạng thái hiện tại

Skeleton ban đầu của dự án bao gồm:

- Java 21
- Spring Boot 4.1
- Maven Wrapper
- PostgreSQL 17
- Docker Compose
- Spring Data JPA
- Flyway
- Spring Boot Actuator
- Testcontainers

## Yêu cầu môi trường

Cài đặt các công cụ sau trước khi chạy dự án:

- Java Development Kit (JDK) 21
- Docker Desktop
- Git

Không cần cài đặt Maven trên máy vì repository đã bao gồm Maven Wrapper.

## Cấu trúc dự án

```text
shopcore/
├── backend/          Ứng dụng Spring Boot
└── .gitignore
```

Các lệnh Maven cần được chạy trong thư mục `backend`.

## Chạy test

Khởi động Docker Desktop, mở terminal tại thư mục `shopcore/backend` và chạy:

```powershell
.\mvnw.cmd clean test
```

Trên macOS hoặc Linux, chạy:

```bash
./mvnw clean test
```

Integration test sẽ khởi tạo PostgreSQL thông qua Testcontainers.

## Chạy ứng dụng trên máy local

Đảm bảo Docker Desktop đang chạy, sau đó thực hiện lệnh sau trong thư mục `shopcore/backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Spring Boot sử dụng [`compose.yaml`](compose.yaml) để tự động khởi động PostgreSQL.
Ứng dụng chạy tại địa chỉ:

```text
http://localhost:8080
```

## Kiểm tra trạng thái ứng dụng

Sau khi ứng dụng khởi động thành công, truy cập:

```text
http://localhost:8080/actuator/health
```

Hoặc chạy lệnh sau trong PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Kết quả mong đợi:

```json
{
  "status": "UP"
}
```

## Cơ sở dữ liệu

PostgreSQL local sử dụng cấu hình mặc định sau:

| Cấu hình | Giá trị |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `shopcore` |
| Username | `shopcore` |

Mật khẩu dành cho môi trường phát triển được lấy từ biến `POSTGRES_PASSWORD`. Nếu biến này chưa được khai báo, Docker Compose sử dụng mật khẩu local mặc định trong `compose.yaml`.

Để đặt mật khẩu tùy chỉnh cho phiên PowerShell hiện tại:

```powershell
$env:POSTGRES_PASSWORD="your-local-password"
.\mvnw.cmd spring-boot:run
```

Không commit mật khẩu production hoặc thông tin bí mật vào repository.

## Dừng dịch vụ local

Nhấn `Ctrl+C` để dừng ứng dụng. Để dừng các dịch vụ Docker Compose, chạy:

```powershell
docker compose down
```

Lệnh trên vẫn giữ lại named volume của PostgreSQL. Không thêm tùy chọn `-v` trừ khi bạn chủ động muốn xóa dữ liệu database local.

## Kế hoạch phát triển

Xem [`PROJECT_PLAN.md`](PROJECT_PLAN.md) để biết các milestone, kiến trúc, phạm vi API và tiêu chí hoàn thành của dự án.

## Tính năng dự kiến

- Quản lý danh mục và sản phẩm
- Xác thực bằng JWT và phân quyền theo vai trò
- Quản lý tồn kho và xử lý concurrent request
- Giỏ hàng và voucher
- Xử lý đơn hàng trong transaction
- Thanh toán giả lập và idempotency
- Cache và rate limiting bằng Redis
- RabbitMQ và Outbox Pattern
- Observability, CI/CD và triển khai ứng dụng

