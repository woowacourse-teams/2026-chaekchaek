import type { ElementType } from 'react';

import type { Props } from './';

const classnameDefault = 'ui-View';

export const View = <T extends ElementType = 'div'>(props: Props<T>) => {
  const { as, ...restProps } = props;
  const Component = as || 'div';

  const classname = classnameDefault;

  return <Component className={classname} {...restProps} />;
};
