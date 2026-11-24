# REST API 文档

- Base URL：`http://localhost:8080/api`
- 编码：UTF-8
- 认证：`Authorization: Bearer <token>`
- 成功响应：`{ "code": 0, "message": "ok", "data": ... }`
- 失败响应：HTTP 4xx/5xx + `{ "code": 400, "message": "...", "data": null }`

## 1. 认证

### 登录

```http
POST /api/auth/login
Content-Type: application/json

{ "username": "admin", "password": "admin123" }
```

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "11292c02-7c7d-4583-8bde-6fa90b22dc2c",
    "user": {
      "username": "admin",
      "role": "ADMIN",
      "roleText": "管理员",
      "displayName": "系统管理员",
      "customerId": ""
    }
  }
}
```

### 注册

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "123456",
  "name": "新客户",
  "idNumber": "TESTREG001",
  "phone": "13900000000",
  "email": "new@example.com"
}
```

注册成功后同时创建 `Customer`（证件号为主键）和 `User`（CUSTOMER 角色），返回 Token。

### 当前用户 / 注销

```http
GET  /api/auth/me
POST /api/auth/logout
```

## 2. 航班

### 查询航班

```http
GET /api/flights?from=桂林&to=西安&date=2026-11-27&onlyAvailable=true
```

| 参数 | 必填 | 说明 |
|---|---|---|
| `from` | 否 | 出发城市（精确匹配） |
| `to` | 否 | 到达城市（精确匹配） |
| `date` | 否 | `yyyy-MM-dd` |
| `onlyAvailable` | 否 | `true` 只看有票 |

返回航班 DTO：

```json
{
  "flightId": "CA1229",
  "date": "2026-11-27",
  "takeOffTime": "14:00",
  "arrivalTime": "16:30",
  "durationMinutes": 150,
  "startCity": "桂林",
  "endCity": "西安",
  "route": "桂林→西安",
  "planeModel": "A118",
  "price": 888.0,
  "discount": 0.85,
  "actualPrice": 754.8,
  "totalSeats": 180,
  "bookedSeats": 2,
  "remainingSeats": 178,
  "full": false,
  "airline": "桂林国际航空",
  "onTimeRate": 96.5
}
```

### 航班详情

```http
GET /api/flights/{flightId}
```

## 3. 订单

### 查询订单

```http
GET /api/orders?status=BOOKED&keyword=张伟
```

- 客户只能看到自己的订单；
- 管理员可以看到全部订单；
- `status`：`BOOKED` / `REFUNDED`；
- `keyword`：订单号 / 航班号 / 客户姓名 / 证件号。

### 订票

```http
POST /api/orders
Content-Type: application/json

{ "flightId": "CA1229", "quantity": 2 }
```

管理员订票时必须额外指定客户：

```json
{ "flightId": "CA1229", "quantity": 2, "customerId": "450102199001011234" }
```

成功返回订单 DTO。余票不足返回 **409**：

```json
{
  "code": 409,
  "message": "航班 CA1235 余票不足：剩余 0 张，本次需要 1 张。",
  "data": {
    "alternatives": [
      {
        "flightId": "CA1229",
        "date": "2026-11-27",
        "takeOffTime": "14:00",
        "route": "桂林→西安",
        "remainingSeats": 178,
        "actualPrice": 754.8
      }
    ]
  }
}
```

### 订单详情 / 退票

```http
GET  /api/orders/{orderId}
POST /api/orders/{orderId}/refund
```

退票后订单状态为 `REFUNDED`，座位自动回补；重复退票返回 400。

## 4. 客户

### 客户本人资料

```http
GET /api/customers/me
PUT /api/customers/me

{ "name": "张伟", "phone": "13800001001", "email": "zhangwei@example.com" }
```

管理员账号没有客户资料，返回 400。

### 管理员客户管理

```http
GET    /api/admin/customers?keyword=
POST   /api/admin/customers
PUT    /api/admin/customers/{customerId}
DELETE /api/admin/customers/{customerId}
GET    /api/admin/customers/{customerId}/orders
```

客户 DTO：

```json
{
  "customerId": "450102199001011234",
  "name": "张伟",
  "phone": "13800001001",
  "email": "zhangwei@example.com",
  "level": "NORMAL",
  "levelText": "普通",
  "createdAt": "2026-09-01 09:00:00"
}
```

存在订单的客户不能删除（409）。

## 5. 航班管理（管理员）

```http
GET    /api/admin/flights?from=&to=&date=
POST   /api/admin/flights
PUT    /api/admin/flights/{flightId}
DELETE /api/admin/flights/{flightId}
```

新增/修改请求体：

```json
{
  "flightId": "CA2000",
  "date": "2026-12-20",
  "takeOffTime": "09:00",
  "durationMinutes": 90,
  "startCity": "桂林",
  "endCity": "广州",
  "planeModel": "A320",
  "price": 500,
  "discount": 0.8,
  "totalSeats": 100,
  "airline": "测试航空",
  "onTimeRate": 99
}
```

## 6. 票价管理（管理员）

### 单航班改价

```http
PUT /api/admin/flights/{flightId}/fare

{ "price": 520, "discount": 0.75 }
```

### 按航线批量折扣

```http
POST /api/admin/fares/bulk

{ "startCity": "桂林", "endCity": "西安", "discount": 0.9 }
```

```json
{ "code": 0, "message": "ok", "data": { "updatedFlights": 3 } }
```

## 7. 用户管理（管理员）

```http
GET    /api/admin/users
POST   /api/admin/users
PUT    /api/admin/users/{username}
PUT    /api/admin/users/{username}/password
DELETE /api/admin/users/{username}
```

新增用户：

```json
{
  "username": "testuser",
  "password": "123456",
  "role": "CUSTOMER",
  "displayName": "测试用户",
  "customerId": "TESTC001"
}
```

约束：

- 客户角色必须关联已存在的客户证件号；
- 不能删除当前登录账号；
- 不能删除/降级最后一个管理员。

## 8. 统计报表（管理员）

```http
GET /api/stats/route?startCity=桂林&endCity=西安&from=2026-11-01&to=2026-11-30
GET /api/stats/monthly
GET /api/stats/summary
```

航线统计：

```json
{
  "flightCount": 3,
  "totalSeats": 368,
  "passengers": 12,
  "occupancyRate": 3.26
}
```

月度统计：

```json
[
  { "month": "2026-09", "soldTickets": 1, "refundedTickets": 0, "netTickets": 1, "netRevenue": 931.0 },
  { "month": "2026-10", "soldTickets": 3, "refundedTickets": 0, "netTickets": 3, "netRevenue": 3648.0 },
  { "month": "2026-11", "soldTickets": 22, "refundedTickets": 1, "netTickets": 21, "netRevenue": 19992.1 }
]
```

汇总卡片：

```json
{
  "flightCount": 10,
  "orderCount": 13,
  "activeOrderCount": 12,
  "passengerCount": 25,
  "revenue": 24571.1
}
```

## 9. 健康检查

```http
GET /api/health
```

```json
{ "status": "ok", "app": "flight-booking-web", "time": "2026-11-24T17:20:25" }
```

## 10. 错误码

| HTTP | 含义 | 示例 |
|---|---|---|
| 400 | 参数/业务校验失败 | 用户名或密码错误、字段不能为空 |
| 401 | 未登录或 Token 过期 | `未登录或登录已过期` |
| 403 | 无权限 | 客户访问管理接口 |
| 404 | 资源不存在 | 航班/订单不存在 |
| 409 | 业务冲突 | 余票不足（带替代航班）、客户/航班存在订单 |
| 500 | 服务器内部错误 | 未知异常 |
