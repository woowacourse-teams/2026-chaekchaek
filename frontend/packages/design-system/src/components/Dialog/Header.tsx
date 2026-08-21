import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Dialog.module.css';

import type { HeaderProps } from './Dialog.types';

const classnameDefault = 'ui-Dialog-Header';

export const Header = <T extends ElementType>(props: HeaderProps<T>) => {
  const { as = 'div', className, children, subTitle, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <div className={styles.title}>{children}</div>
      <div className={styles[`sub-title`]}>{subTitle}</div>
    </View>
  );
};
