#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
backup_dir="$project_dir/deploy/backups"
timestamp=$(date +%Y%m%d_%H%M%S)
sql_file="$backup_dir/.driver_info_${timestamp}.sql"
backup_file="$backup_dir/driver_info_${timestamp}.sql.gz"
compressed_file="$backup_file.tmp"

mkdir -p "$backup_dir"
cd "$project_dir"
trap 'rm -f "$sql_file" "$compressed_file"' EXIT

docker compose exec -T mysql sh -c \
  'exec mysqldump --single-transaction --quick --routines --triggers -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  > "$sql_file"
gzip -c "$sql_file" > "$compressed_file"
mv "$compressed_file" "$backup_file"

find "$backup_dir" -type f -name 'driver_info_*.sql.gz' -mtime +30 -delete
echo "Backup created: $backup_file"
