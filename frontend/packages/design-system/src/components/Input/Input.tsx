import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Input.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Input';

export const Input = <T extends ElementType>(props: Props<T>) => {
  const { as = 'input', className, size, block, reverse, ...restProps } = props;

  const modifiers = {
    size: size && styles?.[`size-${size}`],
    block: block && styles?.[`is-block`],
    reverse: reverse && styles?.[`is-reverse`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <div className={classname}>
      <View as={as} {...restProps} />
    </div>
  );
};
