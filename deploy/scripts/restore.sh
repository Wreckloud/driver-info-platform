#!/bin/sh
set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  echo "Usage: $0 /absolute/path/to/driver_info_backup.sql.gz [/absolute/path/to/driver_photos_backup.tar.gz]" >&2
  exit 1
fi

backup_file=$1
photo_backup_file=${2:-}
if [ ! -f "$backup_file" ]; then
  echo "Backup file not found: $backup_file" >&2
  exit 1
fi
if [ -n "$photo_backup_file" ] && [ ! -f "$photo_backup_file" ]; then
  echo "Photo backup file not found: $photo_backup_file" >&2
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
if [ -n "$photo_backup_file" ]; then
  tar -tzf "$photo_backup_file" >/dev/null
  docker compose exec -T api tar -xzf - -C /app/uploads < "$photo_backup_file"
fi
echo "Restore completed from: $backup_file"
[ -z "$photo_backup_file" ] || echo "Photos restored from: $photo_backup_file"
