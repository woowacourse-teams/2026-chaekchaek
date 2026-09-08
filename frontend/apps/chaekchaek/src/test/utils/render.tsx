import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { render } from '@testing-library/react';

import { authContext } from '@/contexts/AuthContext';
import type { AuthContextValue } from '@/contexts/AuthContext';

const defaultAuthContextValue = {
  isAuthenticated: false,
  user: null,
  updateAccount: () => {},
  guest: null,
  updateGuestAccount: () => {},
};

interface TestProviderProps {
  children: ReactNode;
  route?: string;
  auth: AuthContextValue;
  initialEntries: string[];
}

export const TestProvider = ({
  children,
  auth = defaultAuthContextValue,
  initialEntries = ['/'],
}: TestProviderProps) => {
  return (
    <MemoryRouter initialEntries={initialEntries}>
      <authContext.Provider value={auth}>{children}</authContext.Provider>
    </MemoryRouter>
  );
};

export const renderProvider = (
  children: ReactNode,
  { auth, initialEntries = ['/'] }: { auth: AuthContextValue; initialEntries?: string[] } = {
    auth: defaultAuthContextValue,
  },
) => {
  return render(
    <TestProvider auth={auth} initialEntries={initialEntries}>
      {children}
    </TestProvider>,
  );
};
