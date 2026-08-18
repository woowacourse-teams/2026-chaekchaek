import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  children: ReactNode;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type ContentOwnProps = {
  leading?: ReactNode;
  title?: ReactNode;
  content?: ReactNode;
  description?: ReactNode;
};

export type ContentProps<T extends ElementType = AS> = PolymorphicProps<T, ContentOwnProps>;

export type MediaOwnProps = {
  children: ReactNode;
};

export type MediaProps<T extends ElementType = AS> = PolymorphicProps<T, MediaOwnProps>;
