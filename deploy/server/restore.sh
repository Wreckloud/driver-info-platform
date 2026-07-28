#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 /absolute/path/to/driver_info_backup.sql.gz" >&2
  exit 1
fi

backup_file=$1
[ -f "$backup_file" ] || { echo "Backup file not found: $backup_file" >&2; exit 1; }

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$project_dir/.env"
restore_file="$project_dir/backups/.restore_$(date +%Y%m%d_%H%M%S).sql"

[ -f "$env_file" ] || { echo ".env does not exist" >&2; exit 1; }

dotenv_value() {
  value=$(sed -n "s/^$1=//p" "$env_file" | tail -n 1 | tr -d '\r')
  case "$value" in
    \'*\') value=${value#\'}; value=${value%\'} ;;
    \"*\") value=${value#\"}; value=${value%\"} ;;
  esac
  printf '%s' "$value"
}

mysql_container=$(dotenv_value EXISTING_MYSQL_CONTAINER)
mysql_user=$(dotenv_value MYSQL_USER)
mysql_password=$(dotenv_value MYSQL_PASSWORD)
mysql_database=$(dotenv_value MYSQL_DATABASE)
mysql_container=${mysql_container:-lycan-mysql}
mkdir -p "$(dirname -- "$restore_file")"
trap 'rm -f "$restore_file"' EXIT

gzip -t "$backup_file"
gzip -dc "$backup_file" > "$restore_file"
docker exec -i -e MYSQL_PWD="$mysql_password" "$mysql_container" \
  mysql -u"$mysql_user" "$mysql_database" < "$restore_file"
echo "Restore completed from: $backup_file"
