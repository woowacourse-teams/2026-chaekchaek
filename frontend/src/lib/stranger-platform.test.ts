import assert from "node:assert/strict";
import { test } from "node:test";
import { detectDevice, detectSource } from "./stranger-platform";

test("공유 링크의 utm_source를 이전 사이트보다 우선하고 경로는 저장하지 않는다", () => {
  assert.equal(detectSource("https://chaekchaek.vercel.app/experiments/stranger?utm_source=Instagram", "https://talk.kakao.com/a/b"), "instagram");
  assert.equal(detectSource("https://chaekchaek.vercel.app/experiments/stranger", "https://talk.kakao.com/a/b"), "kakao");
  assert.equal(detectSource("https://chaekchaek.vercel.app/experiments/stranger", ""), "unknown");
  assert.equal(detectSource("https://chaekchaek.vercel.app/experiments/stranger", "https://chaekchaek.vercel.app/"), "unknown");
});

test("접속 기기를 iPhone, Android, iPad, PC로 구분한다", () => {
  assert.equal(detectDevice("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)"), "iPhone");
  assert.equal(detectDevice("Mozilla/5.0 (Linux; Android 15; Pixel 9)"), "Android");
  assert.equal(detectDevice("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15)", 5), "iPad");
  assert.equal(detectDevice("Mozilla/5.0 (Windows NT 10.0; Win64; x64)"), "PC");
});
