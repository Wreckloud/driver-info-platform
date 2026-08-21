# 上线检查单

## 发布前

- [ ] 将项目纳入 Git 或其他版本控制，并为本次源码创建不可变版本标签。
- [ ] 在 `.env` 中设置本次唯一的 `APP_VERSION`，例如 `1.2.0`，后续发布不得覆盖旧标签。
- [ ] `VIEWER_USERNAME` 和 `VIEWER_PASSWORD_BCRYPT` 已配置，只读账号密码未以明文写入文件。
- [ ] 域名 DNS 已指向服务器，腾讯位置服务 Key 已启用逆地址解析并确认配额。
- [ ] 宿主机 NTP 同步正常，只开放 80/443 和受限来源的运维 SSH 端口。
- [ ] 数据库用户密码、root 密码和管理员 BCrypt 均为独立的生产值。
- [ ] `.env` 与 TLS 私钥权限为 `600`，证书链文件和私钥名称正确。
- [ ] 执行 `./deploy/scripts/preflight.sh` 并确认通过。
- [ ] 执行 `cd backend && mvn test`、`cd ../frontend && npm test && npm run build`。

## 构建与启动

```sh
docker compose --env-file .env config
docker compose --env-file .env build --pull
docker compose --env-file .env up -d
docker compose --env-file .env ps
docker compose --env-file .env exec web nginx -t
docker compose --env-file .env logs --tail=200 api web mysql
```

- [ ] MySQL 健康检查通过，API 与 Web 容器没有反复重启。
- [ ] API 日志显示 Flyway 迁移成功，不包含数据库、管理员或腾讯地图凭据。
- [ ] `https://域名/driver`、`https://域名/admin/login` 和前端深层路由均能打开。
- [ ] HTTP 自动跳转 HTTPS，生产环境接口文档页面不可访问。

## 业务验收

- [ ] Android 微信和 iPhone 微信各完成一次定位成功登记。
- [ ] 手机端四个常用信息字段按两列两行显示，定位与照片区域共用一个卡片且没有横向溢出。
- [ ] 至少完成一次拒绝定位或定位失败登记，两者均能正常提交。
- [ ] 成功页优先显示文字地址，不向司机展示经纬度和精度。
- [ ] 项目、数量和可选备注可正常提交、回显、修改、搜索及导出，“发车时间”由服务器提交时间生成。
- [ ] 自动定位会在进入页面时触发，失败后点击定位卡片可重试；每次点击拍摄 1 张照片，1–9 张照片可压缩、预览、提交并在后台放大查看。
- [ ] 只读账号只能查看与导出，修改和删除接口返回 403。
- [ ] 管理员完成登录、查询、日期筛选、编辑、软删除和当前筛选结果导出。
- [ ] 管理列表的相对时间持续更新，完整时间和上海时区日期正确。
- [ ] 连续点击或重复请求同一个 `submissionToken` 只产生一条记录。

## 备份与回滚

- [ ] 上线后立即执行 `./deploy/scripts/backup.sh`，并验证压缩包可通过 `gzip -t`。
- [ ] 数据库与照片备份均已复制到异机或对象存储，并完成一次配套恢复演练。
- [ ] 保留本次和上一个 `APP_VERSION` 对应的源码标签与 Docker 镜像。
- [ ] 回滚应用前先备份当前数据库；不得在未评估 Flyway 迁移兼容性的情况下回退数据库结构。
