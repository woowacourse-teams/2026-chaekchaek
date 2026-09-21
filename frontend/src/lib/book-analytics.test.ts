import assert from "node:assert/strict";
import test from "node:test";
import { formatDuration, summarizeBookAnalytics } from "./book-analytics";
import type { BookEvent } from "./cloud-experiment";

test("책별 조회와 고유 방문자, 체류시간을 집계한다", () => {
  const books = [{ id: "book", title: "책", author: "저자", genre: "에세이" as const }];
  const events: BookEvent[] = [
    { id: "1", bookId: "book", userId: "a", eventType: "view", durationMs: 0, createdAt: "2026-09-21T00:00:00Z" },
    { id: "2", bookId: "book", userId: "a", eventType: "view", durationMs: 0, createdAt: "2026-09-21T00:01:00Z" },
    { id: "3", bookId: "book", userId: "b", eventType: "view", durationMs: 0, createdAt: "2026-09-21T00:02:00Z" },
    { id: "4", bookId: "book", userId: "a", eventType: "dwell", durationMs: 30_000, createdAt: "2026-09-21T00:00:30Z" },
    { id: "5", bookId: "book", userId: "b", eventType: "dwell", durationMs: 60_000, createdAt: "2026-09-21T00:03:00Z" },
  ];
  const [row] = summarizeBookAnalytics(books, events);
  assert.equal(row.views, 3);
  assert.equal(row.visitors, 2);
  assert.equal(row.totalDurationMs, 90_000);
  assert.equal(row.averageDurationMs, 30_000);
  assert.equal(formatDuration(row.totalDurationMs), "1분 30초");
});
