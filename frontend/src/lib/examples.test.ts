import { test } from "node:test";
import assert from "node:assert/strict";
import { initialExperiment, withSourcedReflections } from "./examples";
import { summarizeExperiment } from "./experiment";

test("웹 감상 갱신은 사용자 글과 이전 감상의 반응을 보존하고 중복 추가하지 않는다", () => {
  const data = initialExperiment();
  data.reflections = [
    { ...data.reflections[0], id: "odyssey-example-1", body: "이전 창작 글", sourceUrl: undefined },
    { ...data.reflections[0], id: "user-note", body: "직접 쓴 감상", userId: "reader", isExample: false, sourceUrl: undefined },
  ];
  data.likes = [{ reflectionId: "odyssey-example-1", userId: "reader" }];
  const updated = withSourcedReflections(data);
  assert.equal(updated.reflections.find((note) => note.id === "odyssey-example-1")?.archived, true);
  assert.equal(updated.reflections.find((note) => note.id === "user-note")?.body, "직접 쓴 감상");
  assert.equal(updated.reflections.filter((note) => note.sourceUrl).length, 30);
  assert.equal(summarizeExperiment(updated).total.total, 2);
  assert.deepEqual(withSourcedReflections(updated), updated);
});

test("각 책의 감상 5개는 서로 다른 원문을 가지며 총 30개다", () => {
  const data = initialExperiment();
  assert.equal(data.reflections.length, 30);
  assert.equal(new Set(data.reflections.map((note) => note.id)).size, 30);
  for (const book of data.books) {
    const notes = data.reflections.filter((note) => note.bookId === book.id);
    assert.equal(notes.length, 5);
    assert.equal(new Set(notes.map((note) => note.sourceUrl)).size, 5);
    assert.ok(notes.every((note) => note.body.trim() && note.sourceUrl?.startsWith("https://")));
  }
  assert.equal(summarizeExperiment(data).total.total, 0);
});

test("교체한 원문 리뷰의 반응을 새 리뷰에 옮기지 않는다", () => {
  const data = initialExperiment();
  const previousId = "odyssey-web-review-1";
  data.reflections.push({ ...data.reflections[0], id: previousId, body: "이전 원문 감상" });
  data.likes.push({ reflectionId: previousId, userId: "reader" });
  const updated = withSourcedReflections(data);
  assert.equal(updated.reflections.filter((note) => !note.archived).length, 30);
  assert.equal(updated.reflections.find((note) => note.id === previousId)?.archived, true);
  assert.deepEqual(updated.likes, [{ reflectionId: previousId, userId: "reader" }]);
  assert.deepEqual(withSourcedReflections(updated), updated);
});
