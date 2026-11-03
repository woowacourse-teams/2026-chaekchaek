import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './DisplayCard.module.css';

import type { Props } from './';

const classnameDefault = 'ui-DisplayCard';

export const DisplayCard = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', title, description, extra, children, className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <View className={styles.content}>
        <View className={styles.header}>
          <View className={styles.title}>{title}</View>
          <View className={styles.extra}>{extra}</View>
        </View>
        <View className={styles.description}>{description}</View>
        <View className={styles.showcase}>{children}</View>
      </View>
    </View>
  );
};
