import type { ElementType } from 'react';

import { View } from '#internal/components/View';

import type { Props } from './';

export const ViewTest = <T extends ElementType>({
  as = 'div',
  variant,
  ...restProps
}: Props<T>) => {
  return <View as={as} {...restProps} />;
};
