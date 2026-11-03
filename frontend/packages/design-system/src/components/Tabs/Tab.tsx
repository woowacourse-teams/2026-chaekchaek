import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Tabs.module.css';

import type { TabProps } from './Tabs.types';

const classnameDefault = 'ui-Tabs-Tab';

export const Tab = <T extends ElementType>(props: TabProps<T>) => {
  const { as = 'div', className, active, onClick, ...restProps } = props;

  const modifiers = {
    active: active && styles[`is-active`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} onClick={onClick} {...restProps} />;
};
