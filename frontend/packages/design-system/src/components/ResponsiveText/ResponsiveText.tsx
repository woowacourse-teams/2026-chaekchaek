import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './ResponsiveText.module.css';

import type { Props } from './';

const classnameDefault = 'ui-ResponsiveText';

export const ResponsiveText = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', direction = 'horizontal', className, ...restProps } = props;

  const modifiers = {
    direction: styles[`direction-${direction}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
