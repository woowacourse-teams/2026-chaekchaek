import type { ReactNode } from 'react';

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
}

export const TestProvider = ({ children, auth = defaultAuthContextValue }: TestProviderProps) => {
  return <authContext.Provider value={auth}>{children}</authContext.Provider>;
};

export const renderProvider = (
  children: ReactNode,
  { auth }: { auth: AuthContextValue } = {
    auth: defaultAuthContextValue,
  },
) => {
  return render(<TestProvider auth={auth}>{children}</TestProvider>);
};
