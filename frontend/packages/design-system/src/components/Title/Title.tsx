import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Title.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Title';

export const Title = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    children,
    level = 'page',
    trailing,
    orientation = 'horizontal',
    ...restProps
  } = props;

  const modifiers = {
    level: level && styles[`level-${level}`],
    orientation: orientation && styles[`orientation-${orientation}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <div className={styles.title}>{children}</div>
      {trailing && <div className={styles.trailing}>{trailing}</div>}
    </View>
  );
};
