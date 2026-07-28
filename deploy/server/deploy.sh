#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$project_dir/.env"
compose_file="$project_dir/docker-compose.yml"

fail() {
  echo "Deployment failed: $1" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "docker is not installed"
docker compose version >/dev/null 2>&1 || fail "docker compose is unavailable"
command -v curl >/dev/null 2>&1 || fail "curl is not installed"
[ -f "$env_file" ] || fail ".env does not exist"
[ -f "$compose_file" ] || fail "docker-compose.yml does not exist"
[ -s "$project_dir/artifacts/backend/app.jar" ] || fail "backend artifact is missing"
[ -s "$project_dir/artifacts/frontend/index.html" ] || fail "frontend artifact is missing"

dotenv_value() {
  value=$(sed -n "s/^$1=//p" "$env_file" | tail -n 1 | tr -d '\r')
  case "$value" in
    \'*\') value=${value#\'}; value=${value%\'} ;;
    \"*\") value=${value#\"}; value=${value%\"} ;;
  esac
  printf '%s' "$value"
}

public_domain=$(dotenv_value PUBLIC_DOMAIN)
public_base_url=$(dotenv_value PUBLIC_BASE_URL)

case "$public_domain" in
  ''|example.com|*/*|*' '*) fail "PUBLIC_DOMAIN is invalid" ;;
esac
[ "$public_base_url" = "https://${public_domain}" ] \
  || fail "PUBLIC_BASE_URL must equal https://PUBLIC_DOMAIN"

mysql_network=$(dotenv_value EXISTING_MYSQL_NETWORK)
web_port=$(dotenv_value DRIVER_WEB_PORT)
mysql_network=${mysql_network:-deploy_lycan_net}
web_port=${web_port:-18080}
case "$web_port" in
  *[!0-9]*|'') fail "DRIVER_WEB_PORT must be a port number" ;;
esac

docker network inspect "$mysql_network" >/dev/null 2>&1 \
  || fail "Docker network $mysql_network does not exist"

cd "$project_dir"
chmod 600 "$env_file"
docker compose --env-file "$env_file" config --quiet
"$project_dir/deploy/server/init-existing-mysql.sh"
docker compose --env-file "$env_file" build
docker compose --env-file "$env_file" up -d --remove-orphans

attempt=0
until curl -fsS "http://127.0.0.1:${web_port}/driver" >/dev/null 2>&1 \
  && curl -fsS "http://127.0.0.1:${web_port}/api/admin/auth/csrf" >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 30 ]; then
    docker compose --env-file "$env_file" ps
    docker compose --env-file "$env_file" logs --tail=100 api web
    fail "local health checks did not pass within 60 seconds"
  fi
  sleep 2
done

docker compose --env-file "$env_file" ps
echo "Deployment completed: http://127.0.0.1:${web_port}/driver"
echo "After HTTPS is configured, use ${public_base_url}/driver."
