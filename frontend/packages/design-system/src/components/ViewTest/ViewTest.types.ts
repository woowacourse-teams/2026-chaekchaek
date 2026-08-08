import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  variant: 'test';
};

export type Props<T extends ElementType> = PolymorphicProps<T, OwnProps>;
