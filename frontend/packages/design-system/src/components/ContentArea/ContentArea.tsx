import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './ContentArea.module.css';

import type { Props } from './';

const classnameDefault = 'ui-ContentArea';

export const ContentArea = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', className, spacing, spacingX, spacingY, ...restProps } = props;

  const modifiers = {
    spacing: spacing && styles[`spacing-${spacing}`],
    spacingX: spacingX && styles[`spacingX-${spacingX}`],
    spacingY: spacingY && styles[`spacingY-${spacingY}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
