import { test } from "node:test";
import assert from "node:assert/strict";
import { initialExperiment } from "./examples";
import { metricsCsv, readStoredExperiment, summarizeExperiment, toggleLike } from "./experiment";

test("예시 감상은 제외하고 예시에 남긴 실제 반응만 집계한다", () => {
  const data = initialExperiment();
  assert.deepEqual(summarizeExperiment(data).total, { reflections: 0, replies: 0, likes: 0, participants: 0, total: 0 });
  data.replies.push({ id: "reply", reflectionId: data.reflections[0].id, userId: "reader", nickname: "독자", body: "공감해요", createdAt: new Date().toISOString() });
  const liked = toggleLike(data, data.reflections[0].id, "reader");
  assert.deepEqual(summarizeExperiment(liked).total, { reflections: 0, replies: 1, likes: 1, participants: 1, total: 2 });
  assert.equal(summarizeExperiment(toggleLike(liked, data.reflections[0].id, "reader")).total.likes, 0);
});
test("소설 비중의 분모는 전체 장르 참여이며 참여자는 책을 넘어 중복 제거한다", () => {
  const data = initialExperiment();
  data.books.push({ id: "philosophy", title: "테스트 철학 책", author: "저자", genre: "철학" });
  data.reflections.push({ ...data.reflections[0], id: "real", bookId: "philosophy", userId: "reader", isExample: false });
  const result = summarizeExperiment(toggleLike(data, data.reflections[0].id, "reader"));
  assert.equal(result.novelShare, 0.5);
  assert.equal(result.total.participants, 1);
  assert.equal(result.genres.find((row) => row.genre === "철학")?.reflections, 1);
});
test("참여가 없으면 비중을 판정하지 않는다", () => {
  assert.equal(summarizeExperiment(initialExperiment()).novelShare, null);
});
test("CSV는 수식 실행을 막고 미리보기 데이터임을 표시한다", () => {
  const data = initialExperiment();
  data.books[0].title = "=SUM(1,2)";
  const csv = metricsCsv(data);
  assert.ok(csv.startsWith("\uFEFF"));
  assert.ok(csv.includes("\"'=SUM(1,2)\""));
  assert.ok(csv.includes("브라우저 미리보기"));
});
test("저장 데이터 복원과 잘못된 데이터 거부", () => {
  const data = initialExperiment();
  assert.deepEqual(readStoredExperiment(JSON.stringify(data)), data);
  assert.throws(() => readStoredExperiment('{"version":1}'));
  assert.throws(() => readStoredExperiment(JSON.stringify({ ...data, books: [null] })));
});
