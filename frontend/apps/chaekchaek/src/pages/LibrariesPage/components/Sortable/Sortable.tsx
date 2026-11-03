import type { ElementType } from 'react';

import { useSortable } from '@dnd-kit/react/sortable';

import { SortableContext } from './SortableContext';
import { Handle } from './Handle';
import type { Props } from './Sortable.types';

export const Sortable = <T extends ElementType>(props: Props<T>) => {
  const { as, id, index, children, ...restProps } = props;
  const { ref, handleRef } = useSortable({ id, index });

  const Component = as || 'div';

  return (
    <SortableContext.Provider value={{ ref, handleRef }}>
      <Component ref={ref} {...restProps}>
        {children}
      </Component>
    </SortableContext.Provider>
  );
};

Sortable.Handle = Handle;
