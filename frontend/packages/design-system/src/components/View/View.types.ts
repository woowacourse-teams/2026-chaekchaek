import type { ElementType, ComponentPropsWithoutRef } from 'react';

import type { SpacingType } from '#internal/systems/index';

export type Props<T extends ElementType> = {
  as?: T;
} & ComponentPropsWithoutRef<T> &
  SpacingType;

export type PolymorphicProps<T extends ElementType, P = {}> = {
  as?: T;
} & Omit<ComponentPropsWithoutRef<T>, keyof P> &
  P &
  SpacingType;
