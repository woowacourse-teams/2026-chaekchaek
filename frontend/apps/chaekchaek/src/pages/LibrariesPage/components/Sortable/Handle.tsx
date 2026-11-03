import type { ComponentPropsWithoutRef, ElementType } from 'react';

import { useSortableContext } from './SortableContext';

type Props<T extends ElementType> = {
  as?: T;
} & Omit<ComponentPropsWithoutRef<T>, 'as'>;

export const Handle = <T extends ElementType = 'div'>(props: Props<T>) => {
  const { as, ...restProps } = props;

  const Component = as || 'div';

  const { handleRef } = useSortableContext();

  return <Component ref={handleRef} {...restProps} />;
};
