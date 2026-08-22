# 司机出车登记系统

司机扫码登记出车信息，管理员查询、修改、软删除并导出记录。当前生产环境部署在 `driver.wreckloud.com`。

## 目录

```text
backend/        Spring Boot 后端
frontend/       Vue 前端
scripts/        Windows 本地启动与测试脚本
deploy/server/  当前服务器的部署与备份脚本
```

## 本地运行

```powershell
.\scripts\start-local.ps1
```

- 司机端：`http://127.0.0.1:5173/driver`
- 管理员端：`http://127.0.0.1:5173/admin/login`
- 接口文档：`http://127.0.0.1:8080/doc.html`

停止服务：

```powershell
.\scripts\stop-local.ps1
```

## 测试

```powershell
cd backend
mvn test

cd ..\frontend
npm test
npm run build
```

## 生产更新包

```powershell
.\deploy\server\package-update.ps1
```

生成结果位于 `release/driver-info-platform-1.2.0-update.tar.gz`。`.env`、运行数据、照片、构建结果和发布包均不提交 Git。
