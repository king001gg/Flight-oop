# 航空公司订票管理系统 Web 全栈版 — 设计规格

- **日期**：2026-11-24
- **状态**：已确认，待实施
- **范围**：将现有 Java Swing 航班订票系统改造为“HTML/CSS/JavaScript 前端 + Spring Boot 后端”的全栈系统
- **关联课程**：Web 前端课程设计、面向对象/数据结构课程设计
- **回退点**：`v2.0-java-swing`（完整桌面版）

---

## 1. 背景与目标

现有系统是 Java Swing 桌面应用，已经实现航班管理、查询、订票、退票、航线统计和文件持久化。新的 Web 前端课程设计要求：

1. 至少 4 个界面（登录、注册不计入）；
2. 使用 HTML + CSS + JavaScript；
3. 至少 4 类 JavaScript 事件处理；
4. 完成需求分析、总体设计、详细设计、测试。

本设计把两门课设合并为**一个全栈系统**：

- **唯一 UI**：浏览器中的 HTML/CSS/JS 页面；
- **Java 后端**：Spring Boot 提供 REST API 和静态资源；
- **业务复用**：航班、订单、订票/退票、统计、文件持久化逻辑沿用现有 Java 代码；
- **Swing 退役**：桌面界面从主分支移除，完整版本保存在 `v2.0-java-swing` 标签。

目标交付物是一个可启动、可演示、可测试、可写课程报告的系统，而不是两个互不相干的作业。

## 2. 非目标

- 不做真实支付、短信、邮件、第三方航班数据接入；
- 不做生产级高并发、分布式事务、多实例文件锁；
- 不引入 Vue/React/Bootstrap 等前端框架；
- 不保留 Swing 作为并行客户端（仅标签归档）；
- 不做移动 App、小程序、微服务拆分。

## 3. 总体架构

```text
浏览器（HTML + CSS + JavaScript，ES Modules）
        │  fetch /api/*
        ▼
Spring Boot 3.5.16（Java 17）
  ├─ Controller / DTO / Bean Validation / 全局异常
  ├─ AuthInterceptor（Bearer Token + 角色）
  ├─ Service
  │    ├─ FlightSystem（航班 + 订单 + 统计）
  │    ├─ CustomerStore
  │    ├─ UserStore
  │    └─ AuthService
  ├─ Domain：Flight、Order、Customer、User
  ├─ 文件存储（UTF-8，:: 分隔）
  └─ 静态资源：src/main/resources/static
        ▼
data/
  flights.dat  orders.dat  customers.dat  users.dat
```

- 单进程、单端口 `8080`，同时提供页面和 API，同源，无需 CORS。
- 首次启动从 `src/main/resources/seed/` 复制示例数据到 `data/`。
- `data/`、`target/` 不进入 Git；删除 `data/` 即可恢复示例数据。
- 现有 `Flight` / `Order` / `FlightSystem` 迁移到 Maven 目录并复用；Swing 界面类从主分支移除。

## 4. 技术选型

| 项 | 选择 | 说明 |
|---|---|---|
| 语言 | Java 17 | 当前环境 OpenJDK 17.0.20 |
| 后端框架 | Spring Boot 3.5.16 | Java 17 稳定线 |
| 构建 | Maven + Maven Wrapper | 机器未装 Maven，用 `mvnw` 自动下载 |
| Web | Spring MVC + 内嵌 Tomcat | 静态资源 + REST |
| 校验 | spring-boot-starter-validation | DTO 参数校验 |
| 密码 | BCrypt（spring-security-crypto） | 不启用完整 Spring Security 过滤链 |
| 前端 | 原生 HTML/CSS/JS（ES Modules） | 满足“HTML+CSS+JS”要求 |
| 图表 | Canvas 2D 手写 | 不依赖图表库 |
| 测试 | JUnit 5 + Spring Boot Test + MockMvc；Playwright 冒烟 | 后端自动化 + 前端关键流程 |
| 数据 | UTF-8 文本文件，`::` 分隔 | 沿用现有格式，便于课程展示 |

## 5. 项目结构

```text
oop/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/wrapper/
├── data/                              # 运行时数据（忽略）
├── src/main/java/oop/
│   ├── WebApplication.java            # Spring Boot 入口（扫描 oop.*）
│   ├── Flight.java
│   ├── Order.java
│   ├── Customer.java
│   ├── User.java
│   ├── FlightSystem.java              # 航班/订单/统计/持久化
│   ├── CustomerStore.java
│   ├── UserStore.java
│   ├── AuthService.java
│   ├── config/
│   │   ├── DataConfig.java            # 数据目录、启动加载
│   │   ├── DataInitializer.java       # seed → data 首次复制
│   │   └── WebConfig.java             # 拦截器注册
│   ├── dto/                           # 请求/响应 DTO
│   └── web/
│       ├── AuthController.java
│       ├── FlightController.java
│       ├── OrderController.java
│       ├── CustomerController.java
│       ├── AdminCustomerController.java
│       ├── AdminFlightController.java
│       ├── AdminFareController.java
│       ├── AdminUserController.java
│       ├── StatisticsController.java
│       ├── HealthController.java
│       ├── AuthInterceptor.java
│       ├── ApiExceptionHandler.java
│       └── ApiResponse.java
├── src/main/resources/
│   ├── application.properties
│   ├── seed/
│   │   ├── flights.dat
│   │   ├── orders.dat
│   │   ├── customers.dat
│   │   └── users.dat
│   └── static/
│       ├── login.html
│       ├── register.html
│       ├── flights.html
│       ├── booking.html
│       ├── orders.html
│       ├── admin-flights.html
│       ├── admin-customers.html
│       ├── admin-fares.html
│       ├── admin-users.html
│       ├── reports.html
│       ├── css/{base,components,pages}.css
│       ├── js/{api,auth,layout,ui}.js
│       ├── js/pages/*.js
│       └── assets/
├── src/test/java/oop/                 # 单元测试 + MockMvc 集成测试
├── docs/
└── README.md
```

## 6. 数据模型与文件格式

### 6.1 `flights.dat`（沿用，12 字段）

`flightId::date::takeOffTime::durationMinutes::startCity::endCity::planeModel::price::discount::totalSeats::airline::onTimeRate`

- 日期 `yyyy-MM-dd`，时间 `HH:mm`。
- `bookedSeats` 不入库，由有效订单实时汇总。
- 派生字段：到达时刻、实付价、余票、满仓。

### 6.2 `orders.dat`（沿用，9 字段）

`orderId::flightId::customerName::customerId::quantity::orderTime::unitPrice::status::refundTime`

- 状态：`BOOKED` / `REFUNDED`；退票时间为空表示未退。
- 订单号格式：`ORD + yyyyMMddHHmmss + - + 三位序号`。

### 6.3 `customers.dat`（新增，6 字段）

`customerId::name::phone::email::level::createdAt`

- `customerId` 使用证件号，作为客户主键，与订单的证件号关联。
- `level`：普通 / 银卡 / 金卡。
- `createdAt`：`yyyy-MM-dd HH:mm:ss`。

### 6.4 `users.dat`（新增，5 字段）

`username::passwordHash::role::displayName::customerId`

- `passwordHash` 为 BCrypt 哈希；不存明文。
- `role`：`ADMIN` / `CUSTOMER`。
- `customerId`：客户角色关联的证件号；管理员为空。
- 认证 Token 仅存内存（8 小时），服务重启后需重新登录。

### 6.5 示例数据

- 航班：10 条；订单：12 条；客户：由现有订单中的姓名/证件号生成。
- 管理员：`admin / admin123`。
- 演示客户：`zhangwei / 123456`，关联张伟的证件号。
- 满仓演示航班：`CA1235`（8 座，已被旅行社订单订满），用于验证替代航班推荐。

## 7. 业务规则

1. 余票 = 总座位 − 有效（`BOOKED`）订单数量之和；订票/退票后立即重算。
2. 订票：航班存在、客户资料有效、数量为正、余票足够；不足时返回同航线可替代航班。
3. 退票：整单退票，状态置 `REFUNDED` 并记录退票时间；已退订单不可重复退票；座位自动回补。
4. 修改航班：有订单时禁止修改航班号；总座位不得小于已订数；改价/改折扣只影响后续订票。
5. 删除航班：存在任何关联订单时禁止删除。
6. 删除客户：存在关联订单时禁止删除。
7. 删除用户：不能删除自己；不能删除最后一个管理员。
8. 客户只能查看/退自己的订单；管理员可查看全部。
9. 文件保存采用“临时文件 + 替换”，失败时回滚内存状态并返回错误。

## 8. 认证与授权

- 登录：`POST /api/auth/login`，校验 BCrypt 哈希，生成 UUID Token。
- 注册：`POST /api/auth/register`，同时创建 Customer 与 User（角色 `CUSTOMER`）。
- Token 通过 `Authorization: Bearer <token>` 传递；内存 `ConcurrentHashMap` 保存，过期时间 8 小时。
- 公开路径：`/api/auth/login`、`/api/auth/register`、`/api/health`、静态资源。
- `AuthInterceptor` 校验其余 `/api/**`；`/api/admin/**` 额外要求 `ADMIN`。
- 客户访问订单时，服务端按 `customerId` 过滤，不能通过参数越权。
- 退出登录：`POST /api/auth/logout` 删除 Token。

## 9. REST API

统一响应：

```json
{ "code": 0, "message": "ok", "data": {} }
```

错误使用 HTTP 状态码 + `{ "code": 4xx/5xx, "message": "..." }`。

| 方法 | 路径 | 角色 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 公开 | 登录，返回 token + user |
| POST | `/api/auth/register` | 公开 | 注册客户并登录 |
| POST | `/api/auth/logout` | 登录 | 注销 |
| GET | `/api/auth/me` | 登录 | 当前用户 |
| GET | `/api/flights` | 登录 | 按城市/日期/余票筛选 |
| GET | `/api/flights/{id}` | 登录 | 航班详情 |
| GET | `/api/orders` | 登录 | 客户看自己，管理员看全部，支持状态/关键词筛选 |
| GET | `/api/orders/{id}` | 登录 | 订单详情（权限校验） |
| POST | `/api/orders` | 登录 | 订票；客户自动使用本人资料，管理员可指定客户 |
| POST | `/api/orders/{id}/refund` | 登录 | 退票（本人或管理员） |
| GET | `/api/customers/me` | 客户 | 本人资料 |
| PUT | `/api/customers/me` | 客户 | 修改本人资料 |
| GET | `/api/admin/flights` | 管理员 | 航班列表（含筛选） |
| POST | `/api/admin/flights` | 管理员 | 新增航班 |
| PUT | `/api/admin/flights/{id}` | 管理员 | 修改航班 |
| DELETE | `/api/admin/flights/{id}` | 管理员 | 删除航班 |
| PUT | `/api/admin/flights/{id}/fare` | 管理员 | 修改原价/折扣 |
| POST | `/api/admin/fares/bulk` | 管理员 | 按航线批量设置折扣 |
| GET | `/api/admin/customers` | 管理员 | 客户列表/搜索 |
| POST | `/api/admin/customers` | 管理员 | 新增客户 |
| PUT | `/api/admin/customers/{id}` | 管理员 | 修改客户 |
| DELETE | `/api/admin/customers/{id}` | 管理员 | 删除客户 |
| GET | `/api/admin/customers/{id}/orders` | 管理员 | 客户订单 |
| GET | `/api/admin/users` | 管理员 | 用户列表 |
| POST | `/api/admin/users` | 管理员 | 新增用户 |
| PUT | `/api/admin/users/{username}` | 管理员 | 修改角色/资料 |
| PUT | `/api/admin/users/{username}/password` | 管理员 | 重置密码 |
| DELETE | `/api/admin/users/{username}` | 管理员 | 删除用户 |
| GET | `/api/stats/route` | 管理员 | 航线时段统计 |
| GET | `/api/stats/monthly` | 管理员 | 月度售票统计 |
| GET | `/api/stats/summary` | 登录 | 首页/报表汇总卡片 |
| GET | `/api/health` | 公开 | 健康检查 |

DTO 约定：

- 日期 `yyyy-MM-dd`；时间 `HH:mm`；日期时间 `yyyy-MM-dd HH:mm:ss`。
- 金额为数字，保留两位小数；折扣为 `0~1`。
- 航班 DTO 同时返回派生字段：`arrivalTime`、`actualPrice`、`remainingSeats`、`full`、`route`。
- 订单 DTO 返回 `statusText`、`amount`、`customerName` 等展示字段。

## 10. 前端信息架构与页面

| # | 页面 | 角色 | 关键功能 |
|---|---|---|---|
| 1 | `login.html` | 公开 | 登录、角色跳转 |
| 2 | `register.html` | 公开 | 注册（创建 User + Customer） |
| 3 | `flights.html` | 登录 | 城市/日期/余票筛选、详情弹窗、进入订票 |
| 4 | `booking.html` | 登录 | 航班确认、乘客信息、数量、金额、提交 |
| 5 | `orders.html` | 登录 | 订单列表、筛选、详情、退票；管理员看全部 |
| 6 | `admin-flights.html` | 管理员 | 航班 CRUD、搜索、删除确认 |
| 7 | `admin-customers.html` | 管理员 | 客户 CRUD、查看订单 |
| 8 | `admin-fares.html` | 管理员 | 按航线改价/折扣、批量折扣、实付价预览 |
| 9 | `reports.html` | 管理员 | 航线时段统计、月度售票、Canvas 图表、打印 |
| 10 | `admin-users.html` | 管理员 | 用户 CRUD、角色、重置密码 |

非登录/注册页面共 8 个，满足“至少 4 个”。

### 10.1 共享前端结构

- `layout.js`：注入侧边栏/顶栏，按角色渲染菜单，处理退出登录。
- `api.js`：fetch 封装、Token 注入、401 跳转、统一错误 Toast。
- `auth.js`：登录/注册/会话恢复、角色判断。
- `ui.js`：Toast、Modal、确认框、分页、格式化、加载/空状态。
- `css/base.css`：设计变量、reset、字体、布局。
- `css/components.css`：按钮、卡片、表格、徽章、表单、弹窗、分页。
- `css/pages.css`：页面级布局与响应式。
- 颜色：航空蓝 + 白色 + 浅灰背景；状态色：成功绿、警告橙、危险红。
- 响应式：≥1200 三栏/侧边栏，768~1199 收窄，<768 侧边栏抽屉。

### 10.2 JavaScript 事件

至少在以下 10 类事件上实现处理：

1. `click`：导航、查询、订票、退票、分页、Modal、打印；
2. `submit`：登录、注册、订票、后台表单（`preventDefault` + 校验）；
3. `change`：城市/日期/状态筛选、角色选择、每页条数；
4. `input`：实时搜索、表单校验、数量/票价联动；
5. `keydown`：回车查询、Esc 关闭 Modal；
6. `dblclick`：后台表格行进入编辑；
7. `mouseover` / `mouseout`：行高亮、余票提示；
8. `DOMContentLoaded`：初始化页面、加载数据、渲染角色菜单；
9. `scroll`：顶栏吸附、返回顶部；
10. `resize`：Canvas 图表自适应重绘。

### 10.3 报表与打印

- 航线时段统计：飞行次数、座位数、载客人数、客座率。
- 月度统计：售票、退票、净售票、净销售额。
- Canvas 绘制月度售票柱状图。
- `window.print()` + `@media print` 隐藏导航/按钮，保留报表标题与表格。

## 11. 错误处理

- 前端：`api.js` 统一解析错误；400 显示字段/业务提示，401 清除 Token 并跳转登录，403 提示无权限，5xx 提示系统错误。
- 后端：`ApiExceptionHandler` 将业务异常映射为 400/401/403/404/409；未知异常记录日志并返回 500。
- 校验：DTO 使用 `@NotBlank`、`@Min`、`@DecimalMin` 等；错误信息中文。
- 文件错误：保存失败回滚内存状态并返回 500/业务提示。

## 12. 并发与持久化

- 单进程运行；`FlightSystem`、`CustomerStore`、`UserStore` 的读写方法加 `synchronized` 或统一锁，保证索引和文件一致。
- 写文件采用临时文件 + 原子替换；失败回滚。
- 不保证多进程同时写同一 `data/` 目录；在 README 中说明。
- `data/` 首次启动由 `DataInitializer` 从 classpath `seed/` 复制。

## 13. 测试策略

### 13.1 后端

- 存储层单元测试：航班/订单/客户/用户的加载、保存、约束、统计。
- API 集成测试（MockMvc，临时数据目录）：
  - 登录成功/失败、注册重复、Token 校验；
  - 客户不能访问管理员接口；
  - 航班筛选、订票、满仓替代、退票回补；
  - 客户 CRUD、用户 CRUD、删除约束；
  - 统计接口结果；
  - 非法参数返回 400。
- `./mvnw test` 全部通过。

### 13.2 前端

- Playwright 冒烟：登录 → 航班查询 → 订票 → 订单 → 退票 → 报表打印预览。
- 手工测试用例表：登录/注册、权限、筛选、表单校验、分页、Modal、打印、响应式。
- 截图保存到 `docs/screenshots/web/`。

### 13.3 验收标准

- 8 个非登录/注册页面可访问且功能可用；
- 至少 10 类 JS 事件有实际处理；
- 客户/管理员权限隔离有效；
- 前后端数据一致，重启后数据保留；
- 满仓航班能推荐替代航班；
- 报表可统计、可打印；
- README、设计文档、API 文档、测试用例齐全；
- `./mvnw test` 通过，应用可一条命令启动。

## 14. 迁移与回退

- 桌面版完整保留在标签 `v2.0-java-swing`。
- 在分支 `feature/web-fullstack` 上开发，完成并验证后合并到 `main`。
- 合并后打标签 `v3.0-web-fullstack`。
- 主分支删除 Swing 界面类（`FlightSystemGUI`、各 `*Panel`、`UiUtils`、Swing 版 `Main`）。
- `flights.dat`/`orders.dat` 移入 `src/main/resources/seed/`，运行时复制到 `data/`。
- 旧数据文件格式保持兼容；新增 `customers.dat`、`users.dat`。

## 15. 实施阶段

| 阶段 | 内容 | 验收 |
|---|---|---|
| 0 | 分支、Maven/Spring Boot 骨架、mvnw、目录迁移、健康检查 | `./mvnw spring-boot:run`，`/api/health` 返回 ok |
| 1 | 领域模型、存储、示例数据、客户/用户/认证 | 存储与认证单测通过 |
| 2 | 航班/订单/客户/用户/统计 API + 权限 + 异常 | MockMvc 集成测试通过 |
| 3 | 前端基础样式、布局、登录注册、客户侧三页 | 浏览器可登录、查询、订票、退票 |
| 4 | 后台四页 + 报表页 + 图表/打印 | 管理员全流程可用 |
| 5 | Playwright/手工测试、截图、文档、合并与标签 | 验收标准全部满足 |

## 16. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 机器未装 Maven、首次依赖下载慢 | 提交 Maven Wrapper；首次构建预留时间；使用 Spring Boot 3.5.16 稳定版 |
| Spring Boot 多线程访问文件存储 | 存储层加锁；单进程运行；README 说明不支持多实例 |
| 自定义 Token 认证的安全性 | BCrypt + 随机 Token + 8 小时过期；明确仅课程演示，不上生产 |
| Swing 代码删除后无法回退 | `v2.0-java-swing` 标签完整保留 |
| 前端页面多、工作量大 | 共享布局/组件/工具；按页面模块拆分；分阶段验收 |
| 课程要求“纯前端” | 前端仍然只使用 HTML/CSS/JS；Java 后端是同一系统的服务端，不引入前端框架 |

## 17. 交付物

- 可运行的全栈系统（Spring Boot + 静态前端）；
- 示例数据与首次启动初始化；
- 后端自动化测试、前端冒烟测试与手工测试用例；
- `README.md`：架构、启动、账号、页面、API 摘要、测试；
- 本设计规格；
- `docs/web-course-design.md`：需求分析、总体设计、详细设计、测试——可直接作为 Web 课设报告主体；
- `docs/api.md`：接口文档；
- `docs/test-plan.md`：测试用例与结果；
- 页面截图；
- Git 标签：`v2.0-java-swing`、`v3.0-web-fullstack`。
