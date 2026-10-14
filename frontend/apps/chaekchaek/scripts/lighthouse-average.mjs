import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const [path, key] = process.argv.slice(2);

if (!path || !path.startsWith('/') || path.startsWith('//') || !key) {
  console.error('사용법: npm run lighthouse -- <PATH> <KEY>');
  console.error('예시: npm run lighthouse -- /intro intro-before');
  process.exit(1);
}

const BASE_URL = 'https://dev.chaekchaek.com';
const url = new URL(path, BASE_URL).toString();

const RUN_COUNT = 3;

const OUTPUT_PATH = fileURLToPath(
  new URL('../.performance/lighthouse-results.json', import.meta.url),
);

const OUTPUT_DIR = fileURLToPath(new URL('../.performance/', import.meta.url));

const options = [
  url,
  '--preset=desktop',
  '--screenEmulation.disabled=false',
  '--screenEmulation.mobile=false',
  '--screenEmulation.width=1512',
  '--screenEmulation.height=1100',
  '--screenEmulation.deviceScaleFactor=1',
  '--throttling-method=simulate',
  '--only-categories=performance',
  '--output=json',
  '--output-path=stdout',
  '--quiet',
];

const results = [];

for (let i = 0; i < RUN_COUNT; i++) {
  console.log(`Lighthouse 측정 중... (${i + 1}/${RUN_COUNT})`);

  const output = execFileSync('npx', ['lighthouse', ...options], {
    encoding: 'utf8',
    maxBuffer: 20 * 1024 * 1024,
  });

  const report = JSON.parse(output);

  const getAuditValue = (id) => {
    const value = report.audits[id]?.numericValue;

    if (typeof value !== 'number' || !Number.isFinite(value)) {
      throw new Error(`Lighthouse 지표를 가져올 수 없습니다: ${id}`);
    }

    return value;
  };

  results.push({
    performance: report.categories.performance.score * 100,
    fcp: getAuditValue('first-contentful-paint') / 1000,
    lcp: getAuditValue('largest-contentful-paint') / 1000,
    cls: getAuditValue('cumulative-layout-shift'),
    tbt: getAuditValue('total-blocking-time'),
    speedIndex: getAuditValue('speed-index') / 1000,
    ttfb: getAuditValue('server-response-time') / 1000,
  });
}

const average = (metric, digits = 2) => {
  const sum = results.reduce((acc, result) => acc + result[metric], 0);

  return Number((sum / RUN_COUNT).toFixed(digits));
};

const measurement = {
  url,
  performance: average('performance'),
  fcp: average('fcp'),
  lcp: average('lcp'),
  cls: average('cls', 3),
  tbt: average('tbt'),
  speedIndex: average('speedIndex'),
  ttfb: average('ttfb'),
};

mkdirSync(OUTPUT_DIR, { recursive: true });

const savedResults = existsSync(OUTPUT_PATH) ? JSON.parse(readFileSync(OUTPUT_PATH, 'utf8')) : {};

savedResults[key] = measurement;

writeFileSync(OUTPUT_PATH, JSON.stringify(savedResults, null, 2) + '\n');

console.log(`\n측정 완료: ${key}`);
console.log(JSON.stringify(measurement, null, 2));
console.log(`\n저장 위치: ${OUTPUT_PATH}`);
