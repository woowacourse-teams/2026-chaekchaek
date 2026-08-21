import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './ProgressBar.module.css';

import type { Props } from './';

const classnameDefault = 'ui-ProgressBar';

export const ProgressBar = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, value, max, title, label, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const percent = (value / max) * 100;

  return (
    <View as={as} className={classname} {...restProps}>
      <div className={styles.box}>
        <div className={styles.bar} style={{ width: `${percent}%` }}></div>
      </div>
      {(title || label) && (
        <div className={styles.header}>
          {title && <div className={styles.title}>{title}</div>}
          {label && <div className={styles.label}>{label}</div>}
        </div>
      )}
    </View>
  );
};
