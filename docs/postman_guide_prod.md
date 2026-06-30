# Hướng dẫn Chạy API trên Postman cho Môi trường Production

Tài liệu này hướng dẫn cách chạy toàn bộ ứng dụng (Full Stack) tương tự như môi trường Production thông qua **Docker Compose** và cách cấu hình Postman để gọi API thông qua **API Gateway**.

---

## 1. Khởi chạy Hệ thống trên Production

Trên môi trường Production (hoặc giả lập Production tại Local), chúng ta không chạy từng Spring Boot service riêng lẻ bằng IDE, mà sẽ đóng gói toàn bộ thành Docker Container và chạy đồng loạt.

Mở terminal tại thư mục gốc của dự án và chạy các lệnh sau:

```bash
cd docker

# 1. Chạy tầng Infrastructure trước (DBs, Redis, Kafka...)
docker-compose -f docker-compose.infra.yml up -d

# 2. Đợi khoảng 10-15s cho DB khởi động ổn định, sau đó chạy toàn bộ các Services
docker-compose up -d
```

> [!TIP]
> **Database Migrations:** Tương tự như môi trường Dev, khi các container Spring Boot (order-service, auth-service,...) khởi chạy, Flyway sẽ tự động kết nối vào Database và thực thi script migration. Bạn không cần can thiệp thủ công.
> 
> Bạn có thể kiểm tra xem tất cả các container đã "Healthy" chưa bằng lệnh `docker ps`.

---

## 2. Điểm khác biệt Cốt lõi của Môi trường Production

Ở môi trường Dev, bạn gọi trực tiếp vào port của từng service (ví dụ: Auth 8089, Order 8081).
Tuy nhiên ở **Production**, tất cả các service nội bộ sẽ bị ẩn đi. Toàn bộ request từ bên ngoài (bao gồm cả Postman, Web, Mobile) **BẮT BUỘC** phải đi qua **API Gateway**.

- **Host/Port duy nhất:** `http://localhost:8080` (hoặc tên miền thực tế như `https://api.yourdomain.com`).
- **Cơ chế Routing:** API Gateway sẽ dựa vào tiền tố URL để điều phối request về đúng service nội bộ.
  - `/api/auth/**` ➔ `auth-service`
  - `/api/products/**` ➔ `product-service`
  - `/api/orders/**` ➔ `order-service`
  - `/api/cart/**` ➔ `cart-service`
- **Bảo mật:** Trừ `/api/auth/` và `/api/products/`, các API còn lại đều bị chặn bởi `AuthenticationFilter` tại Gateway. Gateway sẽ kiểm tra tính hợp lệ của Token trước khi cho phép request đi tiếp vào bên trong.

---

## 3. Cấu hình Môi trường Postman (Production)

Việc cấu hình Postman trên Production đơn giản hơn rất nhiều vì bạn chỉ cần quản lý một URL duy nhất.

### Bước 1: Tạo Environment Mới
1. Mở Postman > **Environments** > **Create Environment**.
2. Đặt tên: `Claude-Production`.
3. Thêm các biến sau:

| Variable | Initial Value | Current Value | Mô tả |
| :--- | :--- | :--- | :--- |
| `base_url` | `http://localhost:8080` | `http://localhost:8080` | URL của API Gateway |
| `access_token`| (để trống) | (để trống) | Token lưu tự động sau khi Login |

### Bước 2: Thiết lập Tự động lưu Token (Script)
Giống với Dev, ở API Login, chuyển sang tab **Tests** và dán đoạn script sau:

```javascript
var jsonData = pm.response.json();
if (jsonData.accessToken) {
    pm.environment.set("access_token", jsonData.accessToken);
    console.log("Prod Token updated!");
}
```

---

## 4. Giao diện (Screenshot Mockups) và Dữ liệu Mẫu

Lưu ý: Tất cả các API bây giờ đều sử dụng biến `{{base_url}}`.

### 4.1. Auth Service - Đăng ký Tài khoản (Register)

**Thông tin Request:**
- **Method:** `POST`
- **URL:** `{{base_url}}/api/auth/register`
- **Tab Headers:** `Content-Type: application/json`

**Tab Body (raw > JSON):**
```json
{
  "email": "produser@example.com",
  "password": "Password123!",
  "fullName": "Le Van Prod"
}
```

> [!TIP]
> **Mô phỏng Giao diện Postman:**
> 
> 🟢 **POST** &nbsp; `{{base_url}}/api/auth/register` &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; [ Send ]
> 
> **Params** | **Authorization** | **Headers (1)** | **Body (●)** | **Pre-request Script** | **Tests**
> ( ) none &nbsp; ( ) form-data &nbsp; ( ) x-www-form-urlencoded &nbsp; (●) raw &nbsp; &nbsp; `JSON ▾`

### 4.2. Auth Service - Đăng nhập (Login)

**Thông tin Request:**
- **Method:** `POST`
- **URL:** `{{base_url}}/api/auth/login`

**Tab Body (raw > JSON):**
```json
{
  "email": "produser@example.com",
  "password": "Password123!"
}
```
*(Sau khi Send, token sẽ được tự động lưu vào biến `{{access_token}}` nhờ đoạn Test script).*

### 4.3. Gọi API thông qua Gateway với Token (Order Service)

Bất kỳ request nào cần xác thực (như tạo Order, xem Cart), bạn đều cấu hình tab **Authorization** với Type là **Bearer Token** và Value là `{{access_token}}`.
Gateway sẽ tự động kiểm tra token này và giới hạn tốc độ (Rate Limiting) trước khi forward tới Order Service.

**Thông tin Request:**
- **Method:** `POST`
- **URL:** `{{base_url}}/api/orders`

> [!IMPORTANT]
> **Mô phỏng Giao diện Postman (Auth Tab):**
>
> 🔵 **POST** &nbsp; `{{base_url}}/api/orders` &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; [ Send ]
> 
> **Params** | **Authorization (●)** | **Headers** | **Body** | **Pre-request Script** | **Tests**
>
> Type: `Bearer Token ▾`
>
> Token: `{{access_token}}`


---

## 5. Tổng kết Checklist Chạy App trên Production

1. Chạy lệnh: `docker-compose -f docker-compose.infra.yml up -d`
2. Chạy lệnh: `docker-compose up -d` (Chờ các service boot up và Service Discovery - Eureka đăng ký thành công).
3. Mở Postman, chọn Environment `Claude-Production`.
4. Gọi API **Register** (`{{base_url}}/api/auth/register`).
5. Gọi API **Login** (`{{base_url}}/api/auth/login`) để nhận token.
6. Cấu hình **Bearer Token** `{{access_token}}` và gọi các API khác thông qua `{{base_url}}`.
