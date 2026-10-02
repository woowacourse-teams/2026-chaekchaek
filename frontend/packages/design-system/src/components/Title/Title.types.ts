import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  level?: 'page' | 'main' | 'caption';
  children?: ReactNode;
  description?: ReactNode;
  trailing?: ReactNode;
  orientation?: 'horizontal' | 'vertical';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
