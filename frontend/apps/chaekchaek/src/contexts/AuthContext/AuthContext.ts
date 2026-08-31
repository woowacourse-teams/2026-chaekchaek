import { createContext } from 'react';

import type { GuestData, UserData } from './AuthContext.types';

type ContextValue = {
  isAuthenticated: boolean;
  user: UserData | null;
  login: (user: UserData) => void;
  guest: GuestData | null;
  guestLogin: (guest: GuestData) => void;
};

export const authContext = createContext<ContextValue | undefined>(undefined);
