import type { ElementType, ComponentPropsWithoutRef } from 'react';

export type Props<T extends ElementType = 'div', P = {}> = {
  as?: T;
} & ComponentPropsWithoutRef<T> &
  P;
