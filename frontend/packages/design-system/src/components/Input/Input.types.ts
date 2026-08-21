import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'input';

export type OwnProps = {
  size?: 'small' | 'medium' | 'large';
  block?: boolean;
  reverse?: boolean;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
