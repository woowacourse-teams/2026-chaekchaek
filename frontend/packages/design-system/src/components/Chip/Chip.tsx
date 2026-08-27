import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Chip.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Chip';

export const Chip = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    size = 'medium',
    variant = 'default',
    selected,
    children,
    className,
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    size: styles[`size-${size}`],
    variant: styles[`variant-${variant}`],
    selected: selected && styles['is-selected'],
  };

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
      <span className={styles.label}>{children}</span>
    </View>
  );
};
