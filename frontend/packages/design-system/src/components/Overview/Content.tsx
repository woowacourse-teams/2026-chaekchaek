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
      {leading && <p className={styles.leading}>{leading}</p>}
      {title && <p className={styles.title}>{title}</p>}
      {content && <p className={styles.content}>{content}</p>}
      {description && <p className={styles.description}>{description}</p>}
      {meta && <p className={styles.meta}>{meta}</p>}
    </View>
  );
};
