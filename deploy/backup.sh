#!/bin/bash
# Geri yükleme için deploy/KURULUM.md §7'ye bakın.
set -euo pipefail
umask 077
cd "$(dirname "$0")"

env_value() { grep -E "^$1=" ../.env | tail -1 | cut -d= -f2-; }
POSTGRES_USER=$(env_value POSTGRES_USER)
POSTGRES_DB=$(env_value POSTGRES_DB)
DC="docker compose --env-file ../.env"
DAY=$(date +%F)
DUMP="/backup/db-$DAY.dump"

mkdir -p /backup/files /backup/certs
trap 'rm -f "$DUMP.tmp"' EXIT

FILES_DIR=$(docker volume inspect deploy_files -f '{{ .Mountpoint }}')
rsync -a "$FILES_DIR/" /backup/files/
$DC exec -T db pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB" > "$DUMP.tmp"
mv "$DUMP.tmp" "$DUMP"

cp ../.env "/backup/env-$DAY.bak"
rsync -a nginx/certs/ /backup/certs/
find /backup -maxdepth 1 \( -name 'db-*.dump' -o -name 'env-*.bak' \) -mtime +14 -delete
