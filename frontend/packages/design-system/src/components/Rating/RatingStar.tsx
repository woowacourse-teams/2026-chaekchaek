import type { ElementType } from 'react';

import { View } from '#internal/components/View';
import { resolveSx } from '#internal/systems/index';
import { createClassName } from '#internal/utils/classname';

import styles from './RatingStar.module.css';

import type { Props } from './RatingStar.types';

const classnameDefault = 'ui-RatingStar';

export const RatingStar = <T extends ElementType>(props: Props<T>) => {
  const {
    as = 'div',
    active = false,
    className,
    half = false,
    size = 'large',
    sx,
    style,
    ...restProps
  } = props;

  const modifiers = {
    fill: half ? styles.half : active ? styles.active : styles.inactive,
    size: styles[`size-${size}`],
  };

  const classname = createClassName({
    styles,
    baseName: classnameDefault,
    modifiers,
    className,
  });

  const spacingStyle = resolveSx({ sx });

  return (
    <View as={as} className={classname} style={{ ...spacingStyle, ...style }} {...restProps} />
  );
};
