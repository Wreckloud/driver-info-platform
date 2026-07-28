#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$project_dir/.env"

fail() {
  echo "Database initialization failed: $1" >&2
  exit 1
}

[ -f "$env_file" ] || fail ".env does not exist"

dotenv_value() {
  value=$(sed -n "s/^$1=//p" "$env_file" | tail -n 1 | tr -d '\r')
  case "$value" in
    \'*\') value=${value#\'}; value=${value%\'} ;;
    \"*\") value=${value#\"}; value=${value%\"} ;;
  esac
  printf '%s' "$value"
}

mysql_container=$(dotenv_value EXISTING_MYSQL_CONTAINER)
database_name=$(dotenv_value MYSQL_DATABASE)
database_user=$(dotenv_value MYSQL_USER)
database_password=$(dotenv_value MYSQL_PASSWORD)
mysql_container=${mysql_container:-lycan-mysql}
database_name=${database_name:-driver_info}
database_user=${database_user:-driver_app}

case "$database_name" in
  *[!A-Za-z0-9_]*|'') fail "MYSQL_DATABASE may contain only letters, numbers, and underscores" ;;
esac
case "$database_user" in
  *[!A-Za-z0-9_]*|'') fail "MYSQL_USER may contain only letters, numbers, and underscores" ;;
esac
[ -n "$database_password" ] || fail "MYSQL_PASSWORD is required"
[ "${#database_name}" -le 64 ] || fail "MYSQL_DATABASE is too long"
[ "${#database_user}" -le 32 ] || fail "MYSQL_USER is too long"

docker inspect "$mysql_container" >/dev/null 2>&1 || fail "container $mysql_container does not exist"
[ "$(docker inspect -f '{{.State.Running}}' "$mysql_container")" = "true" ] \
  || fail "container $mysql_container is not running"

password_hex=$(printf '%s' "$database_password" | od -An -tx1 | tr -d ' \n')

docker exec -i "$mysql_container" sh -c \
  'exec mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD"' <<SQL
CREATE DATABASE IF NOT EXISTS \`$database_name\`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET @driver_password = CONVERT(0x$password_hex USING utf8mb4);
SET @create_user_sql = CONCAT(
  'CREATE USER IF NOT EXISTS ''$database_user''@''%'' IDENTIFIED BY ',
  QUOTE(@driver_password)
);
PREPARE create_user_statement FROM @create_user_sql;
EXECUTE create_user_statement;
DEALLOCATE PREPARE create_user_statement;
SET @alter_user_sql = CONCAT(
  'ALTER USER ''$database_user''@''%'' IDENTIFIED BY ',
  QUOTE(@driver_password)
);
PREPARE alter_user_statement FROM @alter_user_sql;
EXECUTE alter_user_statement;
DEALLOCATE PREPARE alter_user_statement;
GRANT ALL PRIVILEGES ON \`$database_name\`.* TO '$database_user'@'%';
FLUSH PRIVILEGES;
SQL

echo "Database and application user are ready in $mysql_container."
