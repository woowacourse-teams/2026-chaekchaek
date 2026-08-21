// analytics/ga.ts
import ReactGA from 'react-ga4';

import { ENV } from '@/configs/env';

const GA_MEASUREMENT_ID = ENV.APP_GA_ID;

export const initializeGA = () => {
  ReactGA.initialize(GA_MEASUREMENT_ID as string);
};
