import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'button';

export type OwnProps = {
  variant?: 'default' | 'primary' | 'secondary';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
