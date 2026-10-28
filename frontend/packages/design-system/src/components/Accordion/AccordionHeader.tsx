import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Accordion.module.css';

import type { AccordionHeaderProps } from './Accordion.types';

const classnameDefault = 'ui-Accordion-Header';

export const AccordionHeader = <T extends ElementType>(props: AccordionHeaderProps<T>) => {
  const { as = 'button', className, children, value, isExpanded, onChange, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View
      as={as}
      type={as === 'button' ? 'button' : undefined}
      className={classname}
      aria-expanded={isExpanded}
      onClick={() => onChange(value)}
      {...restProps}
    >
      <span className={styles.label}>{children}</span>
      <span className={styles.indicator} aria-hidden="true">
        {isExpanded ? '⌃' : '⌄'}
      </span>
    </View>
  );
};
