import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Notice.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Notice';

export const Notice = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, children, height = 200, style, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} style={{ ...style, height }} {...restProps}>
      {children}
    </View>
  );
};
