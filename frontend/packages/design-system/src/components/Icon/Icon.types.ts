import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'svg';

export type OwnProps = {
  size?: 'small' | 'medium' | 'large';
  color?: 'default' | 'secondary' | 'error' | 'inverse';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
