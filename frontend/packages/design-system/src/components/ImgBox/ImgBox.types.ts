import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  img: string;
  size?: 'small' | 'medium' | 'large' | 'x-large';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
