import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Pagination.module.css';

import type { ItemProps } from './Pagination.types';

const classnameDefault = 'ui-Pagination-Item';

export const Item = <T extends ElementType>(props: ItemProps<T>) => {
  const { as = 'button', children, className, isActive, ...restProps } = props;

  const modifiers = {
    isActive: isActive && styles?.['is-active'],
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
