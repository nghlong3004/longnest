# Backend development launcher

One operator command, from root or any working directory:

```sh
bash /home/nghlong3004/projects/longnest/scripts/dev-server.sh
# Equivalent from the repository root:
bash scripts/dev-server.sh
```

This is a real development startup command, not a smoke check. Execution requires
operator authority for containers/database access and any enabled integrations.
Implementation verification never runs the real launcher or private env files.

## Prerequisites

Bash4.4+, GNU env/readlink/timeout, util-linux flock, installed Maven, an installed
Java25 JDK including javac, and Docker Engine with Compose must already be available.
Docker must be running on a local Unix-socket context. No daemon startup, permission
changes, SDKMAN discovery/activation, wrapper bootstrap, download/install command
or global configuration change is implemented. Maven may resolve dependencies
from repositories on a cold cache; this script does not prefetch or install them.

Configure root `.env.dev` using the tracked [.env.example](../.env.example), copying
only if that private file is absent; otherwise merge privately. `.env.dev` is required
even if settings are exported. Root `.env` is separate and never loaded. Never
commit private values, enable shell tracing or paste private configuration/logs.

## Exact sequence and visible progress

1. Parse root `.env.dev` literally, with inherited exports winning even if empty.
   Require datasource URL/user/password and stable LONGNEST_LEDGER_OWNER. Select
   effective JAVA_HOME when nonempty, otherwise resolve Java on inherited PATH.
   Check executable java/javac and Java major25, then pass the selected JAVA_HOME
   consistently to Docker and Maven. Invalid explicit JAVA_HOME fails; no silent
   fallback. File PATH does not choose the initially resolved executables, but can
   affect subprocess helpers just like other trusted runtime configuration.
2. Acquire an ignored `.dev-server.lock` with nonblocking flock. A second launcher
   in the same checkout fails immediately; do not unlink the file while held.
3. Print `Checking development PostgreSQL configuration`. Check local Docker
   context, Compose image/service state and existing container configuration.
   Absent or matching stopped service prints `Starting development PostgreSQL`
   and uses `up -d --no-recreate postgres`. Running service is not restarted for
   failed readiness. Image, port, mount, PGDATA or bootstrap DB/user/password
   mismatch fails without reset/reconfiguration. Docker errors are not absence.
4. Print `Waiting for PostgreSQL readiness`, poll with a60-second deadline, then
   print `PostgreSQL ready`. Docker calls normally allow30 seconds; initial
   start/image pull allows180; forced-kill grace is2 seconds. Pull/inspection/raw
   readiness output is suppressed to avoid private values, but phase labels remain
   visible. `pg_isready` is not authentication proof; real connection follows.
5. Change to actual `apps/server`, print `Starting foreground Maven/one application`,
   and exec installed `mvn -B -ntp spring-boot:run -Dspring-boot.run.profiles=ledger`.
   Its internal LONGNEST_DEV_PREPARE_LEDGER=true env enables the readiness bean; no operator
   mode, profile editing, exports or JAR handling is required. There is no mandatory
   package, Surefire test run, JAR repackage or separate preparation JVM. The pinned
   goal runs through test-compile, so source/test compilation and dependency
   resolution may still take time. Maven remains parent of one application JVM.
6. In that single Spring startup, Flyway initializes and Hibernate validates.
   The dev-only LedgerReady bean then calls existing proxied prepareOwner, logging
   preparation/ready messages. Both integration factories explicitly request that
   bean before constructing clients; Telegram lifecycle registration cannot start
   ahead of it. An ApplicationRunner alone would be too late. Exactly one matching
   owner is accepted unchanged, or generation zero is created only under all-five-
   table empty checks, exclusive locks and required durability. Migration/schema/
   query/owner/uncertain-commit failures abort startup, never become fresh storage.

Direct ordinary runtime without the internal dev flag remains non-seeding.
Default-off integrations, shared authorization, actual-user confirmations, short
memory and one-tool policy are unchanged. Diagnostic strict ledger-init remains
available but is not used by this launcher. No schema/dependency is added.

## Configuration and Compose contract

```dotenv
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:54329/longnest_dev
SPRING_DATASOURCE_USERNAME=longnest_dev
SPRING_DATASOURCE_PASSWORD=longnest_dev_only
LONGNEST_LEDGER_OWNER=synthetic-dev-owner
SPRING_PROFILES_ACTIVE=ledger
LONGNEST_TELEGRAM_ENABLED=false
LONGNEST_DEEPSEEK_ENABLED=false
```

These are synthetic disposable defaults, not real financial-data storage. URL must
be exactly `jdbc:postgresql://127.0.0.1:54329/NAME` without query options; NAME and
username match `[A-Za-z_][A-Za-z0-9_]{0,62}`. Database name is derived internally as
LONGNEST_DEV_DB_NAME, not a second required setting. Explicit conflicting values
fail. Bare POSTGRES_* vars are not alternate app configuration. Effective
SPRING_PROFILES_ACTIVE must be exactly ledger if supplied.

All Compose calls use `docker compose --env-file <absolute-root>/.env.dev
-p longnest-dev -f <absolute-root>/deploy/compose.dev.yaml`. The same effective
datasource username/password enter declared container environment; readiness is
`exec -T postgres pg_isready -U <effective-user> -d <derived-name>`. No service
env_file forwards Telegram/DeepSeek values into PostgreSQL. See
[storage/stop/resume limits](../deploy/README.md).

Optional Telegram names are LONGNEST_TELEGRAM_ENABLED/TOKEN/USER; optional native
conversation names are LONGNEST_DEEPSEEK_ENABLED/API_KEY/BASE_URL/MODEL. Exact
placeholders, required direct endpoint and integration limits are in
[.env.example](../.env.example) and [the server guide](../apps/server/README.md).

## Environment grammar and selected propagation tradeoff

Low to high: application defaults → literal `.env.dev` → already-exported env.
The launcher pins the chosen JAVA_HOME, actual PWD, derived DB name and internal
LONGNEST_DEV_PREPARE_LEDGER=true. Other Maven/JVM/Spring configuration is trusted operator input, not an
authorization boundary; toolchains can affect Maven's application-JDK selection.

Unlike the previous package/JAR launcher, loaded runtime values deliberately reach
Maven/plugins and its child JVM. This is the selected simpler-dev-path tradeoff,
not a guarantee that plugins cannot read or log secrets. Avoid verbose/debug/
prompt/body/environment logging and untrusted plugins; no values are printed by
the launcher. No private file was read/edited during implementation checks.

Literal grammar remains: one case-sensitive NAME=value per line; blank lines and
whole-line comments allowed; LF/CRLF/no final newline supported. No duplicates,
export prefix, inline comments, BOM, NUL, controls, backslashes, multiline, escapes
or evaluation. Bare values contain no whitespace/quotes/#. Whole single/double
quotes preserve spaces, Unicode, #, = and the opposite quote literally, not the
matching quote. Dollar signs/backticks are literal, never interpolated/executed.
Invalid lines fail even when exported values override them. Do not source the file.

## Shutdown and verification limits

There is no background job-control phase runner or wait-f loop. Foreground Ctrl-C
reaches the normal process group; Maven's plugin owns its application child and
shutdown hook. Normal signal/child exit/lock release is checked using fake commands.
The application closes inference admission on context close, drains Telegram handling
for up to five seconds, then requests cancellation and SDK/HTTP cleanup. Boot's
phase budget is 20 seconds, with a five-second executor termination setting.
These are cooperative limits, not a guaranteed process deadline or live signal proof;
see [application shutdown](../apps/server/README.md#opt-in-telegram-sdk-boundary).
The pinned plugin calls destroy then waits, without forced escalation: an externally
SIGSTOPped/unresponsive child can still stall shutdown; SIGKILL of Maven can orphan
a child and release its lock. Confirm old processes are stopped before restarting;
do not treat a stale lock filename as a live lock or delete it to bypass exclusion.
No cross-checkout/distributed bot lease or overall Maven/startup deadline is added.

Ctrl-C or startup failure does not stop/remove PostgreSQL. Same-container stop/
start/restart preserves data; removal/recreation loses this disposable cluster.

Local checks (never invoke the real dev launcher):

```sh
bash -n scripts/dev-server.sh
node scripts/dev-server.test.mjs
```

Node tests use temporary synthetic env files and fake Java/Maven/Docker, including
one-parent/one-app-child signals and lock lifetime. Java context fixtures exercise
real database-initialization dependency detection and integration factories with
mocked migration/schema/service/registration ports. These are not real Docker,
PostgreSQL migration/durability or live Telegram/provider evidence.
