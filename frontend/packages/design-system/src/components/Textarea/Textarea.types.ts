import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'textarea';

export type OwnProps = {
  variant?: 'soft';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
