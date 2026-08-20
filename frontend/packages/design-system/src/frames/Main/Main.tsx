import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Main.module.css';

import type { Props } from './';

const classnameDefault = 'frame-Main';

export const Main = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
