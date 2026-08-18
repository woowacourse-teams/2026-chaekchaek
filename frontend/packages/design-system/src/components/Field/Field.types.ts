import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type LabelOwnProps = {
  leading?: ReactNode;
  trailing?: ReactNode;
  children: ReactNode;
};

export type LabelProps<T extends ElementType = AS> = PolymorphicProps<T, LabelOwnProps>;

export type ContentOwnProps = {
  children: ReactNode;
};

export type ContentProps<T extends ElementType = AS> = PolymorphicProps<T, ContentOwnProps>;

export type DescriptionOwnProps = {
  leading?: ReactNode;
  trailing?: ReactNode;
  children: ReactNode;
};

export type DescriptionProps<T extends ElementType = AS> = PolymorphicProps<T, DescriptionOwnProps>;
