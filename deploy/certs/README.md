# HTTPS 证书目录

生产启动前，将域名证书放到本目录并使用固定文件名：

- `fullchain.pem`：完整证书链
- `privkey.pem`：私钥

证书文件已被 `.gitignore` 排除，禁止提交到版本库。可使用 Certbot 在宿主机申请和续期，续期后执行 `docker compose exec web nginx -s reload`。
