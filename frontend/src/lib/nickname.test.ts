import { test } from "node:test";
import assert from "node:assert/strict";
import { ADJECTIVES, BIRDS, assignNickname } from "./nickname";
test("닉네임은 형용사 한 개와 새 종류 한 개를 조합한다", () => {
  assert.equal(assignNickname(() => 0), "다정한 참새");
  assert.equal(assignNickname((length) => length - 1), "꿈꾸는 파랑새");
  const result = assignNickname();
  assert.ok(ADJECTIVES.some((word) => result.startsWith(word + " ")));
  assert.ok(BIRDS.some((bird) => result.endsWith(" " + bird)));
});
