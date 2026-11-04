const http = require('node:http');
const books = require('./books.json');

const port = Number(process.env.PORT || 18080);
const normalize = (value) => value.replace(/\s+/g, '').toLocaleLowerCase('ko-KR');

const groups = new Map();
for (const book of books) {
  const key = normalize(book.title);
  if (!groups.has(key)) {
    groups.set(key, { title: book.title, isbns: [] });
  }
  groups.get(key).isbns.push({ isbn13: book.isbn13, edition: book.edition });
}

if (groups.size !== 10 || [...groups.values()].some((group) => group.isbns.length !== 10)) {
  throw new Error('Expected 10 search terms with 10 ISBNs each in books.json');
}

const authors = new Map([
  [normalize('노인과 바다'), ['어니스트 헤밍웨이']],
  [normalize('데미안'), ['헤르만 헤세']],
  [normalize('돈키호테'), ['미겔 데 세르반테스']],
  [normalize('멋진 신세계'), ['올더스 헉슬리']],
  [normalize('어린 왕자'), ['앙투안 드 생텍쥐페리']],
  [normalize('오만과 편견'), ['제인 오스틴']],
  [normalize('오즈의 마법사'), ['L. 프랭크 바움']],
  [normalize('위대한 개츠비'), ['F. 스콧 피츠제럴드']],
  [normalize('이방인'), ['알베르 카뮈']],
  [normalize('피노키오'), ['카를로 콜로디']],
]);

const server = http.createServer((request, response) => {
  const url = new URL(request.url, `http://${request.headers.host || '127.0.0.1'}`);
  if (request.method !== 'GET' || url.pathname !== '/v3/search/book') {
    response.writeHead(404).end();
    return;
  }

  const query = url.searchParams.get('query') || '';
  const group = groups.get(normalize(query));
  if (!group) {
    response.writeHead(404, { 'content-type': 'application/json; charset=utf-8' });
    response.end(JSON.stringify({ error: 'Unknown fixed search query', query }));
    return;
  }

  const documents = group.isbns.map(({ isbn13, edition }) => ({
    title: `${group.title} (테스트 판본 ${String(edition).padStart(2, '0')})`,
    thumbnail: `https://example.invalid/performance/${isbn13}.jpg`,
    authors: authors.get(normalize(group.title)) || [],
    translators: [],
    publisher: '성능 측정용',
    datetime: '2020-01-01T00:00:00.000+09:00',
    isbn: isbn13,
  }));

  response.writeHead(200, { 'content-type': 'application/json; charset=utf-8' });
  response.end(JSON.stringify({
    meta: {
      total_count: documents.length,
      pageable_count: documents.length,
      is_end: true,
    },
    documents,
  }));
});

server.listen(port, '127.0.0.1', () => {
  process.stdout.write(`Fixed book-search API stub listening on http://127.0.0.1:${port}\n`);
});
