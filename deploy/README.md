# Disposable PostgreSQL development definition

`compose.dev.yaml` contains **only PostgreSQL**, not the application or a deployment
stack. It lives in the root layout's infrastructure area, outside Java source.
Creating/validating this file does not authorize running it, connecting the server
or applying migrations. No container, image pull or database execution was performed.

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

## Commands for a separately authorized operator

Run from `/home/nghlong3004/projects/longnest` with Docker Engine/Compose available
and permission to use the selected local Docker context. These are instructions,
not operations performed or newly authorized by this file. The explicit env-file
prevents Compose from auto-reading private `.env` files. No `.env.dev` is loaded.

```sh
# Static validation only: no service or database starts.
docker compose --env-file /dev/null -p longnest-dev -f deploy/compose.dev.yaml config --quiet

# Start PostgreSQL only (may pull the image); does not wait for database readiness.
docker compose --env-file /dev/null -p longnest-dev -f deploy/compose.dev.yaml up -d postgres

# Stop without removing the container/data.
docker compose --env-file /dev/null -p longnest-dev -f deploy/compose.dev.yaml stop postgres

# Resume that stopped container, preserving its data.
docker compose --env-file /dev/null -p longnest-dev -f deploy/compose.dev.yaml start postgres

# Restart the same container, preserving its data.
docker compose --env-file /dev/null -p longnest-dev -f deploy/compose.dev.yaml restart postgres

# DESTRUCTIVE: remove this dev container/data and its unused anonymous volume.
docker compose --env-file /dev/null -p longnest-dev -f deploy/compose.dev.yaml down --volumes
```

Do not use this project name for unrelated containers. Do not run cleanup against
anything containing wanted data. The definition has no automatic restart policy,
application service, migrations, owner-head seed, init script or mount.

## What environment files actually configure

The commands above deliberately use `--env-file /dev/null`: neither root `.env`
nor `.env.dev` is loaded. Compose `--env-file` supplies **model interpolation**,
not blanket container injection. This YAML has no `${...}` references and has fixed
`environment` values, so replacing it with `--env-file .env.dev` alone does not
change the PostgreSQL settings. For example, from root:

```sh
# Optional interpolation input only; not needed by this fixed-value definition.
docker compose --env-file .env.dev -p longnest-dev -f deploy/compose.dev.yaml config --quiet
```

No app variable from the example launcher configuration is consumed by this model.
Compose's own control variables (such as COMPOSE_*), if supplied, can affect Compose
itself; do not treat an arbitrary application env file as a Compose configuration.
The fixed commands with explicit project/file and `/dev/null` remain recommended.
There is no service `env_file`; only the declared `POSTGRES_*` and `PGDATA` constants
are passed by this definition into the DB container. Wiring env-based credentials
would require an explicit future Compose change; none was made or is needed here.

The Java backend is a **separate host process**, not a Compose service. The
[backend launcher](../scripts/README.md) explicitly reads root `.env.dev` for Java;
it neither starts Compose nor changes the PostgreSQL container's environment.
Direct Java startup still has no dotenv loader. No automatic `.env` merge exists.

## Connection placeholders and remaining inputs

The only published port is `127.0.0.1:54329` → container 5432. Port availability,
image pull, startup, readiness and same-container restart behavior remain untested.
Database/user/password are the public **synthetic dev-only** placeholders
`longnest_dev` / `longnest_dev` / `longnest_dev_only`. The image-created user is a
bootstrap superuser; it is not a production/application-role security design.
Do not use these credentials, this storage or this file for real financial data.

After separately authorized startup/readiness, a host-side client would target
`jdbc:postgresql://127.0.0.1:54329/longnest_dev`. Compose does not inject database
settings into the Java process. An authorized server test still needs explicit
DataSource URL/user/password, the `ledger` profile and a synthetic
`longnest.ledger.owner`. Starting that profile can execute Flyway and contact the
database: it is **not** part of static validation. Also needed: authorized fixture
head initialization and bounded migration/save/replay/reopen test scope. No backup,
restore guarantee or durable-ledger acceptance follows from this disposable file.

## Telegram token readiness

An opt-in TelegramBots SDK adapter now binds `LONGNEST_TELEGRAM_TOKEN` and the
single numeric `LONGNEST_TELEGRAM_USER`, only when
`LONGNEST_TELEGRAM_ENABLED=true` and the `ledger` profile are selected. Exact
candidate disposition is in [CURRENT STATUS](../docs/WORK_STATUS.md); no live bot
is established. The [backend launcher](../scripts/README.md#optional-telegram-configuration)
loads root `.env.dev` into the separate Java process with inherited-env precedence.
Direct Spring/Java startup does not import dotenv files; `.env` is not loaded.
This fixed-value Compose model neither consumes nor injects Telegram settings.
Do not paste tokens into chat or put them in this Compose file. Database and
Telegram activation still require their own runtime scope; configuration/code
availability is not permission or evidence of live execution.
