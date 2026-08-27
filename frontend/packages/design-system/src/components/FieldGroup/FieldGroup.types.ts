import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  children: ReactNode;
  orientation?: 'horizontal' | 'vertical';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
