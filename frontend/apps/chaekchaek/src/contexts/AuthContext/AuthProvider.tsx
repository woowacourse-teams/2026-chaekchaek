import { useState, useMemo, useCallback, useEffect } from 'react';

import { getMembersMe } from '@/services/apis/membersMe/repository';
import { useLoadData } from '@/services/core/useLoadData';

import { authContext } from './AuthContext';
import type { Props, UserData } from './AuthContext.types';

export const AuthProvider = ({ children }: Props) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [user, setUser] = useState<UserData | null>(null);

  const login = useCallback((userData: UserData) => {
    setIsAuthenticated(true);
    setUser(userData);
  }, []);

  const getMembersMeLoadData = useCallback(async () => {
    return await getMembersMe({});
  }, []);
  const {
    status: { data: membersMeData },
  } = useLoadData({
    queryFn: getMembersMeLoadData,
  });

  useEffect(() => {
    if (membersMeData) return login(membersMeData);
  }, [membersMeData]);

  const value = useMemo(() => ({ isAuthenticated, user, login }), [isAuthenticated, user, login]);

  return <authContext.Provider value={value}>{children}</authContext.Provider>;
};
