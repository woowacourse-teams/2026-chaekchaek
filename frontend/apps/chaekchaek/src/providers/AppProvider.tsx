import type { ReactNode } from 'react';

type Props = {
  children: ReactNode;
};

const AppProviders = ({ children }: Props) => {
  return children;
};

export { AppProviders };
