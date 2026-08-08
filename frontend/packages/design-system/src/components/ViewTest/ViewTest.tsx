import type { ElementType } from 'react';

import { View } from '#internal/components/View';

import type { Props } from './';

export const ViewTest = <T extends ElementType>(props: Props<T>) => {
  const { as = 'div', variant, ...restProps } = props;
  return <View as={as} {...restProps} />;
};
