import type { ElementType, ComponentPropsWithoutRef } from 'react';

export type Props<T extends ElementType> = {
  as?: T;
} & ComponentPropsWithoutRef<T>;

export type PolymorphicProps<T extends ElementType, P = {}> = {
  as?: T;
} & Omit<ComponentPropsWithoutRef<T>, keyof P> &
  P;
