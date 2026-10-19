import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

type Item = { text: ReactNode; onClick: () => void };

export type OwnProps = {
  /** 트리거 버튼 안에 표시할 아이콘 또는 텍스트. */
  trigger: ReactNode;
  items: Item[];
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;
