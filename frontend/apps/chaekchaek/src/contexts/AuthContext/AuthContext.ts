import { createContext } from 'react';

import type { GuestData, UserData } from './AuthContext.types';

type ContextValue = {
  isAuthenticated: boolean;
  user: UserData | null;
  updateAccount: (user: UserData) => void;
  guest: GuestData | null;
  updateGuestAccount: (guest: GuestData) => void;
};

export const authContext = createContext<ContextValue | undefined>(undefined);
