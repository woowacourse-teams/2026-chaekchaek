import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');
const SCENARIO = (__ENV.SCENARIO || 'single').toLowerCase();
const WARMUP = (__ENV.WARMUP || 'false').toLowerCase() === 'true';
const PAGE = __ENV.PAGE || '1';
const RATE = Number(__ENV.RATE || 5);
const VUS = Number(__ENV.VUS || 100);
const DURATION = __ENV.DURATION || '2m';
const SEARCH_TERMS = [
  '노인과 바다',
  '데미안',
  '돈키호테',
  '멋진 신세계',
  '어린 왕자',
  '오만과 편견',
  '오즈의 마법사',
  '위대한 개츠비',
  '이방인',
  '피노키오',
];

if (!['single', 'mixed'].includes(SCENARIO)) {
  throw new Error(`SCENARIO must be "single" or "mixed", got: ${SCENARIO}`);
}
if (!Number.isInteger(RATE) || RATE < 1 || !Number.isInteger(VUS) || VUS < 1) {
  throw new Error('RATE and VUS must be positive integers');
}

export const options = {
  scenarios: WARMUP
    ? {
        warmup: {
          executor: 'shared-iterations',
          vus: 1,
          iterations: 10,
          maxDuration: '30s',
        },
      }
    : {
        measure: {
          executor: 'constant-arrival-rate',
          rate: RATE,
          timeUnit: '1s',
          duration: DURATION,
          preAllocatedVUs: VUS,
          gracefulStop: '30s',
        },
      },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

export default function () {
  // With SCENARIO=mixed, iterationInTest cycles deterministically through
  // ten terms, giving each term the same request share over a full cycle.
  const query = SCENARIO === 'single'
    ? SEARCH_TERMS[0]
    : SEARCH_TERMS[exec.scenario.iterationInTest % SEARCH_TERMS.length];
  const url = `${BASE_URL}/api/v1/books?query=${encodeURIComponent(query)}&page=${PAGE}`;
  const response = http.get(url, {
    tags: {
      name: '/api/v1/books',
      search_scenario: SCENARIO,
      search_query: query,
    },
  });

  let body;
  try {
    body = response.json();
  } catch (_) {
    body = null;
  }

  check(response, {
    'search returns HTTP 200': (res) => res.status === 200,
    'search response contains 10 books': () => Array.isArray(body?.items) && body.items.length === 10,
    'all books are registered': () => Array.isArray(body?.items)
      && body.items.length === 10
      && body.items.every(item => item.bookId !== null && item.reviewCount !== null && item.replyCount !== null),
    'first edition has seeded counts': () => Array.isArray(body?.items)
      && body.items[0]?.reviewCount === 6300
      && body.items[0]?.replyCount === 12600,
  });
}
