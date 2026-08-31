import { useState, useMemo, useCallback, useEffect } from 'react';

import { getMembersMe } from '@/services/apis/membersMe/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { postAuthGuestToken } from '@/services/apis/authGuestToken/repository';
import { useExecute } from '@/services/core/useExecute';

import { authContext } from './AuthContext';
import type { Props, UserData, GuestData } from './AuthContext.types';

export const AuthProvider = ({ children }: Props) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [user, setUser] = useState<UserData | null>(null);
  const [guest, setGuest] = useState<GuestData | null>(null);

  const login = useCallback((userData: UserData) => {
    setIsAuthenticated(true);
    setUser(userData);
  }, []);

  const getMembersMeLoadData = useCallback(async () => {
    return await getMembersMe({});
  }, []);
  const { status: membersMeStatus } = useLoadData({
    queryFn: getMembersMeLoadData,
  });

  const guestLogin = useCallback((guestData: GuestData) => {
    setIsAuthenticated(false);
    setGuest(guestData);
  }, []);

  const {
    mutate: postAuthGuestTokenMutate,
    status: { data: authGuestToken },
  } = useExecute({
    executeFn: postAuthGuestToken,
  });

  useEffect(() => {
    if (membersMeStatus.data) return login(membersMeStatus.data);

    if (
      membersMeStatus.status === 'error' &&
      membersMeStatus.error &&
      membersMeStatus.error?.status === 401
    ) {
      postAuthGuestTokenMutate({});
    }
  }, [membersMeStatus]);

  useEffect(() => {
    if (authGuestToken) {
      guestLogin(authGuestToken);
    }
  }, [authGuestToken]);

  const value = useMemo(
    () => ({ isAuthenticated, user, login, guest, guestLogin }),
    [isAuthenticated, user, login, guest, guestLogin],
  );

  return <authContext.Provider value={value}>{children}</authContext.Provider>;
};
