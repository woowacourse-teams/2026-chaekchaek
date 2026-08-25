import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Overview.module.css';

import type { ContentProps } from './Overview.types';

const classnameDefault = 'ui-Overview-Content';

export const Content = <T extends ElementType>(props: ContentProps<T>) => {
  const { as = 'div', className, leading, title, content, description, meta, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {leading && <div className={styles.leading}>{leading}</div>}
      {title && <div className={styles.title}>{title}</div>}
      {content && <div className={styles.content}>{content}</div>}
      {description && <div className={styles.description}>{description}</div>}
      {meta && <div className={styles.meta}>{meta}</div>}
    </View>
  );
};
