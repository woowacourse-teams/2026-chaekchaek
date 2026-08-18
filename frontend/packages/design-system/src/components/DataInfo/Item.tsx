import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './DataInfo.module.css';

import type { ItemProps } from './DataInfo.types';

const classnameDefault = 'ui-DataInfo-Item';

export const Item = <T extends ElementType>(props: ItemProps<T>) => {
  const { as = 'dl', title, content, className, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      {title && <dt className={styles.title}>{title}</dt>}
      {content && <dd className={styles.content}>{content}</dd>}
    </View>
  );
};
