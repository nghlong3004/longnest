# Backend development launcher

`dev-server.sh` is a host-side development launcher, not a safe smoke command or
production provisioning tool. It requires Bash 4.4+, GNU `env`/`timeout`, util-linux
`flock`, Java 25, Maven and Docker Engine with Compose on inherited `PATH`.
Docker's local Unix-socket daemon must already be available; the script never
installs tools, starts the daemon or changes host permissions. One command builds/
tests, ensures the dev PostgreSQL service is running/ready, migrates/prepares the
configured ledger owner, then runs the backend. No operator JAR handling is needed.
The small root `scripts/` directory separates host tooling from Java and Compose.

## Usage

With private `.env.dev` configured as below, and **only with separately authorized
database/container connection/migration scope** (and Telegram/provider authority
if those integrations are enabled):

```sh
# From the repository root:
bash scripts/dev-server.sh

# From any other directory (use the actual repository path):
bash /home/nghlong3004/projects/longnest/scripts/dev-server.sh
```

The launcher resolves root relative to itself and validates **only root `.env.dev`**.
It runs `mvn -B -ntp package` from `apps/server`, with tests enabled. On success it
prepares PostgreSQL, then runs the packaged JAR once with `ledger-init` and internal
`--longnest.ledger.initialize-if-absent=true`. That process migrates/validates and
commits owner preparation, excludes both integrations, and exits. Only a zero exit
allows the launcher to exec the same JAR under `ledger`. No arguments, extra mode,
manual exports, profile edits or separate init command are required.
This follows the repository's documented installed-Maven convention (3.10.0 in
the verified environment), avoiding wrapper distribution download/bootstrap and
a long-lived Maven parent for the application. No `clean`, test-skipping,
dependency prefetch or install goal is added. Normal Maven dependency/plugin
resolution can require repository access on a cold cache; inherited Maven
configuration remains operator-controlled. The launcher does not install tools.

File-loaded values, including secrets and any MAVEN_OPTS/JAVA_HOME/PATH keys, are
**not supplied to Maven**; only its original inherited environment is passed.
Already exported values still reach Maven, so keep runtime secrets in the private
file when they need not be exposed to build plugins/tests. Missing config/tools
fail before the build. Build failure returns Maven's exit code without launching
Java, even if an older JAR exists; successful packaging must provide a readable
artifact at the expected path. There is no prebuilt-JAR shortcut/failure fallback.
The artifact path is currently `apps/server/target/longnest-0.0.1-SNAPSHOT.jar`;
update it if the Maven artifact/version changes. Java's exit status and signals
are preserved without a remaining parent-shell wrapper; Ctrl-C stops that process.
During packaging, DB startup and preparation, INT/TERM is forwarded to the current
process group; interruption stops before normal backend startup (exit130/143).
If PostgreSQL was already started, interruption/failure does not remove or stop it.
Run in the foreground; backgrounding from a shell
that ignores SIGINT has that shell's normal inherited-signal limitations.

A readable `.env.dev` is required even when values are supplied via exported
environment. Users manage that private file themselves; never commit it, paste
secrets into chat, or run this launcher with shell tracing. The tracked root
[.env.example](../.env.example) lists current backend and default-off integration
settings. Copy it to `.env.dev` only if that private file is absent; otherwise
merge deliberately without overwriting existing values. Root `.env` is a separate
prepared file and is still not loaded by this launcher. Safe **synthetic-only**
example content, matching the disposable [dev database](../deploy/README.md):

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:54329/longnest_dev
SPRING_DATASOURCE_USERNAME=longnest_dev
SPRING_DATASOURCE_PASSWORD=longnest_dev_only
LONGNEST_LEDGER_OWNER=synthetic-dev-owner
# Optional; omission selects ledger. No dev profile exists in the application.
SPRING_PROFILES_ACTIVE=ledger
```

The first four names are required and must be nonblank after precedence. They
bind `spring.datasource.url/username/password` and `longnest.ledger.owner`;
application validation still checks the actual owner/values. If present, the
effective `SPRING_PROFILES_ACTIVE` must be exactly `ledger`, not `smoke`, `dev`
or a list. The launcher explicitly selects that existing profile on the Java
command line; it does not add a new dev profile or alter YAML/JPA/Flyway settings.
Required URL shape is exactly `jdbc:postgresql://127.0.0.1:54329/NAME`, with NAME
and username matching `[A-Za-z_][A-Za-z0-9_]{0,62}`. Remote URLs, other ports, query
options and conflicting `LONGNEST_DEV_DB_NAME` fail before build/Docker. Database
name is derived from that URL, not a second operator setting. Compose interpolates
the same effective datasource username/password; bare `POSTGRES_*` env values do
not configure the app. Keep a stable owner across launches. Root `.env.example`
uses synthetic defaults, not mandatory names/passwords. Do not use real finance
data with this disposable storage.

Compose calls use absolute paths equivalent to:

```text
docker compose --env-file <root>/.env.dev -p longnest-dev -f <root>/deploy/compose.dev.yaml ...
```

The launcher supplies effective file/inherited values through the command environment,
without password/token arguments or full Compose-config output. It checks a local
Docker context, inspects service/container state and compares image, published port,
PGDATA/mount policy and bootstrap DB/user/password before reuse. Absent or matching
stopped service starts with `up -d --no-recreate postgres`; running service is not
restarted/recreated for failed readiness. Mismatch, Docker access/config/exec errors,
unsupported state or failed startup stops clearly. Readiness uses
`exec -T postgres pg_isready -U <effective-user> -d <derived-name>` with a 60-second
deadline; Docker calls use 30-second timeouts (180 for initial start/image pull)
and a 2-second forced-kill grace. Passwords/inspection contents are not
printed. `pg_isready` is not authentication proof: preflight's real connection is.

Preflight runs Flyway from `db/ledger`, Hibernate validation, then a proxied
transaction taking exclusive locks on all five ledger tables. Exactly one matching
owner is accepted unchanged; a generation-zero owner is inserted only when every
ledger table is empty and fsync/synchronous_commit are on. Foreign/multiple owners,
headless nonempty state, query/connection/migration/durability or failed/uncertain
commit cannot seed or launch integrations. No error is reclassified as fresh
storage and no data is reset/deleted. Concurrent preflights serialize in PostgreSQL.
The default standalone runtime still never auto-seeds; explicit diagnostic init
remains compatible but is no longer a dev prerequisite.

An ignored `.dev-server.lock` holds a kernel lock from before build through final
Java shutdown, refusing a second launcher in the same checkout. Do not remove the
file while a process holds it. This is not cross-checkout/distributed bot exclusion;
one operator/process must own the bot. Ctrl-C stops backend resources, not PostgreSQL.
Storage persists across stop/start/restart of the same container, but container
removal/recreation loses it; see [storage lifecycle](../deploy/README.md).
These implemented checks do not establish real DB durability or live bot readiness.

### Optional Telegram configuration

The backend now has an opt-in TelegramBots SDK capture/get/recent adapter; exact
candidate acceptance and unverified runtime limits are in
[CURRENT STATUS](../docs/WORK_STATUS.md). It is disabled unless
`LONGNEST_TELEGRAM_ENABLED=true`. Only for a separately authorized live session,
provide these names in root `.env.dev` or the inherited environment:

- `LONGNEST_TELEGRAM_ENABLED`: `true` to opt in; otherwise omit or use `false`.
- `LONGNEST_TELEGRAM_TOKEN`: private token supplied by the operator, never committed
  or pasted into chat; a placeholder is not a usable token.
- `LONGNEST_TELEGRAM_USER`: the one allowed positive numeric Telegram user ID,
  not a username or the application owner string.

These bind `longnest.telegram.enabled/token/user`. The already-required
`LONGNEST_LEDGER_OWNER` is the internal principal, supplied only after the adapter
authenticates that user's non-bot private-chat message. Launcher loading and
inherited-env precedence apply unchanged; direct Spring/Java startup does not
read `.env.dev`. Compose does not inject Telegram settings into PostgreSQL. Enabled
startup contacts Telegram and PostgreSQL; do not run it as a synthetic smoke.
No token, user identity, live startup or Telegram message has been configured here.

## Exact loading contract and precedence

The same literal loader also supports the optional `LONGNEST_DEEPSEEK_ENABLED`,
`LONGNEST_DEEPSEEK_API_KEY`, `LONGNEST_DEEPSEEK_BASE_URL` and
`LONGNEST_DEEPSEEK_MODEL` names. No values are provided or private file edited here.
The [server fallback documentation](../apps/server/README.md#opt-in-deepseek-capture-fallback)
defines required settings, timeout/recovery limits and separate live egress/spend
approval. Omission keeps deterministic interpretation; this launcher grants no
provider permission. Compose does not forward these names into PostgreSQL.
Native conversation leaves output-token limits unset for provider/model defaults;
legacy DEEPSEEK_MAX_TOKENS is not mapped. Output length/cost are not guaranteed.

Low → high: existing application defaults → literal `.env.dev` values → already
exported process environment. An exported empty value wins, so an empty required
value fails rather than falling back. `.env` is **never read**; no base/dev merge,
automatic Spring dotenv support or production secret loader is implemented.
Other exported/JVM/Spring configuration remains trusted operator input; this is
not an environment sanitizer or an authorization boundary. The Java executable
is resolved from inherited PATH before applying file values (not JAVA_HOME or
file PATH); other file variables are passed into its environment when not exported.

The deliberately small dotenv grammar is **not shell or full dotenv syntax**:

- One `NAME=value` per line; names match `[A-Za-z_][A-Za-z0-9_]*`. Outer spaces/tabs
  around the assignment/value are allowed; names are case-sensitive, duplicates fail.
- Blank lines and whole-line `#` comments are allowed; LF/CRLF and no final newline
  are supported. No inline comments, `export` prefix, BOM, NUL or control characters.
- Bare values contain no whitespace, quote, backslash or `#`; empty is allowed.
  Single- or double-quoted whole values preserve spaces, Unicode, `#`, `=` and the
  opposite quote literally. The matching quote cannot appear inside the value.
- No escapes/backslashes, multiline values, variable interpolation, command
  substitution or evaluation. `$HOME`, `$(...)` and backticks inside quotes are
  literal text, never executed. Invalid lines fail even if process values override
  them. Diagnostics identify line numbers or required names, not file values.

Use the grammar above even if a different dotenv tool accepts broader syntax.
The launcher does not silently execute a shell-compatible env file.

## Synthetic verification (no real env file or Java startup)

```sh
bash -n scripts/dev-server.sh
node scripts/dev-server.test.mjs
```

Node's built-in tests copy the launcher into temporary synthetic repositories,
use fake Docker/Maven/Java executables and a Maven-produced synthetic JAR, and invoke from outside root. They test
quoting, literal payloads, inherited precedence, missing/unreadable/invalid config,
build-before-run without a prebuilt JAR, build failure despite a stale JAR, Compose
paths/credential precedence, absent/stopped/running/mismatched storage, bounded
readiness, preflight failures, checkout exclusion, build
environment isolation, exact invocation, exit status and SIGINT/SIGTERM during
build/init and runtime (runtime retains the launcher PID). No dependency
installation, real private env read, Java, DB, provider or container is involved.
The test prints its retained temporary fixture directory. Node is needed only for
these checks, not for running the Bash launcher. This is launcher evidence, not
a replacement for the prior accepted Java tests or PostgreSQL runtime evidence.
