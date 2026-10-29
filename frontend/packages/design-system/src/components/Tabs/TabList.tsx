import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Tabs.module.css';

import type { TabListProps } from './Tabs.types';

const classnameDefault = 'ui-Tabs-TabList';

export const TabList = <T extends ElementType>(props: TabListProps<T>) => {
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
