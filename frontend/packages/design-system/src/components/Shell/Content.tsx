import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Shell.module.css';

import type { ContentProps } from './Shell.types';

const classnameDefault = 'ui-Shell-Content';

export const Content = <T extends ElementType>(props: ContentProps<T>) => {
  const {
    as = 'div',
    className,
    verticalAlign = 'center',
    leading,
    title,
    content,
    description,
    ...restProps
  } = props;

  const modifiers = {
    verticalAlign: styles[`vertical-align-${verticalAlign}`],
  };

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
    </View>
  );
};
