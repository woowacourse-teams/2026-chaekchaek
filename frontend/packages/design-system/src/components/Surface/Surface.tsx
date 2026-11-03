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
    align = 'center',
    verticalAlign = 'top',
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    variant: variant && styles[`variant-${variant}`],
    spacing: spacing && styles[`spacing-${spacing}`],
    align: align && styles[`align-${align}`],
    verticalAlign: verticalAlign && styles[`vertical-align-${verticalAlign}`],
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
