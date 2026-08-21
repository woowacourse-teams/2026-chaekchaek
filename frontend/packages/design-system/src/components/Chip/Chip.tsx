import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Chip.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Chip';

export const Chip = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    size = 'medium',
    variant = 'ghost',
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
