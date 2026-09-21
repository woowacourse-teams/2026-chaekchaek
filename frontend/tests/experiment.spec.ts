import { test, expect } from "@playwright/test";

test("참여자는 팝업과 이름 표시 없이 인라인으로 감상과 답글을 남기고 집계에서 확인한다", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/");
  await expect(page.getByTestId("reflection")).toHaveCount(5);
  await expect(page.getByRole("navigation")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "닉네임 설정" })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "실험 집계" })).toHaveCount(0);
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.locator(".avatar, .author-line, .sample-badge")).toHaveCount(0);
  await expect(page.getByRole("textbox")).toHaveCount(1);
  await page.getByLabel("감상", { exact: true }).fill("고향을 떠올리게 하는 이야기였습니다.");
  await page.getByRole("button", { name: "남기기", exact: true }).click();
  await expect(page.getByLabel("감상", { exact: true })).toHaveValue("");
  const first = page.getByTestId("reflection").first();
  await expect(first).toContainText("고향을 떠올리게 하는 이야기였습니다.");
  await first.getByRole("button", { name: "답글 0" }).click();
  await first.getByLabel("답글 내용").fill("다시 읽어보고 싶어요.");
  await first.getByRole("button", { name: "답글 남기기" }).click();
  await expect(first.locator(".reply")).toContainText("다시 읽어보고 싶어요.");
  await first.getByRole("button", { name: "좋아요 0" }).click();
  await expect(first.getByRole("button", { name: "좋아요 1" })).toHaveAttribute("aria-pressed", "true");
  await page.reload();
  await expect(page.getByTestId("reflection").first()).toContainText("고향을 떠올리게 하는 이야기였습니다.");
  await expect(page.getByTestId("reflection").first().getByRole("button", { name: "좋아요 1" })).toHaveAttribute("aria-pressed", "true");
  await page.goto("/admin");
  await expect(page.getByTestId("total-participation")).toHaveText("3");
  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "CSV 내려받기" }).click();
  expect((await download).suggestedFilename()).toBe("chaekchaek-preview-metrics.csv");
  expect(errors).toEqual([]);
});

test("예시 감상에 좋아요를 남기고 취소하면 참여 집계도 줄어든다", async ({ page }) => {
  await page.goto("/");
  const first = page.getByTestId("reflection").first();
  await first.getByRole("button", { name: "좋아요 0" }).click();
  await page.goto("/admin");
  await expect(page.getByTestId("total-participation")).toHaveText("1");
  await page.goto("/");
  await page.getByTestId("reflection").first().getByRole("button", { name: "좋아요 1" }).click();
  await page.goto("/admin");
  await expect(page.getByTestId("total-participation")).toHaveText("0");
});

test("다른 장르의 책 등록 후 책을 선택하면 해당 책의 감상만 표시한다", async ({ page }) => {
  await page.goto("/admin");
  await page.getByRole("button", { name: "책 등록하기" }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("책 제목").fill("테스트용 철학 책");
  await dialog.getByLabel("저자").fill("테스트 저자");
  await dialog.getByLabel("장르").selectOption("철학");
  await dialog.getByRole("button", { name: "등록하기", exact: true }).click();
  await expect(dialog).not.toBeVisible();
  await page.goto("/");
  await page.getByRole("button", { name: "테스트용 철학 책 선택" }).click();
  await expect(page.getByText("아직 나눈 감상이 없어요.")).toBeVisible();
  await page.getByLabel("감상", { exact: true }).fill("새로 등록한 책의 감상");
  await page.getByRole("button", { name: "남기기", exact: true }).click();
  await expect(page.getByTestId("reflection")).toHaveCount(1);
  await page.getByRole("button", { name: "오디세이아 선택" }).click();
  await expect(page.getByTestId("reflection")).toHaveCount(5);
});

test("저장 실패 시 성공 표시를 하지 않고 감상 입력을 유지한다", async ({ page }) => {
  await page.addInitScript(() => {
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key, value) {
      if (key === "chaekchaek-experiment-preview-v1") throw new DOMException("Quota exceeded", "QuotaExceededError");
      return original.call(this, key, value);
    };
  });
  await page.goto("/");
  await page.getByLabel("감상", { exact: true }).fill("저장 실패에도 남아야 하는 입력");
  await page.getByRole("button", { name: "남기기", exact: true }).click();
  await expect(page.getByRole("main").getByRole("alert")).toContainText("저장하지 못했습니다");
  await expect(page.getByLabel("감상", { exact: true })).toHaveValue("저장 실패에도 남아야 하는 입력");
  await expect(page.getByTestId("reflection")).toHaveCount(5);
});

test("PC와 작은 화면에서 바로 입력할 수 있고 가로 넘침이 없다", async ({ page }) => {
  for (const width of [1440, 1024, 390, 320]) {
    await page.setViewportSize({ width, height: 1000 });
    await page.goto("/");
    await expect(page.getByRole("heading", { name: "함께 읽을 책" })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await expect(page.getByLabel("감상", { exact: true })).toBeVisible();
  }
});
