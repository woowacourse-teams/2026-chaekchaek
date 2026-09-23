import { expect, test } from "@playwright/test";

test("읽음 여부, 발췌문, 감상 반응과 별도 통계를 유지한다", async ({ page }) => {
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.goto("/experiments/stranger?utm_source=instagram");
  await expect(page.getByRole("heading", { name: "발췌문 99-104쪽" })).toHaveCount(0);
  await page.getByRole("button", { name: "읽었어요", exact: true }).click();
  await expect(page.getByRole("heading", { name: "발췌문 99-104쪽" })).toBeVisible();
  await page.getByRole("button", { name: "다음" }).click();
  await expect(page.getByText("2 / 6")).toBeVisible();
  await page.getByRole("button", { name: "필기까지 크게 보기" }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page.getByRole("button", { name: "닫기" }).click();
  await page.getByLabel("감상", { exact: true }).fill("다른 사람의 필기를 보며 다시 생각했어요.");
  await page.getByRole("button", { name: "남기기", exact: true }).click();
  await expect(page.getByTestId("reflection")).toHaveCount(1);
  const note = page.getByTestId("reflection").first();
  await note.getByRole("button", { name: "답글 0" }).click();
  await note.getByLabel("답글 내용").fill("저도 그렇게 느꼈어요.");
  await note.getByRole("button", { name: "답글 남기기" }).click();
  await note.getByRole("button", { name: "좋아요 0" }).click();
  await page.reload();
  await expect(page.getByRole("button", { name: "읽었어요", exact: true })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByTestId("reflection")).toHaveCount(1);
  await page.goto("/admin/stranger");
  const readRow = page.getByRole("row", { name: /^읽었어요/ });
  await expect(readRow).toContainText("100.0%");
  await expect(readRow).toContainText("1");
  await expect(page.getByRole("row", { name: /^instagram/ })).toContainText("1");
  await expect(page.getByRole("row", { name: /^PC/ })).toContainText("1");
  await page.goto("/admin");
  await expect(page.getByTestId("total-participation")).toHaveText("0");
});

test("320px, 390px와 데스크톱에서 책과 페이지 조작이 넘치지 않는다", async ({ page }) => {
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  for (const width of [320, 390, 1440]) {
    await page.setViewportSize({ width, height: 850 });
    await page.goto("/experiments/stranger");
    await page.getByRole("button", { name: "안 읽었어요", exact: true }).click();
    await expect(page.locator(".stranger-scan")).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.getByRole("button", { name: "다음" }).click();
    await expect(page.getByText("2 / 6")).toBeVisible();
  }
});

test("저장 실패 뒤 입력을 보존하고 다시 제출할 수 있다", async ({ page }) => {
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.goto("/experiments/stranger");
  await page.getByRole("button", { name: "읽었어요", exact: true }).click();
  await page.evaluate(() => {
    const original = Storage.prototype.setItem;
    (window as typeof window & { restoreStrangerStorage: () => void }).restoreStrangerStorage = () => { Storage.prototype.setItem = original; };
    Storage.prototype.setItem = function (key, value) {
      if (key === "chaekchaek-stranger-preview-v1") throw new DOMException("Quota exceeded", "QuotaExceededError");
      return original.call(this, key, value);
    };
  });
  const input = page.getByLabel("감상", { exact: true });
  await input.fill("다시 제출할 수 있는 감상");
  await page.getByRole("button", { name: "남기기", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("저장하지 못했습니다");
  await expect(input).toHaveValue("다시 제출할 수 있는 감상");
  await page.evaluate(() => (window as typeof window & { restoreStrangerStorage: () => void }).restoreStrangerStorage());
  await page.getByRole("button", { name: "남기기", exact: true }).click();
  await expect(page.getByTestId("reflection")).toHaveCount(1);
});
