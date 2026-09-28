import assert from "node:assert/strict";
import { test } from "node:test";
import { emptyStrangerExperiment } from "./stranger-experiment";
import { summarizeReadingTraffic } from "./reading-traffic";

test("책 선택자만 첫 기록 시각으로 집계하고 재방문과 초기 작성자는 제외한다", () => {
  const data = emptyStrangerExperiment();
  const person = (id: string, readingStatus: "read" | "unread" | null, createdAt?: string) => ({
    id, nickname: id, readingStatus, source: "unknown", device: "PC" as const,
    viewedPages: [], composerStarted: false, createdAt,
  });
  data.participants = [
    person("author", null, "2026-09-27T14:00:00Z"),
    person("old-unselected", null, "2026-09-27T14:00:00Z"),
    person("first", "read", "2026-09-27T14:30:00Z"),
    person("second", "unread", "2026-09-27T14:45:00Z"),
    person("third", "read", "2026-09-28T00:10:00Z"),
    person("legacy", "read"),
  ];
  const result = summarizeReadingTraffic(data, "author", Date.parse("2026-09-28T01:00:00Z"));
  assert.equal(result.visitors, 4);
  assert.equal(result.timedVisitors, 3);
  assert.deepEqual(result.hourly.filter((bucket) => bucket.arrivals), [
    { key: String(Math.floor(Date.parse("2026-09-27T14:00:00Z") / 3_600_000)), label: "09.27 23:00", arrivals: 2, cumulative: 2 },
    { key: String(Math.floor(Date.parse("2026-09-28T00:00:00Z") / 3_600_000)), label: "09.28 09:00", arrivals: 1, cumulative: 3 },
  ]);
  assert.deepEqual(result.daily.filter((bucket) => bucket.arrivals).map(({ label, arrivals, cumulative }) => ({ label, arrivals, cumulative })), [
    { label: "09.27", arrivals: 2, cumulative: 2 },
    { label: "09.28", arrivals: 1, cumulative: 3 },
  ]);
});

test("최근 72시간 이전 방문은 시간별 누적의 기준값에 포함한다", () => {
  const data = emptyStrangerExperiment();
  data.participants = [{ id: "reader", nickname: "독자", readingStatus: "read", source: "unknown", device: "PC",
    viewedPages: [], composerStarted: false, createdAt: "2026-09-20T00:00:00Z" }];
  const result = summarizeReadingTraffic(data, "author", Date.parse("2026-09-28T00:00:00Z"));
  assert.equal(result.hourly[0].cumulative, 1);
  assert.equal(result.hourly.at(-1)?.cumulative, 1);
  assert.equal(result.daily.at(-1)?.cumulative, 1);
});
