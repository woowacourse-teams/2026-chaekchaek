import { initializeGA } from './ga';
import { initializeGTM } from './gtm';

export const initializeAnalytics = () => {
  initializeGA();
  initializeGTM();
};
