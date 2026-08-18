import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  defaultPage: number;
  totalPages: number;
  onChange?: (page: number) => void;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type ItemOwnProps = {
  children: ReactNode;
  isActive?: boolean;
};

export type ItemProps<T extends ElementType = AS> = PolymorphicProps<T, ItemOwnProps>;
