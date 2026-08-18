import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Field.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Field-Label';

export const Label = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, children, leading, trailing, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {leading && <span className={styles.leading}>{leading}</span>}
      {children}
      {trailing && <span className={styles.trailing}>{trailing}</span>}
    </View>
  );
};
