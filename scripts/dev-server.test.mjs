import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import { once } from 'node:events';
import { chmodSync, copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const source = fileURLToPath(new URL('./dev-server.sh', import.meta.url));
const archive = mkdtempSync(path.join(tmpdir(), 'longnest-dev-simple-'));
console.log(`Synthetic fixtures retained at ${archive}`);
const valid = [
  'SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:54329/synthetic_only',
  'SPRING_DATASOURCE_USERNAME=synthetic_user',
  'SPRING_DATASOURCE_PASSWORD=synthetic_password',
  'LONGNEST_LEDGER_OWNER=synthetic_owner',
].join('\n');

function fixture(contents = valid, extra = {}) {
  const root = mkdtempSync(path.join(archive, 'repo with spaces-'));
  for (const name of ['scripts', 'apps/server', 'deploy', 'bin']) mkdirSync(path.join(root, name), { recursive: true });
  const script = path.join(root, 'scripts/dev-server.sh');
  copyFileSync(source, script);
  copyFileSync(fileURLToPath(new URL('../deploy/compose.dev.yaml', import.meta.url)), path.join(root, 'deploy/compose.dev.yaml'));
  if (contents !== null) writeFileSync(path.join(root, '.env.dev'), contents);
  writeFileSync(path.join(root, '.env'), 'never parse this private base file\n');
  const java = path.join(root, 'bin/java');
  writeFileSync(java, `#!${process.execPath}
if (process.argv[2] === '-version') {
  console.error('openjdk version "' + (process.env.STUB_JAVA_VERSION || '25.0.1') + '"'); process.exit(0);
}
if (process.env.STUB_APP_WAIT === 'yes') {
  console.log(JSON.stringify({ ready: true, appPid: process.pid }));
  setInterval(() => {}, 1000);
} else console.log(JSON.stringify({ env: process.env, cwd: process.cwd(), appPid: process.pid }));
`);
  chmodSync(java, 0o755);
  symlinkSync(java, path.join(root, 'bin/javac'));
  const maven = path.join(root, 'bin/mvn');
  writeFileSync(maven, `#!${process.execPath}
const fs = require('node:fs'), {spawn} = require('node:child_process');
const maven = { env: process.env, args: process.argv.slice(2), cwd: process.cwd(), pid: process.pid };
fs.writeFileSync('maven.json', JSON.stringify(maven));
if (process.env.STUB_MAVEN_EXIT) process.exit(Number(process.env.STUB_MAVEN_EXIT));
const child = spawn(process.env.JAVA_HOME + '/bin/java', ['synthetic-main'], {stdio: ['ignore', 'pipe', 'inherit']});
child.stdout.on('data', data => {
  for (const line of String(data).trim().split('\\n')) console.log(JSON.stringify({...JSON.parse(line), maven}));
});
let stopping;
for (const signal of ['SIGTERM', 'SIGINT']) process.on(signal, () => {
  stopping = signal;
  child.kill(signal);
  // Pinned Spring Boot RunProcess uses destroy() then waitFor(), not forced escalation.
});
child.on('exit', (code, signal) => process.exit(stopping === 'SIGINT' ? 130 : stopping ? 143 : code ?? 1));
`);
  chmodSync(maven, 0o755);
  const docker = path.join(root, 'bin/docker');
  writeFileSync(docker, `#!${process.execPath}
const fs = require('node:fs'), args = process.argv.slice(2);
fs.appendFileSync(${JSON.stringify(path.join(root, 'docker.jsonl'))}, JSON.stringify({args, env: process.env}) + '\\n');
const statePath = ${JSON.stringify(path.join(root, 'docker-state'))};
const state = fs.existsSync(statePath) ? fs.readFileSync(statePath, 'utf8') : process.env.STUB_DOCKER_STATE || 'running';
if (process.env.STUB_DOCKER_ERROR === 'yes') process.exit(1);
if (args[0] === 'context') { console.log(process.env.STUB_DOCKER_ENDPOINT || 'unix:///synthetic.sock'); process.exit(0); }
if (args[0] === 'inspect') {
  const bad = process.env.STUB_DOCKER_MISMATCH || '';
  console.log([state, bad === 'image' ? 'foreign-image' : 'postgres:18.6-bookworm',
    bad === 'port' ? '0.0.0.0:54329' : '127.0.0.1:54329', bad === 'mount' ? 'mounted' : '',
    'POSTGRES_DB=' + (bad === 'db' ? 'foreign' : process.env.LONGNEST_DEV_DB_NAME),
    'POSTGRES_USER=' + (bad === 'user' ? 'foreign' : process.env.SPRING_DATASOURCE_USERNAME),
    'POSTGRES_PASSWORD=' + (bad === 'password' ? 'DO_NOT_DISCLOSE' : process.env.SPRING_DATASOURCE_PASSWORD),
    'PGDATA=/tmp/longnest-pgdata', 'POSTGRES_INITDB_ARGS=--encoding=UTF8 --locale=C'].join('\\n'));
} else if (args.includes('--images')) console.log('postgres:18.6-bookworm');
else if (args.includes('ps')) { if (state !== 'absent') console.log('a'.repeat(64)); }
else if (args.includes('up')) fs.writeFileSync(statePath, 'running');
else if (args.includes('pg_isready')) {
  if (process.env.STUB_READY_ERROR === 'yes') process.exit(126);
  if (process.env.STUB_READY_WAIT === 'yes') { process.on('SIGTERM', () => {}); setInterval(() => {}, 1000); }
  else if (process.env.STUB_NOT_READY === 'yes' || (process.env.STUB_NOT_READY_ONCE === 'yes' && !fs.existsSync(statePath + '-checked'))) {
    fs.writeFileSync(statePath + '-checked', 'yes'); console.log('synthetic: not ready'); process.exit(1);
  } else console.log('synthetic: accepting connections');
} else process.exit(95);
`);
  chmodSync(docker, 0o755);
  return { root, script, env: { PATH: `${root}/bin:/usr/bin:/bin`, LANG: 'C.UTF-8', ...extra } };
}
function run(f, args = []) {
  return spawnSync('/bin/bash', [f.script, ...args], { cwd: '/tmp', env: f.env, encoding: 'utf8', timeout: 6000 });
}
function success(f) {
  const result = run(f);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stderr, /PostgreSQL ready/);
  assert.match(result.stderr, /runtime env reaches Maven\/plugins/);
  assert.doesNotMatch(result.stderr, /synthetic_password|DO_NOT_DISCLOSE/);
  return JSON.parse(result.stdout);
}
function failure(f, message) {
  const result = run(f);
  assert.equal(result.status, 1, result.stderr);
  assert.equal(result.stdout, '');
  assert.match(result.stderr, message);
  assert.doesNotMatch(result.stderr, /synthetic_password|DO_NOT_DISCLOSE/);
}
function dockerCalls(f) {
  return readFileSync(path.join(f.root, 'docker.jsonl'), 'utf8').trim().split('\n').map(JSON.parse);
}

test('one foreground Maven run from backend cwd; no package, tests, JAR or init JVM', () => {
  const f = fixture();
  const actual = success(f);
  assert.equal(actual.maven.cwd, `${f.root}/apps/server`);
  assert.deepEqual(actual.maven.args, ['-B', '-ntp', 'spring-boot:run', '-Dspring-boot.run.profiles=ledger']);
  assert.equal(actual.env.LONGNEST_DEV_PREPARE_LEDGER, 'true');
  assert.equal(existsSync(path.join(f.root, 'apps/server/target')), false);
  assert.equal(actual.maven.env.JAVA_HOME, f.root);
  assert.equal(actual.env.SPRING_DATASOURCE_PASSWORD, 'synthetic_password');
  assert.equal(actual.maven.env.SPRING_DATASOURCE_PASSWORD, 'synthetic_password');
  assert.equal(actual.env.PWD, `${f.root}/apps/server`);
});
test('literal file runtime values and Maven options now deliberately reach Maven/plugins', () => {
  const actual = success(fixture(`${valid}\nMAVEN_OPTS=-Dfile-only\nTOKEN=synthetic-file-only`));
  assert.equal(actual.maven.env.MAVEN_OPTS, '-Dfile-only');
  assert.equal(actual.maven.env.TOKEN, 'synthetic-file-only');
});
test('Maven failure status propagates without a stale-JAR fallback', () => {
  const f = fixture(valid, { STUB_MAVEN_EXIT: '23' });
  assert.equal(run(f).status, 23);
});
test('quotes/comments/CRLF/empty/Unicode/no-final-newline retain literal values', () => {
  const actual = success(fixture(`${valid}\n # comment\nDOUBLE="hello # = ' Việt"\nSINGLE='literal " value'\nEMPTY=\nURL=a=b:c/123`.replaceAll('\n', '\r\n')));
  assert.equal(actual.env.DOUBLE, "hello # = ' Việt");
  assert.equal(actual.env.SINGLE, 'literal " value');
  assert.equal(actual.env.EMPTY, '');
  assert.equal(actual.env.URL, 'a=b:c/123');
});
test('substitution/backticks/variables are literal and never executed', () => {
  const f = fixture();
  const marker = path.join(f.root, 'must-not-exist');
  writeFileSync(path.join(f.root, '.env.dev'), `${valid}\nPAYLOAD="$(touch '${marker}') $HOME \`id\`"`);
  assert.equal(success(f).env.PAYLOAD, `$(touch '${marker}') $HOME \`id\``);
  assert(!existsSync(marker));
});
test('exported values override file, including empty optional values', () => {
  const actual = success(fixture(`${valid}\nOPTIONAL=file`, { SPRING_DATASOURCE_PASSWORD: 'inherited-password', OPTIONAL: '' }));
  assert.equal(actual.env.SPRING_DATASOURCE_PASSWORD, 'inherited-password');
  assert.equal(actual.env.OPTIONAL, '');
});
test('empty file accepts exported required values while dev readiness stays pinned on', () => {
  const inherited = Object.fromEntries(valid.split('\n').map(line => line.split('=')));
  const actual = success(fixture('', { ...inherited, LONGNEST_DEV_PREPARE_LEDGER: 'false' }));
  assert.equal(actual.env.LONGNEST_DEV_PREPARE_LEDGER, 'true');
  assert.equal(actual.env.LONGNEST_LEDGER_OWNER, 'synthetic_owner');
});
test('explicit file/inherited JAVA_HOME selects only an existing Java25 JDK', () => {
  const f = fixture();
  writeFileSync(path.join(f.root, '.env.dev'), `${valid}\nJAVA_HOME="${f.root}"`);
  assert.equal(success(f).maven.env.JAVA_HOME, f.root);
  f.env.JAVA_HOME = '/not-an-installed-jdk';
  failure(f, /JAVA_HOME must point/);
});
for (const version of ['21.0.1', '24', '26', '250.1']) test(`reject Java${version} before Docker/Maven`, () => {
  const f = fixture(valid, { STUB_JAVA_VERSION: version });
  failure(f, /must be version 25/);
  assert(!existsSync(path.join(f.root, 'docker.jsonl')));
});
test('file PATH cannot replace selected executables', () => {
  const f = fixture(`${valid}\nPATH=/not-a-real-path`);
  assert.equal(success(f).maven.env.JAVA_HOME, f.root);
});
for (const name of ['SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'LONGNEST_LEDGER_OWNER']) {
  test(`missing ${name} fails before tools`, () => failure(fixture(valid.split('\n').filter(line => !line.startsWith(name + '=')).join('\n')), new RegExp(name)));
  test(`empty inherited ${name} never falls back`, () => failure(fixture(valid, { [name]: '' }), new RegExp(name)));
}
for (const [label, content] of [
  ['export', 'export KEY=DO_NOT_DISCLOSE'], ['statement', 'touch DO_NOT_DISCLOSE'],
  ['name', 'BAD-NAME=DO_NOT_DISCLOSE'], ['inline comment', 'KEY=DO_NOT_DISCLOSE # comment'],
  ['unquoted spaces', 'KEY=DO_NOT_DISCLOSE value'], ['quote', 'KEY="DO_NOT_DISCLOSE'],
  ['multiline', 'KEY="DO_NOT_DISCLOSE\nmore"'], ['backslash', 'KEY="DO_NOT_DISCLOSE\\n"'],
  ['control', 'KEY=DO_NOT_DISCLOSE\u0001'], ['NUL', 'KEY=DO_NOT_DISCLOSE\u0000'],
  ['duplicate', 'KEY=one\nKEY=DO_NOT_DISCLOSE'], ['BOM', '\ufeffKEY=DO_NOT_DISCLOSE'],
]) test(`reject ${label} without disclosing values`, () => failure(fixture(`${valid}\n${content}`), /\.env\.dev/));
test('missing env file and conflicting profile fail clearly', () => {
  failure(fixture(null), /readable regular file/);
  failure(fixture(`${valid}\nSPRING_PROFILES_ACTIVE=DO_NOT_DISCLOSE`), /must be ledger/);
  assert.equal(run(fixture(), ['init']).status, 1);
});
test('missing Java fails without finding host Java by accident', () => {
  const f = fixture();
  const tools = path.join(f.root, 'isolated-tools'); mkdirSync(tools);
  for (const name of ['dirname', 'env']) symlinkSync('/usr/bin/' + name, path.join(tools, name));
  f.env.PATH = tools;
  failure(f, /Java 25 must be available/);
});
test('Compose paths, credentials and readiness identifiers use effective app configuration', () => {
  const f = fixture(valid, { SPRING_DATASOURCE_USERNAME: 'override_user', SPRING_DATASOURCE_PASSWORD: 'override_password' }); success(f);
  for (const {args, env} of dockerCalls(f)) {
    assert(!args.some(arg => arg.includes('password')));
    if (args[0] !== 'compose') continue;
    assert.deepEqual(args.slice(0, 7), ['compose', '--env-file', `${f.root}/.env.dev`, '-p', 'longnest-dev', '-f', `${f.root}/deploy/compose.dev.yaml`]);
    assert.equal(env.SPRING_DATASOURCE_PASSWORD, 'override_password');
    assert.equal(env.LONGNEST_DEV_DB_NAME, 'synthetic_only');
  }
  assert.deepEqual(dockerCalls(f).find(call => call.args.includes('pg_isready')).args.slice(-8), ['exec', '-T', 'postgres', 'pg_isready', '-U', 'override_user', '-d', 'synthetic_only']);
});
for (const state of ['absent', 'exited', 'running']) test(`reuse/start ${state} service without recreation`, () => {
  const f = fixture(valid, { STUB_DOCKER_STATE: state }); success(f);
  const ups = dockerCalls(f).filter(call => call.args.includes('up'));
  assert.equal(ups.length, state === 'running' ? 0 : 1);
  if (ups.length) assert.deepEqual(ups[0].args.slice(-4), ['up', '-d', '--no-recreate', 'postgres']);
});
for (const mismatch of ['db', 'user', 'password', 'image', 'port', 'mount']) test(`container ${mismatch} mismatch never starts Maven`, () => {
  const f = fixture(valid, { STUB_DOCKER_MISMATCH: mismatch }); failure(f, /differs/);
  assert(!dockerCalls(f).some(call => call.args.includes('up')));
  assert(!existsSync(path.join(f.root, 'apps/server/maven.json')));
});
test('unready running service is polled, not restarted', () => {
  const f = fixture(valid, { STUB_NOT_READY_ONCE: 'yes' }); success(f);
  assert.equal(dockerCalls(f).filter(call => call.args.includes('pg_isready')).length, 2);
  assert(!dockerCalls(f).some(call => call.args.includes('up')));
});
test('Docker access/exec errors are not absence', () => {
  for (const extra of [{ STUB_DOCKER_ERROR: 'yes' }, { STUB_READY_ERROR: 'yes' }]) {
    const f = fixture(valid, extra); failure(f, /Cannot inspect Docker|readiness command failed/);
    assert(!dockerCalls(f).some(call => call.args.includes('up')));
  }
});
test('remote/ambiguous JDBC and remote Docker endpoints are refused', () => {
  for (const url of ['jdbc:postgresql://remote:54329/data', 'jdbc:postgresql://127.0.0.1:5432/data', 'jdbc:postgresql://127.0.0.1:54329/data?user=other']) failure(fixture(valid, { SPRING_DATASOURCE_URL: url }), /Managed dev database requires/);
  failure(fixture(valid, { STUB_DOCKER_ENDPOINT: 'ssh://remote' }), /local Docker context/);
});
test('env names cannot overwrite validated command/database/timeout controls', () => {
  const values = { dev_database: 'wrong_db', compose_timeout: '0', timeout_path: '/invalid/timeout', docker_path: '/invalid/docker', maven_path: '/invalid/maven', dev_java_home: '/invalid/jdk', assignments: 'literal-array', inherited: 'literal-map', inherited_environment: 'literal-list', file_values: 'literal-file', container_lines: 'literal-lines', container_env: 'literal-env' };
  for (const inherited of [true, false]) {
    const f = fixture(inherited ? valid : valid + '\n' + Object.entries(values).map(([k,v]) => `${k}=${v}`).join('\n'), inherited ? values : {});
    const actual = success(f);
    for (const [key,value] of Object.entries(values)) assert.equal(actual.maven.env[key], value, key);
    for (const {env} of dockerCalls(f)) assert.equal(env.LONGNEST_DEV_DB_NAME, 'synthetic_only');
  }
});
for (const ignoreTerm of [false, true]) test(`bounded readiness${ignoreTerm ? ' with TERM-ignoring client' : ''}`, () => {
  const f = fixture(valid, ignoreTerm ? { STUB_READY_WAIT: 'yes' } : { STUB_NOT_READY: 'yes' });
  const original = readFileSync(f.script, 'utf8'); assert(original.includes('ready_deadline=$((SECONDS + 60))'));
  writeFileSync(f.script, original.replace('ready_deadline=$((SECONDS + 60))', 'ready_deadline=$((SECONDS + 1))'));
  failure(f, /readiness deadline exceeded|readiness command failed/);
  assert(!existsSync(path.join(f.root, 'apps/server/maven.json')));
});
for (const signal of ['SIGINT', 'SIGTERM']) test(`foreground ${signal} terminates Maven/app child and releases checkout lock`, { timeout: 10000 }, async () => {
  const f = fixture(valid, { STUB_APP_WAIT: 'yes' });
  const child = spawn('/bin/bash', [f.script], { cwd: '/tmp', env: f.env, detached: true });
  const closed = once(child, 'close', { signal: AbortSignal.timeout(7000) });
  let appPid;
  try {
    const [data] = await once(child.stdout, 'data', { signal: AbortSignal.timeout(3000) });
    const ready = JSON.parse(String(data)); appPid = ready.appPid;
    assert.equal(ready.maven.pid, child.pid, 'launcher execs Maven, no shell parent remains');
    assert.notEqual(appPid, child.pid);
    failure(f, /already running/);
    process.kill(-child.pid, signal);
    const [status] = await closed;
    assert.equal(status, signal === 'SIGINT' ? 130 : 143);
    assert.throws(() => process.kill(appPid, 0), { code: 'ESRCH' });
    f.env.STUB_APP_WAIT = 'no'; success(f);
  } finally { try { process.kill(-child.pid, 'SIGKILL'); } catch {} }
});
test('externally stopped child needs explicit continuation; no automatic-recovery claim', { timeout: 10000 }, async () => {
  const f = fixture(valid, { STUB_APP_WAIT: 'yes' });
  const child = spawn('/bin/bash', [f.script], { cwd: '/tmp', env: f.env, detached: true });
  const closed = once(child, 'close', { signal: AbortSignal.timeout(7000) });
  let appPid;
  try {
    const [data] = await once(child.stdout, 'data', { signal: AbortSignal.timeout(3000) });
    appPid = JSON.parse(String(data)).appPid;
    process.kill(appPid, 'SIGSTOP'); child.kill('SIGTERM');
    await new Promise(resolve => setTimeout(resolve, 100));
    assert.equal(child.exitCode, null, 'plugin-model shutdown waits for the stopped child');
    process.kill(appPid, 'SIGCONT'); // Only the exact synthetic child created by this fixture.
    assert.equal((await closed)[0], 143);
    assert.throws(() => process.kill(appPid, 0), { code: 'ESRCH' });
  } finally { try { process.kill(-child.pid, 'SIGKILL'); } catch {} }
});
