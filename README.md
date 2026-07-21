# 白馬樹海 予約管理システム

基于 Java 17、Spring Boot、React、MyBatis 和 PostgreSQL 的民宿预约管理系统。

## 功能

- 房间管理：登记、停用、恢复房间，管理入住与清扫状态。
- 预约管理：登记、取消预约，防止日期冲突，校验人数并计算预计金额。
- 价格管理：按房间设置季节价格规则，每晚使用最高优先级规则计价。
- 仪表盘：查看房间、空房、有效预约和近期预约。
- 自动退房：默认每小时同步到期预约，可通过 `APP_CHECKOUT_SYNC_CRON` 调整。

## 本地启动

只使用 Docker：

```bash
export DB_PASSWORD=minshuku
docker compose up --build
```

使用本机运行应用：

```bash
docker compose up -d db
export DB_PASSWORD=minshuku
npm ci
npm run build
mvn spring-boot:run
```

访问地址：

- 预约一览：<http://localhost:8000/jukai-internal/dashboard>
- 房间管理：<http://localhost:8000/jukai-internal/rooms>
- 预约管理：<http://localhost:8000/jukai-internal/reservations>
- 价格管理：<http://localhost:8000/jukai-internal/prices>
- 健康检查：<http://localhost:8000/jukai-internal/actuator/health>

如需去掉 `/jukai-internal` 前缀，设置 `APP_CONTEXT_PATH=/`。

## 数据库迁移

应用启动时由 Flyway 自动执行 `src/main/resources/db/migration` 下的版本化迁移。已有数据库首次接入时会建立版本基线；之后不要修改已执行的迁移文件，应新增例如 `V2__add_audit_log.sql`。

测试继续使用 H2 专用的 `src/test/resources/db/schema.sql`，避免 PostgreSQL 扩展语法影响快速测试。预约重叠约束使用 PostgreSQL `btree_gist`，部署账号首次迁移时需要创建 extension 的权限。

## 质量检查

```bash
npm run check
mvn verify
```

`mvn verify` 包含 Java 测试和 Spotless 格式检查。GitHub Actions 会执行相同命令，并额外连接 PostgreSQL 运行数据库集成测试。

## 运维

重要环境变量：

| 变量                     | 默认值                                       | 说明             |
| ------------------------ | -------------------------------------------- | ---------------- |
| `DB_URL`                 | `jdbc:postgresql://localhost:55432/minshuku` | 数据库地址       |
| `DB_USERNAME`            | `minshuku`                                   | 数据库用户       |
| `DB_PASSWORD`            | 无                                           | 必填，数据库密码 |
| `APP_CONTEXT_PATH`       | `/jukai-internal`                            | URL 前缀         |
| `APP_CHECKOUT_SYNC_CRON` | `0 0 * * * *`                                | 自动退房 cron    |
| `APP_TIME_ZONE`          | `Asia/Tokyo`                                 | 定时任务时区     |

备份与恢复：

```bash
DB_PASSWORD=minshuku ./scripts/db-backup.sh ./backups
DB_PASSWORD=minshuku ./scripts/db-restore.sh ./backups/minshuku-YYYYMMDD-HHMMSS.dump
```

恢复会覆盖同名数据库内容，只应在确认目标环境后执行。生产环境应由平台定时运行备份、加密保存，并定期做恢复演练。
