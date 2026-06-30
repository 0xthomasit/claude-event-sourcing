# Hướng dẫn Chạy API trên Postman cho Môi trường Dev

Tài liệu này hướng dẫn chi tiết cách thiết lập môi trường, chạy Database Migrations và sử dụng Postman để gọi API cho các microservices trong dự án **Claude Event Sourcing**.

---

## 1. Chuẩn bị Cơ sở dữ liệu và Môi trường

Trước khi gọi API, bạn cần đảm bảo các dịch vụ hạ tầng (Database, Redis, Message Broker...) đã được khởi chạy.

### Khởi chạy Docker Compose
Mở terminal tại thư mục gốc của dự án và chạy lệnh sau để khởi động hạ tầng:

```bash
cd docker
docker-compose -f docker-compose.infra.yml up -d
```

> [!TIP]
> Lệnh này sẽ tải và chạy các container như PostgreSQL, MongoDB, Redis, Kafka (nếu có) trên máy local của bạn. Bạn có thể kiểm tra trạng thái các container bằng lệnh `docker ps`.

---

## 2. Khởi chạy Database Migrations

Dự án này sử dụng **Flyway** để quản lý DB Migrations (được khai báo trong `pom.xml` của từng service).

**Cách Flyway hoạt động trong môi trường Dev:**
Khác với việc phải chạy script migration bằng tay, Flyway được tích hợp trực tiếp vào quá trình khởi động của Spring Boot.
1. Khởi động các database thông qua Docker Compose (như bước 1).
2. Chạy (Run/Debug) các service (ví dụ `AuthServiceApplication`, `OrderServiceApplication`) từ IntelliJ IDEA hoặc thông qua Maven (`mvn spring-boot:run`).
3. Trong quá trình khởi động, Spring Boot sẽ tự động quét thư mục `src/main/resources/db/migration` và thực thi các script SQL (như `V1__init.sql`) lên database tương ứng.

> [!NOTE]
> **Không cần thao tác thủ công:** Miễn là database đã sẵn sàng, mọi bảng (tables) và dữ liệu mẫu (nếu có trong script migration) sẽ được tự động tạo khi service khởi động thành công.

---

## 3. Cấu hình Môi trường Postman (Postman Environment)

Để dễ dàng quản lý việc gọi API giữa các service, bạn nên tạo một **Environment** trong Postman.

### Bước 1: Tạo Environment Mới
1. Mở Postman, ở góc trên bên phải, click vào **Environments** > **Create Environment**.
2. Đặt tên: `Claude-Dev-Local`.
3. Thêm các biến (Variables) theo danh sách các cổng của service:

| Variable | Initial Value | Current Value | Mô tả |
| :--- | :--- | :--- | :--- |
| `auth_url` | `http://localhost:8089` | `http://localhost:8089` | Base URL Auth Service |
| `order_url` | `http://localhost:8081` | `http://localhost:8081` | Base URL Order Service |
| `product_url`| `http://localhost:8086` | `http://localhost:8086` | Base URL Product Service |
| `cart_url` | `http://localhost:8088` | `http://localhost:8088` | Base URL Cart Service |
| `access_token`| (để trống) | (để trống) | Token lưu tự động sau khi Login |

### Bước 2: Thiết lập Tự động lưu Token (Script)
Khi thực hiện API Login, ta cần lưu access token lại để dùng cho các request sau. Ở API Login, chuyển sang tab **Tests** và thêm đoạn script sau:

```javascript
var jsonData = pm.response.json();
if (jsonData.accessToken) {
    pm.environment.set("access_token", jsonData.accessToken);
    console.log("Token updated!");
}
```

---

## 4. Giao diện (Screenshot Mockups) và Dữ liệu Mẫu

Do môi trường chat hạn chế hình ảnh thực tế, dưới đây là mô phỏng giao diện Postman (thông qua Text UI UI Mockup) và chi tiết dữ liệu JSON cho các API quan trọng.

### 4.1. Auth Service - Đăng ký Tài khoản (Register)

**Thông tin Request:**
- **Method:** `POST`
- **URL:** `{{auth_url}}/api/auth/register`
- **Tab Headers:** `Content-Type: application/json`

**Tab Body (raw > JSON):**
```json
{
  "email": "testuser@example.com",
  "password": "Password123!",
  "fullName": "Nguyen Van A"
}
```

> [!TIP]
> **Mô phỏng Giao diện Postman:**
> 
> 🟢 **POST** &nbsp; `{{auth_url}}/api/auth/register` &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; [ Send ]
> 
> **Params** | **Authorization** | **Headers (1)** | **Body (●)** | **Pre-request Script** | **Tests**
> ( ) none &nbsp; ( ) form-data &nbsp; ( ) x-www-form-urlencoded &nbsp; (●) raw &nbsp; &nbsp; `JSON ▾`
> 
> *Response (201 Created):*
> ```json
> {
>     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
>     "refreshToken": "d7a4b8e2-...",
>     "expiresIn": 3600
> }
> ```

### 4.2. Auth Service - Đăng nhập (Login)

**Thông tin Request:**
- **Method:** `POST`
- **URL:** `{{auth_url}}/api/auth/login`

**Tab Body (raw > JSON):**
```json
{
  "email": "testuser@example.com",
  "password": "Password123!"
}
```
*(Lưu ý: Nhớ thêm Script ở tab **Tests** như hướng dẫn phần 3 để tự động lưu `{{access_token}}`)*

### 4.3. Các API yêu cầu Xác thực (Bearer Token)

Đối với các API ở các service khác yêu cầu người dùng phải đăng nhập, bạn cần truyền Token vào Header.

**Cách thiết lập trong Postman:**
1. Mở Request mới (ví dụ: Tạo đơn hàng `POST {{order_url}}/api/orders`).
2. Chuyển sang tab **Authorization**.
3. Chọn Type là **Bearer Token**.
4. Ở ô Token, điền biến môi trường: `{{access_token}}`.

> [!IMPORTANT]
> **Mô phỏng Giao diện Postman (Auth Tab):**
>
> 🔵 **POST** &nbsp; `{{order_url}}/api/orders` &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; [ Send ]
> 
> **Params** | **Authorization (●)** | **Headers** | **Body** | **Pre-request Script** | **Tests**
>
> Type: `Bearer Token ▾`
>
> Token: `{{access_token}}`


### Tổng hợp danh sách Port cho toàn bộ Service

Nếu bạn cần test trực tiếp các service bỏ qua API Gateway, đây là danh sách port đang cấu hình tại môi trường dev cục bộ (`application.yml`):

- **Order Service**: `8081`
- **Payment Service**: `8082`
- **Inventory Service**: `8083`
- **Notification Service**: `8084`
- **Shipping Service**: `8085`
- **Product Service**: `8086`
- **User Service**: `8087`
- **Cart Service**: `8088`
- **Auth Service**: `8089`

---

## 5. Các bước thực hiện tổng quát (Checklist)

1. Mở terminal, chạy `docker-compose -f docker-compose.infra.yml up -d` để chạy hạ tầng (DB, Redis...).
2. Mở IDE (IntelliJ/Eclipse), khởi chạy `AuthServiceApplication` (Flyway tự động migrate cho Auth DB).
3. Khởi chạy các service khác bạn muốn test (Flyway sẽ migrate tương ứng cho Product DB, Order DB...).
4. Mở Postman, chọn Environment `Claude-Dev-Local`.
5. Chạy API **Register** để tạo user mới.
6. Chạy API **Login**, Postman tự động lưu `{{access_token}}`.
7. Chuyển sang các tab API khác, chọn Auth Type là `Bearer Token` với value `{{access_token}}` và tiến hành gọi API bình thường.
