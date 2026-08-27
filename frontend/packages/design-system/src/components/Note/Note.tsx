import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Note.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Note';

export const Note = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    children,
    variant = 'plain',
    title,
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    variant: variant && styles[`variant-${variant}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return (
    <View as={as} className={classname} style={customStyles} {...restProps}>
      {title && <p className={styles.title}>{title}</p>}
      {children}
    </View>
  );
};
