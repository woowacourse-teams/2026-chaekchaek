import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type Option = {
  value: any;
  text: ReactNode;
  meta?: ReactNode;
};

export type OwnProps = {
  title?: ReactNode;
  options: Option[];
  value: any;
  onChange?: (value: any) => void;
  shape?: 'default' | 'normal';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type ItemOwnProps = {
  value: any;
  children: ReactNode;
  isActive: boolean;
  meta?: ReactNode;
};

export type ItemProps<T extends ElementType = AS> = PolymorphicProps<T, ItemOwnProps>;
