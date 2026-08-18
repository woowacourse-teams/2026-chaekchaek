import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Dialog.module.css';

import type { DimProps } from './Dialog.types';

const classnameDefault = 'ui-Dialog-Dim';

export const Dim = <T extends ElementType>(props: DimProps<T>) => {
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
