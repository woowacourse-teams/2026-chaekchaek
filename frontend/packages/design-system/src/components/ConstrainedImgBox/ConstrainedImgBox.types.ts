import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  img: string;
  maxWidth?: string;
  maxHeight?: string;
  width?: string;
  height?: string;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
