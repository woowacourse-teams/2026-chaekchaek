import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Tabs.module.css';

import type { TabPanelProps } from './Tabs.types';

const classnameDefault = 'ui-Tabs-TabPanel';

export const TabPanel = <T extends ElementType>(props: TabPanelProps<T>) => {
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
