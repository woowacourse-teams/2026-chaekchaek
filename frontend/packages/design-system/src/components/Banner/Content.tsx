import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Banner.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Banner-Content';

export const Content = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, title, content, description, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <div className={styles.title}>{title}</div>
      <div className={styles.content}>{content}</div>
      <div className={styles.description}>{description}</div>
    </View>
  );
};
