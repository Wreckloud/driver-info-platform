#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$project_dir/.env"
base_compose_file="$project_dir/docker-compose.server.yml"
git_compose_file="$project_dir/docker-compose.git.yml"
branch=${1:-main}

fail() {
  echo "Git deployment failed: $1" >&2
  exit 1
}

command -v git >/dev/null 2>&1 || fail "git is not installed"
command -v docker >/dev/null 2>&1 || fail "docker is not installed"
docker compose version >/dev/null 2>&1 || fail "docker compose is unavailable"
command -v curl >/dev/null 2>&1 || fail "curl is not installed"
[ -d "$project_dir/.git" ] || fail "$project_dir is not a Git repository"
[ -f "$env_file" ] || fail ".env does not exist"
[ -f "$base_compose_file" ] || fail "docker-compose.server.yml does not exist"
[ -f "$git_compose_file" ] || fail "docker-compose.git.yml does not exist"

if ! git -C "$project_dir" diff --quiet \
  || ! git -C "$project_dir" diff --cached --quiet; then
  fail "tracked files contain local changes; commit or discard them before deployment"
fi

git -C "$project_dir" fetch --prune origin
git -C "$project_dir" checkout "$branch"
git -C "$project_dir" merge --ff-only "origin/$branch"

deploy_version="git-$(git -C "$project_dir" rev-parse --short=12 HEAD)"

dotenv_value() {
  value=$(sed -n "s/^$1=//p" "$env_file" | tail -n 1 | tr -d '\r')
  case "$value" in
    \'*\') value=${value#\'}; value=${value%\'} ;;
    \"*\") value=${value#\"}; value=${value%\"} ;;
  esac
  printf '%s' "$value"
}

compose() {
  APP_VERSION="$deploy_version" docker compose \
    --env-file "$env_file" \
    -f "$base_compose_file" \
    -f "$git_compose_file" \
    "$@"
}

public_domain=$(dotenv_value PUBLIC_DOMAIN)
public_base_url=$(dotenv_value PUBLIC_BASE_URL)
mysql_network=$(dotenv_value EXISTING_MYSQL_NETWORK)
web_port=$(dotenv_value DRIVER_WEB_PORT)
mysql_network=${mysql_network:-deploy_lycan_net}
web_port=${web_port:-18080}

case "$public_domain" in
  ''|example.com|*/*|*' '*) fail "PUBLIC_DOMAIN is invalid" ;;
esac
[ "$public_base_url" = "https://${public_domain}" ] \
  || fail "PUBLIC_BASE_URL must equal https://PUBLIC_DOMAIN"
case "$web_port" in
  *[!0-9]*|'') fail "DRIVER_WEB_PORT must be a port number" ;;
esac

docker network inspect "$mysql_network" >/dev/null 2>&1 \
  || fail "Docker network $mysql_network does not exist"

chmod 600 "$env_file"
compose config --quiet
"$project_dir/deploy/server/init-existing-mysql.sh"

# Build sequentially so the small server does not run Maven and Node builds together.
compose build api
compose build web
compose up -d --remove-orphans

attempt=0
until curl -fsS "http://127.0.0.1:${web_port}/driver" >/dev/null 2>&1 \
  && curl -fsS "http://127.0.0.1:${web_port}/api/admin/auth/csrf" >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 30 ]; then
    compose ps
    compose logs --tail=100 api web
    fail "local health checks did not pass within 60 seconds"
  fi
  sleep 2
done

compose ps
echo "Deployment completed at commit $(git -C "$project_dir" rev-parse --short HEAD)."
echo "Public URL: ${public_base_url}/driver"
