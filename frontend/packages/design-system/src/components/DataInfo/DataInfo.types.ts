import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  heading?: ReactNode;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type ItemOwnProps = {
  title?: ReactNode;
  content?: ReactNode;
};

export type ItemProps<T extends ElementType = AS> = PolymorphicProps<T, ItemOwnProps>;
