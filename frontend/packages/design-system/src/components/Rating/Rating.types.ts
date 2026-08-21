import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  value: number;
  onChange?: (value: number) => void;
  size?: 'small' | 'medium';
  title?: React.ReactNode;
  description?: React.ReactNode;
  block?: boolean;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
