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
jar_path="$repo_root/apps/server/target/longnest-0.0.1-SNAPSHOT.jar"
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
# Resolve tools before applying file environment (e.g. PATH).
java_path=$(type -P java) || fail 'Java 25 must be available on the inherited PATH.'
maven_path=$(type -P mvn) || fail 'Maven must be available on the inherited PATH.'
flock_path=$(type -P flock) || fail 'flock (util-linux) must be available on the inherited PATH.'
docker_path=$(type -P docker) || fail 'Docker Engine and Compose must already be available.'
timeout_path=$(type -P timeout) || fail 'GNU timeout must be available on the inherited PATH.'
compose_timeout=30
# Never unlink this lock file while a launcher/application may still hold it.
exec 9>"$repo_root/.dev-server.lock"
"$flock_path" -n 9 || fail 'Another dev launcher/backend is already running for this checkout.'

docker_cli() (
  # Freeze controls before importing arbitrary env names; positional parameters cannot collide.
  set -- "${inherited_environment[@]}" "${assignments[@]}" "LONGNEST_DEV_DB_NAME=$dev_database" \
    -- "$timeout_path" --foreground --kill-after=2 "$compose_timeout" "$docker_path" "$@"
  unset inherited_environment inherited file_values assignments container_lines container_env
  # Builtin export keeps private values out of process arguments, with no script array attributes.
  while [[ "$1" != -- ]]; do
    export "$1" 2>/dev/null || fail 'Cannot preserve Docker command environment.'
    shift
  done
  shift
  exec "$@"
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
  docker_endpoint=$(docker_cli context inspect --format '{{.Endpoints.docker.Host}}' 2>/dev/null) \
    || fail 'Cannot inspect Docker context; Docker Engine must already be available.'
  [[ "$docker_endpoint" == unix://* ]] || fail 'Managed dev PostgreSQL requires a local Docker context.'
  dev_image=$(compose config --images 2>/dev/null) \
    || fail 'Cannot resolve dev Compose configuration; check Docker/Compose and configuration.'
  [[ -n "$dev_image" ]] || fail 'Dev PostgreSQL image is missing.'
  container_state=''
  inspect_postgres
  if [[ -z "$container_id" || "$container_state" == exited || "$container_state" == created ]]; then
    compose_timeout=180 # Allow a bounded initial image pull without changing host infrastructure.
    compose up -d --no-recreate postgres >/dev/null 2>&1 \
      || fail 'Cannot start dev PostgreSQL; check Docker access, port and image availability.'
    compose_timeout=30
    inspect_postgres
  fi
  [[ -n "$container_id" && "$container_state" == running ]] \
    || fail 'Dev PostgreSQL is not running; no reset or recreation attempted.'
  ready_deadline=$((SECONDS + 60))
  while (( SECONDS < ready_deadline )); do
    compose_timeout=$((ready_deadline - SECONDS))
    if ready_output=$(compose exec -T postgres pg_isready -U "$dev_user" -d "$dev_database" 2>/dev/null); then
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
cd -- "$repo_root/apps/server"
# A separate job group lets INT/TERM stop Maven and its test JVMs, not just this shell.
stop_build() {
  trap '' INT TERM
  if [[ -n "${build_pid:-}" ]]; then
    kill -s "$1" -- "-$build_pid" 2>/dev/null || :
    kill -CONT -- "-$build_pid" 2>/dev/null || :
    wait -f "$build_pid" || :
  fi
  printf 'dev-server: %s interrupted; backend not started.\n' "$phase_label" >&2
  exit "$2"
}
run_phase() {
  phase_label=$1
  shift
  build_pid=''
  trap 'stop_build INT 130' INT
  trap 'stop_build TERM 143' TERM
  set -m
  "$@" &
  build_pid=$!
  # With job control, plain wait can report a stop instead of process termination.
  if wait -f "$build_pid"; then phase_status=0; else phase_status=$?; fi
  trap - INT TERM
  set +m
}
# Do not give file-loaded runtime values to Maven, including tests/plugins.
run_phase Build env -i -- "${inherited_environment[@]}" "PWD=$repo_root/apps/server" \
  "$maven_path" -B -ntp package
build_status=$phase_status
if (( build_status != 0 )); then
  printf 'dev-server: Maven package failed; backend not started. Exit status: %s.\n' "$build_status" >&2
  exit "$build_status"
fi
[[ -f "$jar_path" && -r "$jar_path" ]] || fail 'Maven package did not produce the expected readable executable JAR.'
cd -- "$repo_root"
run_phase 'PostgreSQL startup' prepare_postgres
if (( phase_status != 0 )); then exit "$phase_status"; fi
# Migrate/validate and prepare in a separate one-shot process with both integrations excluded.
run_phase 'Ledger preparation' env -i -- "${inherited_environment[@]}" "${assignments[@]}" \
  "PWD=$repo_root" "$java_path" -jar "$jar_path" --spring.profiles.active=ledger-init \
  --longnest.ledger.initialize-if-absent=true
if (( phase_status != 0 )); then
  printf 'dev-server: Ledger preparation failed; backend not started. Inspect storage before retry.\n' >&2
  exit "$phase_status"
fi
exec env -i -- "${inherited_environment[@]}" "${assignments[@]}" "PWD=$repo_root" \
  "$java_path" -jar "$jar_path" --spring.profiles.active=ledger
