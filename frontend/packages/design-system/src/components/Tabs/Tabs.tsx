import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Tabs.module.css';

import type { Props } from './';

import { TabList } from './TabList';
import { Tab } from './Tab';
import { TabPanels } from './TabPanels';
import { TabPanel } from './TabPanel';

const classnameDefault = 'ui-Tabs';

export const Tabs = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, distribution, items, active, onChange, ...restProps } = props;

  const modifiers = {
    distribution: distribution && styles[`distribution-${distribution}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <TabList>
        {items.map((item) => {
          const isActive = item.value === active;
          return (
            <Tab
              key={item.value}
              active={isActive}
              onClick={() => {
                onChange(item.value);
              }}
            >
              {item.text}
            </Tab>
          );
        })}
      </TabList>
      <TabPanels>
        {items
          .filter((item) => item.value === active)
          .map((item) => {
            return <TabPanel key={item.value}>{item.panel}</TabPanel>;
          })}
      </TabPanels>
    </View>
  );
};
