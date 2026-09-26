import { expect, test } from "@playwright/test";

test("클라우드 방문은 읽음 여부를 선택한 책에만 기록한다", async ({ page }) => {
  const mutations: Record<string, string[]> = { stranger: [], "love-fragments": [] };
  const empty = { participants: [], reflections: [], replies: [], likes: [], attentionEvents: [] };
  for (const book of ["stranger", "love-fragments"]) {
    await page.route(`/api/experiments/${book}`, async (route) => {
      if (route.request().method() === "GET") {
        await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(empty) });
        return;
      }
      mutations[book].push(JSON.parse(route.request().postData() ?? "{}").type);
      await route.fulfill({ status: 200, contentType: "application/json", body: '{"ok":true}' });
    });
  }
  await page.goto("/experiments/stranger?utm_source=threads");
  await expect(page.locator('[data-book="stranger"] button').first()).toBeEnabled();
  expect(mutations.stranger).toEqual([]);
  expect(mutations["love-fragments"]).toEqual([]);
  await page.locator('[data-book="stranger"]').getByRole("button", { name: "읽었어요", exact: true }).click();
  await expect.poll(() => mutations.stranger.includes("attention")).toBe(true);
  expect(mutations.stranger.slice(0, 2)).toEqual(["visit", "reading"]);
  expect(mutations["love-fragments"]).toEqual([]);
});

test("한 페이지의 두 책에서 읽음 여부를 따로 유지하고 상세 내용을 전환한다", async ({ page }) => {
  await page.route("/api/experiments/love-fragments", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.goto("/experiments/stranger");
  const stranger = page.locator('[data-book="stranger"]');
  const love = page.locator('[data-book="love-fragments"]');
  await expect(stranger).toBeVisible();
  await expect(love).toBeVisible();
  await expect(stranger.getByRole("button")).toHaveCount(2);
  await expect(love.getByRole("button")).toHaveCount(2);
  await expect(page.getByRole("heading", { name: /발췌문/ })).toHaveCount(0);
  expect(await page.evaluate(() => localStorage.getItem("chaekchaek-stranger-preview-v1"))).toBeNull();
  expect(await page.evaluate(() => localStorage.getItem("chaekchaek-love-fragments-preview-v1"))).toBeNull();
  await stranger.getByRole("button", { name: "읽었어요", exact: true }).click();
  await expect(page.getByRole("heading", { name: "발췌문 99-104쪽" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "함께 나눈 감상 2" })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem("chaekchaek-love-fragments-preview-v1"))).toBeNull();
  await expect.poll(() => page.evaluate(() => JSON.parse(localStorage.getItem("chaekchaek-stranger-preview-v1") ?? "{}")
    .attentionEvents?.some((event: { target: string }) => event.target === "page:1"))).toBe(true);
  await love.getByRole("button", { name: "안 읽었어요", exact: true }).click();
  await expect(page.getByRole("heading", { name: "발췌문 34-36쪽, 86-88쪽" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "발췌문 99-104쪽" })).toHaveCount(0);
  await expect(page.getByRole("heading", { name: "함께 나눈 감상 3" })).toBeVisible();
  await expect(stranger.getByRole("button", { name: "읽었어요", exact: true })).toHaveAttribute("aria-pressed", "true");
  await expect(love.getByRole("button", { name: "안 읽었어요", exact: true })).toHaveAttribute("aria-pressed", "true");
  const participants = await page.evaluate(() => ["stranger", "love-fragments"].map((book) =>
    JSON.parse(localStorage.getItem(`chaekchaek-${book}-preview-v1`) ?? "{}").participants?.length));
  expect(participants).toEqual([1, 1]);
  await page.reload();
  await expect(page.getByRole("heading", { name: "발췌문 34-36쪽, 86-88쪽" })).toBeVisible();
  await stranger.getByRole("button", { name: "읽었어요", exact: true }).click();
  await expect(page.getByRole("heading", { name: "발췌문 99-104쪽" })).toBeVisible();
  await expect(love.getByRole("button", { name: "안 읽었어요", exact: true })).toHaveAttribute("aria-pressed", "true");
});

test("사랑의 편린들 발췌문과 초기 감상 세 편에 참여하고 별도 통계를 확인한다", async ({ page }) => {
  await page.route("/api/experiments/love-fragments", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.goto("/experiments/love-fragments?utm_source=kakao");
  await expect(page).toHaveURL(/\/experiments\/stranger\?.*utm_source=kakao.*book=love-fragments/);
  const love = page.locator('[data-book="love-fragments"]');
  await expect(page.locator('[data-book="stranger"]')).toBeVisible();
  await expect(page.getByRole("heading", { name: "사랑의 편린들" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "앞선 줄거리" })).toHaveCount(0);
  await love.getByRole("button", { name: "안 읽었어요", exact: true }).click();
  await expect(page.getByRole("heading", { name: "발췌문 34-36쪽, 86-88쪽" })).toBeVisible();
  await expect(page.getByTestId("reflection")).toHaveCount(3);
  await expect(page.getByTestId("reflection").first()).toContainText("헤어진 뒤에는 이상하게 과거까지 다시 평가하게 된다.");
  await expect(page.getByTestId("reflection").nth(1)).toContainText("헤어졌으니 미워하면 편할 것 같은데");
  await expect(page.getByTestId("reflection").nth(2)).toContainText("사랑이 끝난 것과 사랑이 익숙해진 것은 어떻게 구별할 수 있을까.?");
  const first = page.getByTestId("reflection").first();
  await first.getByRole("button", { name: "좋아요 0" }).click();
  await first.getByRole("button", { name: "답글 0" }).click();
  await first.getByLabel("답글 내용").fill("기억을 남기는 방식이 인상적이에요.");
  await first.getByRole("button", { name: "답글 남기기" }).click();
  for (const [index, printedPage] of [35, 36, 86, 87, 88].entries()) {
    await page.getByRole("button", { name: "다음", exact: true }).click();
    await expect(page.getByText(`${index + 2} / 6`)).toBeVisible();
    await expect(page.getByText(`책 ${printedPage}쪽`)).toBeVisible();
    await expect(page.locator(".stranger-scan")).toHaveAttribute("src", new RegExp(`page-${index + 2}\\.jpg$`));
  }
  await page.getByLabel("감상", { exact: true }).fill("두 장면이 서로 다르게 다가왔어요.");
  await page.getByRole("button", { name: "남기기", exact: true }).click();
  await expect(page.getByTestId("reflection")).toHaveCount(4);
  await page.goto("/admin/love-fragments");
  await expect(page.getByRole("link", { name: "사랑의 편린들 실험" })).toHaveAttribute("aria-current", "page");
  const unreadRow = page.getByRole("row", { name: /^안 읽었어요/ });
  await expect(unreadRow).toContainText("100.0%");
  await expect(unreadRow.locator("td").nth(8)).toHaveText("1");
  await page.goto("/admin/stranger");
  await expect(page.getByRole("row", { name: /^안 읽었어요/ }).locator("td").nth(1)).toHaveText("0");
});

test("320px와 390px에서 이미지 이동 버튼은 이미지 바깥에 남는다", async ({ page }) => {
  await page.route("/api/experiments/love-fragments", (route) => route.fulfill({ status: 503, body: "{}" }));
  for (const width of [320, 390]) {
    await page.setViewportSize({ width, height: 850 });
    await page.goto("/experiments/love-fragments");
    await page.locator('[data-book="love-fragments"]').getByRole("button", { name: "읽었어요", exact: true }).click();
    const scan = await page.locator(".stranger-scan-frame").boundingBox();
    const previous = await page.getByRole("button", { name: "이전", exact: true }).boundingBox();
    const next = await page.getByRole("button", { name: "다음", exact: true }).boundingBox();
    expect(scan && previous && next).toBeTruthy();
    expect(previous!.x + previous!.width).toBeLessThanOrEqual(scan!.x);
    expect(next!.x).toBeGreaterThanOrEqual(scan!.x + scan!.width);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  }
});
