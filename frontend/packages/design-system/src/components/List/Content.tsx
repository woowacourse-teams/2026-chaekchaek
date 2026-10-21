import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './List.module.css';

import type { ContentProps } from './';

const classnameDefault = 'ui-List-Item-Content';

export const Content = <T extends ElementType>(props: ContentProps<T>) => {
  const {
    as = 'div',
    className,
    title,
    content,
    description,
    ellipsis = false,
    ...restProps
  } = props;

  const modifiers = {
    ellipsis: ellipsis && styles[`is-ellipsis`],
  };

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
