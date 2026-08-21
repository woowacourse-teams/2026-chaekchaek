import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './SegmentedControl.module.css';

import type { ItemProps } from './SegmentedControl.types';

const classnameDefault = 'ui-SegmentedControl-Item';

export const Item = <T extends ElementType>(props: ItemProps<T>) => {
  const { as = 'div', children, value, isActive, className, ...restProps } = props;

  const modifiers = {
    isActive: isActive && styles?.[`is-active`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {children}
    </View>
  );
};
