import { createContext, useContext } from 'react';

type SortableContextValue = {
  ref: (ref: HTMLElement | null) => void;
  handleRef: (ref: HTMLElement | null) => void;
};

export const SortableContext = createContext<SortableContextValue | null>(null);

export const useSortableContext = () => {
  const context = useContext(SortableContext);

  if (!context) throw new Error('Sortable Context');

  return context;
};
