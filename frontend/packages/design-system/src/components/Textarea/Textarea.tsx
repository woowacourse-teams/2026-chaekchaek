import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './Textarea.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Textarea';

export const Textarea = <T extends ElementType>(props: Props<T>) => {
  const { as = 'textarea', className, variant, height, style, ...restProps } = props;

  const modifiers = {
    variant: variant && styles?.[`variant-${variant}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return (
    <div className={classname}>
      <View as={as} style={height === undefined ? style : { ...style, height }} {...restProps} />
    </div>
  );
};
