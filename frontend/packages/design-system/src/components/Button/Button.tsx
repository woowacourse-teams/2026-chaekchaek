import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Button.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Button';

export const Button = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'button',
    shape = 'default',
    variant = 'default',
    size = 'medium',
    block,
    children,
    leading,
    trailing,
    className,
    ...restProps
  } = props;

  const modifiers = {
    shape: shape && styles?.[`shape-${shape}`],
    variant: variant && styles?.[`variant-${variant}`],
    size: size && styles?.[`size-${size}`],
    block: block && styles?.[`is-block`],
  };

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
