import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { createClassName } from '#internal/utils/classname';

import styles from './VisuallyHidden.module.css';

import type { Props } from './';

const classnameDefault = 'ui-VisuallyHidden';

export const VisuallyHidden = <T extends ElementType = 'span'>(props: Props<T>) => {
  const { as = 'span', className, ...restProps } = props;

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    className,
  });

  return <View as={as} className={classname} {...restProps} />;
};
