import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type LeadingOwnProps = {};

export type LeadingProps<T extends ElementType = AS> = PolymorphicProps<T, LeadingOwnProps>;

export type ContentOwnProps = {
  title?: ReactNode;
  content?: ReactNode;
  description?: ReactNode;
};

export type ContentProps<T extends ElementType = AS> = PolymorphicProps<T, ContentOwnProps>;

export type TrailingOwnProps = {};

export type TrailingProps<T extends ElementType = AS> = PolymorphicProps<T, TrailingOwnProps>;
