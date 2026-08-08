import type { ElementType, ComponentPropsWithoutRef } from 'react';

export type Props<T extends ElementType = 'div'> = {
  as?: T;
} & ComponentPropsWithoutRef<T>;
