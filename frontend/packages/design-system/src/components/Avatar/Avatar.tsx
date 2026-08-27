import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Avatar.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Avatar';

export const Avatar = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, img, size = 'medium', sx, style, ...restProps } = props;

  const modifiers = {
    size: size && styles?.[`size-${size}`],
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
    <View as={as} className={classname} style={customStyles} {...restProps}>
      <img src={img} />
    </View>
  );
};
