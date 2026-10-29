import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './ScrollArea.module.css';

import type { Props } from './';
import { resolveSx } from '#internal/systems/index';

const classnameDefault = 'ui-ScrollArea';

export const ScrollArea = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, height, sx, style, ...restProps } = props;

  const modifiers = {};

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, height, ...style };

  return <View as={as} className={classname} style={customStyles} {...restProps} />;
};
