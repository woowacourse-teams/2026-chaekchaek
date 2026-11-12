import posthog from 'posthog-js';

import { ENV } from '@/configs/env';

import type { AnalyticsEventMap } from './event.types';

export const initializePosthog = () => {
  if (ENV.APP_POSTHOG_KEY && ENV.APP_POSTHOG_HOST) {
    posthog.init(ENV.APP_POSTHOG_KEY, {
      api_host: ENV.APP_POSTHOG_HOST,
      defaults: '2026-05-30',
      autocapture: true,
      capture_pageview: 'history_change',
    });
  }
};

export const posthogTrack = <TEventName extends keyof AnalyticsEventMap>(
  eventName: TEventName,
  params?: AnalyticsEventMap[TEventName],
) => {
  posthog.capture(eventName, params);
};
