import ReactGA from 'react-ga4';

import type { AnalyticsEventMap } from './event.types';

export const track = <TEventName extends keyof AnalyticsEventMap>(
  eventName: TEventName,
  params?: AnalyticsEventMap[TEventName],
) => {
  ReactGA.event(eventName, params);
};
