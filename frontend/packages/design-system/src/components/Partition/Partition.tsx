import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Partition.module.css';

import { Item } from './Item';

import type { Props } from './';

const classnameDefault = 'ui-Partition';

export const Partition = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, direction, ...restProps } = props;

  const modifiers = {
    direction: direction && styles[`direction-${direction}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};

Partition.Item = Item;
