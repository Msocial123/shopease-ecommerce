#!/usr/bin/env bash
# =====================================================================================
#  Restore the ShopEase volumes from ./backups
#
#    ./docker/volumes/restore.sh                          # list available backups
#    ./docker/volumes/restore.sh 20261119-101500          # both volumes from the .tar.gz files
#    ./docker/volumes/restore.sh 20261119-101500 --dump   # database only, from the pg_dump file
#    add -y to skip the confirmation question
#
#  Volume restore : stops app + db, empties shopease-pgdata and shopease-uploads, unpacks the
#                   archives, starts everything again. Works even after "compose down -v".
#                   Everything written after the backup is lost.
#  --dump restore : keeps the volumes, stops only the app, replaces the database objects with
#                   pg_restore --clean, starts the app again. Uploaded images are not touched.
# =====================================================================================
set -euo pipefail
cd "$(dirname "$0")/../.."
. docker/volumes/compose-cmd.sh

STAMP="${1:-}"; [ $# -gt 0 ] && shift
MODE=volumes; YES=false
for a in "$@"; do
    case "$a" in
        --dump) MODE=dump ;;
        -y) YES=true ;;
        *) echo "unknown option $a" >&2; exit 2 ;;
    esac
done
if [ -z "$STAMP" ]; then
    echo "Usage: $0 <STAMP> [--dump] [-y]"; echo "Available backups:"
    ls backups/SHA256SUMS-* 2>/dev/null | sed 's/.*SHA256SUMS-/  /' || echo "  (none)"
    exit 2
fi

if [ -f "backups/SHA256SUMS-$STAMP" ]; then
    echo "== Verifying checksums"
    ( cd backups && sha256sum -c "SHA256SUMS-$STAMP" )
fi

if [ "$MODE" = dump ]; then
    F="backups/shopease-db-$STAMP.dump"; [ -f "$F" ] || { echo "Missing $F" >&2; exit 1; }
    $YES || { read -r -p "Replace the database contents with $F? [y/N] " a; [ "$a" = y ]; }
    stop_services app
    trap 'echo "== Starting app"; $COMPOSE start app >/dev/null && wait_healthy shopease-app' EXIT
    echo "== pg_restore --clean --if-exists"
    $COMPOSE exec -T db pg_restore --clean --if-exists --no-owner -U shopease -d shopease < "$F"
    echo "Database restored from $F."
    exit 0
fi

for v in pgdata uploads; do
    [ -f "backups/$v-$STAMP.tar.gz" ] || { echo "Missing backups/$v-$STAMP.tar.gz" >&2; exit 1; }
done
$YES || { read -r -p "Overwrite volumes shopease-pgdata and shopease-uploads with backup $STAMP? [y/N] " a; [ "$a" = y ]; }

echo "== Stopping app and db"
stop_services app db
trap 'echo "== Starting db and app"; start_stack' EXIT
export STAMP
$COMPOSE --profile ops run --rm restore
echo "Restore of $STAMP complete."
