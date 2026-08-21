# 司机出车登记管理系统

面向司机扫码登记和管理员集中管理的轻量系统。司机进入页面后自动尝试定位，每次出车提交项目、姓名、电话、车牌、车型、数量、目的地、可选备注以及 1–9 张自动压缩照片；管理员登录后可查询、查看照片、修改、软删除和导出记录。发车时间由服务器在提交成功时生成，并支持只能查看和导出的只读账号。

## 项目结构

```text
backend/    Java 17 + Spring Boot + MyBatis + MySQL
frontend/   Vue 3 + Vite + Element Plus
deploy/     Nginx、HTTPS、备份恢复和密码工具
docs/       接口与验收文档
```

## 本地开发

### Windows 一键启动（推荐）

本机已安装 Java 17、Maven、Node.js 和 MySQL 8 时，可在项目根目录运行：

```powershell
.\scripts\start-local.ps1
```

脚本会在 `runtime/` 中初始化一套仅绑定 `127.0.0.1:3307` 的隔离 MySQL 数据目录，不会启动或修改系统 MySQL 服务。首次运行会提示设置本地管理员密码，后续复用本地 BCrypt 摘要。后端、前端和数据库日志均保存在 `runtime/logs/`；该目录已被 Git 忽略。

启动后访问：

- 司机端：`http://127.0.0.1:5173/driver`
- 管理员端：`http://127.0.0.1:5173/admin/login`
- 接口文档：`http://127.0.0.1:8080/doc.html`

执行真实 MySQL、登录、CSRF、增改查、软删除和 Excel 导出冒烟测试：

```powershell
.\scripts\test-local.ps1 -AdminPassword '首次启动时设置的密码'
```

停止这套本地服务：

```powershell
.\scripts\stop-local.ps1
```

以下步骤适用于需要复用已有 MySQL 服务的手动启动方式。

### 1. 准备 MySQL

创建数据库：

```sql
CREATE DATABASE driver_info CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

默认开发用户名为 `root`，不提供默认数据库密码。启动前请设置 `DB_PASSWORD`；如本机配置不同，同时设置 `DB_URL`、`DB_USERNAME`。Flyway 会在后端启动时自动建表。

项目不提供默认密码。先运行 `deploy/scripts/generate-password.ps1`，分别生成管理员和只读账号的 BCrypt 摘要，再设置 `ADMIN_PASSWORD_BCRYPT` 与 `VIEWER_PASSWORD_BCRYPT`。管理员默认用户名为 `admin`；只读账号通过 `VIEWER_USERNAME` 配置。

### 2. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

接口文档地址为 `http://localhost:8080/doc.html`。

### 3. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

- 司机端：`http://localhost:5173/driver`
- 管理员端：`http://localhost:5173/admin/login`

浏览器允许在 `localhost` 使用定位；局域网 IP 通常需要 HTTPS 才能获取位置。

## 生产部署

完整步骤见 [部署与运维说明](docs/deployment.md)，逐项操作见 [上线检查单](docs/release-checklist.md)。生产环境必须配置 HTTPS、腾讯位置服务 Key、随机数据库密码和 BCrypt 管理员密码，禁止使用本地默认凭据。

`wreckloud.com` 现有服务器请使用 [低内存共存部署说明](docs/wreckloud-server-deployment.md)。该方案复用现有 MySQL、使用 `driver.wreckloud.com` 子域名，并且不会占用主站已有的 80/443/8080 端口。首次上线后可按 [GitHub 私有仓库部署说明](docs/github-deployment.md) 切换为只读 Deploy Key 拉取和更新。

## 验证命令

```powershell
cd backend
mvn test

cd ..\frontend
npm test
npm run build
```

详细接口见 [API 说明](docs/api.md)，真机验收项见 [验收清单](docs/acceptance.md)。
