import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './FloatingButton.module.css';

import type { Props } from './';

const classnameDefault = 'ui-FloatingButton';

export const FloatingButton = <T extends ElementType = 'button'>(props: Props<T>) => {
  const { as = 'button', children, className, ...restProps } = props;

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    className,
  });

  return (
    <View
      as={as}
      type={as === 'button' ? 'button' : undefined}
      className={classname}
      {...restProps}
    >
      {children}
    </View>
  );
};
