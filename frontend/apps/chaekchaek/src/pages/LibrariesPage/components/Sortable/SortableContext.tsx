import { createContext, useContext } from 'react';
import type { Ref } from 'react';

type SortableContextValue = {
  ref: Ref<HTMLElement>;
  handleRef: Ref<HTMLElement>;
};

export const SortableContext = createContext<SortableContextValue | null>(null);

export const useSortableContext = () => {
  const context = useContext(SortableContext);

  if (!context) throw new Error('Sortable Context');

  return context;
};
