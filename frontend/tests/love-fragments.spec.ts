import { expect, test } from "@playwright/test";

test("사랑의 편린들 발췌문과 초기 감상 세 편에 참여하고 별도 통계를 확인한다", async ({ page }) => {
  await page.route("/api/experiments/love-fragments", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.route("/api/experiments/stranger", (route) => route.fulfill({ status: 503, body: "{}" }));
  await page.goto("/experiments/love-fragments?utm_source=kakao");
  await expect(page.getByRole("heading", { name: "사랑의 편린들" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "앞선 줄거리" })).toHaveCount(0);
  await page.getByRole("button", { name: "안 읽었어요", exact: true }).click();
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
    await page.getByRole("button", { name: "읽었어요", exact: true }).click();
    const scan = await page.locator(".stranger-scan-frame").boundingBox();
    const previous = await page.getByRole("button", { name: "이전", exact: true }).boundingBox();
    const next = await page.getByRole("button", { name: "다음", exact: true }).boundingBox();
    expect(scan && previous && next).toBeTruthy();
    expect(previous!.x + previous!.width).toBeLessThanOrEqual(scan!.x);
    expect(next!.x).toBeGreaterThanOrEqual(scan!.x + scan!.width);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  }
});
