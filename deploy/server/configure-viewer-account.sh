#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$project_dir/.env"
viewer_username=HYHTLLWLYXGS

fail() {
  echo "Read-only account update failed: $1" >&2
  exit 1
}

[ -f "$env_file" ] || fail ".env does not exist"
[ -t 0 ] || fail "run this script from an interactive terminal"

dotenv_value() {
  value=$(sed -n "s/^$1=//p" "$env_file" | tail -n 1 | tr -d '\r')
  case "$value" in
    \'*\') value=${value#\'}; value=${value%\'} ;;
    \"*\") value=${value#\"}; value=${value%\"} ;;
  esac
  printf '%s' "$value"
}

restore_tty() {
  stty echo 2>/dev/null || true
}
trap restore_tty EXIT HUP INT TERM

printf 'Password for read-only account %s (at least 12 characters): ' "$viewer_username"
stty -echo
IFS= read -r password
stty echo
printf '\nConfirm password: '
stty -echo
IFS= read -r password_confirmation
stty echo
printf '\n'
trap - EXIT HUP INT TERM

[ "${#password}" -ge 12 ] || fail "password must contain at least 12 characters"
[ "$password" = "$password_confirmation" ] || fail "the two passwords do not match"

app_version=$(dotenv_value APP_VERSION)
app_version=${app_version:-1.2.0}
api_image="driver-info-platform-api:${app_version}"
docker image inspect "$api_image" >/dev/null 2>&1 || fail "image $api_image does not exist"

password_hash=$(docker run --rm \
  -e ADMIN_PASSWORD_PLAIN="$password" \
  --entrypoint java \
  "$api_image" \
  -Dloader.main=com.wreckloud.driver.tool.PasswordHashTool \
  -cp /app/app.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher)
unset password password_confirmation

case "$password_hash" in
  \$2a\$*|\$2b\$*|\$2y\$*) ;;
  *) fail "password generator did not return a BCrypt hash" ;;
esac

temporary_env=$(mktemp "$project_dir/.env.XXXXXX")
trap 'rm -f "$temporary_env"' EXIT HUP INT TERM
escaped_hash=$(printf '%s' "$password_hash" | sed 's/[&|]/\\&/g')
awk -v username="$viewer_username" -v hash="$escaped_hash" '
  BEGIN { username_written = 0; hash_written = 0 }
  /^VIEWER_USERNAME=/ { print "VIEWER_USERNAME=" username; username_written = 1; next }
  /^VIEWER_PASSWORD_BCRYPT=/ { print "VIEWER_PASSWORD_BCRYPT=\047" hash "\047"; hash_written = 1; next }
  { print }
  END {
    if (!username_written) print "VIEWER_USERNAME=" username
    if (!hash_written) print "VIEWER_PASSWORD_BCRYPT=\047" hash "\047"
  }
' "$env_file" > "$temporary_env"
chmod 600 "$temporary_env"
mv "$temporary_env" "$env_file"
trap - EXIT HUP INT TERM
unset password_hash escaped_hash

cd "$project_dir"
docker compose --env-file "$env_file" config --quiet
docker compose --env-file "$env_file" up -d --force-recreate api web
docker compose --env-file "$env_file" ps
echo "Read-only account $viewer_username configured. Wait for the API health check to become healthy."
