import { createContext } from 'react';

import type { UserData } from './AuthContext.types';

type ContextValue = {
  isAuthenticated: boolean;
  user: UserData | null;
  login: (user: UserData) => void;
};

export const authContext = createContext<ContextValue | undefined>(undefined);
