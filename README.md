# 白馬樹海 予約管理システム

基于 Java 17、Spring Boot、React、MyBatis、PostgreSQL 的民宿预约管理系统。

## 功能

- 房间管理：登记房间、设置房型、人数、基础价格、入住状态和清扫状态。
- 预约管理：登记住宿预约、校验入住日期冲突、校验人数上限、计算预计金额。
- 价格管理：按房间设置季节价格规则，预约金额按每晚命中的最高优先级规则计算。
- 仪表盘：查看房间数、空房数、有效预约数和近期预约。

## 启动

使用 Docker：

```bash
docker compose up -d db
export DB_PASSWORD=minshuku
mvn spring-boot:run
```

没有 Docker 时使用本机 PostgreSQL：

```bash
chmod +x scripts/db-start-local.sh scripts/db-stop-local.sh
./scripts/db-start-local.sh
export DB_PASSWORD=minshuku
mvn spring-boot:run
```

访问：

- 预约一览：http://localhost:8000/dashboard
- 房间管理：http://localhost:8000/rooms
- 预约管理：http://localhost:8000/reservations
- 价格管理：http://localhost:8000/prices

## 数据库

默认连接：

- URL：`jdbc:postgresql://localhost:55432/minshuku`
- 用户：`minshuku`
- 密码：通过 `DB_PASSWORD` 环境变量注入

数据库结构和初始数据位于：

- `src/main/resources/db/schema.sql`
- `src/main/resources/db/data.sql`

`schema.sql` 包含 PostgreSQL 专用的预约重複防止排他制約：

- 同一房间的 `booked` 预约不能有住宿日期重叠。
- 日期区间按 `[check_in_date, check_out_date)` 处理，因此同一天退房和入住可以衔接。
- 该制约依赖 `btree_gist` extension。正式环境需要用具备建 extension 权限的用户执行 schema/migration。
