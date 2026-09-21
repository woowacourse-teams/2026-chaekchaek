import { test } from "node:test";
import assert from "node:assert/strict";
import { mergeCloudExperiment } from "./cloud-experiment";

test("클라우드 참여 데이터와 준비된 책·감상을 합친다", () => {
  const merged = mergeCloudExperiment({
    books: [{ id: "new-book", title: "새 책", author: "작가", genre: "에세이" }],
    reflections: [{ id: "user-note", bookId: "odyssey", userId: "reader", nickname: "고요한 백조",
      title: "", quote: "", source: "", body: "직접 남긴 감상", createdAt: "2026-09-21T00:00:00.000Z", isExample: false }],
    replies: [{ id: "reply", reflectionId: "odyssey-web-review-1", userId: "reader", nickname: "고요한 백조",
      body: "답글", createdAt: "2026-09-21T00:00:00.000Z" }],
    likes: [{ reflectionId: "odyssey-web-review-1", userId: "reader" }],
  });
  assert.equal(merged.books.length, 7);
  assert.equal(merged.reflections.filter((note) => !note.archived).length, 31);
  assert.equal(merged.replies.length, 1);
  assert.equal(merged.likes.length, 1);
});
