import { createClassName } from '#internal/utils/classname';

import styles from './Radio.module.css';

import type { Props } from './Radio.types';

const classnameDefault = 'ui-Radio';

export const Radio = (props: Props) => {
  const { children, className, disabled, ...restProps } = props;

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    className,
  });

  return (
    <label className={classname}>
      <input {...restProps} className={styles.input} disabled={disabled} type="radio" />
      <span aria-hidden="true" className={styles.indicator} />
      {children && <span className={styles.label}>{children}</span>}
    </label>
  );
};
