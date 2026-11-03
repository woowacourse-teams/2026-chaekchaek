import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Accordion.module.css';

import type { Props } from './';

import { AccordionItem } from './AccordionItem';
import { AccordionHeader } from './AccordionHeader';
import { AccordionPanel } from './AccordionPanel';
import type { AccordionValue } from './Accordion.types';

const classnameDefault = 'ui-Accordion';

export const Accordion = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, value = [], items, onChange, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const handleToggle = (target: AccordionValue) => {
    const nextValue = value.includes(target)
      ? value.filter((item) => item !== target)
      : [...value, target];

    onChange(nextValue);
  };

  return (
    <View as={as} className={classname} {...restProps}>
      {items.map((item) => {
        const isExpanded = value.includes(item.value);
        return (
          <AccordionItem key={item.value}>
            <AccordionHeader value={item.value} isExpanded={isExpanded} onChange={handleToggle}>
              {item.text}
            </AccordionHeader>
            {isExpanded && <AccordionPanel>{item.panel}</AccordionPanel>}
          </AccordionItem>
        );
      })}
    </View>
  );
};
