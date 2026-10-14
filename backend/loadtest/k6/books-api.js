import http from 'k6/http';
import { check } from 'k6';

const baseUrl = (__ENV.BASE_URL || '').replace(/\/$/, '');
const api = __ENV.API || 'search';
const targetRps = Number(__ENV.RPS || 0.5);
const iterationsPerMinute = Math.round(targetRps * 60);
const duration = __ENV.DURATION || '10s';
const detailRatio = Number(__ENV.DETAIL_RATIO || 0.77);

const defaultIsbns = [
    '9788930001106',
    '9788937473197',
    '9788954681179',
    '9788954682152',
    '9791141988494',
    '9791161571379',
    '9791162203620',
    '9791166836114',
    '9791172131791',
    '9791196214784',
];

const isbns = (__ENV.ISBN_LIST || defaultIsbns.join(','))
    .split(',')
    .map((isbn) => isbn.trim())
    .filter(Boolean);

if (!baseUrl) {
    throw new Error('Set BASE_URL to the development API URL.');
}

export const options = {
    scenarios: {
        api_load: {
            executor: 'constant-arrival-rate',
            rate: iterationsPerMinute,
            timeUnit: '1m',
            duration,
            preAllocatedVUs: 5,
            maxVUs: 20,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        checks: ['rate>0.99'],
    },
};

function getSearch(page) {
    return http.get(
        `${baseUrl}/api/v1/books?query=test&page=${page}`,
        { tags: { name: 'books-search' } },
    );
}

function getDetail(isbn) {
    return http.get(
        `${baseUrl}/api/v1/books/by-isbn/${isbn}`,
        { tags: { name: 'book-detail' } },
    );
}

export default function () {
    if (api === 'search') {
        const page = (__ITER % 2) + 1;
        const response = getSearch(page);
        let hasItems = false;

        try {
            const body = response.json();
            hasItems = Array.isArray(body.items) && body.items.length > 0;
        } catch (_) {}

        check(response, {
            'search returns 200': (res) => res.status === 200,
            'search has items': () => hasItems,
        });
        return;
    }

    if (api === 'detail') {
        const isbn = isbns[(__VU + __ITER) % isbns.length];
        const response = getDetail(isbn);
        let hasIsbn = false;

        try {
            hasIsbn = Boolean(response.json().isbn13);
        } catch (_) {}

        check(response, {
            'detail returns 200': (res) => res.status === 200,
            'detail has ISBN': () => hasIsbn,
        });
        return;
    }

    if (api === 'journey') {
        const page = (__ITER % 2) + 1;
        const searchResponse = getSearch(page);
        let items = [];

        try {
            const body = searchResponse.json();
            if (Array.isArray(body.items)) items = body.items;
        } catch (_) {}

        const searchOk = check(searchResponse, {
            'journey search returns 200': (res) => res.status === 200,
            'journey search has items': () => items.length > 0,
        });

        if (!searchOk || items.length === 0 || Math.random() >= detailRatio) return;

        const book = items[Math.floor(Math.random() * items.length)];
        if (!book.isbn13) return;

        const detailResponse = getDetail(book.isbn13);
        check(detailResponse, {
            'journey detail returns 200': (res) => res.status === 200,
        });
    }
}