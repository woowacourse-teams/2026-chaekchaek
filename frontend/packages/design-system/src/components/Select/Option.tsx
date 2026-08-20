import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Select.module.css';

import type { OptionProps } from './Select.types';

const classnameDefault = 'ui-Select';

export const Option = <T extends ElementType>(props: OptionProps<T>) => {
  const { as = 'li', className, isActive, ...restProps } = props;

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
