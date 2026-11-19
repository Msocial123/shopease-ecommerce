# ShopEase with Docker Compose – volumes, bind mounts, tmpfs, backup & restore

`docker-compose.yml` (project root) runs ShopEase on Tomcat 11 with **PostgreSQL 17** and shows
the three kinds of container storage, persistent Tomcat logs and uploaded files, and how to back
up and restore volumes. Everything below was run on Ubuntu 26.04 with rootless **Podman 5.7 +
podman-compose 1.5**; the same commands work with Docker (`docker compose ...`).

```
                 :8081                    backend network (no published DB port)
  Browser ───────────────► shopease-app ───────────────────► shopease-db
                           Tomcat 11 (UID 10001)              PostgreSQL 17
                            │  uploads  → volume shopease-uploads
                            │  logs     → bind  ./logs/tomcat
                            │  temp,work→ tmpfs (RAM)          data → volume shopease-pgdata
                                                               /tmp → tmpfs (RAM)
```

## 1. Start

```bash
cp .env.example .env          # then edit POSTGRES_PASSWORD and APP_ADMIN_PASSWORD
chmod 600 .env
docker compose up -d --build  # Podman: podman-compose up -d --build
docker compose ps             # db and app "healthy", logs-init "Exited (0)"
```

Open `http://<host>:8081/` · health: `http://<host>:8081/health` · admin: `admin@shopease.com` /
the `APP_ADMIN_PASSWORD` from `.env`.

On first start the app creates the PostgreSQL tables (`db/schema-postgresql.sql`), loads the sample
catalogue and the admin account. The same WAR still runs on MySQL – only `DB_DRIVER` / `DB_URL` differ.

## 2. The three kinds of storage

| | Named volume | Bind mount | tmpfs |
|---|---|---|---|
| Where the data lives | Managed by the engine (`docker volume inspect`) | A folder you choose on the host | RAM of the host |
| Survives container re-creation | **Yes** | **Yes** | No – empty on every start |
| Survives `compose down -v` | No (deleted) | Yes (it's just a folder) | No |
| Created empty with image content/owner | Yes (copies the image folder's files and owner) | No – host folder as-is | Yes, empty |
| Used here for | PostgreSQL data, uploaded images | Tomcat logs, backups | Tomcat `temp` + `work`, PostgreSQL `/tmp` |
| Good for | Application data | Files you want to read/edit on the host | Scratch data, secrets that must not hit disk |

See them:

```bash
docker volume ls --filter name=shopease
docker volume inspect shopease-pgdata --format '{{.Mountpoint}}'
docker inspect shopease-app --format '{{range .Mounts}}{{.Type}}  {{.Source}} -> {{.Destination}}{{println}}{{end}}'
docker exec shopease-app df -h /usr/local/tomcat/temp /usr/local/tomcat/work     # tmpfs 128M
```

Output on the test server (Podman, rootless):

```
shopease-pgdata  -> /home/ubuntu/.local/share/containers/storage/volumes/shopease-pgdata/_data
volume  .../volumes/shopease-uploads/_data -> /usr/local/tomcat/uploads
bind    /home/ubuntu/compose-test/logs/tomcat -> /usr/local/tomcat/logs
tmpfs   128M  0     128M   0% /usr/local/tomcat/temp
tmpfs   128M  1.3M  127M   1% /usr/local/tomcat/work
```

## 3. Persisting Tomcat logs and uploaded files

**Logs – bind mount `./logs/tomcat`.** Tomcat writes `catalina.<date>.log`, `localhost.<date>.log`
and `localhost_access_log.<date>.txt` there, so they can be read on the host without entering the
container – and they are still there after the container is deleted:

```bash
tail -f logs/tomcat/localhost_access_log.*.txt
```

Two details make this work for a non-root container:
* the one-shot service `logs-init` (busybox, runs as root, exits) gives the folder to UID 10001,
  because a bind mount keeps the **host** owner;
* `UMASK=0022` makes Tomcat create the files world-readable (Tomcat's default 0027 would hide them).

`docker logs shopease-app` still shows everything too (the access log goes to stdout as well).

**Uploaded files – named volume `shopease-uploads`.** In *Admin → Products → Edit* choose
*Upload image*. The file is stored in `/usr/local/tomcat/uploads` (= the volume) and served at
`/uploads/product-xxxx.png`. Only JPG/PNG/GIF/WEBP are accepted (checked by the file's bytes; SVG is
refused because it can contain JavaScript). The image folder is created in the image owned by
`tomcat`, so the empty volume inherits that owner on first use.

**Prove it** – re-create everything except the volumes:

```bash
docker compose down            # removes containers + network, KEEPS volumes and ./logs
docker compose up -d
# uploaded image still served (200), orders still in the database, old log lines still in
# ./logs/tomcat, but /usr/local/tomcat/work (tmpfs) starts empty: JSPs are compiled again
```

## 4. PostgreSQL with a named volume

```yaml
db:
  image: docker.io/library/postgres:17-alpine
  volumes:
    - pgdata:/var/lib/postgresql/data      # named volume "shopease-pgdata"
```

* The database is created only when the volume is **empty** (first start). Changing
  `POSTGRES_PASSWORD` later does not change the password stored in an existing volume.
* No `ports:` – only the app can reach the database (network `shopease-backend`).
  Use it from the host with: `docker exec -it shopease-db psql -U shopease -d shopease`
* `postgres:17` on purpose: from 18 the image expects the volume at `/var/lib/postgresql`.

Tomcat's JNDI pool from `server.xml` is pointed at PostgreSQL purely by environment variables:

```yaml
DB_DRIVER: org.postgresql.Driver
DB_URL: jdbc:postgresql://db:5432/shopease
```

## 5. Backup and restore of volumes

| Command | What it does | Downtime |
|---|---|---|
| `./docker/volumes/backup.sh` | `pg_dump` + archives of **both** volumes + checksums | app+db stopped for the archive step (seconds) |
| `./docker/volumes/backup.sh --online` | `pg_dump` + uploads archive | none |
| `./docker/volumes/restore.sh` | lists the available backups | – |
| `./docker/volumes/restore.sh STAMP` | empties and refills both volumes from the archives | app+db stopped |
| `./docker/volumes/restore.sh STAMP --dump` | database only, `pg_restore --clean` from the dump | app stopped |

Files in `./backups` (bind mount):

```
shopease-db-20261119-104012.dump    pg_dump custom format (logical, taken online)
pgdata-20261119-104012.tar.gz       raw data directory (taken with PostgreSQL stopped)
uploads-20261119-104012.tar.gz      uploaded images
SHA256SUMS-20261119-104012          checked again before every restore
```

**Why the database is stopped for the volume archive:** copying PostgreSQL's files while it writes
can give an inconsistent copy. `pg_dump` is consistent while running – that's why both exist.

The archive and restore steps are the compose services `backup` / `restore` (profile `ops`, never
started by `up`). Under the hood it is the classic pattern – a throw-away container that mounts the
volume and a host folder and runs `tar`:

```bash
docker run --rm --network none \
  -v shopease-uploads:/volumes/uploads:ro -v "$PWD/backups":/backups \
  docker.io/library/busybox:1.37 tar czf /backups/uploads-manual.tar.gz -C /volumes/uploads .
```

**Disaster drill (tested):**

```bash
./docker/volumes/backup.sh                 # STAMP 20261119-104012: 1 order, 1 image
# ... place a new order, upload a new image (now 2 orders, 2 images)
docker compose down -v                     # containers AND volumes deleted
./docker/volumes/restore.sh 20261119-104012 -y
# -> checksums OK, volumes re-created and filled, db + app healthy: 1 order, 1 image again
```

Copy `./backups` off the server (scp, S3, ...) – a backup on the same disk is not a backup.

## 6. Troubleshooting

### `UnknownHostException: db` – app shows 503 "Database unavailable"

```
ERROR GlobalControllerAdvice - Database unavailable on GET /: Failed to obtain JDBC Connection
WARN  ShopEasePool - Pool is empty, failed to create/setup connection
Caused by: java.net.UnknownHostException: db
```

**Cause:** the app container could not look up the name `db`. Seen with **root Podman**
(`sudo podman-compose`) on a host where **ufw is enabled** (Day 1 guide): ufw drops the DNS
queries that containers send to the Podman bridge gateway (`/var/log/ufw.log` shows
`UFW BLOCK IN=podmanN ... DST=10.89.x.1 ... DPT=53`). Because the pool in server.xml cannot be
created, the app falls back to its own pool (`ShopEasePool`), which fails the same way, and
`/health` shows `"dataSource":"hikari:..."` with `"database":"DOWN"`.

**Fix (already in docker-compose.yml):** `db` has a fixed IP on the backend network
(`DB_IP`, default `10.123.45.10`) and the app gets `extra_hosts: db:<that IP>`, i.e. an entry in
its `/etc/hosts`. No DNS lookup is needed any more, and the firewall stays as it is. The image is
also built with `network: host`, because the same ufw rule broke Maven downloads during the build.

If you started the stack with an older version of this file, re-create the network once
(volumes and data are kept):

```bash
docker compose down                    # sudo podman-compose down
docker network rm shopease-backend     # sudo podman network rm shopease-backend
docker compose up -d --build
docker exec shopease-app getent hosts db        # -> 10.123.45.10  db
curl -s http://localhost:8081/health            # -> "database":"UP", "dataSource":"jndi:..."
```

`subnet 10.123.45.0/24 is already used on the host` → another network uses it (for example a
second ShopEase stack): set `BACKEND_SUBNET` and `DB_IP` in `.env`, e.g. `10.123.46.0/24` and
`10.123.46.10`.

## 7. Useful commands

```bash
docker compose logs -f app                 # container log stream
docker compose exec db psql -U shopease -d shopease -c "SELECT count(*) FROM products;"
docker compose down                        # stop + remove containers (data kept)
docker compose down -v                     # ...and DELETE the named volumes (data lost!)
docker system df -v | grep shopease        # volume sizes
```

Podman: use `podman-compose` instead of `docker compose`, and `podman` instead of `docker`.
The backup/restore scripts detect which one is installed (override with `COMPOSE=...`).
