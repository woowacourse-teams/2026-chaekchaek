import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
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

  results.push({
    performance: report.categories.performance.score * 100,
    fcp: report.audits['first-contentful-paint'].numericValue / 1000,
    lcp: report.audits['largest-contentful-paint'].numericValue / 1000,
    ttfb: report.audits['server-response-time'].numericValue / 1000,
  });
}

const average = (metric) => {
  const sum = results.reduce((acc, result) => acc + result[metric], 0);

  return Number((sum / RUN_COUNT).toFixed(2));
};

const measurement = {
  url,
  performance: average('performance'),
  fcp: average('fcp'),
  lcp: average('lcp'),
  ttfb: average('ttfb'),
};

const savedResults = existsSync(OUTPUT_PATH) ? JSON.parse(readFileSync(OUTPUT_PATH, 'utf8')) : {};

savedResults[key] = measurement;

writeFileSync(OUTPUT_PATH, JSON.stringify(savedResults, null, 2) + '\n');

console.log(`\n측정 완료: ${key}`);
console.log(JSON.stringify(measurement, null, 2));
console.log(`\n저장 위치: ${OUTPUT_PATH}`);
