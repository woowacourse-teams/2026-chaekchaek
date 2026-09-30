import { useEffect } from 'react';

import { resolveStoreDestination } from './store-download';

export const DownloadPage = () => {
  useEffect(() => {
    const destination = resolveStoreDestination(navigator.userAgent);

    window.location.replace(destination ?? '/');
  }, []);

  return null;
};
