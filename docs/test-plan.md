# 测试计划与结果

- 系统：航空公司订票管理系统（Web 全栈版）
- 测试日期：2026-11-24
- 测试环境：Windows 11 / OpenJDK 17.0.20 / Spring Boot 3.5.16 / Chrome + Playwright
- 测试数据：`src/main/resources/seed/` 示例数据（10 个航班、13 张订单、13 个客户）

## 1. 测试命令

```bat
:: 后端集成测试（独立临时数据目录，不污染 data/）
mvnw.cmd test

:: 启动系统
mvnw.cmd spring-boot:run
```

访问 `http://localhost:8080`。

默认账号：

- 管理员：`admin / admin123`
- 客户：`zhangwei / 123456`

## 2. 后端集成测试（MockMvc）

测试类：`src/test/java/oop/web/ApiIntegrationTest.java`

| 编号 | 测试方法 | 覆盖内容 | 结果 |
|---|---|---|---|
| T01 | `healthIsPublic` | `/api/health` 无需登录 | PASS |
| T02 | `loginAndPermissions` | 管理员/客户登录；未登录 401；客户访问管理接口 403；客户资料 | PASS |
| T03 | `registerCreatesCustomerAndUser` | 注册创建 Customer + User；重复注册 400 | PASS |
| T04 | `queryAndStatistics` | 城市查询；航线统计 3 次/368 座/12 人；月度统计 3 个月 | PASS |
| T05 | `fullFlightReturnsAlternatives` | 满仓 CA1235 订票返回 409 + CA1229/CA1236 | PASS |
| T06 | `bookAndRefundRestoresSeats` | 订票余票减少；退票恢复；状态 REFUNDED | PASS |
| T07 | `customerCannotTouchOthersOrder` | 客户访问他人订单 403 | PASS |
| T08 | `adminCrudConstraints` | 有订单客户/航班删除约束；客户/用户/航班 CRUD；批量折扣 | PASS |

执行结果：

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 3. 浏览器端到端测试（Playwright）

工具：`playwright-cli` + Chromium，视口 1440×1000。

| 编号 | 场景 | 步骤 | 预期 | 结果 |
|---|---|---|---|---|
| E01 | 管理员登录 | 打开 `/` → 登录页 → admin/admin123 | 跳转 `reports.html`，KPI 加载 | PASS |
| E02 | 航班号筛选 | 航班查询 → 输入 CA1235 | 1 条，满仓红色状态 | PASS |
| E03 | 满仓推荐 | 订票页选 CA1235 → 管理员查询客户 → 提交 | 409 弹窗展示 CA1229/CA1236 | PASS |
| E04 | 选择替代航班 | 点击 CA1229 的“选择” | 弹窗关闭，航班摘要切换 | PASS |
| E05 | 客户登录 | 退出 → zhangwei/123456 | 跳转航班查询；菜单仅 3 项 | PASS |
| E06 | 客户订票 | 订票页 CA1229 → 提交确认 | 生成订单并跳转订单页；订单数 +1 | PASS |
| E07 | 客户退票 | 订单页退新订单 → 确认 | 状态变“已退”；CA1229 余票恢复 178/180 | PASS |
| E08 | 角色隔离 | 客户访问 `admin-users.html` | 自动跳转 `flights.html` | PASS |
| E09 | 后台 Modal | 航班/客户/票价/用户页打开新增/编辑 Modal | 标题、回填、角色切换正常 | PASS |
| E10 | 报表数据 | 报表页加载 | 8 次/1268 座/23 人/1.8%；月度 9/10/11 月 | PASS |
| E11 | 响应式 | 1280 宽与 1440 宽浏览 | 侧边栏/表格/图表布局正常 | PASS |

说明：

- E06/E07 会在 `data/` 中产生测试订单；测试前已备份当前数据，测试完成后停止服务、恢复备份并重启，**未保留测试订单**。
- E03 使用满仓航班，不会产生订单。
- 浏览器控制台无 JavaScript 异常；仅有密码输入框 autocomplete 的建议提示（非错误，已补 `autocomplete` 属性）。

## 4. 手工接口冒烟

```bash
# 健康检查
curl http://localhost:8080/api/health

# 管理员登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 未登录访问
curl -i http://localhost:8080/api/flights
# 预期 401

# 客户访问管理接口
curl -i -H "Authorization: Bearer <customer-token>" \
  http://localhost:8080/api/admin/users
# 预期 403
```

中文查询参数请使用 UTF-8 百分号编码，例如：

```text
/api/flights?from=%E6%A1%82%E6%9E%97&to=%E8%A5%BF%E5%AE%89
```

## 5. 测试数据核对

| 项目 | 期望 |
|---|---|
| 航班数 | 10 |
| 订单数 | 13（含 1 条已退） |
| 客户数 | 13 |
| 航线 桂林→西安 2026-11 | 3 个航班 / 368 座 / 12 人 / 3.26% |
| 全部航线 2026-11 | 8 个航班 / 1268 座 / 23 人 / 1.8% |
| 月度净售票 | 2026-09: 1；2026-10: 3；2026-11: 21 |
| 满仓航班 | CA1235（0/8），替代 CA1229 / CA1236 |

## 6. 已知限制与说明

- Token 保存在内存中，服务重启后已登录用户需要重新登录；
- 单进程运行，不支持多实例同时写同一 `data/` 目录；
- 退票为整单退票，不支持部分退票/改签；
- 测试覆盖以 API 集成测试和关键 UI 流程为主，未对所有组合做穷举；
- 首次 Maven 构建需要网络下载依赖。
