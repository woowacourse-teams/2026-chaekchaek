import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  size?: 'small' | 'medium' | 'large';
  variant?: 'ghost' | 'primary';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
