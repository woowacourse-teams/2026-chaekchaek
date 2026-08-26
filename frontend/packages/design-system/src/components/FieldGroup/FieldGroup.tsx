import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './FieldGroup.module.css';

import type { Props } from './';

const classnameDefault = 'ui-FieldGroup';

export const FieldGroup = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, orientation = 'horizontal', ...restProps } = props;

  const modifiers = {
    orientation: styles[`orientation-${orientation}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
