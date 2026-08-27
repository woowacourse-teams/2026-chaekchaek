import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Checkbox.module.css';

import type { Props } from './Checkbox.types';

const classnameDefault = 'ui-Checkbox';

export const Checkbox = (props: Props) => {
  const { children, className, disabled, sx, style, ...restProps } = props;

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return (
    <label className={classname} style={customStyles}>
      <input {...restProps} className={styles.input} disabled={disabled} type="checkbox" />
      <span aria-hidden="true" className={styles.indicator} />
      {children && <span className={styles.label}>{children}</span>}
    </label>
  );
};
