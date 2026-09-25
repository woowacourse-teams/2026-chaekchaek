import assert from "node:assert/strict";
import { test } from "node:test";
import { applyStrangerMutation, emptyStrangerExperiment, summarizeStrangerExperiment, summarizeStrangerPlatforms, INITIAL_REFLECTION_AUTHOR_ID } from "./stranger-experiment";
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
    { value: "unknown", visitors: 1, read: 0, unread: 0, submitted: 0 },
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
