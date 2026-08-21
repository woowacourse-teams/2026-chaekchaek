import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import { Item } from './Item';

import styles from './List.module.css';

import type { Props } from './';

const classnameDefault = 'ui-List';

export const List = <T extends ElementType>(props: Props<T>) => {
  const { as = 'ul', className, columns, ...restProps } = props;

  const modifiers = {
    columns: styles[`column-${columns}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};

List.Item = Item;
