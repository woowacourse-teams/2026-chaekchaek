import type { CSSProperties, ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'textarea';

export type OwnProps = {
  variant?: 'soft';
  height?: CSSProperties['height'];
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
