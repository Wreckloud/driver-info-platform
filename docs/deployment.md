# 部署与运维说明

## 前置条件

- 一台安装了 Docker Engine 和 Docker Compose 的 Linux 云服务器。
- 已解析到服务器的正式域名。
- 域名对应的 HTTPS 完整证书链和私钥。
- 已启用 WebService API 逆地址解析的腾讯位置服务 Key。
- 宿主机已启用 NTP 时间同步（如 `systemd-timesyncd` 或 `chrony`），因为登记提交时间以服务器 UTC 时钟为准。

## 首次部署

1. 复制配置模板：

   ```sh
   cp .env.example .env
   ```

2. 生成管理员 BCrypt 密码：

   ```sh
   chmod +x deploy/scripts/*.sh
   ./deploy/scripts/generate-password.sh
   ```

   脚本只依赖项目已使用的 Java 17 和 Maven。将输出放入 `.env` 的 `ADMIN_PASSWORD_BCRYPT`。由于摘要包含 `$`，建议在 `.env` 中使用单引号包裹完整值。

3. 为 MySQL 用户和 root 分别生成不同的长随机密码，填写 `.env`；再填写正式域名、公开地址和腾讯地图 Key。

   ```sh
   chmod 600 .env
   ```

4. 将证书保存为：

   ```text
   deploy/certs/fullchain.pem
   deploy/certs/privkey.pem
   ```

   私钥文件建议设置为仅部署用户可读：`chmod 600 deploy/certs/privkey.pem`。

5. 执行上线前预检。脚本会检查 Docker Compose、占位配置、证书有效性和私钥格式，但不会启动服务：

   ```sh
   chmod +x deploy/scripts/*.sh
   ./deploy/scripts/preflight.sh
   ```

6. 构建并启动：

   ```sh
   docker compose config
   docker compose build
   docker compose up -d
   docker compose ps
   ```

7. 访问 `https://域名/driver` 和 `https://域名/admin/login`，完成验收清单。

后端和 MySQL 不映射公网端口，外部流量仅从 Nginx 的 80/443 进入。HTTP 自动跳转 HTTPS。
生产 Compose 固定启用安全会话 Cookie，并关闭 Swagger、Springdoc 与 Knife4j 的公网接口文档页面；本地开发不受影响。
服务器防火墙或云安全组只应开放业务所需的 80/443，以及限制来源后的运维 SSH 端口。

## 固定二维码

在可以运行 Node.js 的机器上执行：

```sh
cd frontend
PUBLIC_BASE_URL=https://example.com npm run generate:qr
```

Windows PowerShell：

```powershell
$env:PUBLIC_BASE_URL='https://example.com'
npm run generate:qr
```

输出位于 `frontend/dist-qr/driver-qr.png` 和 `driver-qr.svg`。确认域名和 `/driver` 地址长期不变后再印刷。

## 备份与恢复

手动备份：

```sh
./deploy/scripts/backup.sh
```

脚本会在 `deploy/backups/` 同时保存 gzip 压缩的 SQL 与照片归档，并删除超过 30 天的同类备份。生产服务器通过 cron 每日执行一次，例如：

```cron
20 2 * * * /opt/DriverInfoPlatform/deploy/scripts/backup.sh >> /var/log/driver-info-backup.log 2>&1
```

本地备份无法应对整机或磁盘损坏，应再将备份同步到受控的异机或对象存储，并设置独立访问权限与生命周期。

恢复会覆盖数据库中的同名数据，执行前先停止业务写入并额外备份当前库：

```sh
docker compose stop web api
./deploy/scripts/restore.sh /absolute/path/to/driver_info_20260727_022000.sql.gz /absolute/path/to/driver_photos_20260727_022000.tar.gz
docker compose start api web
```

## 日常维护

- 更新代码后执行 `docker compose build --pull && docker compose up -d`。
- 证书续期后执行 `docker compose exec web nginx -s reload`。
- 修改管理员密码后更新 `.env` 中的 BCrypt 摘要，再执行 `docker compose up -d --force-recreate api`。
- 腾讯地址解析失败不会阻止司机登记；出现大量空地址时检查 API Key、接口权限、配额和后端日志。
- 定期检查宿主机 NTP 同步状态；容器沿用宿主机时钟，无需在应用或数据库中手动改时区时间。
- 恢复误删记录时，由运维在数据库中将对应行的 `deleted` 改为 `0`，并清空 `deleted_at/deleted_by`；V1 不提供回收站页面。
