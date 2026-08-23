import { useState, useMemo, useCallback } from 'react';

import { authContext } from './AuthContext';
import type { Props, UserData } from './AuthContext.types';

export const AuthProvider = ({ children }: Props) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [user, setUser] = useState<UserData | null>(null);

  const login = useCallback((userData: UserData) => {
    setIsAuthenticated(true);
    setUser(userData);
  }, []);

  const value = useMemo(() => ({ isAuthenticated, user, login }), [isAuthenticated, login]);

  return <authContext.Provider value={value}>{children}</authContext.Provider>;
};
