import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type ContentOwnProps = {
  title?: ReactNode;
  content?: ReactNode;
  description?: ReactNode;
};
