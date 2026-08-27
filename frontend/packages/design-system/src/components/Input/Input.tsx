import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Input.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Input';

export const Input = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'input',
    className,
    size,
    block,
    leading,
    trailing,
    reverse,
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    size: size && styles?.[`size-${size}`],
    block: block && styles?.[`is-block`],
    leading: Boolean(leading) && styles?.[`has-leading`],
    trailing: Boolean(trailing) && styles?.[`has-trailing`],
    reverse: reverse && styles?.[`is-reverse`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return (
    <div className={classname} style={customStyles}>
      {leading && <span className={styles.leading}>{leading}</span>}
      <View as={as} {...restProps} />
      {trailing && <span className={styles.trailing}>{trailing}</span>}
    </div>
  );
};
