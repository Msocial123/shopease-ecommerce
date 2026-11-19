#!/usr/bin/env bash
# =====================================================================================
#  Back up the ShopEase volumes into ./backups
#
#    ./docker/volumes/backup.sh             # dump + both volumes (app/db stopped a few seconds)
#    ./docker/volumes/backup.sh --online    # dump + uploads only, no downtime
#
#  Creates (STAMP = yyyymmdd-HHMMSS):
#    backups/shopease-db-STAMP.dump      pg_dump custom format - logical, taken ONLINE
#    backups/pgdata-STAMP.tar.gz         raw PostgreSQL data directory - taken while db is STOPPED
#    backups/uploads-STAMP.tar.gz        uploaded product images
#    backups/SHA256SUMS-STAMP            checksums of the above
#
#  Why two kinds of database backup?
#    - pg_dump is consistent while the database runs and restores into any PostgreSQL version.
#    - The volume archive is an exact copy of the files; copying them while PostgreSQL is
#      writing could produce a broken copy, so app + db are stopped for that step.
# =====================================================================================
set -euo pipefail
cd "$(dirname "$0")/../.."                       # project root (docker-compose.yml)
. docker/volumes/compose-cmd.sh

ONLINE=false
[ "${1:-}" = "--online" ] && ONLINE=true

export STAMP="${STAMP:-$(date +%Y%m%d-%H%M%S)}"
mkdir -p backups

echo "== 1/3 Logical database dump (online): backups/shopease-db-$STAMP.dump"
$COMPOSE exec -T db pg_dump -U shopease -d shopease -Fc > "backups/shopease-db-$STAMP.dump"

if $ONLINE; then
    echo "== 2/3 Archiving the uploads volume (online mode: pgdata archive skipped)"
    $ENGINE run --rm --network none -v shopease-uploads:/volumes/uploads:ro -v "$PWD/backups":/backups \
        docker.io/library/busybox:1.37 tar czf "/backups/uploads-$STAMP.tar.gz" -C /volumes/uploads .
else
    echo "== 2/3 Stopping app and db for a consistent file-level copy of the volumes"
    trap 'echo "== Starting db and app again"; start_stack' EXIT
    stop_services app db
    $COMPOSE --profile ops run --rm backup
fi

echo "== 3/3 Checksums"
( cd backups && sha256sum ./*-"$STAMP".* > "SHA256SUMS-$STAMP" && cat "SHA256SUMS-$STAMP" )
echo "Backup $STAMP complete. Restore with:  ./docker/volumes/restore.sh $STAMP"
