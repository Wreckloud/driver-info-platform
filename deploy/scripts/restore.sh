#!/bin/sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 /absolute/path/to/driver_info_backup.sql.gz" >&2
  exit 1
fi

backup_file=$1
if [ ! -f "$backup_file" ]; then
  echo "Backup file not found: $backup_file" >&2
  exit 1
fi

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
restore_file="$project_dir/deploy/backups/.restore_$(date +%Y%m%d_%H%M%S).sql"
cd "$project_dir"
mkdir -p "$(dirname -- "$restore_file")"
trap 'rm -f "$restore_file"' EXIT

gzip -t "$backup_file"
gzip -dc "$backup_file" > "$restore_file"
docker compose exec -T mysql sh -c \
  'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < "$restore_file"
echo "Restore completed from: $backup_file"
