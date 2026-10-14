import type { ComponentPropsWithoutRef, ReactNode } from 'react';

import type { SpacingType } from '#internal/systems/index';

export type Props = Omit<ComponentPropsWithoutRef<'input'>, 'children' | 'size' | 'type'> & {
  variant?: 'default' | 'accent';
  size?: 'medium' | 'large';
  boxed?: boolean;
  block?: boolean;
  children?: ReactNode;
} & SpacingType;
