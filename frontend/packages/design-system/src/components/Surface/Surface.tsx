import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Surface.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Surface';

export const Surface = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    variant = 'filled',
    spacing = 'small',
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    variant: variant && styles[`variant-${variant}`],
    spacing: spacing && styles[`spacing-${spacing}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return <View as={as} className={classname} style={customStyles} {...restProps} />;
};
