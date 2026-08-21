# wreckloud.com 服务器部署说明

本文适用于当前服务器环境：Ubuntu 24.04、宿主机 Nginx、已有 `lycan-mysql` 容器，以及 `wreckloud.com` 正在运行的 LycanClaw 服务。

## 部署结构

- 司机系统使用 `driver.wreckloud.com`，不占用主站已有的 `/api` 和 `/admin`。
- 宿主机 Nginx 继续独占 80/443，并反向代理到 `127.0.0.1:18080`。
- 新增的 Web 容器限制为 64MB，API 容器限制为 512MB。
- 复用 `lycan-mysql` 进程，但使用独立的 `driver_info` 数据库和 `driver_app` 账号。
- 新项目 Compose 名为 `driver_info_platform`，不会管理或重启 LycanClaw 容器。

## 1. 配置 DNS

在 `wreckloud.com` 的 DNS 服务商中新增：

```text
类型：A
主机记录：driver
记录值：当前服务器公网 IP
```

等待下面的命令返回服务器公网 IP 后再申请证书：

```sh
getent ahostsv4 driver.wreckloud.com
```

## 2. 本地构建发布包

在 Windows PowerShell 中运行：

```powershell
cd D:\Portfolio\project\DriverInfoPlatform
.\deploy\server\package-server.ps1
```

脚本会运行后端和前端测试、构建 JAR 与前端静态文件、生成正式二维码，然后输出：

```text
release\driver-info-platform-1.1.0.tar.gz
```

压缩包包含 `.env`，不得公开上传或提交到 Git。

## 3. 上传并启动容器

将压缩包上传到服务器的 `/home/wreckloud/`，然后在服务器执行：

```sh
mkdir -p /home/wreckloud/apps
tar -xzf /home/wreckloud/driver-info-platform-1.1.0.tar.gz -C /home/wreckloud/apps
cd /home/wreckloud/apps/driver-info-platform-1.1.0
chmod +x deploy/server/*.sh
./deploy/server/deploy.sh
```

部署脚本会：

1. 检查配置、构建产物、Docker 网络和现有 MySQL；
2. 在现有 MySQL 中创建或更新独立数据库账号；
3. 构建两个轻量运行时镜像；
4. 启动新项目容器；
5. 检查司机页面和 API。

它不会修改 `/opt/lycanclaw`，也不会重启 `lycan-*` 容器。

如果管理员密码需要修改，使用交互式脚本。脚本会隐藏输入、生成 BCrypt 摘要并重建新系统容器：

```sh
./deploy/server/reset-admin-password.sh
```

## 4. 接入宿主机 Nginx

先安装 HTTP 站点配置。该脚本会执行 `nginx -t`，只有检查通过才会重新加载 Nginx：

```sh
sudo /home/wreckloud/apps/driver-info-platform-1.1.0/deploy/server/install-nginx-site.sh
```

确认 `http://driver.wreckloud.com/driver` 能到达页面后，使用现有 Certbot 签发独立证书并自动启用 HTTPS：

```sh
sudo certbot --nginx --cert-name driver.wreckloud.com -d driver.wreckloud.com --redirect
```

执行前必须确认 DNS 已生效。不要扩展或替换主站 `wreckloud.com` 的现有证书。

## 5. 验收

```text
https://driver.wreckloud.com/driver
https://driver.wreckloud.com/admin/login
```

二维码文件位于发布目录：

```text
artifacts/qrcode/driver-qr.png
artifacts/qrcode/driver-qr.svg
```

## 6. 备份

手动备份：

```sh
./deploy/server/backup.sh
```

备份保存在发布目录的 `backups/`，默认删除超过 30 天的历史文件。恢复属于覆盖性操作，应先停止 API 并再次备份当前数据库，再运行：

```sh
./deploy/server/restore.sh /absolute/path/to/driver_info_backup.sql.gz
```

## 注意事项

- `.env` 中的 `MYSQL_ROOT_PASSWORD` 是独立 MySQL 部署使用的配置；共存版不会读取它，也不需要改成现有 MySQL 的 root 密码。
- 共存版通过现有容器内部的 root 环境变量初始化数据库，不会输出 root 密码。
- 新 API 出现内存不足时会退出并由 Docker 重启，不会无限制占用服务器内存。
- 现有 MySQL 停止时两个项目都会暂时不可用，但两个数据库及业务账号相互独立。
