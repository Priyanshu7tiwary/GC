#!/bin/sh
# Dumps the database once a day into /backups (./backups on the server) and keeps the last KEEP_DAYS days.
set -eu
mkdir -p /backups
while true; do
  file="/backups/99ile-$(date +%Y%m%d-%H%M).sql.gz"
  if pg_dump --no-owner --no-privileges | gzip > "$file.tmp"; then
    mv "$file.tmp" "$file"
    echo "backup written: $file"
  else
    rm -f "$file.tmp"
    echo "backup FAILED" >&2
  fi
  find /backups -name '99ile-*.sql.gz' -mtime +"${KEEP_DAYS:-14}" -delete
  sleep 86400
done
