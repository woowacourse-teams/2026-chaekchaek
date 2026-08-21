import type { ComponentPropsWithoutRef, ReactNode } from 'react';

export type Props = Omit<ComponentPropsWithoutRef<'input'>, 'children' | 'type'> & {
  children?: ReactNode;
};
