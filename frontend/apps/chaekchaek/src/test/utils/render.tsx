import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';

import { render } from '@testing-library/react';

import { authContext } from '@/contexts/AuthContext';
import type { AuthContextValue } from '@/contexts/AuthContext';

export const defaultNonLoggedAuthContextValue = {
  isAuthenticated: false,
  user: null,
  updateAccount: () => {},
  guest: null,
  updateGuestAccount: () => {},
};

const user = {
  accountStatus: 'ACTIVE',
  anonymousNickname: '의욕적인 희뿌연 참새',
  displayAnonymous: false,
  memberId: 3,
  nickname: '먼지',
  profileImageUrl:
    'https://lh3.googleusercontent.com/a/ACg8ocIp6Hr4YvPjE_Fi00Bw-ACYdgnl0L2W74CycWwW4dGYPjOICDk=s96-c',
};

export const defaultLoggedAuthContextValue = {
  isAuthenticated: true,
  user: user,
  updateAccount: () => {},
  guest: null,
  updateGuestAccount: () => {},
};

const defaultAuthContextValue = defaultNonLoggedAuthContextValue;

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
