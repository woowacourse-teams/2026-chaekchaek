import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Choice.module.css';

import type { OptionProps } from './Choice.types';

const classnameDefault = 'ui-Choice-Option';

export const Option = <T extends ElementType>(props: OptionProps<T>) => {
  const { as = 'div', className, isActive = false, ...restProps } = props;

  const modifiers = {
    isActive: isActive && styles?.[`is-active`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
