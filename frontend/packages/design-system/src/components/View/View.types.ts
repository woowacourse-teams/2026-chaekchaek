import type { ElementType } from 'react';

export type Props<T extends ElementType = 'div'> = {
  as?: T;
};
