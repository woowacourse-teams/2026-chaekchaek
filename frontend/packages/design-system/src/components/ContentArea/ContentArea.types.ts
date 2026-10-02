import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  spacing?: 'none' | 'medium' | 'large';
  spacingX?: 'none' | 'medium' | 'large';
  spacingY?: 'none' | 'medium' | 'large';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
