import type { ElementType } from 'react';

import type { Props } from './';

export const View = <T extends ElementType = 'div'>(props: Props<T>) => {
  const { as, ...restProps } = props;
  const Component = as || 'div';

  return <Component {...restProps} />;
};
