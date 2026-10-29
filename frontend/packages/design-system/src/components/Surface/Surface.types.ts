import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type Variant = 'filled' | 'outlined' | 'dashed';

type VerticalAlign = 'top' | 'center' | 'bottom';

export type OwnProps = {
  variant?: Variant;
  spacing?: 'medium' | 'small';
  align?: 'left' | 'center' | 'right';
  verticalAlign?: VerticalAlign;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
