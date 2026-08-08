import type { ElementType, ComponentPropsWithoutRef } from 'react';

export type Props<T extends ElementType = 'div', P = {}> = {
  as?: T;
} & Omit<ComponentPropsWithoutRef<T>, keyof P> &
  P;
