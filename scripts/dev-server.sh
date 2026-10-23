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
# Resolve tools before applying file environment (e.g. PATH).
java_path=$(type -P java) || fail 'Java 25 must be available on the inherited PATH.'
maven_path=$(type -P mvn) || fail 'Maven must be available on the inherited PATH.'
cd -- "$repo_root/apps/server"
# A separate job group lets INT/TERM stop Maven and its test JVMs, not just this shell.
stop_build() {
  trap '' INT TERM
  if [[ -n "${build_pid:-}" ]]; then
    kill -s "$1" -- "-$build_pid" 2>/dev/null || :
    wait "$build_pid" || :
  fi
  printf 'dev-server: Build interrupted; backend not started.\n' >&2
  exit "$2"
}
build_pid=''
trap 'stop_build INT 130' INT
trap 'stop_build TERM 143' TERM
set -m
# Do not give file-loaded runtime values to Maven, including tests/plugins.
env -i -- "${inherited_environment[@]}" "PWD=$repo_root/apps/server" \
  "$maven_path" -B -ntp package &
build_pid=$!
if wait "$build_pid"; then build_status=0; else build_status=$?; fi
trap - INT TERM
set +m
if (( build_status != 0 )); then
  printf 'dev-server: Maven package failed; backend not started.\n' >&2
  exit "$build_status"
fi
[[ -f "$jar_path" && -r "$jar_path" ]] || fail 'Maven package did not produce the expected readable executable JAR.'
cd -- "$repo_root"
exec env -i -- "${inherited_environment[@]}" "${assignments[@]}" "PWD=$repo_root" \
  "$java_path" -jar "$jar_path" --spring.profiles.active=ledger
