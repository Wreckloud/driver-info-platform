# GitHub 私有仓库部署

本文用于将服务器部署方式从上传发布包切换为从 GitHub 私有仓库拉取源码。服务器只配置仓库级只读 Deploy Key，不保存个人 GitHub 密码或 Personal Access Token。

## 1. 在服务器生成只读部署密钥

```bash
mkdir -p /home/wreckloud/.ssh
chmod 700 /home/wreckloud/.ssh
ssh-keygen -t ed25519 -C "driver-info-platform-deploy" -f /home/wreckloud/.ssh/driver_info_platform_deploy -N ''
cat /home/wreckloud/.ssh/driver_info_platform_deploy.pub
```

复制公钥输出，在 GitHub 仓库的 `Settings > Deploy keys > Add deploy key` 中添加：

- Title：`production-server`
- Key：粘贴完整公钥
- 不勾选 `Allow write access`

## 2. 克隆仓库并沿用线上配置

```bash
GIT_SSH_COMMAND='ssh -i /home/wreckloud/.ssh/driver_info_platform_deploy -o IdentitiesOnly=yes' \
  git clone git@github.com:Wreckloud/driver-info-platform.git \
  /home/wreckloud/apps/driver-info-platform
```

```bash
git -C /home/wreckloud/apps/driver-info-platform config core.sshCommand \
  'ssh -i /home/wreckloud/.ssh/driver_info_platform_deploy -o IdentitiesOnly=yes'
```

```bash
cp /home/wreckloud/apps/driver-info-platform-1.0.0/.env \
  /home/wreckloud/apps/driver-info-platform/.env
chmod 600 /home/wreckloud/apps/driver-info-platform/.env
```

`.env` 始终只保存在服务器，不提交到 GitHub。

## 3. 首次部署及后续更新

```bash
cd /home/wreckloud/apps/driver-info-platform
chmod +x deploy/server/*.sh
./deploy/server/deploy-from-git.sh
```

脚本会执行以下操作：

1. 拒绝覆盖服务器上的未提交源码修改；
2. 仅以 fast-forward 方式同步 `origin/main`；
3. 按 API、前端顺序构建，降低小内存服务器的峰值占用；
4. 复用现有 MySQL、Docker 网络、`.env` 和 `127.0.0.1:18080`；
5. 只更新 `driver_info_platform` Compose 项目的两个容器；
6. 完成司机页面及 API 健康检查。

宿主机 Nginx、证书和 LycanClaw 容器不会被该脚本修改。
