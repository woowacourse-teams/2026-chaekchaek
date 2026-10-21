import type { ElementType } from 'react';

import { useSortable } from '@dnd-kit/react/sortable';

import type { Props } from './Sortable.types';

export const Sortable = <T extends ElementType>(props: Props<T>) => {
  const { as, id, index, children, ...restProps } = props;
  const { ref } = useSortable({ id, index });

  const Component = as || 'div';

  return (
    <Component ref={ref} {...restProps}>
      {children}
    </Component>
  );
};
