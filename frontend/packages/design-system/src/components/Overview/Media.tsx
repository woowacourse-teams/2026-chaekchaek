import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Overview.module.css';

import type { MediaProps } from './Overview.types';

const classnameDefault = 'ui-Overview-Media';

export const Media = <T extends ElementType>(props: MediaProps<T>) => {
  const { as = 'div', className, children, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <div className={styles.box}>{children}</div>
    </View>
  );
};
