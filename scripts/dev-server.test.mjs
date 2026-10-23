import assert from 'node:assert/strict';
import { spawn, spawnSync } from 'node:child_process';
import { once } from 'node:events';
import { chmodSync, copyFileSync, existsSync, mkdirSync, mkdtempSync, readFileSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const source = fileURLToPath(new URL('./dev-server.sh', import.meta.url));
const archive = mkdtempSync(path.join(tmpdir(), 'longnest-dev-start-'));
console.log(`Synthetic fixtures retained at ${archive}`);
const valid = [
  'SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:54329/synthetic_only',
  'SPRING_DATASOURCE_USERNAME=synthetic_user',
  'SPRING_DATASOURCE_PASSWORD=synthetic_password',
  'LONGNEST_LEDGER_OWNER=synthetic_owner',
].join('\n');

function fixture(contents = valid, extra = {}) {
  const root = mkdtempSync(path.join(archive, 'repo with spaces-'));
  mkdirSync(path.join(root, 'scripts'));
  mkdirSync(path.join(root, 'apps/server'), { recursive: true });
  mkdirSync(path.join(root, 'deploy'));
  copyFileSync(fileURLToPath(new URL('../deploy/compose.dev.yaml', import.meta.url)), path.join(root, 'deploy/compose.dev.yaml'));
  mkdirSync(path.join(root, 'bin'));
  const script = path.join(root, 'scripts/dev-server.sh');
  copyFileSync(source, script);
  if (contents !== null) writeFileSync(path.join(root, '.env.dev'), contents);
  // Poison base file must never be parsed or executed by the launcher.
  writeFileSync(path.join(root, '.env'), 'not a supported dotenv assignment\n');
  const maven = path.join(root, 'bin/mvn');
  writeFileSync(maven, `#!${process.execPath}
const { writeSync, writeFileSync, mkdirSync } = require('node:fs');
writeFileSync('build.json', JSON.stringify({ env: process.env, args: process.argv.slice(2), cwd: process.cwd(), pid: process.pid }));
if (process.env.STUB_BUILD_STOP === 'yes') {
  writeSync(1, 'BUILD_STOP ' + process.pid + '\\n');
  process.kill(process.pid, 'SIGSTOP');
}
if (process.env.STUB_BUILD_WAIT === 'yes') {
  writeSync(1, 'BUILD_READY ' + process.pid + '\\n');
  setInterval(() => {}, 1000);
} else {
  const status = Number(process.env.STUB_BUILD_EXIT || 0);
  if (process.env.STUB_BUILD_SUCCESS_TEXT === 'yes') writeSync(1, 'BUILD SUCCESS\\n');
  if (status === 0 && process.env.STUB_BUILD_NO_JAR !== 'yes') {
    mkdirSync('target', { recursive: true });
    writeFileSync('target/longnest-0.0.1-SNAPSHOT.jar', 'fresh-built-synthetic');
  }
  process.exit(status);
}
`);
  chmodSync(maven, 0o755);
  const java = path.join(root, 'bin/java');
  writeFileSync(java, `#!${process.execPath}
const { writeSync, readFileSync, writeFileSync } = require('node:fs');
const build = JSON.parse(readFileSync('apps/server/build.json', 'utf8'));
if (readFileSync(process.argv[3], 'utf8') !== 'fresh-built-synthetic') process.exit(90);
if (process.argv.includes('--spring.profiles.active=ledger-init')) {
  writeFileSync('preparation.json', JSON.stringify({ env: process.env, args: process.argv.slice(2) }));
  if (process.env.STUB_INIT_WAIT === 'yes') {
    writeSync(1, 'INIT_READY ' + process.pid + '\\n');
    setInterval(() => {}, 1000);
  } else process.exit(Number(process.env.STUB_INIT_EXIT || 0));
} else {
if (!require('node:fs').existsSync('preparation.json')) process.exit(91);
if (process.env.STUB_WAIT === 'yes') {
  writeSync(1, 'READY ' + process.pid + '\\n');
  setInterval(() => {}, 1000);
} else {
  writeSync(1, JSON.stringify({ env: process.env, args: process.argv.slice(2), cwd: process.cwd(), pid: process.pid, build }) + '\\n');
  process.exit(Number(process.env.STUB_EXIT || 0));
}
}
`);
  chmodSync(java, 0o755);
  const docker = path.join(root, 'bin/docker');
  writeFileSync(docker, `#!${process.execPath}
const fs = require('node:fs');
const args = process.argv.slice(2);
fs.appendFileSync(${JSON.stringify(path.join(root, 'docker.jsonl'))}, JSON.stringify({args, env: process.env}) + '\\n');
const statePath = ${JSON.stringify(path.join(root, 'docker-state'))};
const initial = process.env.STUB_DOCKER_STATE || 'running';
const state = fs.existsSync(statePath) ? fs.readFileSync(statePath, 'utf8') : initial;
if (process.env.STUB_DOCKER_ERROR === 'yes') process.exit(1);
if (args[0] === 'context') { console.log(process.env.STUB_DOCKER_ENDPOINT || 'unix:///synthetic.sock'); process.exit(0); }
if (args[0] === 'inspect') {
  const mismatch = process.env.STUB_DOCKER_MISMATCH || '';
  console.log([state, 'postgres:18.6-bookworm', '127.0.0.1:54329', '',
    'POSTGRES_DB=' + (mismatch === 'db' ? 'foreign_db' : process.env.LONGNEST_DEV_DB_NAME),
    'POSTGRES_USER=' + (mismatch === 'user' ? 'foreign_user' : process.env.SPRING_DATASOURCE_USERNAME),
    'POSTGRES_PASSWORD=' + (mismatch === 'password' ? 'DO_NOT_DISCLOSE' : process.env.SPRING_DATASOURCE_PASSWORD),
    'PGDATA=/tmp/longnest-pgdata', 'POSTGRES_INITDB_ARGS=--encoding=UTF8 --locale=C'].join('\\n'));
} else if (args.includes('--images')) console.log('postgres:18.6-bookworm');
else if (args.includes('ps')) { if (state !== 'absent') console.log('a'.repeat(64)); }
else if (args.includes('up')) fs.writeFileSync(statePath, 'running');
else if (args.includes('pg_isready')) {
  if (process.env.STUB_READY_ERROR === 'yes') process.exit(126);
  if (process.env.STUB_NOT_READY_ONCE === 'yes' && !fs.existsSync(statePath + '-checked')) {
    fs.writeFileSync(statePath + '-checked', 'yes');
    console.log('synthetic: rejecting connections'); process.exit(1);
  }
  if (process.env.STUB_NOT_READY === 'yes') { console.log('synthetic: no response'); process.exit(2); }
  if (process.env.STUB_READY_WAIT === 'yes') {
    process.on('SIGTERM', () => {});
    // Wait beyond the launcher's deadline without a real Docker process.
    setInterval(() => {}, 1000);
  } else console.log('synthetic: accepting connections');
} else process.exit(95);
`);
  chmodSync(docker, 0o755);
  const env = { PATH: `${root}/bin:/usr/bin:/bin`, LANG: 'C.UTF-8', ...extra };
  return { root, script, env };
}
function run(f, args = []) {
  return spawnSync('/bin/bash', [f.script, ...args], { cwd: '/tmp', env: f.env, encoding: 'utf8', timeout: 5000 });
}
function success(f) {
  const result = run(f);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(result.stderr, '');
  return JSON.parse(result.stdout);
}
function failure(f, message) {
  const result = run(f);
  assert.equal(result.status, 1);
  assert.equal(result.stdout, '');
  assert.match(result.stderr, message);
  assert.doesNotMatch(result.stderr, /synthetic_password|DO_NOT_DISCLOSE/);
}

test('no prebuilt JAR: Maven package with tests precedes exact ledger JAR invocation from another cwd', () => {
  const f = fixture();
  assert.equal(existsSync(path.join(f.root, 'apps/server/target')), false);
  const actual = success(f);
  assert.equal(actual.build.cwd, `${f.root}/apps/server`);
  assert.deepEqual(actual.build.args, ['-B', '-ntp', 'package']);
  assert.equal(actual.cwd, f.root);
  const preparation = JSON.parse(readFileSync(path.join(f.root, 'preparation.json')));
  assert.deepEqual(preparation.args, ['-jar', `${f.root}/apps/server/target/longnest-0.0.1-SNAPSHOT.jar`, '--spring.profiles.active=ledger-init', '--longnest.ledger.initialize-if-absent=true']);
  assert.equal(preparation.env.SPRING_DATASOURCE_PASSWORD, actual.env.SPRING_DATASOURCE_PASSWORD);
  assert.deepEqual(actual.args, ['-jar', `${f.root}/apps/server/target/longnest-0.0.1-SNAPSHOT.jar`, '--spring.profiles.active=ledger']);
  for (const line of valid.split('\n')) {
    const index = line.indexOf('=');
    assert.equal(actual.env[line.slice(0, index)], line.slice(index + 1));
  }
});
test('file runtime secrets and Maven options do not reach Maven; inherited values still do', () => {
  const actual = success(fixture(`${valid}\nMAVEN_OPTS=-Dfile-only\nTOKEN=synthetic-file-only`, { INHERITED_SETTING: 'preserved' }));
  for (const key of ['SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'LONGNEST_LEDGER_OWNER', 'TOKEN', 'MAVEN_OPTS']) {
    assert.equal(Object.hasOwn(actual.build.env, key), false, key);
    assert.equal(Object.hasOwn(actual.env, key), true, key);
  }
  assert.equal(actual.build.env.INHERITED_SETTING, 'preserved');
});
test('config failure prevents Maven invocation', () => {
  const f = fixture('');
  failure(f, /SPRING_DATASOURCE_URL/);
  assert.equal(existsSync(path.join(f.root, 'apps/server/build.json')), false);
});
test('build failure preserves status and never launches a stale JAR', () => {
  const f = fixture(valid, { STUB_BUILD_EXIT: '23' });
  mkdirSync(path.join(f.root, 'apps/server/target'));
  const jar = path.join(f.root, 'apps/server/target/longnest-0.0.1-SNAPSHOT.jar');
  writeFileSync(jar, 'old-stale-jar');
  const result = run(f);
  assert.equal(result.status, 23);
  assert.equal(result.stdout, '');
  assert.match(result.stderr, /Maven package failed; backend not started/);
  assert.equal(readFileSync(jar, 'utf8'), 'old-stale-jar');
});
test('successful build replaces an older JAR before startup', () => {
  const f = fixture();
  mkdirSync(path.join(f.root, 'apps/server/target'));
  writeFileSync(path.join(f.root, 'apps/server/target/longnest-0.0.1-SNAPSHOT.jar'), 'old-stale-jar');
  success(f);
});
test('successful package without expected artifact fails clearly', () => {
  failure(fixture(valid, { STUB_BUILD_NO_JAR: 'yes' }), /did not produce the expected readable executable JAR/);
});

test('quotes, whitespace, comments, CRLF, empty values, Unicode and no final newline', () => {
  const actual = success(fixture(`${valid}\n\t# ignored\n\n DOUBLE = "hello # = ' Việt"  \nSINGLE='literal " value'\nEMPTY=\nQUOTED_EMPTY=''\nURL=a=b:c/123`.replaceAll('\n', '\r\n')));
  assert.equal(actual.env.DOUBLE, "hello # = ' Việt");
  assert.equal(actual.env.SINGLE, 'literal " value');
  assert.equal(actual.env.EMPTY, '');
  assert.equal(actual.env.QUOTED_EMPTY, '');
  assert.equal(actual.env.URL, 'a=b:c/123');
});

test('substitution, backticks and variables remain literal; nothing executes', () => {
  const f = fixture();
  const marker = path.join(f.root, 'must-not-exist');
  writeFileSync(path.join(f.root, '.env.dev'), `${valid}\nPAYLOAD="$(touch '${marker}') $HOME \`id\`"`);
  const actual = success(f);
  assert.equal(actual.env.PAYLOAD, `$(touch '${marker}') $HOME \`id\``);
  assert.equal(existsSync(marker), false);
});

test('exported process values beat file values, including optional empty values', () => {
  const actual = success(fixture(`${valid}\nOPTIONAL=file\nSPRING_PROFILES_ACTIVE=ledger`, {
    SPRING_DATASOURCE_PASSWORD: 'process_password', OPTIONAL: '', SPRING_PROFILES_ACTIVE: 'ledger',
  }));
  assert.equal(actual.env.SPRING_DATASOURCE_PASSWORD, 'process_password');
  assert.equal(actual.env.OPTIONAL, '');
});
test('empty file is allowed when required values are exported', () => {
  const actual = success(fixture('', Object.fromEntries(valid.split('\n').map(line => line.split('=')))));
  assert.equal(actual.env.LONGNEST_LEDGER_OWNER, 'synthetic_owner');
});
test('file PATH does not choose another executable', () => {
  const f = fixture(`${valid}\nPATH=/not-a-real-path`);
  assert.equal(success(f).env.PATH, f.env.PATH);
});
test('inherited names colliding with script variables retain exact values', () => {
  const extra = { name: 'original-name', value: 'original-value', repo_root: 'original-root', inherited_environment: 'original-array-name', OTHER: 'line1\nline2' };
  const actual = success(fixture(`${valid}\nname=file-name\nvalue=file-value`, extra));
  for (const [key, value] of Object.entries(extra)) assert.equal(actual.env[key], value);
});

for (const name of ['SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD', 'LONGNEST_LEDGER_OWNER']) {
  test(`missing ${name} fails before Java`, () => failure(fixture(valid.split('\n').filter(line => !line.startsWith(`${name}=`)).join('\n')), new RegExp(name)));
  test(`empty inherited ${name} does not fall back to file`, () => failure(fixture(valid, { [name]: '' }), new RegExp(name)));
}
test('blank required configuration is rejected', () => failure(fixture(`${valid.replace('synthetic_password', '"   "')}`), /SPRING_DATASOURCE_PASSWORD/));
test('profile mismatch fails without disclosing its value', () => failure(fixture(`${valid}\nSPRING_PROFILES_ACTIVE=DO_NOT_DISCLOSE`), /must be ledger/));
test('inherited ledger profile overrides mismatched file profile', () => success(fixture(`${valid}\nSPRING_PROFILES_ACTIVE=not_ledger`, { SPRING_PROFILES_ACTIVE: 'ledger' })));

for (const [label, content] of [
  ['export prefix', 'export KEY=DO_NOT_DISCLOSE'],
  ['shell statement', 'touch DO_NOT_DISCLOSE'],
  ['invalid name', 'BAD-NAME=DO_NOT_DISCLOSE'],
  ['inline comment', 'KEY=DO_NOT_DISCLOSE # comment'],
  ['unquoted spaces', 'KEY=DO_NOT_DISCLOSE value'],
  ['unterminated quote', 'KEY="DO_NOT_DISCLOSE'],
  ['multiline quote', 'KEY="DO_NOT_DISCLOSE\nmore"'],
  ['backslash escape', 'KEY="DO_NOT_DISCLOSE\\n"'],
  ['control byte', 'KEY=DO_NOT_DISCLOSE\u0001'],
  ['NUL byte', 'KEY=DO_NOT_DISCLOSE\u0000'],
  ['duplicate', 'KEY=one\nKEY=DO_NOT_DISCLOSE'],
  ['BOM', '\ufeffKEY=DO_NOT_DISCLOSE'],
]) test(`reject ${label} without echoing values`, () => failure(fixture(`${valid}\n${content}`), /\.env\.dev/));

test('missing env file fails even if all required values are inherited', () => {
  const f = fixture(null, Object.fromEntries(valid.split('\n').map(line => line.split('='))));
  failure(f, /readable regular file/);
});
test('unreadable env file fails clearly', { skip: process.getuid() === 0 }, () => {
  const f = fixture();
  chmodSync(path.join(f.root, '.env.dev'), 0o000);
  try { failure(f, /readable regular file/); } finally { chmodSync(path.join(f.root, '.env.dev'), 0o600); }
});
test('missing Java and missing Maven fail before building', () => {
  const f = fixture();
  // An isolated PATH prevents finding host Java/Maven by accident.
  const tools = path.join(f.root, 'tools');
  mkdirSync(tools);
  symlinkSync('/usr/bin/dirname', path.join(tools, 'dirname'));
  symlinkSync('/usr/bin/env', path.join(tools, 'env'));
  f.env.PATH = tools;
  failure(f, /Java 25 must be available/);
  copyFileSync(path.join(f.root, 'bin/java'), path.join(tools, 'java'));
  chmodSync(path.join(tools, 'java'), 0o755);
  failure(f, /Maven must be available/);
  assert.equal(existsSync(path.join(f.root, 'apps/server/build.json')), false);
});
test('arguments are refused and Java exit status is preserved', () => {
  assert.equal(run(fixture(), ['--unexpected']).status, 1);
  assert.equal(run(fixture(valid, { STUB_EXIT: '37' })).status, 37);
});

function dockerCalls(f) {
  return readFileSync(path.join(f.root, 'docker.jsonl'), 'utf8').trim().split('\n').map(JSON.parse);
}
test('Compose uses absolute .env.dev/model paths and effective datasource credentials/name', () => {
  const f = fixture(valid, { SPRING_DATASOURCE_USERNAME: 'override_user', SPRING_DATASOURCE_PASSWORD: 'override_password' });
  success(f);
  for (const { args, env } of dockerCalls(f)) {
    assert(!args.some(arg => arg.includes('password')));
    if (args[0] !== 'compose') continue;
    assert.deepEqual(args.slice(0, 7), ['compose', '--env-file', `${f.root}/.env.dev`, '-p', 'longnest-dev', '-f', `${f.root}/deploy/compose.dev.yaml`]);
    assert.equal(env.SPRING_DATASOURCE_USERNAME, 'override_user');
    assert.equal(env.SPRING_DATASOURCE_PASSWORD, 'override_password');
    assert.equal(env.LONGNEST_DEV_DB_NAME, 'synthetic_only');
  }
  assert.deepEqual(dockerCalls(f).find(call => call.args.includes('pg_isready')).args.slice(-8),
    ['exec', '-T', 'postgres', 'pg_isready', '-U', 'override_user', '-d', 'synthetic_only']);
});
for (const state of ['absent', 'exited', 'running']) {
  test(`Docker service ${state}: starts only if absent/stopped and never recreates`, () => {
    const f = fixture(valid, { STUB_DOCKER_STATE: state });
    success(f);
    const ups = dockerCalls(f).filter(call => call.args.includes('up'));
    assert.equal(ups.length, state === 'running' ? 0 : 1);
    if (ups.length) assert.deepEqual(ups[0].args.slice(-4), ['up', '-d', '--no-recreate', 'postgres']);
  });
}
for (const mismatch of ['db', 'user', 'password']) {
  test(`existing ${mismatch} mismatch fails without up or Java`, () => {
    const f = fixture(valid, { STUB_DOCKER_MISMATCH: mismatch });
    failure(f, /configuration differs/);
    assert(!dockerCalls(f).some(call => call.args.includes('up')));
    assert(!existsSync(path.join(f.root, 'preparation.json')));
  });
}
test('unready running service is polled without up/restart', () => {
  const f = fixture(valid, { STUB_NOT_READY_ONCE: 'yes' });
  success(f);
  assert.equal(dockerCalls(f).filter(call => call.args.includes('pg_isready')).length, 2);
  assert(!dockerCalls(f).some(call => call.args.includes('up')));
});
test('readiness deadline is finite and failure never launches init', () => {
  const f = fixture(valid, { STUB_NOT_READY: 'yes' });
  const original = readFileSync(f.script, 'utf8');
  assert(original.includes('ready_deadline=$((SECONDS + 60))'));
  // Accelerate only the synthetic fixture's deadline, retaining the exact loop/timeout branch.
  writeFileSync(f.script, original.replace('ready_deadline=$((SECONDS + 60))', 'ready_deadline=$((SECONDS + 1))'));
  failure(f, /readiness deadline exceeded/);
  assert(!existsSync(path.join(f.root, 'preparation.json')));
});
test('Docker access/exec errors fail rather than treating service as absent', () => {
  for (const extra of [{ STUB_DOCKER_ERROR: 'yes' }, { STUB_READY_ERROR: 'yes' }]) {
    const f = fixture(valid, extra);
    failure(f, /Cannot inspect Docker|readiness command failed/);
    assert(!dockerCalls(f).some(call => call.args.includes('up')));
    assert(!existsSync(path.join(f.root, 'preparation.json')));
  }
});
test('remote or ambiguous JDBC configuration fails before build/Docker', () => {
  for (const url of ['jdbc:postgresql://remote:54329/data', 'jdbc:postgresql://127.0.0.1:5432/data', 'jdbc:postgresql://127.0.0.1:54329/data?user=other']) {
    const f = fixture(valid, { SPRING_DATASOURCE_URL: url });
    failure(f, /Managed dev database requires/);
    assert(!existsSync(path.join(f.root, 'docker.jsonl')));
  }
});
test('remote Docker context fails before inspecting/starting service', () => {
  const f = fixture(valid, { STUB_DOCKER_ENDPOINT: 'ssh://remote' });
  failure(f, /local Docker context/);
  assert(!dockerCalls(f).some(call => call.args.includes('up')));
});
test('Docker helper controls cannot be overwritten by literal file/inherited names', () => {
  const collisions = {
    dev_database: 'wrong_db', compose_timeout: '0', timeout_path: '/invalid/timeout',
    docker_path: '/invalid/docker', assignments: 'literal-array-name', inherited: 'literal-map-name',
    inherited_environment: 'literal-list-name', file_values: 'literal-file-map',
    container_lines: 'literal-inspection-array', container_env: 'literal-inspection-map',
  };
  for (const inherited of [false, true]) {
    const f = fixture(inherited ? valid : valid + '\n' + Object.entries(collisions).map(([k,v]) => `${k}=${v}`).join('\n'),
      { STUB_DOCKER_STATE: 'absent', ...(inherited ? collisions : {}) });
    success(f);
    for (const { env } of dockerCalls(f)) {
      assert.equal(env.LONGNEST_DEV_DB_NAME, 'synthetic_only');
      for (const [key, value] of Object.entries(collisions)) assert.equal(env[key], value, key);
    }
  }
});
test('TERM-ignoring Docker client is forcibly bounded and cannot start Java', { timeout: 7000 }, () => {
  const f = fixture(valid, { STUB_READY_WAIT: 'yes' });
  const original = readFileSync(f.script, 'utf8');
  assert(original.includes('--kill-after=2'));
  writeFileSync(f.script, original.replace('ready_deadline=$((SECONDS + 60))', 'ready_deadline=$((SECONDS + 1))'));
  const started = Date.now();
  const result = run(f);
  assert.equal(result.status, 1, result.stderr);
  assert(Date.now() - started < 5000);
  assert.match(result.stderr, /readiness command failed/);
  assert(!existsSync(path.join(f.root, 'preparation.json')));
});
test('failed or uncertain init status stops before normal runtime and preserves status', () => {
  const f = fixture(valid, { STUB_INIT_EXIT: '29', STUB_EXIT: '99' });
  const result = run(f);
  assert.equal(result.status, 29);
  assert.equal(result.stdout, '');
  assert.match(result.stderr, /Ledger preparation failed; backend not started/);
});
test('BUILD SUCCESS text cannot override a nonzero Maven process exit', () => {
  const f = fixture(valid, { STUB_BUILD_EXIT: '23', STUB_BUILD_SUCCESS_TEXT: 'yes' });
  const result = run(f);
  assert.equal(result.status, 23);
  assert.equal(result.stdout, 'BUILD SUCCESS\n');
  assert.match(result.stderr, /Exit status: 23/);
  assert(!existsSync(path.join(f.root, 'docker.jsonl')));
});
test('stopped Maven is not mistaken for a nonzero process termination', { timeout: 6000 }, async () => {
  const f = fixture(valid, { STUB_BUILD_STOP: 'yes' });
  const child = spawn('/bin/bash', [f.script], { cwd: '/tmp', env: f.env });
  const closed = once(child, 'close', { signal: AbortSignal.timeout(5000) });
  let pid;
  try {
    const [data] = await once(child.stdout, 'data', { signal: AbortSignal.timeout(2000) });
    pid = Number(/^BUILD_STOP (\d+)/.exec(String(data))[1]);
    // Give wait time to receive the stop notification; it must keep waiting for termination.
    await new Promise(resolve => setTimeout(resolve, 100));
    assert.equal(child.exitCode, null);
    assert(!existsSync(path.join(f.root, 'docker.jsonl')));
    process.kill(pid, 'SIGCONT');
    assert.deepEqual(await closed, [0, null]);
  } finally {
    child.kill('SIGKILL');
    if (pid) { try { process.kill(pid, 'SIGKILL'); } catch {} }
  }
});
test('checkout lock is held across init and final exec, with no stale-lock cleanup needed', { timeout: 10000 }, async () => {
  const f = fixture(valid, { STUB_WAIT: 'yes' });
  const child = spawn('/bin/bash', [f.script], { cwd: '/tmp', env: f.env });
  const closed = once(child, 'close');
  try {
    await once(child.stdout, 'data', { signal: AbortSignal.timeout(4000) });
    const duplicate = run(f);
    assert.equal(duplicate.status, 1);
    assert.match(duplicate.stderr, /already running/);
    child.kill('SIGTERM');
    await closed;
    f.env.STUB_WAIT = 'no';
    success(f);
  } finally { child.kill('SIGKILL'); }
});
for (const signal of ['SIGINT', 'SIGTERM']) {
  test(`${signal} during init stops its group and never starts runtime`, { timeout: 5000 }, async () => {
    const f = fixture(valid, { STUB_INIT_WAIT: 'yes' });
    const child = spawn('/bin/bash', [f.script], { cwd: '/tmp', env: f.env });
    const closed = once(child, 'close', { signal: AbortSignal.timeout(4000) });
    let pid;
    try {
      const [data] = await once(child.stdout, 'data', { signal: AbortSignal.timeout(2000) });
      const match = /^INIT_READY (\d+)\s*$/.exec(String(data));
      assert(match, String(data)); pid = Number(match[1]);
      child.kill(signal);
      assert.deepEqual(await closed, [signal === 'SIGINT' ? 130 : 143, null]);
      assert.throws(() => process.kill(pid, 0), { code: 'ESRCH' });
    } finally {
      child.kill('SIGKILL');
      if (pid) { try { process.kill(pid, 'SIGKILL'); } catch {} }
    }
  });
}
for (const signal of ['SIGTERM', 'SIGINT']) {
  test(`${signal} reaches the exec-replaced process without a wrapper`, { timeout: 5000 }, async () => {
    const f = fixture(valid, { STUB_WAIT: 'yes' });
    const child = spawn('/bin/bash', [f.script], { cwd: '/tmp', env: f.env });
    const closed = once(child, 'close', { signal: AbortSignal.timeout(4000) });
    try {
      const [data] = await once(child.stdout, 'data', { signal: AbortSignal.timeout(2000) });
      assert.equal(String(data).trim(), `READY ${child.pid}`);
      child.kill(signal);
      assert.deepEqual(await closed, [null, signal]);
    } finally { child.kill('SIGKILL'); }
  });
  test(`${signal} during Maven stops the build group and does not launch Java`, { timeout: 5000 }, async () => {
    const f = fixture(valid, { STUB_BUILD_WAIT: 'yes' });
    const child = spawn('/bin/bash', [f.script], { cwd: '/tmp', env: f.env });
    const closed = once(child, 'close', { signal: AbortSignal.timeout(4000) });
    let buildPid;
    try {
      const [data] = await once(child.stdout, 'data', { signal: AbortSignal.timeout(2000) });
      const match = /^BUILD_READY (\d+)\s*$/.exec(String(data));
      assert(match, String(data));
      buildPid = Number(match[1]);
      const stderr = [];
      child.stderr.on('data', chunk => stderr.push(chunk));
      child.kill(signal);
      assert.deepEqual(await closed, [signal === 'SIGINT' ? 130 : 143, null]);
      assert.match(Buffer.concat(stderr).toString(), /Build interrupted; backend not started/);
      assert.equal(existsSync(path.join(f.root, 'apps/server/target')), false);
      assert.throws(() => process.kill(buildPid, 0), { code: 'ESRCH' });
    } finally {
      child.kill('SIGKILL');
      if (buildPid) { try { process.kill(buildPid, 'SIGKILL'); } catch {} }
    }
  });
}
