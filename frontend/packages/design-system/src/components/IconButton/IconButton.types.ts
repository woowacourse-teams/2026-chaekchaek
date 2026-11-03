import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  shape?: 'default' | 'normal' | 'link';
  variant?:
    'default' | 'primary' | 'secondary' | 'accent' | 'ghost' | 'soft' | 'danger' | 'danger-weak';
  size?: 'small' | 'medium' | 'large';
  edge?: 'start' | 'center' | 'end';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
