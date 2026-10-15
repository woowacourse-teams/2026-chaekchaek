import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  shape?: 'default' | 'normal' | 'link';
  variant?: 'default' | 'primary' | 'secondary' | 'accent' | 'ghost' | 'danger' | 'danger-weak';
  size?: 'x-small' | 'small' | 'medium' | 'large';
  align?: 'center' | 'left' | 'right';
  block?: boolean;
  leading?: ReactNode;
  trailing?: ReactNode;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
