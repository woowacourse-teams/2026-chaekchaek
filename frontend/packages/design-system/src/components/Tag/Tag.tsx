import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Tag.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Tag';

export const Tag = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    size = 'medium',
    variant = 'default',
    children,
    className,
    ...restProps
  } = props;

  const modifiers = {
    size: styles[`size-${size}`],
    variant: styles[`variant-${variant}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <span className={styles.label}>{children}</span>
    </View>
  );
};
