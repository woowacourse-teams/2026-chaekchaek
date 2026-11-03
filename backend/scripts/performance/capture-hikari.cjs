const fs = require('node:fs');
const path = require('node:path');

const url = process.env.METRICS_URL || 'http://127.0.0.1:9090/actuator/prometheus';
const output = process.env.OUTPUT || path.join(__dirname, '../../build/performance/hikari.csv');
const pool = process.env.POOL || 'HikariPool-1';
const names = [
  'hikaricp_connections_active',
  'hikaricp_connections_pending',
  'hikaricp_connections_acquire_seconds_sum',
  'hikaricp_connections_acquire_seconds_count',
  'hikaricp_connections_timeout_total',
];

function metric(body, name) {
  for (const line of body.split('\n')) {
    if (!line.startsWith(`${name}{`) && !line.startsWith(`${name} `)) continue;
    if (line.includes('{') && !line.includes(`pool="${pool}"`)) continue;
    return Number(line.trim().split(/\s+/).at(-1));
  }
  return '';
}

fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, `timestamp,${names.join(',')}\n`, 'utf8');
console.log(`Recording ${url} to ${output}; press Ctrl+C after k6 finishes`);

let running = true;
process.on('SIGINT', () => { running = false; });

async function poll() {
  if (!running) return;
  try {
    const response = await fetch(url, { signal: AbortSignal.timeout(3000) });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const body = await response.text();
    const values = names.map(name => metric(body, name));
    if (values.every(value => value === '')) throw new Error('Hikari metrics not found');
    fs.appendFileSync(output, `${new Date().toISOString()},${values.join(',')}\n`, 'utf8');
  } catch (error) {
    console.error(`Metrics collection failed: ${error.message}`);
  }
  if (running) setTimeout(poll, 1000);
}

poll();
