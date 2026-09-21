import { test } from "node:test";
import assert from "node:assert/strict";
import { withCollectedBooks } from "./books";
import { initialExperiment } from "./examples";

test("이전 책 목록을 갱신해도 기존 감상, 반응과 직접 등록한 책은 유지한다", () => {
  const old = initialExperiment();
  old.books = [{ id: "odyssey", title: "오디세이아", author: "호메로스", genre: "고전소설" },
    { id: "custom", title: "직접 등록", author: "저자", genre: "철학" }];
  old.likes.push({ reflectionId: old.reflections[0].id, userId: "reader" });
  const updated = withCollectedBooks(old);
  assert.equal(updated.books.length, 7);
  assert.equal(updated.books.find((book) => book.id === "odyssey")?.title, "오디세이");
  assert.equal(updated.books.filter((book) => book.coverUrl).length, 6);
  assert.deepEqual(updated.reflections, old.reflections);
  assert.deepEqual(updated.likes, old.likes);
  assert.deepEqual(withCollectedBooks(updated), updated);
});
