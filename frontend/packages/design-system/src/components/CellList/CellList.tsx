import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import { Item } from './Item';

import styles from './CellList.module.css';

import type { Props } from './';

const classnameDefault = 'ui-CellList';

export const CellList = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, children, title, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {title && <p className={styles[`cell-list-title`]}>{title}</p>}
      <div as={as} className={styles.content} {...restProps}>
        {children}
      </div>
    </View>
  );
};

CellList.Item = Item;
