# 白馬樹海 予約管理システム

基于 Java 17、Spring Boot、React、MyBatis 和 PostgreSQL 的民宿预约管理系统。

## 功能

- 房间管理：登记、停用、恢复房间，管理入住与清扫状态。
- 预约管理：登记、取消、办理入住和退房，按住宿日期防止冲突，校验人数并计算应收金额。
- 房态判定：未来预约不会覆盖当前房态；当日预约额外要求客房空闲且已清扫。
- 价格管理：按房间设置季节价格规则，每晚使用最高优先级规则计价。
- 客户档案：只保存姓名、电话、邮箱和历史住宿记录。
- 收退款：每笔订单保存一条收款和一条退款，支持现金、银行卡、转账、平台收款与部分退款。
- 营业报表：按日期范围导出预约列表、住宿记录、收退款明细、月度汇总和支付方式汇总 Excel。
- 管理员登录：首次启动创建唯一的本地管理员，密码使用 BCrypt 保存，连续失败会临时锁定。
- 日中双语：默认日语，可切换中文，并在当前浏览器中记住上次选择。
- 本机备份：每天 02:00 自动保存 PostgreSQL 自定义格式备份，也可在营业与收款页面立即备份。
- 仪表盘：查看房间、空房、有效预约和近期预约。
- 自动退房：默认每小时同步到期预约，可通过 `APP_CHECKOUT_SYNC_CRON` 调整。

界面支持电脑、平板和手机宽度；本地版只监听 `127.0.0.1`，不会直接暴露到局域网。

## macOS 本地启动

1. 安装并启动 Docker Desktop。
2. 双击 `scripts/start-macos.command`，或在终端运行：

```bash
./scripts/start-local.sh
```

首次运行会从 `.env.example` 创建 `.env`。建议先把 `DB_PASSWORD` 改成长随机密码，并按需要指定本机备份目录：

```dotenv
DB_PASSWORD=your-long-random-password
BACKUP_DIRECTORY=/Users/your-name/Documents/minshuku-backups
```

## Windows 本地启动

安装并启动 Docker Desktop 后，在 PowerShell 中运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local.ps1
```

Windows 的 `.env` 可使用正斜杠路径，例如：

```dotenv
BACKUP_DIRECTORY=C:/Users/your-name/Documents/minshuku-backups
```

停止系统不会删除数据库和备份：

```bash
./scripts/stop-local.sh
```

```powershell
.\scripts\stop-local.ps1
```

## 开发模式

全容器运行：

```bash
cp .env.example .env
docker compose up -d --build
```

使用本机运行应用：

```bash
docker compose up -d db
export DB_PASSWORD=minshuku
npm ci
npm run build
mvn spring-boot:run
```

Docker Compose 默认仅向本机 `127.0.0.1` 暴露应用和数据库端口。

访问地址：

- 预约一览：<http://localhost:8000/jukai-internal/dashboard>
- 房间管理：<http://localhost:8000/jukai-internal/rooms>
- 预约管理：<http://localhost:8000/jukai-internal/reservations>
- 价格管理：<http://localhost:8000/jukai-internal/prices>
- 客户管理：<http://localhost:8000/jukai-internal/customers>
- 营业、收退款和备份：<http://localhost:8000/jukai-internal/finance>
- 健康检查：<http://localhost:8000/jukai-internal/actuator/health>

如需去掉 `/jukai-internal` 前缀，设置 `APP_CONTEXT_PATH=/`。

## 演示数据

系统运行后可一次写入 30 间客房、30 条价格规则、30 名顾客、30 笔住宿订单、30 名同行者和 30 条财务记录。数据包含已退房、已取消、未来预订、未付款、已付款、部分退款及全额退款等状态，并使用固定编号保证重复执行不会重复插入。

macOS、Linux：

```bash
./scripts/seed-demo-data.sh
```

Windows PowerShell：

```powershell
.\scripts\seed-demo-data.ps1
```

为保持“单管理员”和真实备份记录的业务含义，脚本不会生成额外管理员、伪造备份历史或修改 Flyway 系统表。执行数据填充前建议先运行手动备份脚本。

## 数据库迁移

应用启动时由 Flyway 自动执行 `src/main/resources/db/migration` 下的版本化迁移。V2 会保留现有客房、预约和价格数据，并为旧预约建立客户及收款兼容记录。已有数据库首次接入时会建立版本基线；之后不要修改已执行的迁移文件，应继续新增迁移文件。

升级已有环境前先运行手动备份：

```bash
./scripts/backup-now.sh
```

```powershell
.\scripts\backup-now.ps1
```

测试继续使用 H2 专用的 `src/test/resources/db/schema.sql`，避免 PostgreSQL 扩展语法影响快速测试。预约重叠约束使用 PostgreSQL `btree_gist`，部署账号首次迁移时需要创建 extension 的权限。

## 质量检查

```bash
npm run check
mvn verify
```

`mvn verify` 包含 Java 测试和 Spotless 格式检查。GitHub Actions 会执行相同命令，并额外连接 PostgreSQL 运行数据库集成测试。

## 运维

重要环境变量：

| 变量                     | 默认值                                       | 说明                       |
| ------------------------ | -------------------------------------------- | -------------------------- |
| `DB_URL`                 | `jdbc:postgresql://localhost:55432/minshuku` | 数据库地址                 |
| `DB_USERNAME`            | `minshuku`                                   | 数据库用户                 |
| `DB_PASSWORD`            | 无                                           | 必填，数据库密码           |
| `APP_CONTEXT_PATH`       | `/jukai-internal`                            | URL 前缀                   |
| `APP_CHECKOUT_SYNC_CRON` | `0 0 * * * *`                                | 到期订单同步 cron          |
| `APP_TIME_ZONE`          | `Asia/Tokyo`                                 | 定时任务时区               |
| `BACKUP_ENABLED`         | `true`                                       | 是否启用每日自动备份       |
| `BACKUP_CRON`            | `0 0 2 * * *`                                | 每日备份 Spring 六段 cron  |
| `BACKUP_DIRECTORY`       | `./backups`                                  | 宿主机上的指定备份文件夹   |
| `PG_DUMP_COMMAND`        | `pg_dump`                                    | 非 Docker 运行时的命令路径 |

备份与恢复：

```bash
./scripts/backup-now.sh
./scripts/restore.sh /absolute/path/to/minshuku-YYYYMMDD-HHMMSS.dump

.\scripts\backup-now.ps1
.\scripts\restore.ps1 -BackupFile C:\path\to\minshuku-YYYYMMDD-HHMMSS.dump
```

系统不会自动删除旧备份，因此可保留三年以上；请自行确认磁盘容量。恢复脚本会要求输入 `RESTORE`，并覆盖当前数据库，只应在确认目标环境后执行。

## 自有服务器部署

准备 Linux 服务器、域名及指向服务器的 DNS 记录，在 `.env` 设置 `DB_PASSWORD`、`APP_DOMAIN` 和服务器备份目录后运行：

```bash
docker compose -f docker-compose.server.yml up -d --build
```

服务器配置使用 Caddy 自动申请 HTTPS 证书，数据库不映射到公网，应用仍保持单管理员模式。迁移本机数据时，先用本地脚本导出 `.dump`，复制到服务器后用恢复脚本导入。正式上线前还应配置防火墙、系统更新和异地加密备份。
