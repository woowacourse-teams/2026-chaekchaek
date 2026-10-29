import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Text.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Text';

export const Text = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    color = 'default',
    size = 'medium',
    strong = false,
    underline = false,
    className,
    ...restProps
  } = props;

  const modifiers = {
    color: styles[`color-${color}`],
    size: styles[`size-${size}`],
    strong: strong && styles['is-strong'],
    underline: underline && styles['is-underline'],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
