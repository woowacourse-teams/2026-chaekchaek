import TagManager from 'react-gtm-module';

import { ENV } from '@/configs/env';

export const initializeGTM = () => {
  TagManager.initialize({
    gtmId: ENV.APP_GTM_ID as string,
  });
};
