import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Dialog.module.css';

import type { BodyProps } from './Dialog.types';

const classnameDefault = 'ui-Dialog-Body';

export const Body = <T extends ElementType>(props: BodyProps<T>) => {
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
