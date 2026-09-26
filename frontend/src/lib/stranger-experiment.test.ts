import assert from "node:assert/strict";
import { test } from "node:test";
import { applyStrangerMutation, emptyStrangerExperiment, summarizeReadingAttention, summarizeStrangerExperiment, summarizeStrangerPlatforms, INITIAL_REFLECTION_AUTHOR_ID } from "./stranger-experiment";
import { initialStrangerReflections } from "./stranger-initial-reflections";
import { LOVE_AUTHOR_ID, belongsToReadingBook } from "./reading-book-config";
import { initialLoveReflections } from "./love-initial-reflections";

test("읽음 여부별 열람, 작성, 제출을 고유 참여자로 집계하고 재전송을 중복하지 않는다", () => {
  let data = emptyStrangerExperiment();
  data = applyStrangerMutation(data, { type: "visit", userId: "a", nickname: "A", source: "instagram", device: "iPhone" });
  data = applyStrangerMutation(data, { type: "visit", userId: "a", nickname: "A", source: "kakao", device: "Android" });
  data = applyStrangerMutation(data, { type: "visit", userId: "b", nickname: "B", source: "unknown", device: "PC" });
  data = applyStrangerMutation(data, { type: "visit", userId: INITIAL_REFLECTION_AUTHOR_ID, nickname: "책췍", source: "internal", device: "other" });
  data = applyStrangerMutation(data, { type: "reflection", reflection: initialStrangerReflections[0] });
  data = applyStrangerMutation(data, { type: "reading", userId: "a", readingStatus: "read" });
  for (let page = 1; page <= 6; page++) data = applyStrangerMutation(data, { type: "page", userId: "a", page });
  data = applyStrangerMutation(data, { type: "page", userId: "a", page: 1 });
  data = applyStrangerMutation(data, { type: "composer", userId: "a" });
  data = applyStrangerMutation(data, { type: "composer", userId: "a" });
  const reflection = { id: "r", userId: "a", nickname: "A", body: "감상", createdAt: "2026-09-23T00:00:00Z" };
  data = applyStrangerMutation(data, { type: "reflection", reflection });
  data = applyStrangerMutation(data, { type: "reflection", reflection });
  const summary = summarizeStrangerExperiment(data);
  assert.equal(summary.unselected, 1);
  assert.deepEqual(summary.groups[0], { status: "read", participants: 1, pageViews: [1, 1, 1, 1, 1, 1],
    completedPages: 1, composerStarted: 1, submitted: 1, submissionRate: 1, replies: 0, likes: 0 });
  assert.equal(summary.groups[1].submissionRate, null);
  assert.equal(data.reflections.length, 2);
  assert.deepEqual(summarizeStrangerPlatforms(data).sources, [
    { value: "instagram", visitors: 1, read: 1, unread: 0, submitted: 1 },
  ]);
});

test("사랑의 편린들 식별자와 초기 글은 이방인 집계와 구별된다", () => {
  assert.equal(belongsToReadingBook("love-fragments", initialLoveReflections[0].id), true);
  assert.equal(belongsToReadingBook("stranger", initialLoveReflections[0].id), false);
  assert.equal(belongsToReadingBook("stranger", initialStrangerReflections[0].id), true);
  let data = emptyStrangerExperiment();
  data = applyStrangerMutation(data, { type: "visit", userId: LOVE_AUTHOR_ID, nickname: "책췍", source: "internal", device: "other" });
  for (const reflection of initialLoveReflections) data = applyStrangerMutation(data, { type: "reflection", reflection });
  assert.equal(summarizeStrangerExperiment(data, LOVE_AUTHOR_ID).unselected, 0);
  assert.equal(summarizeStrangerPlatforms(data, LOVE_AUTHOR_ID).sources.length, 0);
});

test("책 선택자만 방문자로 세고 화면 노출과 관심 행동을 중복 없이 집계한다", () => {
  let data = emptyStrangerExperiment();
  data = applyStrangerMutation(data, { type: "visit", userId: "old", nickname: "이전 방문자", source: "slack", device: "PC" });
  data = applyStrangerMutation(data, { type: "visit", userId: "reader", nickname: "독자", source: "threads", device: "iPhone" });
  data = applyStrangerMutation(data, { type: "reading", userId: "reader", readingStatus: "unread" });
  data = applyStrangerMutation(data, { type: "page", userId: "reader", page: 1 });
  const event = { id: "attention-1", userId: "reader", target: "page:1", eventType: "dwell" as const,
    durationMs: 12_000, createdAt: "2026-09-26T00:00:00Z" };
  data = applyStrangerMutation(data, { type: "attention", event });
  data = applyStrangerMutation(data, { type: "attention", event });
  data = applyStrangerMutation(data, { type: "attention", event: { ...event, id: "attention-2", target: "section:excerpt", durationMs: 8_000 } });
  data = applyStrangerMutation(data, { type: "attention", event: { ...event, id: "attention-3", target: "action:page-move", eventType: "view", durationMs: 0 } });
  const summary = summarizeReadingAttention(data);
  assert.equal(summary.visitors, 1);
  assert.equal(summary.pages[0].durationMs, 12_000);
  assert.equal(summary.pages[0].averageMs, 12_000);
  assert.equal(summary.sections.find((row) => row.section === "excerpt")?.durationMs, 8_000);
  assert.equal(summary.pageMoves, 1);
  assert.deepEqual(summarizeStrangerPlatforms(data).sources.map((row) => row.value), ["threads"]);
});
