import { createClassName } from '#internal/utils/classname';

import styles from './Checkbox.module.css';

import type { Props } from './Checkbox.types';

const classnameDefault = 'ui-Checkbox';

export const Checkbox = (props: Props) => {
  const { children, className, disabled, ...restProps } = props;

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    className,
  });

  return (
    <label className={classname}>
      <input {...restProps} className={styles.input} disabled={disabled} type="checkbox" />
      <span aria-hidden="true" className={styles.indicator} />
      {children && <span className={styles.label}>{children}</span>}
    </label>
  );
};
