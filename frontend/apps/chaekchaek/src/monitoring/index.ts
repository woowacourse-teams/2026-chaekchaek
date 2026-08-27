import * as Sentry from '@sentry/react';

import { ENV } from '@/configs/env';

export const initializeErrorTracking = () => {
  Sentry.init({
    dsn: ENV.APP_SENTRY_DSN,

    replaysOnErrorSampleRate: 1,
  });
};
