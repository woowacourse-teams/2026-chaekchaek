import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './OptionList.module.css';

import type { ItemProps } from './OptionList.types';

const classnameDefault = 'ui-OptionList-Item';

export const Item = <T extends ElementType>(props: ItemProps<T>) => {
  const { as = 'div', children, value, isActive, meta, className, ...restProps } = props;

  const modifiers = {
    isActive: isActive && styles?.[`is-active`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <span className={styles.content}>{children}</span>
      <span className={styles.meta}>{meta}</span>
    </View>
  );
};
