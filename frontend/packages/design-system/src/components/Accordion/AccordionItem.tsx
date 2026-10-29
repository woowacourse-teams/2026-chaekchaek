import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Accordion.module.css';

import type { AccordionItemProps } from './Accordion.types';

const classnameDefault = 'ui-Accordion-Item';

export const AccordionItem = <T extends ElementType>(props: AccordionItemProps<T>) => {
  const { as = 'div', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
