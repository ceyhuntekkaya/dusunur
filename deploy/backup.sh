#!/bin/bash
# Geri yükleme:
#   1. Servisleri durdurun: docker compose stop backend
#   2. Veritabanı: docker compose exec -T db pg_restore -c -U $POSTGRES_USER -d $POSTGRES_DB < /backup/db-YYYY-MM-DD.dump
#   3. Dosyalar: rsync -a /backup/files/ ile dosya volume'unun içeriğine geri kopyalayın
#   4. Servisi başlatın: docker compose start backend
set -euo pipefail
cd "$(dirname "$0")"
set -a; . ../.env; set +a

mkdir -p /backup/files
docker compose exec -T db pg_dump -Fc -U "$POSTGRES_USER" "$POSTGRES_DB" > "/backup/db-$(date +%F).dump"
FILES_DIR=$(docker volume inspect deploy_files -f '{{ .Mountpoint }}')
rsync -a "$FILES_DIR/" /backup/files/
find /backup -maxdepth 1 -name 'db-*.dump' -mtime +14 -delete
