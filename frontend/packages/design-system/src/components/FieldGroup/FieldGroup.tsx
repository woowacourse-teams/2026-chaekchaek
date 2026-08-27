import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './FieldGroup.module.css';

import type { Props } from './';

const classnameDefault = 'ui-FieldGroup';

export const FieldGroup = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, orientation = 'horizontal', sx, style, ...restProps } = props;

  const modifiers = {
    orientation: styles[`orientation-${orientation}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return <View as={as} className={classname} style={customStyles} {...restProps} />;
};
