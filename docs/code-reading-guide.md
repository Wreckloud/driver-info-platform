# 代码阅读与维护指南

这份指南用于把项目从“能部署的成品”逐步变成自己能够解释、修改和排查的系统。阅读时不要从每个文件逐行开始，先沿一条真实业务请求理解各层职责。

## 一、先建立整体地图

```text
司机或管理员页面
  -> frontend/src/views        页面与交互
  -> frontend/src/api          HTTP 请求
  -> backend/controller        接口接收与校验
  -> backend/service           业务规则与事务
  -> backend/mapper + XML      数据库访问
  -> db/migration              表结构演进
```

项目刻意保持前后端分离，但生产环境通过同一域名访问。Nginx 返回前端页面，并将 `/api` 转发给 Spring Boot。

## 二、推荐阅读顺序

### 1. 从司机提交开始

按下面顺序阅读，先看方法名和数据流，不必立即理解每一行：

1. `frontend/src/router/index.js`：页面地址如何对应 Vue 页面。
2. `frontend/src/views/driver/DriverFormView.vue`：表单、自动定位、拍照、确认和提交。
3. `frontend/src/utils/driver.js`：提交令牌、定位错误和本机常用信息。
4. `frontend/src/utils/photo.js`：浏览器端照片压缩。
5. `frontend/src/api/driver.js`：页面实际调用的司机接口。
6. `backend/.../controller/DriverRecordController.java`：后端如何接收登记和照片。
7. `backend/.../dto/DriverRecordCreateRequest.java`：后端最终信任的字段与校验规则。
8. `backend/.../service/impl/DriverRecordServiceImpl.java` 的 `create`：服务器时间、重复提交保护、地址解析、照片保存和数据库事务。
9. `backend/.../service/impl/DriverRecordPhotoServiceImpl.java`：照片格式验证、文件落盘和失败回滚。
10. `backend/src/main/resources/mapper/DriverRecordMapper.xml`：SQL 查询和软删除条件。
11. `backend/src/main/resources/db/migration/`：数据库如何从 V1 演进到当前结构。

读完这条链路后，应当能够回答：为什么连续点击只产生一条记录、为什么发车时间不能由手机提供、定位失败为什么仍能提交、照片为什么不直接存在 MySQL。

### 2. 再看登记成功页

1. `frontend/src/views/driver/DriverSuccessView.vue`：成功摘要、服务器发车时间和照片预览。
2. `backend/.../vo/DriverRecordSummaryVO.java`：创建接口返回给成功页的数据。
3. `backend/.../controller/DriverRecordPhotoController.java`：司机如何凭本次提交令牌读取照片。

这里重点理解：照片接口不是公开静态目录，照片 ID 必须与随机 `submissionToken` 属于同一条登记。

### 3. 再看管理员系统

1. `frontend/src/stores/auth.js`：前端保存的登录状态。
2. `frontend/src/views/admin/AdminLoginView.vue`：登录入口。
3. `frontend/src/views/admin/AdminRecordsView.vue`：列表、搜索、日期筛选、删除和导出。
4. `frontend/src/views/admin/AdminRecordDetailView.vue`：详情、照片和可编辑字段。
5. `frontend/src/api/admin.js`：管理员接口与 CSRF Token 获取方式。
6. `backend/.../controller/AdminAuthController.java`：登录、退出和当前账号。
7. `backend/.../controller/AdminRecordController.java`：查询、修改、软删除和 Excel 导出。
8. `backend/.../config/SecurityConfig.java`：ADMIN 与 VIEWER 的权限边界。
9. `backend/.../security/LoginAttemptService.java`：登录失败限流。

读完后，应当能够解释：为什么只读账号看不到编辑入口、为什么构造写请求仍会返回 403、为什么 Cookie 登录还需要 CSRF Token。

### 4. 最后看部署

1. `docker-compose.server.yml`：生产容器、内存限制、网络和照片卷。
2. `deploy/server/Dockerfile.api` 与 `Dockerfile.web`：成品如何进入镜像。
3. `deploy/server/deploy.sh`：服务器更新顺序和健康检查。
4. `deploy/server/nginx.conf`：前端路由和 `/api` 代理。
5. `deploy/server/backup.sh` 与 `restore.sh`：数据库和照片备份恢复。
6. `docs/wreckloud-server-deployment.md`：当前服务器的实际部署约定。

## 三、必须记住的业务规则

- `createdAt` 是服务器生成的发车时间，客户端不能提交或修改。
- `submissionToken` 是一次登记的幂等标识，相同令牌重试返回原记录。
- 定位失败、拒绝或超时都允许提交，但不能携带旧坐标。
- 删除是软删除，正常列表、详情和导出始终排除 `deleted=1`。
- 管理员可修改司机填写字段，不能修改定位和发车时间。
- ADMIN 可以修改和删除；VIEWER 只能查看和导出。
- 照片保存在 Docker 持久卷，数据库只保存元数据和文件名。
- 数据库迁移只能新增 Flyway 版本文件，不能修改已经在线执行过的迁移。

## 四、适合练手的修改

建议按风险从低到高进行：

1. 修改一处页面说明文字，并运行前端测试和构建。
2. 调整一个只影响样式的间距或手机断点，用 320px 和常用手机宽度检查。
3. 给已有字段增加一个前后端一致的边界测试。
4. 给管理员列表增加一个纯前端显示项。
5. 新增业务字段：同步修改 DTO、VO、Entity、Mapper、Flyway、前端表单、详情、搜索、导出和测试。

每次只做一个小改动，先确认 `git status`，完成后看 `git diff`，测试通过再提交。这样最容易知道某次改变为什么有效或为什么出错。

## 五、本地运行与验证

```powershell
.\scripts\start-local.ps1
```

访问：

- 司机端：`http://127.0.0.1:5173/driver`
- 管理员端：`http://127.0.0.1:5173/admin/login`
- 接口文档：`http://127.0.0.1:8080/doc.html`

自动化验证：

```powershell
cd backend
mvn test

cd ..\frontend
npm test
npm run build
```

停止本地环境：

```powershell
.\scripts\stop-local.ps1
```

## 六、哪些文件可以清理

可以重新生成，不应提交到 Git：

- `backend/target/`
- `frontend/node_modules/`
- `frontend/dist/`
- `release/` 中已经被最新版替代的旧压缩包和展开目录
- 停止本地服务后的 `runtime/logs/`

不要删除：

- 根目录 `.env`：本地密钥与账号摘要。
- `runtime/mysql-data/`：本地数据库。
- `runtime/photo-storage/`：本地登记照片。
- 服务器 `.env`、Docker 数据卷及 `backups/`。
- 已经执行过的 `backend/src/main/resources/db/migration/V*.sql`。

拿不准时先执行 `git status --short`。Git 跟踪的源码应通过正常修改和提交管理；未跟踪的大型压缩包先查看内容，再决定是否删除。
