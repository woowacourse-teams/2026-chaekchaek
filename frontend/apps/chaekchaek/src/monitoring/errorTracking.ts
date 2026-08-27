import * as Sentry from '@sentry/react';

export const captureError = (error: unknown) => {
  Sentry.captureException(error);
};
