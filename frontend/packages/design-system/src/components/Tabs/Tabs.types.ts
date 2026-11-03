import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

type Item = {
  text: ReactNode;
  value: string | number;
  panel: ReactNode;
};

export type OwnProps = {
  distribution?: 'equal' | 'content';
  items: Item[];
  active: string | number;
  onChange: (active: string | number) => void;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type TabListOwnProps = {};

export type TabListProps<T extends ElementType = AS> = PolymorphicProps<T, TabListOwnProps>;

export type TabOwnProps = {
  active: boolean;
  onClick: () => void;
};

export type TabProps<T extends ElementType = AS> = PolymorphicProps<T, TabOwnProps>;

export type TabPanelsOwnProps = {};

export type TabPanelsProps<T extends ElementType = AS> = PolymorphicProps<T, TabPanelsOwnProps>;

export type TabPanelOwnProps = {};

export type TabPanelProps<T extends ElementType = AS> = PolymorphicProps<T, TabPanelOwnProps>;
