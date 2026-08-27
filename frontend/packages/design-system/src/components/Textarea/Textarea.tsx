import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Textarea.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Textarea';

export const Textarea = <T extends ElementType>(props: Props<T>) => {
  const { as = 'textarea', className, variant, height, sx, style, ...restProps } = props;

  const modifiers = {
    variant: variant && styles?.[`variant-${variant}`],
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
      <View as={as} style={height === undefined ? undefined : { height }} {...restProps} />
    </div>
  );
};
