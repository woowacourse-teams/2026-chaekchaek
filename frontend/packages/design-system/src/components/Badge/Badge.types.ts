import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  size?: 'x-small' | 'small' | 'medium';
  variant?: 'default' | 'ghost' | 'soft' | 'subtle';
  reverse?: boolean;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
