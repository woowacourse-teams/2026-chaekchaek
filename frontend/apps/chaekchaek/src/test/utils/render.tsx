import type { ReactNode } from 'react';

import { render } from '@testing-library/react';

interface TestProviderProps {
  children: ReactNode;
  route?: string;
}

export const TestProvider = ({ children }: TestProviderProps) => {
  return <>{children}</>;
};

export const renderProvider = (children: ReactNode) => {
  return render(<TestProvider>{children}</TestProvider>);
};
