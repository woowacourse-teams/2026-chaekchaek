import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import { Item } from './Item';

import styles from './CellList.module.css';

import type { Props } from './';

const classnameDefault = 'ui-CellList';

export const CellList = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, children, title, sx, style, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return (
    <View as={as} className={classname} style={customStyles} {...restProps}>
      {title && <p className={styles[`cell-list-title`]}>{title}</p>}
      <div as={as} className={styles.content} {...restProps}>
        {children}
      </div>
    </View>
  );
};

CellList.Item = Item;
