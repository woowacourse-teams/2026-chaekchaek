const fs = require('node:fs');
const path = require('node:path');

const books = require('./books.json');
const output = process.argv[2] || path.join(__dirname, '../../build/performance/book-search-load.sql');
const groups = new Map();
const usedIsbns = new Set();

for (const book of books) {
  if (usedIsbns.has(book.isbn13)) throw new Error(`Duplicate ISBN: ${book.isbn13}`);
  usedIsbns.add(book.isbn13);
  if (!groups.has(book.title)) groups.set(book.title, []);
  groups.get(book.title).push(book);
}
if (books.length !== 100 || groups.size !== 10 || [...groups.values()].some(group => group.length !== 10)) {
  throw new Error('Expected 10 works with 10 editions each');
}

function isbn13(sequence) {
  const digits = `979${String(200000000 + sequence)}`;
  let sum = 0;
  for (let index = 0; index < digits.length; index++) {
    sum += Number(digits[index]) * (index % 2 === 0 ? 1 : 3);
  }
  return `${digits}${(10 - sum % 10) % 10}`;
}

function quote(value) {
  return `'${value.replaceAll("'", "''")}'`;
}

function insertBatches(table, columns, rows, batchSize = 500) {
  const statements = [];
  for (let start = 0; start < rows.length; start += batchSize) {
    statements.push(`INSERT INTO ${table} (${columns}) VALUES\n${rows.slice(start, start + batchSize).join(',\n')};`);
  }
  return statements.join('\n');
}

const plan = books.map(book => {
  const reviews = book.edition === 1 ? 6300 : book.edition === 2 ? 630 : book.edition === 3 ? 70 : 0;
  return { isbn13: book.isbn13, title: `${book.title} (측정 판본 ${String(book.edition).padStart(2, '0')})`, reviews };
});
for (let index = 1; index <= 9900; index++) {
  const value = isbn13(index);
  if (usedIsbns.has(value)) throw new Error(`Background ISBN collision: ${value}`);
  plan.push({ isbn13: value, title: `측정 배경 도서 ${index}`, reviews: index <= 3000 ? 10 : 0 });
}
const totalReviews = plan.reduce((sum, book) => sum + book.reviews, 0);
if (plan.length !== 10000 || totalReviews !== 100000) {
  throw new Error(`Unexpected fixture size: ${plan.length} books, ${totalReviews} reviews`);
}

const planRows = plan.map(book => `(${quote(book.isbn13)}, ${quote(book.title)}, ${book.reviews})`);
const sequenceRows = Array.from({ length: 6300 }, (_, index) => `(${index + 1})`);
const sql = `-- Local load fixture. Run only against a fresh, dedicated chaekchaek_perf schema.
-- 10,000 books, 100,000 reviews, 200,000 replies; 70% of activity is on search-result books.
USE chaekchaek_perf;

DROP PROCEDURE IF EXISTS assert_book_search_load_target;
DELIMITER $$
CREATE PROCEDURE assert_book_search_load_target()
BEGIN
    IF DATABASE() <> 'chaekchaek_perf' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected chaekchaek_perf schema';
    END IF;
    IF EXISTS (SELECT 1 FROM book LIMIT 1)
        OR EXISTS (SELECT 1 FROM review LIMIT 1)
        OR EXISTS (SELECT 1 FROM reply LIMIT 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Book, review and reply tables must be empty';
    END IF;
END$$
DELIMITER ;
CALL assert_book_search_load_target();
DROP PROCEDURE assert_book_search_load_target;

CREATE TEMPORARY TABLE perf_book_plan (
    isbn13 CHAR(13) NOT NULL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    review_count INT NOT NULL
);
${insertBatches('perf_book_plan', 'isbn13, title, review_count', planRows)}

CREATE TEMPORARY TABLE perf_sequence (n INT NOT NULL PRIMARY KEY);
${insertBatches('perf_sequence', 'n', sequenceRows)}

START TRANSACTION;
INSERT INTO actor (actor_type, guest_token_hash, guest_nickname, created_at, guest_token_issued_at, expires_at)
VALUES ('GUEST', SHA2(CONCAT('book-search-load-', UUID()), 256), '성능 측정용',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 365 DAY));
SET @perf_actor_id = LAST_INSERT_ID();

INSERT INTO book (isbn13, title, cover_image_url, publisher, category)
SELECT p.isbn13, p.title, CONCAT('https://example.invalid/performance/', p.isbn13, '.jpg'),
       '성능 측정용', '고전 문학'
FROM perf_book_plan p;

INSERT INTO review (book_id, actor_id, content, quote, chapter,
                    is_spoiler, anonymous, deleted_at, created_at, updated_at)
SELECT b.id, @perf_actor_id, '테스트 감상 1', NULL, NULL,
       FALSE, TRUE, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM perf_book_plan p
JOIN book b ON b.isbn13 = p.isbn13
JOIN perf_sequence s ON s.n <= p.review_count;

INSERT INTO reply (review_id, actor_id, content, anonymous, deleted_at, created_at, updated_at)
SELECT r.review_id, @perf_actor_id, '테스트 답글 1', TRUE, NULL,
       CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM review r
JOIN (SELECT 1 AS n UNION ALL SELECT 2) copies;
COMMIT;

SELECT (SELECT COUNT(*) FROM book) AS books,
       (SELECT COUNT(*) FROM review) AS reviews,
       (SELECT COUNT(*) FROM reply) AS replies;
SELECT p.title, COUNT(r.review_id) AS reviews
FROM perf_book_plan p JOIN book b ON b.isbn13 = p.isbn13
LEFT JOIN review r ON r.book_id = b.id
WHERE p.review_count > 0 AND p.title LIKE '%측정 판본%'
GROUP BY p.isbn13, p.title ORDER BY p.title;
`;

fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, sql, 'utf8');
console.log(`Generated ${output}: ${plan.length} books, ${totalReviews} reviews, 200000 replies`);
