import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  direction?: 'horizontal' | 'vertical';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type ItemOwnProps = {};

export type ItemProps<T extends ElementType = AS> = PolymorphicProps<T, ItemOwnProps>;
