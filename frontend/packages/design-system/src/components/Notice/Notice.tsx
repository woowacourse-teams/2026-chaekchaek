import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './Notice.module.css';

import type { Props } from './';

const classnameDefault = 'ui-Notice';

export const Notice = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, children, height = 200, sx, style, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style, height };

  return (
    <View as={as} className={classname} style={customStyles} {...restProps}>
      {children}
    </View>
  );
};
