import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type Option = {
  value: any;
  text: ReactNode;
};

export type OwnProps = {
  distribution?: 'equal' | 'content';
  options: Option[];
  value?: any;
  onChange?: (value: any) => void;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type OptionOwnProps = {
  isActive?: boolean;
};

export type OptionProps<T extends ElementType = AS> = PolymorphicProps<T, OptionOwnProps>;
