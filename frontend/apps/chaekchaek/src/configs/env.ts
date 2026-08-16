const ENV = {
  APP_NAME: process.env.APP_NAME,
  APP_URL: process.env.APP_URL,
  APP_API_URL: process.env.APP_API_URL,
} as const;

export { ENV };
