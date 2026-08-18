import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Dialog.module.css';

import type { FooterProps } from './Dialog.types';

const classnameDefault = 'ui-Dialog-Footer';

export const Footer = <T extends ElementType>(props: FooterProps<T>) => {
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
