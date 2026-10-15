import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './InfoMessage.module.css';

import type { Props } from './';

const classnameDefault = 'ui-InfoMessage';

export const InfoMessage = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, visual, title, content, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {visual && <div className={styles.visual}>{visual}</div>}
      {title && <div className={styles.title}>{title}</div>}
      {content && <div className={styles.content}>{content}</div>}
    </View>
  );
};
