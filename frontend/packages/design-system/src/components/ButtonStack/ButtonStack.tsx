import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './ButtonStack.module.css';

import type { Props } from './';

const classnameDefault = 'ui-ButtonStack';

export const ButtonStack = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', direction = 'horizontal', className, sx, style, ...restProps } = props;

  const modifiers = {
    direction: styles[`direction-${direction}`],
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
