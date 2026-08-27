import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Badge.module.css';

import type { Props } from '.';

const classnameDefault = 'ui-Badge';

export const Badge = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    size = 'medium',
    variant = 'default',
    reverse = false,
    children,
    className,
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    size: styles[`size-${size}`],
    variant: styles[`variant-${variant}`],
    reverse: variant === 'ghost' && reverse && styles['is-reverse'],
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
      {children}
    </View>
  );
};
