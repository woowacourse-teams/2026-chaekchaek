import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './ButtonStack.module.css';

import type { Props } from './';

const classnameDefault = 'ui-ButtonStack';

export const ButtonStack = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', direction = 'horizontal', className, ...restProps } = props;

  const modifiers = {
    direction: styles[`direction-${direction}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
