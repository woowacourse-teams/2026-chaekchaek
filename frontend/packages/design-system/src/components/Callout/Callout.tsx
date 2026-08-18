import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Callout.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Callout';

export const Callout = <T extends ElementType>(props: Props<T>) => {
  const { as = 'p', className, children, leading, ...restProps } = props;

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
    </View>
  );
};
