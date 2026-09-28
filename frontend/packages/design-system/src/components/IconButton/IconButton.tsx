import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './IconButton.module.css';

import type { Props } from './';

const classnameDefault = 'ui-IconButton';

export const IconButton = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'button',
    shape = 'default',
    variant = 'default',
    size = 'medium',
    children,
    className,
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    shape: shape && styles?.[`shape-${shape}`],
    variant: variant && styles?.[`variant-${variant}`],
    size: size && styles?.[`size-${size}`],
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
