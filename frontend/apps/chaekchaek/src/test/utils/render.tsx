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

const defaultInitialEntries = ['/'];

interface TestProviderProps {
  children: ReactNode;
  route?: string;
  auth: AuthContextValue;
  initialEntries: string[];
}

export const TestProvider = ({
  children,
  auth = defaultAuthContextValue,
  initialEntries = defaultInitialEntries,
}: TestProviderProps) => {
  return (
    <MemoryRouter initialEntries={initialEntries}>
      <authContext.Provider value={auth}>{children}</authContext.Provider>
    </MemoryRouter>
  );
};

const defaultConfig = {
  auth: defaultAuthContextValue,
  initialEntries: defaultInitialEntries,
};

export const renderProvider = (
  children: ReactNode,
  config: { auth?: AuthContextValue; initialEntries?: string[] } = {},
) => {
  const resolvedConfig = { ...defaultConfig, ...config };
  const { auth, initialEntries } = resolvedConfig;

  return render(
    <TestProvider auth={auth} initialEntries={initialEntries}>
      {children}
    </TestProvider>,
  );
};
