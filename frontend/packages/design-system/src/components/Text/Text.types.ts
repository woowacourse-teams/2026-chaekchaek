import type { ElementType } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type OwnProps = {
  /** 기본 본문, 보조 본문, 부가 설명, 강조, 오류, 어두운 배경 위의 텍스트 색상. */
  color?: 'default' | 'secondary' | 'muted' | 'accent' | 'error' | 'inverse';
  strong?: boolean;
  /** xx-small부터 순서대로 11, 12, 13, 14, 15, 16px에 대응하는 rem 크기. */
  size?: 'xx-small' | 'x-small' | 'small' | 'medium' | 'large' | 'x-large';
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
