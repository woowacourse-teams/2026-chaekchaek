import type { CSSProperties, ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  children: ReactNode;
  height?: CSSProperties['height'];
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
