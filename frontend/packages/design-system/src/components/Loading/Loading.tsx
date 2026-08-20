import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Loading.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Loading';

export const Loading = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps} />
  );
};
