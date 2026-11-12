import { gaTrack } from './ga';
import { posthogTrack } from './posthog';
import type { AnalyticsEventMap } from './event.types';

export const track = <TEventName extends keyof AnalyticsEventMap>(
  eventName: TEventName,
  params?: AnalyticsEventMap[TEventName],
) => {
  gaTrack(eventName, params);
  posthogTrack(eventName, params);
};
