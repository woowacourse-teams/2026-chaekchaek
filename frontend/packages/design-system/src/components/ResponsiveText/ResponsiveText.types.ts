import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  /** 기본값은 horizontal. vertical은 글자를 똑바로 세워 위에서 아래로 표시합니다. */
  direction?: 'horizontal' | 'vertical';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
