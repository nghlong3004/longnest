#!/usr/bin/env bash
# Bash 4.4+; literal dotenv subset only. Never source/eval the development file.
set -euo pipefail

fail() { printf 'dev-server: %s\n' "$1" >&2; exit 1; }
(( BASH_VERSINFO[0] > 4 || (BASH_VERSINFO[0] == 4 && BASH_VERSINFO[1] >= 4) )) \
  || fail 'Bash 4.4 or newer is required.'
(( $# == 0 )) || fail 'No arguments supported; configure via .env.dev or exported environment.'
# Snapshot before assigning script variables, including names a caller also exports.
mapfile -d '' inherited_environment < <(env -0)
repo_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
env_path="$repo_root/.env.dev"
[[ -f "$env_path" && -r "$env_path" ]] || fail 'Repository-root .env.dev must be a readable regular file.'

# Read once and reject NUL rather than letting Bash silently discard it.
if IFS= read -r -d '' dotenv_text < "$env_path"; then
  fail '.env.dev contains a NUL byte.'
fi
declare -A file_values=() inherited=()
for exported_entry in "${inherited_environment[@]}"; do
  inherited["${exported_entry%%=*}"]=${exported_entry#*=}
done
line_number=0
while IFS= read -r line; do
  (( line_number += 1 ))
  line=${line%$'\r'}
  [[ "$line" =~ ^[[:blank:]]*(#.*)?$ ]] && continue
  [[ "$line" =~ ^[[:blank:]]*([A-Za-z_][A-Za-z0-9_]*)[[:blank:]]*=[[:blank:]]*(.*)$ ]] \
    || fail "Invalid .env.dev assignment at line $line_number."
  name=${BASH_REMATCH[1]}
  value=${BASH_REMATCH[2]}
  [[ ! -v file_values[$name] ]] || fail "Duplicate .env.dev key at line $line_number."
  # Trim only outer spaces/tabs; quoted contents are literal, including $ and `.
  value="${value%"${value##*[!$' \t']}"}"
  [[ "$value" != *'\'* && ! "$value" =~ [[:cntrl:]] ]] \
    || fail "Unsupported .env.dev character at line $line_number."
  if [[ "$value" =~ ^\"[^\"]*\"$ || "$value" =~ ^\'[^\']*\'$ ]]; then
    value=${value:1:${#value}-2}
  elif [[ "$value" == *[[:space:]\"\'#]* ]]; then
    fail "Invalid .env.dev quoting at line $line_number."
  fi
  file_values["$name"]=$value
done <<< "$dotenv_text"

declare -a assignments=()
for name in "${!file_values[@]}"; do
  [[ -v inherited[$name] ]] || assignments+=("$name=${file_values[$name]}")
done
for name in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD LONGNEST_LEDGER_OWNER; do
  if [[ -v inherited[$name] ]]; then value=${inherited[$name]}; else value=${file_values[$name]-}; fi
  [[ "$value" =~ [^[:space:]] ]] || fail "Missing or blank required setting: $name."
done
name=SPRING_PROFILES_ACTIVE
if [[ -v inherited[$name] ]]; then value=${inherited[$name]}; else value=${file_values[$name]-ledger}; fi
[[ "$value" == ledger ]] || fail 'SPRING_PROFILES_ACTIVE must be ledger for this launcher.'
# One local endpoint only; never infer a managed database from an arbitrary JDBC URL.
effective() { if [[ -v inherited[$1] ]]; then REPLY=${inherited[$1]}; else REPLY=${file_values[$1]-}; fi; }
effective SPRING_DATASOURCE_URL
[[ "$REPLY" =~ ^jdbc:postgresql://127\.0\.0\.1:54329/([a-zA-Z_][a-zA-Z0-9_]{0,62})$ ]] \
  || fail 'Managed dev database requires jdbc:postgresql://127.0.0.1:54329/NAME (no URL options).'
dev_database=${BASH_REMATCH[1]}
effective SPRING_DATASOURCE_USERNAME
dev_user=$REPLY
[[ "$dev_user" =~ ^[a-zA-Z_][a-zA-Z0-9_]{0,62}$ ]] || fail 'Unsupported managed dev database username.'
effective SPRING_DATASOURCE_PASSWORD
dev_password=$REPLY
effective LONGNEST_DEV_DB_NAME
[[ -z "$REPLY" || "$REPLY" == "$dev_database" ]] || fail 'Conflicting managed database name.'
effective DOCKER_HOST
[[ -z "$REPLY" || "$REPLY" == unix://* ]] || fail 'Managed dev PostgreSQL requires a local Docker endpoint.'
# Select one installed JDK without executing SDKMAN/project configuration.
effective JAVA_HOME
if [[ -n "$REPLY" ]]; then
  dev_java_home=$(cd -- "$REPLY" 2>/dev/null && pwd -P) \
    || fail 'JAVA_HOME must point to an installed Java 25 JDK.'
  java_path="$dev_java_home/bin/java"
  [[ -x "$java_path" ]] || fail 'JAVA_HOME must point to an installed Java 25 JDK.'
else
  java_path=$(type -P java) || fail 'Java 25 must be available on the inherited PATH.'
  java_path=$(readlink -f -- "$java_path")
  dev_java_home=$(dirname -- "$(dirname -- "$java_path")")
fi
[[ -x "$dev_java_home/bin/javac" ]] || fail 'The selected Java installation must include javac (JDK required).'
java_version=$("$java_path" -version 2>&1) || fail 'Cannot inspect the selected Java installation.'
[[ "$java_version" =~ version[[:space:]]+\"25([.+\"-]) ]] \
  || fail 'The selected installed Java must be version 25.'
maven_path=$(type -P mvn) || fail 'Maven must be available on the inherited PATH.'
flock_path=$(type -P flock) || fail 'flock (util-linux) must be available on the inherited PATH.'
docker_path=$(type -P docker) || fail 'Docker Engine and Compose must already be available.'
timeout_path=$(type -P timeout) || fail 'GNU timeout must be available on the inherited PATH.'
compose_timeout=30
# Never unlink this lock file while a launcher/application may still hold it.
exec 9>"$repo_root/.dev-server.lock"
"$flock_path" -n 9 || fail 'Another dev launcher/backend is already running for this checkout.'
printf 'dev-server: Configuration loaded; installed Java 25 selected.\n' >&2

exec_with_env() {
  # Freeze controls before importing arbitrary env names; positional parameters cannot collide.
  set -- "${inherited_environment[@]}" "${assignments[@]}" "LONGNEST_DEV_DB_NAME=$dev_database" \
    "JAVA_HOME=$dev_java_home" "PWD=$PWD" "LONGNEST_DEV_PREPARE_LEDGER=true" -- "$@"
  unset inherited_environment inherited file_values assignments container_lines container_env
  # Builtin export keeps private values out of process arguments, with no script array attributes.
  while [[ "$1" != -- ]]; do
    export "$1" 2>/dev/null || fail 'Cannot preserve command environment.'
    shift
  done
  shift
  exec "$@"
}

docker_cli() (
  exec_with_env "$timeout_path" --foreground --kill-after=2 "$compose_timeout" "$docker_path" "$@"
)

compose() {
  docker_cli compose --env-file "$env_path" -p longnest-dev \
    -f "$repo_root/deploy/compose.dev.yaml" "$@"
}

inspect_postgres() {
  container_id=$(compose ps --all --quiet postgres 2>/dev/null) \
    || fail 'Cannot inspect dev PostgreSQL service; check Docker/Compose access.'
  [[ -n "$container_id" ]] || return 0
  [[ "$container_id" =~ ^[a-f0-9]{12,64}$ ]] || fail 'Expected one dev PostgreSQL container.'
  container_info=$(docker_cli inspect --format \
    '{{.State.Status}}{{println}}{{.Config.Image}}{{println}}{{range (index .HostConfig.PortBindings "5432/tcp")}}{{.HostIp}}:{{.HostPort}}{{end}}{{println}}{{range .Mounts}}{{if or (ne .Destination "/var/lib/postgresql") (ne .Type "volume")}}mounted{{end}}{{end}}{{println}}{{range .Config.Env}}{{println .}}{{end}}' \
    "$container_id" 2>/dev/null) || fail 'Cannot inspect existing dev PostgreSQL container.'
  mapfile -t container_lines <<< "$container_info"
  container_state=${container_lines[0]-}
  [[ "${container_lines[1]-}" == "$dev_image" && "${container_lines[2]-}" == '127.0.0.1:54329' \
      && -z "${container_lines[3]-}" ]] || fail 'Existing dev PostgreSQL image/port/storage differs; not changing it.'
  declare -A container_env=()
  for setting in "${container_lines[@]:4}"; do
    [[ "$setting" == *=* ]] && container_env["${setting%%=*}"]=${setting#*=}
  done
  [[ "${container_env[POSTGRES_DB]-}" == "$dev_database" \
      && "${container_env[POSTGRES_USER]-}" == "$dev_user" \
      && "${container_env[POSTGRES_PASSWORD]-}" == "$dev_password" \
      && "${container_env[PGDATA]-}" == /tmp/longnest-pgdata \
      && "${container_env[POSTGRES_INITDB_ARGS]-}" == '--encoding=UTF8 --locale=C' ]] \
    || fail 'Existing dev PostgreSQL configuration differs; not changing it.'
}

prepare_postgres() {
  printf 'dev-server: Checking development PostgreSQL configuration.\n' >&2
  docker_endpoint=$(docker_cli context inspect --format '{{.Endpoints.docker.Host}}' 2>/dev/null) \
    || fail 'Cannot inspect Docker context; Docker Engine must already be available.'
  [[ "$docker_endpoint" == unix://* ]] || fail 'Managed dev PostgreSQL requires a local Docker context.'
  dev_image=$(compose config --images 2>/dev/null) \
    || fail 'Cannot resolve dev Compose configuration; check Docker/Compose and configuration.'
  [[ -n "$dev_image" ]] || fail 'Dev PostgreSQL image is missing.'
  container_state=''
  inspect_postgres
  if [[ -z "$container_id" || "$container_state" == exited || "$container_state" == created ]]; then
    printf 'dev-server: Starting development PostgreSQL without recreation (up to 180s).\n' >&2
    compose_timeout=180 # Allow a bounded initial image pull without changing host infrastructure.
    compose up -d --no-recreate postgres >/dev/null 2>&1 \
      || fail 'Cannot start dev PostgreSQL; check Docker access, port and image availability.'
    compose_timeout=30
    inspect_postgres
  fi
  [[ -n "$container_id" && "$container_state" == running ]] \
    || fail 'Dev PostgreSQL is not running; no reset or recreation attempted.'
  ready_deadline=$((SECONDS + 60))
  printf 'dev-server: Waiting for PostgreSQL readiness (up to 60s).\n' >&2
  while (( SECONDS < ready_deadline )); do
    compose_timeout=$((ready_deadline - SECONDS))
    if ready_output=$(compose exec -T postgres pg_isready -U "$dev_user" -d "$dev_database" 2>/dev/null); then
      printf 'dev-server: PostgreSQL ready.\n' >&2
      return 0
    else
      ready_status=$?
    fi
    [[ "$ready_status" == 1 || "$ready_status" == 2 ]] && [[ -n "$ready_output" ]] \
      || fail 'PostgreSQL readiness command failed; no restart or recreation attempted.'
    sleep 1
  done
  fail 'PostgreSQL readiness deadline exceeded; backend not started.'
}
prepare_postgres
cd -- "$repo_root/apps/server"
printf 'dev-server: Starting foreground Maven/one application; runtime env reaches Maven/plugins.\n' >&2
# Only this dev path opts into the early database-ready bean; command profiles remain ledger.
exec_with_env "$maven_path" -B -ntp spring-boot:run -Dspring-boot.run.profiles=ledger
