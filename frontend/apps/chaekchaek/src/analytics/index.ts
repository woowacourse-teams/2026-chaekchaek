import { initializeGA } from './ga';
import { initializeGTM } from './gtm';
import { initializePosthog } from './posthog';

export const initializeAnalytics = () => {
  initializeGA();
  initializeGTM();
  initializePosthog();
};
