# Disposable PostgreSQL development definition

`compose.dev.yaml` contains **only PostgreSQL**, not the application or a deployment
stack. The host [dev launcher](../scripts/README.md) now manages this service and
readiness before ledger preparation/backend startup. No container, image pull or
database execution was performed in implementation verification; those require
operator runtime authority. Docker Engine must already be running locally.

## Selected image and storage

`postgres:18.6-bookworm` selects a stable supported PostgreSQL 18 minor release
and an explicit Debian variant instead of `latest` or a release candidate.
[Version policy](https://www.postgresql.org/support/versioning/) and
[official image tags](https://hub.docker.com/_/postgres) were checked 2026-10-22.
This is a version tag, not an immutable digest; registry/image identity and
compatibility with the application have not been runtime-verified.

There are no Compose bind mounts, volume entries/declarations, named/external
volumes, tmpfs mounts or host data paths. `PGDATA=/tmp/longnest-pgdata` is a path
**inside the container**, stored in its writable layer, not a host `/tmp` binding.
It is intentionally outside the image's declared volume path.

Important: the [selected image Dockerfile](https://github.com/docker-library/postgres/blob/e00e1bd34ec5c8a8e7ad89b273b3d42efaf6d5bc/18/bookworm/Dockerfile)
declares `VOLUME /var/lib/postgresql` and defaults PGDATA beneath it. Docker can
therefore create an **unused anonymous volume** there even though this Compose
file declares none. The [entrypoint](https://github.com/docker-library/postgres/blob/e00e1bd34ec5c8a8e7ad89b273b3d42efaf6d5bc/docker-entrypoint.sh)
honors our PGDATA override; database files and WAL are intended to stay in the
container writable layer, not that volume. This is not a promise of zero runtime
mounts or zero Docker-managed host storage. No custom image is introduced merely
to remove inherited image metadata.

Stop/start and restart preserve the **same container** and its data. Removing or
recreating the container loses this cluster, including its transactions and schema
(see [container-layer storage](https://docs.docker.com/engine/storage/)).
An `up` after image/config changes can recreate it; it is not a backup or durable
finance store. Ordinary `down` can leave unused anonymous volumes behind;
[down --volumes](https://docs.docker.com/reference/cli/docker/compose/down/) removes
attached anonymous volumes too. Use the scoped cleanup below, not global pruning.

## Single-command development startup

Run from `/home/nghlong3004/projects/longnest` with Docker Engine/Compose available
and permission to use the selected local Docker context. Configure root `.env.dev`
using the tracked safe `.env.example` and run:

```sh
bash scripts/dev-server.sh
```

The launcher builds/tests, ensures the sole dev service exists/runs without
recreation, waits up to 60 seconds for readiness (plus up to 2 seconds kill grace),
then migrates/validates/prepares
the owner before launching the backend. It uses absolute paths:

```text
docker compose --env-file <root>/.env.dev -p longnest-dev -f <root>/deploy/compose.dev.yaml ...
```

Readiness is `exec -T postgres pg_isready -U <effective-user> -d <derived-name>`.
Only absent or matching stopped containers receive `up -d --no-recreate postgres`;
an unready running container is polled, never reset or recreated. Existing image,
port, storage and bootstrap credential/name mismatch fails before preflight.
Local Docker access/configuration errors are not treated as an absent container.
Full Compose config and private inspection values are never printed by the launcher.

## Stop/resume and deliberate disposal

After Ctrl-C stops the host backend, an authorized operator may use the following
scoped commands with the same local Docker context. These instructions were not
executed here. Inspect the exact dev container ID without printing its environment:

```sh

docker ps -a --filter label=com.docker.compose.project=longnest-dev \
  --filter label=com.docker.compose.service=postgres --format '{{.ID}} {{.Status}}'

# Substitute the single inspected dev ID; preserves its data.
docker stop REPLACE_WITH_INSPECTED_DEV_CONTAINER_ID

# DESTRUCTIVE, only when deliberately discarding all disposable dev data:
# after stopping it, substitute that same inspected ID; also removes unused volumes.
docker rm -v REPLACE_WITH_INSPECTED_DEV_CONTAINER_ID
```

Alternatively re-run the dev launcher to resume a matching stopped DB and backend.
Do not use this project name for unrelated containers. Do not run cleanup against
anything containing wanted data. The definition has no automatic restart policy,
application service, schema SQL/init script or mount. Migrations and safe owner
preparation happen in the separate host Java preflight, not inside PostgreSQL.

## What environment files actually configure

The launcher explicitly passes root `.env.dev` as Compose interpolation input, not
as a blanket service `env_file`. It exports the same effective settings (inherited
environment wins even if empty) to prevent a separate Compose precedence contract.
YAML maps `SPRING_DATASOURCE_USERNAME/PASSWORD` to `POSTGRES_USER/PASSWORD` and
internal `LONGNEST_DEV_DB_NAME` to `POSTGRES_DB`. The launcher derives that name
only from `jdbc:postgresql://127.0.0.1:54329/NAME`; NAME/username must be ASCII
identifiers up to63 characters. Remote/alternate-port/query-bearing URLs and an
explicit conflicting derived name fail; do not set that internal name separately.
Only these declared PostgreSQL values, PGDATA and init arguments enter the container.
Telegram/DeepSeek values are not injected. Root `.env` is never loaded. Bare
`POSTGRES_*` variables are not alternate application configuration.

The Java backend is a **separate host process**, not a Compose service. The
[backend launcher](../scripts/README.md) explicitly reads root `.env.dev` for Java;
it prepares Compose before Java but never reconfigures an existing mismatched container.
Direct Java startup still has no dotenv loader. No automatic `.env` merge exists.

## Connection placeholders and remaining inputs

The only published port is `127.0.0.1:54329` → container 5432. Port availability,
image pull, startup, readiness and same-container restart behavior remain untested.
The tracked example's database/user/password are public **synthetic dev-only** placeholders
`longnest_dev` / `longnest_dev` / `longnest_dev_only`. The image-created user is a
bootstrap superuser; it is not a production/application-role security design.
Do not use these credentials, this storage or this file for real financial data.

They are defaults in the example only; container and app use the effective file/
inherited datasource values. The launcher supplies both Java processes with these
values and the stable configured owner. Preflight's `ledger-init` profile can
execute Flyway and prepare only genuinely fresh storage; the subsequent `ledger`
process never auto-seeds. None is part of static validation. No backup,
restore guarantee or durable-ledger acceptance follows from this disposable file.

## Telegram token readiness

An opt-in TelegramBots SDK adapter now binds `LONGNEST_TELEGRAM_TOKEN` and the
single numeric `LONGNEST_TELEGRAM_USER`, only when
`LONGNEST_TELEGRAM_ENABLED=true` and the `ledger` profile are selected. Exact
candidate disposition is in [CURRENT STATUS](../docs/WORK_STATUS.md); no live bot
is established. The [backend launcher](../scripts/README.md#optional-telegram-configuration)
loads root `.env.dev` into the separate Java process with inherited-env precedence.
Direct Spring/Java startup does not import dotenv files; `.env` is not loaded.
This Compose model does not inject Telegram settings into PostgreSQL.
Do not paste tokens into chat or put them in this Compose file. Database and
Telegram activation still require their own runtime scope; configuration/code
availability is not permission or evidence of live execution.
