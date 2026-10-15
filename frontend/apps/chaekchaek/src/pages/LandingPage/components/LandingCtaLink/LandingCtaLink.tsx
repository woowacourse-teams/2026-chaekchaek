import { useSyncExternalStore } from 'react';
import { Link } from 'react-router-dom';

import { resolveStoreDestination } from '../../storeDestination';
import type { LandingCtaLinkProps } from './LandingCtaLink.types';

const MOBILE_MEDIA_QUERY = '(max-width: 767px)';

const subscribeToMobileViewport = (onChange: () => void) => {
  const mediaQuery = window.matchMedia(MOBILE_MEDIA_QUERY);

  mediaQuery.addEventListener('change', onChange);

  return () => mediaQuery.removeEventListener('change', onChange);
};

const getIsMobileViewport = () => window.matchMedia(MOBILE_MEDIA_QUERY).matches;

export const LandingCtaLink = ({ to, ...props }: LandingCtaLinkProps) => {
  const isMobileViewport = useSyncExternalStore(subscribeToMobileViewport, getIsMobileViewport);
  const storeDestination = isMobileViewport
    ? resolveStoreDestination(window.navigator.userAgent)
    : null;

  return <Link {...props} to={storeDestination ?? to} />;
};
