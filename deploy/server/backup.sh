#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
env_file="$project_dir/.env"
backup_dir="$project_dir/backups"
timestamp=$(date +%Y%m%d_%H%M%S)
sql_file="$backup_dir/.driver_info_${timestamp}.sql"
backup_file="$backup_dir/driver_info_${timestamp}.sql.gz"
compressed_file="$backup_file.tmp"

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
mkdir -p "$backup_dir"
trap 'rm -f "$sql_file" "$compressed_file"' EXIT

docker exec -i -e MYSQL_PWD="$mysql_password" "$mysql_container" \
  mysqldump --single-transaction --quick --routines --triggers \
  -u"$mysql_user" "$mysql_database" > "$sql_file"
gzip -c "$sql_file" > "$compressed_file"
mv "$compressed_file" "$backup_file"

find "$backup_dir" -type f -name 'driver_info_*.sql.gz' -mtime +30 -delete
echo "Backup created: $backup_file"
