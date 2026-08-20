import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Field.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Field-Content';

export const Content = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, children, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {children}
    </View>
  );
};
