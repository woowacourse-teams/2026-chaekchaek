type AppEnv = 'local' | 'development' | 'production';

const getAppEnv = (): AppEnv => {
  const appEnv = process.env.APP_ENV;

  if (appEnv !== 'local' && appEnv !== 'development' && appEnv !== 'production') {
    throw new Error(`Invalid APP_ENV: ${appEnv}`);
  }

  return appEnv;
};

const ENV = {
  APP_NAME: process.env.APP_NAME,
  APP_URL: process.env.APP_URL,
  APP_API_URL: process.env.APP_API_URL,
  APP_GA_ID: process.env.APP_GA_ID,
  APP_SENTRY_DSN: process.env.APP_SENTRY_DSN,
  APP_ENV: getAppEnv(),
} as const;

export { ENV };
