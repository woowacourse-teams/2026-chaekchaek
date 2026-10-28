import type { ElementType, ReactNode } from 'react';

import type { PolymorphicProps } from '#internal/components/View';

export type AS = 'div';

export type AccordionValue = string | number;

type Item = {
  text: ReactNode;
  value: AccordionValue;
  panel: ReactNode;
};

export type OwnProps = {
  value: AccordionValue[];
  items: Item[];
  onChange: (value: AccordionValue[]) => void;
};

export type Props<T extends ElementType = AS> = PolymorphicProps<T, OwnProps>;

export type AccordionItemOwnProps = {};

export type AccordionItemProps<T extends ElementType = 'div'> = PolymorphicProps<
  T,
  AccordionItemOwnProps
>;

export type AccordionHeaderOwnProps = {
  isExpanded: boolean;
  value: AccordionValue;
  onChange: (value: AccordionValue) => void;
};

export type AccordionHeaderProps<T extends ElementType = 'button'> = PolymorphicProps<
  T,
  AccordionHeaderOwnProps
>;

export type AccordionPanelOwnProps = {};

export type AccordionPanelProps<T extends ElementType = 'div'> = PolymorphicProps<
  T,
  AccordionPanelOwnProps
>;
