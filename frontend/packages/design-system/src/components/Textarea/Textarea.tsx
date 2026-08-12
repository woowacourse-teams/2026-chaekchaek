import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Textarea.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Textarea';

export const Textarea = <T extends ElementType>(props: Props<T>) => {
  const { as = 'textarea', className, ...restProps } = props;

  const modifiers = {};

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
