import type { ComponentProps, MouseEvent } from 'react';
import { Link } from 'react-router-dom';

import { resolveStoreDestination } from '../storeDestination';

type LandingCtaLinkProps = ComponentProps<typeof Link>;

export const LandingCtaLink = ({ onClick, ...props }: LandingCtaLinkProps) => {
  const handleClick = (event: MouseEvent<HTMLAnchorElement>) => {
    onClick?.(event);

    if (event.defaultPrevented || !window.matchMedia('(max-width: 767px)').matches) return;

    const storeDestination = resolveStoreDestination(window.navigator.userAgent);

    if (!storeDestination) return;

    event.preventDefault();
    window.location.assign(storeDestination);
  };

  return <Link {...props} onClick={handleClick} />;
};
