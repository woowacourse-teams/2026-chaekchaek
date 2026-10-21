import type { ComponentPropsWithoutRef, ElementType, ReactNode } from 'react';

export type OwnProps = { id: number; index: number; children: ReactNode };

export type Props<T extends ElementType> = { as?: T } & OwnProps &
  Omit<ComponentPropsWithoutRef<T>, keyof OwnProps>;
