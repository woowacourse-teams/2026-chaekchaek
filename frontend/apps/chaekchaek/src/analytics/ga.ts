// analytics/ga.ts
import ReactGA from 'react-ga4';

import { ENV } from '@/configs/env';

import type { AnalyticsEventMap } from './event.types';

const GA_MEASUREMENT_ID = ENV.APP_GA_ID;

export const initializeGA = () => {
  ReactGA.initialize(GA_MEASUREMENT_ID as string);
};

export const gaTrack = <TEventName extends keyof AnalyticsEventMap>(
  eventName: TEventName,
  params?: AnalyticsEventMap[TEventName],
) => {
  ReactGA.event(eventName, params);
};
