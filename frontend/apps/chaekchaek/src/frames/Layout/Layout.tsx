import type { ElementType } from 'react';

import { View } from '@chaekchaek/design-system';
import { createClassName } from '@chaekchaek/design-system';

import styles from './Layout.module.css';

import type { Props } from '.';

const classnameDefault = 'frame-Layout';

export const Layout = <T extends ElementType>(props: Props<T>) => {
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
