# Backend development launcher

`dev-server.sh` is a host-side backend launcher, not a container, database setup
or safe smoke command. It requires Bash 4.4+, GNU `env`, a Java 25 JDK and Maven
on the inherited `PATH`. It automatically packages/tests the backend, then runs its
executable JAR; no prebuilt artifact is required. It starts no other service.
The small root `scripts/` directory separates host tooling from Java and Compose.

## Usage

With private `.env.dev` configured as below, and **only with separately authorized
database connection/migration scope**:

```sh
# From the repository root:
bash scripts/dev-server.sh

# From any other directory (use the actual repository path):
bash /home/nghlong3004/projects/longnest/scripts/dev-server.sh
```

The launcher resolves root relative to itself and validates **only root `.env.dev`**.
It runs `mvn -B -ntp package` from `apps/server`, with tests enabled, then changes
working directory to root and execs Java with the packaged absolute JAR path and
`--spring.profiles.active=ledger`. No arguments are accepted or forwarded.
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
During packaging, INT/TERM is forwarded to Maven's separate process group
(including test JVMs); the launcher waits for the build to stop and exits 130/143
without launching the backend. Run in the foreground; backgrounding from a shell
that ignores SIGINT has that shell's normal inherited-signal limitations.

A readable `.env.dev` is required even when values are supplied via exported
environment. Users manage that private file themselves; never commit it, paste
secrets into chat, or run this launcher with shell tracing. Safe **synthetic-only**
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
Configuration checks do not establish reachability, credentials, a correct schema
or a successful live application start.

Normal startup **can contact the configured database and execute Flyway** from
`db/ledger`, followed by Hibernate schema validation. SQL initialization remains
disabled. There is no owner-head seed: authorized fixture head
initialization and save/replay/reopen verification are still required. The launcher
does not grant execution permission or prove a usable ledger. Use the separate
test-only no-database smoke for safe bootstrap checks, not this command.

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
read `.env.dev`. Compose does not pass these values to either process. Enabled
startup contacts Telegram and PostgreSQL; do not run it as a synthetic smoke.
No token, user identity, live startup or Telegram message has been configured here.

## Exact loading contract and precedence

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
use fake Maven/Java executables and a Maven-produced synthetic JAR, and invoke from outside root. They test
quoting, literal payloads, inherited precedence, missing/unreadable/invalid config,
build-before-run without a prebuilt JAR, build failure despite a stale JAR, build
environment isolation, exact invocation, exit status and SIGINT/SIGTERM during
build and runtime (runtime retains the launcher PID). No dependency
installation, real private env read, Java, DB, provider or container is involved.
The test prints its retained temporary fixture directory. Node is needed only for
these checks, not for running the Bash launcher. This is launcher evidence, not
a replacement for the prior accepted Java tests or PostgreSQL runtime evidence.
