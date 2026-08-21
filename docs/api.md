# API 说明

所有 JSON 接口统一返回：

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

失败时 `code` 为 `0`，同时使用对应的 HTTP 状态码。管理员写接口需要有效会话及 CSRF 请求头。

## 司机登记

### `POST /api/driver/records`

无需登录，使用 `multipart/form-data`。同一个 `submissionToken` 重试只返回原记录。

- `record`：`application/json` 类型的登记参数。
- `photos`：1–9 个客户端压缩后的 JPEG 文件，单张不超过 2MB、尺寸不超过 2048×2048。

```json
{
  "submissionToken": "ec88439a-32f0-4750-bae8-60160f4bf174",
  "project": "冷链A1",
  "driverName": "张三",
  "phone": "13800138000",
  "licensePlate": "京A12345",
  "vehicleType": "厢式货车",
  "quantity": "20件（冻品）",
  "destination": "天津市",
  "remark": "需要全程冷藏",
  "locationStatus": "SUCCESS",
  "latitude": 39.9042,
  "longitude": 116.4074,
  "locationAccuracy": 18.5,
  "locatedAt": "2026-07-27T01:30:00Z"
}
```

`project` 和 `quantity` 为必填文本，`remark` 为可选文本。`SUCCESS` 必须提供四个定位字段；`DENIED`、`FAILED`、`TIMEOUT`、`NOT_REQUESTED` 必须将定位字段省略或设为 `null`。文字地址和发车时间（响应字段仍为 `createdAt`）由后端生成。

创建成功摘要会返回 `latitude`、`longitude`、`locationAddress` 和 `locationAccuracy`，供司机成功页显示本次起始位置。地址解析失败时 `locationAddress` 为空，但坐标和精度仍会返回。
创建成功摘要还会返回 `photoCount`。照片不加水印，定位与发车时间仍由独立字段保存。

## 管理员认证

- `GET /api/admin/auth/csrf`：取得 CSRF Token；登录前及写操作前调用。
- `POST /api/admin/auth/login`：JSON 参数为 `username/password`。
- `POST /api/admin/auth/logout`：退出并销毁会话。
- `GET /api/admin/auth/me`：查询当前登录账号，并通过 `canManage` 表示是否具有修改和删除权限。

## 登记管理

- `GET /api/admin/records?page=1&pageSize=20&startDate=2026-07-01&endDate=2026-07-31&keyword=京A`
- `GET /api/admin/records/{id}`
- `GET /api/admin/record-photos/{photoId}`：在有效管理员或只读会话中查看照片。
- `PUT /api/admin/records/{id}`：只接收 `project/driverName/phone/licensePlate/vehicleType/quantity/destination/remark`。
- `DELETE /api/admin/records/{id}`：软删除。
- `GET /api/admin/records/export`：使用与列表相同的日期和关键词条件，返回 `.xlsx`。

日期条件按上海时区的完整自然日解释。关键词可匹配项目、姓名、车牌、目的地和备注。所有正常查询均排除软删除记录。

分页结果中的 `serverTime` 是服务器生成的 UTC 时间。管理员前端以它为基准计算“几分钟前”等相对时间，避免浏览器本机时钟不准确。

`ROLE_ADMIN` 可以查看、修改、软删除和导出；`ROLE_VIEWER` 只能查看列表、详情、照片和导出，后端会拒绝其修改及删除请求。
