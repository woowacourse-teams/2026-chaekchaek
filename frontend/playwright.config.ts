import { defineConfig } from "@playwright/test";
import { existsSync } from "node:fs";

export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  retries: 0,
  use: {
    baseURL: "http://127.0.0.1:3100",
    viewport: { width: 1440, height: 1000 },
    ...(existsSync("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome") ? { channel: "chrome" } : {}),
    trace: "retain-on-failure",
  },
  webServer: { command: "npm run dev -- --port 3100", url: "http://127.0.0.1:3100", reuseExistingServer: !process.env.CI },
});
