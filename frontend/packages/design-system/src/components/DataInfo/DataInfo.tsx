import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import { Item } from './Item';

import styles from './DataInfo.module.css';

import type { Props } from './';

const classnameDefault = 'ui-DataInfo';

export const DataInfo = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, heading, children, sx, style, ...restProps } = props;

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
      {heading && <div className={styles.heading}>{heading}</div>}
      {children}
    </View>
  );
};

DataInfo.Item = Item;
