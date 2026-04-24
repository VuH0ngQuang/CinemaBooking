# CinemaBooking

Full-stack cinema booking system built with **Spring Boot** (Java 21) and **React + Vite**, containerised with Docker. The repository ships a `docker-compose.yml` that builds and runs the `backend` and `frontend` services; **MySQL** and **aistor-server (MinIO AIStor)** are run separately and wired in through `Backend/.env`.

---

## Stack & ports

| Service         | Tech                         | Container port | Host port |
| --------------- | ---------------------------- | -------------- | --------- |
| backend         | Spring Boot / Java 21        | `1325`         | `4994`    |
| frontend        | React + Vite (served by nginx) | `80`         | `5007`    |
| mysql           | MySQL 8                      | `3306`         | `3306`    |
| aistor-server   | MinIO AIStor (S3 + console)  | `9000` / `9001`| `9000` / `9001` |

---

## Prerequisites

- Docker Engine **24+** and Docker Compose **v2** (`docker compose …`)
- Git
- *(optional, for local dev without Docker)* JDK 21, Maven 3.9+, Node 20+

---

## Repository layout

```
CinemaBooking/
├── Backend/               # Spring Boot service (Maven, Java 21)
│   ├── Dockerfile
│   ├── .env.example       # Copy to .env and fill in
│   └── src/
├── Frontend/client/       # React + Vite SPA (served by nginx in Docker)
│   ├── Dockerfile
│   └── nginx.conf
└── docker-compose.yml     # Builds backend + frontend
```

---

## Quick start

The full path is four steps:

1. Start **MySQL** (external container)
2. Start **aistor-server** (external container) and create a bucket
3. Create `Backend/.env`
4. `docker compose up --build`

### Step 1 — Start MySQL

```bash
docker volume create cinema-mysql-data

docker run -d \
  --name cinema-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=change-me \
  -e MYSQL_DATABASE=cinemabooking \
  -v cinema-mysql-data:/var/lib/mysql \
  --restart unless-stopped \
  mysql:8
```

Notes:

- The database name must match the one in your `DB_HOST` JDBC URL (the default is `cinemabooking`).
- Spring JPA is configured with `ddl-auto: update`, so tables are created on the backend's first run — you do **not** need to import a schema manually.
- Prefer creating a dedicated user in prod (see `CREATE USER … GRANT …`). For local dev, using `root` is fine.

### Step 2 — Start aistor-server (MinIO AIStor)

```bash
docker volume create cinema-aistor-data

docker run -d \
  --name cinema-aistor \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=admin \
  -e MINIO_ROOT_PASSWORD=change-me-please \
  -v cinema-aistor-data:/data \
  --restart unless-stopped \
  minio/aistor server /data --console-address ":9001"
```

- `9000` is the S3 API the backend talks to (`MINIO_URL`).
- `9001` is the admin console — open `http://localhost:9001` in a browser and log in with `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`.

**Create the bucket.** The backend does not create the bucket automatically. In the console, go to **Buckets → Create Bucket** and name it exactly what you will put in `MINIO_BUCKET` (e.g. `cinema-booking`). Or, with the MinIO client:

```bash
mc alias set local http://localhost:9000 admin change-me-please
mc mb local/cinema-booking
```

> `minio/aistor` is MinIO's commercial AIStor image. If you do not have an AIStor license, the OSS `minio/minio` image is a drop-in replacement with the same API — just swap the image name in the command above.

### Step 3 — Create `Backend/.env`

Copy the template and fill it in:

```bash
cp Backend/.env.example Backend/.env
```

Fill in every variable below. `Backend/.env` is loaded by `docker-compose.yml` via `env_file`, so the backend container sees these values at runtime.

| Variable               | Required | Example                                                   | Notes |
| ---------------------- | :------: | --------------------------------------------------------- | ----- |
| `DB_HOST`              | ✅ | `jdbc:mysql://host.docker.internal:3306/cinemabooking` | Full JDBC URL. See **Hostname tips** below. |
| `DB_USERNAME`          | ✅ | `root`                                                    | Matches Step 1. |
| `DB_PASSWORD`          | ✅ | `change-me`                                               | Matches Step 1. |
| `JWT_SECRET`           | ✅ | *(run `openssl rand -base64 64`)*                          | ≥ 512 bits for HS512. Generate a new one per environment. |
| `JWT_EXPIRATION_MS`    | ✅ | `3600000`                                                 | Access-token TTL in milliseconds (1 hour). |
| `PAYOS_CLIENT_ID`      | ⚠️ | `dummy-client-id`                                         | Real value required only for live PayOS payments. |
| `PAYOS_API_KEY`        | ⚠️ | `dummy-api-key`                                           | Same as above. |
| `PAYOS_CHECKSUM_KEY`   | ⚠️ | `dummy-checksum`                                          | Same as above. |
| `MINIO_URL`            | ✅ | `http://host.docker.internal:9000`                        | Points at aistor-server's S3 port. |
| `MINIO_ACCESS_KEY`     | ✅ | `admin`                                                   | `MINIO_ROOT_USER` from Step 2, or an app user you created in the console. |
| `MINIO_SECRET_KEY`     | ✅ | `change-me-please`                                        | `MINIO_ROOT_PASSWORD` from Step 2, or an app user you created. |
| `MINIO_BUCKET`         | ✅ | `cinema-booking`                                          | Must already exist (Step 2). |
| `MAIL_HOST`            | ⚠️ | `smtp.gmail.com`                                          | Needed for email features (booking confirmations, password reset). |
| `MAIL_PORT`            | ⚠️ | `587`                                                     | Typically `587` for STARTTLS. |
| `MAIL_USERNAME`        | ⚠️ | `you@example.com`                                         | SMTP user. |
| `MAIL_PASSWORD`        | ⚠️ | `app-password`                                            | Use an app-specific password for Gmail etc. |

Legend: ✅ required for the app to boot, ⚠️ required only for the corresponding feature (payments / mail) — any non-empty placeholder keeps the backend happy during local dev.

Optional variables also honoured by `application.yaml` (set them in `Backend/.env` if you need to override the defaults):

- `APP_DOMAIN` — public URL used in email links (default `http://cinema.vuhongquang.com/`).
- `AUTH_COOKIE_SECURE` / `AUTH_COOKIE_SAME_SITE` — cookie flags; set to `false` / `Lax` for plain-HTTP local dev.
- `CORS_ALLOW_ALL`, `CORS_ALLOW_CREDENTIALS`, `CORS_ALLOWED_ORIGINS` — override the default allow-list if your frontend runs on a non-standard origin.

**Hostname tips.** The backend runs inside Docker but MySQL and aistor-server run on the host:

- **Docker Desktop (macOS / Windows):** use `host.docker.internal` as shown in the examples.
- **Linux:** `host.docker.internal` is not resolved by default. The simplest option is to use the Docker bridge IP `172.17.0.1`, e.g. `DB_HOST=jdbc:mysql://172.17.0.1:3306/cinemabooking` and `MINIO_URL=http://172.17.0.1:9000`. Alternatively, put all four containers on the same user-defined Docker network and use the container names (`cinema-mysql`, `cinema-aistor`) as hostnames.

Generate a strong `JWT_SECRET`:

```bash
openssl rand -base64 64
```

### Step 4 — Build and run the app

From the repo root:

```bash
docker compose up --build -d
docker compose logs -f backend
```

The first build compiles the Spring Boot jar and the Vite bundle, so it takes a few minutes. Subsequent runs are cached.

---

## Accessing the app

| URL                          | What               |
| ---------------------------- | ------------------ |
| `http://localhost:5007`      | Frontend SPA       |
| `http://localhost:4994`      | Backend REST API (see [`Backend/API_REFERENCE.md`](Backend/API_REFERENCE.md)) |
| `http://localhost:9001`      | aistor-server console |

---

## Local development (without Docker)

Backend:

```bash
cd Backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd Frontend/client
npm install
npm run dev
```

Vite serves on `http://localhost:5173`. If your dev origin is not already in `CORS_ALLOWED_ORIGINS`, add it to `Backend/.env`:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174
```

---

## Common operations

```bash
# Tail backend logs
docker compose logs -f backend

# Restart just the backend after a rebuild
docker compose build backend && docker compose up -d backend

# Stop the app (keeps data volumes)
docker compose down

# Stop and wipe app containers/images (data in MySQL/aistor-server volumes is preserved)
docker compose down --rmi local

# Stop the external MySQL / aistor-server containers
docker stop cinema-mysql cinema-aistor
docker start cinema-mysql cinema-aistor
```

---

## Troubleshooting

- **Backend can't connect to MySQL** — confirm the host from inside a container:
  `docker compose exec backend sh -c 'getent hosts host.docker.internal'`. On Linux, swap to `172.17.0.1` or create a shared Docker network.
- **`NoSuchBucket` / uploads fail** — the bucket in `MINIO_BUCKET` does not exist. Create it via the console at `http://localhost:9001` or with `mc mb`.
- **`Access Denied` from aistor-server** — `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` must match the MinIO user. During initial setup they equal `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD`.
- **CORS errors in the browser** — add the frontend origin to `CORS_ALLOWED_ORIGINS` in `Backend/.env` and `docker compose restart backend`.
- **Port already in use** — adjust the host-side mapping in `docker-compose.yml` (for `4994` / `5007`) or the `-p` flags in the `docker run` commands for MySQL / aistor-server.
- **`ddl-auto: update`** — fine for local dev, but do not rely on it for production migrations.

---

## Related docs

- [`Backend/API_REFERENCE.md`](Backend/API_REFERENCE.md) — REST endpoints
- [`Backend/BUSINESS_FLOW.md`](Backend/BUSINESS_FLOW.md) — domain overview
- [`Backend/BOOKING_PAYMENT_TICKET_RUNBOOK.md`](Backend/BOOKING_PAYMENT_TICKET_RUNBOOK.md) — booking + payment ops runbook
- [`AGENTS.md`](AGENTS.md) — repo conventions for AI agents
