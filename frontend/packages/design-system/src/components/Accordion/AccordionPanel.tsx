import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Accordion.module.css';

import type { AccordionPanelProps } from './Accordion.types';

const classnameDefault = 'ui-Accordion-Panel';

export const AccordionPanel = <T extends ElementType>(props: AccordionPanelProps<T>) => {
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
