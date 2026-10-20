import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type Variant = 'filled' | 'outlined' | 'dashed';

export type OwnProps = {
  variant?: Variant;
  spacing?: 'medium' | 'small';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
