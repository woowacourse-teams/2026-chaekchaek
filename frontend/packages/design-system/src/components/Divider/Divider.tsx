import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Divider.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Divider';

export const Divider = <T extends ElementType>(props: Props<T>) => {
  const { as = 'hr', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
