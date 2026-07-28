#!/bin/sh
set -eu

if [ "$(id -u)" -ne 0 ]; then
  echo "Run this script with sudo." >&2
  exit 1
fi

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$project_dir/.env"
site_available=/etc/nginx/sites-available/driver-info-platform
site_enabled=/etc/nginx/sites-enabled/driver-info-platform

[ -f "$env_file" ] || { echo ".env does not exist" >&2; exit 1; }

public_domain=$(sed -n 's/^PUBLIC_DOMAIN=//p' "$env_file" | tail -n 1 | tr -d '\r' | sed "s/^['\"]//;s/['\"]$//")
web_port=$(sed -n 's/^DRIVER_WEB_PORT=//p' "$env_file" | tail -n 1 | tr -d '\r' | sed "s/^['\"]//;s/['\"]$//")
web_port=${web_port:-18080}

case "$public_domain" in
  ''|example.com|*/*|*' '*) echo "PUBLIC_DOMAIN is invalid" >&2; exit 1 ;;
esac
case "$web_port" in
  *[!0-9]*|'') echo "DRIVER_WEB_PORT must be a port number" >&2; exit 1 ;;
esac

backup_file=
if [ -e "$site_available" ]; then
  backup_file="${site_available}.bak.$(date +%Y%m%d_%H%M%S)"
  cp -a "$site_available" "$backup_file"
fi

cat > "$site_available" <<NGINX
server {
  listen 80;
  listen [::]:80;
  server_name $public_domain;

  client_max_body_size 1m;

  location / {
    proxy_pass http://127.0.0.1:$web_port;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$remote_addr;
    proxy_set_header X-Forwarded-Proto \$scheme;
  }
}
NGINX

ln -sfn "$site_available" "$site_enabled"
if ! nginx -t; then
  rm -f "$site_enabled"
  if [ -n "$backup_file" ]; then
    mv "$backup_file" "$site_available"
    ln -sfn "$site_available" "$site_enabled"
  else
    rm -f "$site_available"
  fi
  nginx -t || true
  echo "Nginx configuration was not installed." >&2
  exit 1
fi

systemctl reload nginx
echo "HTTP site installed for $public_domain."
echo "Next, issue its HTTPS certificate with Certbot."
