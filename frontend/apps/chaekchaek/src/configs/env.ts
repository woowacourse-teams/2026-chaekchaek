const ENV = {
  APP_NAME: process.env.APP_NAME,
  APP_URL: process.env.APP_URL,
  APP_API_URL: process.env.APP_API_URL,
  APP_GA_ID: process.env.APP_GA_ID,
  APP_SENTRY_DSN: process.env.APP_SENTRY_DSN,
} as const;

export { ENV };
