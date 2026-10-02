import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Media.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Media';

export const Media = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    className,
    variant = 'default',
    media,
    title,
    description,
    ...restProps
  } = props;

  const modifiers = {
    variant: styles[`variant-${variant}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <View as={as} className={classname} {...restProps}>
      <div className={styles.media}>{media}</div>
      <div className={styles.title}>{title}</div>
      {description && <div className={styles.description}>{description}</div>}
    </View>
  );
};
