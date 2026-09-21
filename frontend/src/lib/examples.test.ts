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
  assert.equal(updated.reflections.filter((note) => note.sourceUrl).length, 6);
  assert.equal(summarizeExperiment(updated).total.total, 2);
  assert.deepEqual(withSourcedReflections(updated), updated);
});
