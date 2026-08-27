import type { ComponentPropsWithoutRef, ReactNode } from 'react';

import type { SpacingType } from '#internal/systems/index';

export type Props = Omit<ComponentPropsWithoutRef<'input'>, 'children' | 'type'> & {
  children?: ReactNode;
} & SpacingType;
