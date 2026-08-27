import type { ElementType } from 'react';

import { resolveSx } from '#internal/systems/index';

import type { Props } from './';

export const View = <T extends ElementType>(props: Props<T>) => {
  const { as, sx, style, ...restProps } = props;
  const Component = as || 'div';

  const spacingStyle = resolveSx({ sx });

  const customStyles = { ...spacingStyle, ...style };

  return <Component style={customStyles} {...restProps} />;
};
