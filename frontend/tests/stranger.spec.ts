import { expect, test } from "@playwright/test";

test("읽음 여부, 발췌문, 감상 반응과 별도 통계를 유지한다", async ({ page }) => {
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.goto("/experiments/stranger?utm_source=instagram");
  const stranger = page.locator('[data-book="stranger"]');
  await expect(page.locator('[data-book="love-fragments"]')).toBeVisible();
  await expect(page.getByRole("heading", { name: "발췌문 99-104쪽" })).toHaveCount(0);
  await stranger.getByRole("button", { name: "읽었어요", exact: true }).click();
  await expect(page.getByRole("heading", { name: "앞선 줄거리" })).toBeVisible();
  await expect(page.getByText("뫼르소는 범죄 사건에 연루되어", { exact: false })).toBeVisible();
  await expect(page.getByRole("heading", { name: "발췌문 99-104쪽" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "제공된 감상평" })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "함께 나눈 감상 2" })).toBeVisible();
  await expect(page.getByTestId("reflection")).toHaveCount(2);
  await expect(page.getByTestId("reflection").first()).toContainText("가끔 삶이 거대한 어항 같다는 생각이 듭니다.");
  await expect(page.getByTestId("reflection").nth(1)).toContainText("모두가 가면을 쓴 세상에서 민낯의 주인공은 이방인이었다");
  const initialNote = page.getByTestId("reflection").first();
  await initialNote.getByRole("button", { name: "답글 0" }).click();
  await initialNote.getByLabel("답글 내용").fill("어항이라는 비유가 와닿아요.");
  await initialNote.getByRole("button", { name: "답글 남기기" }).click();
  await initialNote.getByRole("button", { name: "좋아요 0" }).click();
  await page.getByRole("button", { name: "다음" }).click();
  await expect(page.getByText("2 / 6")).toBeVisible();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByLabel("감상", { exact: true }).fill("다른 사람의 필기를 보며 다시 생각했어요.");
  await page.getByRole("button", { name: "남기기", exact: true }).click();
  await expect(page.getByTestId("reflection")).toHaveCount(3);
  const note = page.getByTestId("reflection").last();
  await note.getByRole("button", { name: "답글 0" }).click();
  await note.getByLabel("답글 내용").fill("저도 그렇게 느꼈어요.");
  await note.getByRole("button", { name: "답글 남기기" }).click();
  await note.getByRole("button", { name: "좋아요 0" }).click();
  await page.reload();
  await expect(stranger.getByRole("button", { name: "읽었어요", exact: true })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByTestId("reflection")).toHaveCount(3);
  await expect(page.getByTestId("reflection").first().getByRole("button", { name: "좋아요 1" })).toBeVisible();
  await page.goto("/admin/stranger");
  const readRow = page.getByRole("row", { name: /^읽었어요/ });
  await expect(readRow).toContainText("100.0%");
  await expect(readRow).toContainText("1");
  await expect(readRow.locator("td").nth(12)).toHaveText("2");
  await expect(readRow.locator("td").nth(13)).toHaveText("2");
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
    await page.locator('[data-book="stranger"]').getByRole("button", { name: "안 읽었어요", exact: true }).click();
    await expect(page.locator(".stranger-scan")).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    const stage = await page.locator(".stranger-scan-stage").boundingBox();
    const scan = await page.locator(".stranger-scan-frame").boundingBox();
    const previous = await page.getByRole("button", { name: "이전", exact: true }).boundingBox();
    const next = await page.getByRole("button", { name: "다음", exact: true }).boundingBox();
    expect(stage && scan && previous && next).toBeTruthy();
    expect(previous!.height).toBeGreaterThanOrEqual(44);
    expect(next!.height).toBeGreaterThanOrEqual(44);
    expect(previous!.x).toBeGreaterThanOrEqual(stage!.x);
    expect(previous!.x + previous!.width).toBeLessThanOrEqual(scan!.x);
    expect(next!.x).toBeGreaterThanOrEqual(scan!.x + scan!.width);
    expect(next!.x + next!.width).toBeLessThanOrEqual(stage!.x + stage!.width);
    expect(previous!.y).toBeGreaterThan(stage!.y);
    expect(next!.y + next!.height).toBeLessThan(stage!.y + stage!.height);
    await page.getByRole("button", { name: "다음" }).click();
    await expect(page.getByText("2 / 6")).toBeVisible();
    await page.getByRole("button", { name: "이전" }).click();
    await expect(page.getByText("1 / 6")).toBeVisible();
    await page.locator(".stranger-scan").click();
    await expect(page.getByRole("dialog")).toHaveCount(0);
  }
});

test("다음 이미지가 늦게 도착해도 현재 페이지를 유지한 뒤 전환한다", async ({ page }) => {
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  let releaseImage!: () => void;
  const delayedImage = new Promise<void>((resolve) => { releaseImage = resolve; });
  await page.route("**/experiments/stranger/page-2.jpg", async (route) => {
    await delayedImage;
    await route.continue();
  });
  await page.goto("/experiments/stranger");
  await page.locator('[data-book="stranger"]').getByRole("button", { name: "읽었어요", exact: true }).click();
  await page.getByRole("button", { name: "다음" }).click();
  await expect(page.getByText("불러오는 중…")).toBeVisible();
  await expect(page.locator(".stranger-scan")).toHaveAttribute("src", /page-1\.jpg$/);
  await expect(page.getByRole("button", { name: "다음" })).toBeDisabled();
  releaseImage();
  await expect(page.getByText("2 / 6")).toBeVisible();
  await expect(page.locator(".stranger-scan")).toHaveJSProperty("complete", true);
  expect(await page.locator(".stranger-scan").evaluate((image: HTMLImageElement) => image.naturalWidth)).toBeGreaterThan(0);
});

test("저장 실패 뒤 입력을 보존하고 다시 제출할 수 있다", async ({ page }) => {
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.goto("/experiments/stranger");
  await page.locator('[data-book="stranger"]').getByRole("button", { name: "읽었어요", exact: true }).click();
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
  await expect(page.getByTestId("reflection")).toHaveCount(3);
});
