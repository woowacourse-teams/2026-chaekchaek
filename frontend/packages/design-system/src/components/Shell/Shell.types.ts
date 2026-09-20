import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

type VerticalAlign = 'top' | 'center' | 'bottom';

export type OwnProps = {
  reverse?: boolean;
  verticalAlign?: VerticalAlign;
  children: ReactNode;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type ContentOwnProps = {
  verticalAlign?: VerticalAlign;
  title?: ReactNode;
  content?: ReactNode;
  description?: ReactNode;
};

export type ContentProps<T extends ElementType = AS> = PolymorphicProps<T, ContentOwnProps>;
