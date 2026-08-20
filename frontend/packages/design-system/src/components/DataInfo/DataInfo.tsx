import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import { Item } from './Item';

import styles from './DataInfo.module.css';

import type { Props } from './';

const classnameDefault = 'ui-DataInfo';

export const DataInfo = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, heading, children, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {heading && <div className={styles.heading}>{heading}</div>}
      {children}
    </View>
  );
};

DataInfo.Item = Item;
