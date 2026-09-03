import { useState, useMemo, useCallback, useEffect } from 'react';

import { getMembersMe } from '@/services/apis/membersMe/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { postAuthGuestToken } from '@/services/apis/authGuestToken/repository';
import { postAuthGuestTokenRefreshs } from '@/services/apis/authGuestTokenRefreshs/repository';
import { useExecute } from '@/services/core/useExecute';
import { RequestAjaxError } from '@/services/core/http/requestAjaxError';

import { authContext } from './AuthContext';
import type { Props, UserData, GuestData } from './AuthContext.types';

const RENEWABLE_BEFORE_MS = 14 * 24 * 60 * 60 * 1000;

const canRenew = (expiresAt: string) => {
  const remaining = new Date(expiresAt).getTime() - Date.now();

  return remaining <= RENEWABLE_BEFORE_MS;
};

export const AuthProvider = ({ children }: Props) => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [user, setUser] = useState<UserData | null>(null);

  const guestStorageString = localStorage.getItem('guest');
  const guestStorage =
    guestStorageString && guestStorageString !== null ? JSON.parse(guestStorageString) : null;
  const [guest, setGuest] = useState<GuestData | null>(guestStorage || null);

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

  const {
    mutate: postAuthGuestTokenRefreshsMutate,
    status: { data: authGuestTokenRefreshs },
  } = useExecute({
    executeFn: postAuthGuestTokenRefreshs,
  });

  useEffect(() => {
    if (membersMeStatus.data) return login(membersMeStatus.data);

    if (
      membersMeStatus.status === 'error' &&
      membersMeStatus.error &&
      membersMeStatus.error?.status === 401
    ) {
      if (guest === null) {
        postAuthGuestTokenMutate({});
      }

      if (guest) {
        if (canRenew(guest.expiresAt)) {
          postAuthGuestTokenRefreshsMutate({}, { guestToken: 'ss' });
        }
      }
    }
  }, [membersMeStatus]);

  useEffect(() => {
    if (authGuestToken) {
      localStorage.setItem('guest', JSON.stringify(authGuestToken));
      guestLogin(authGuestToken);
    }
  }, [authGuestToken]);

  useEffect(() => {
    if (authGuestTokenRefreshs) {
      localStorage.setItem('guest', JSON.stringify(authGuestTokenRefreshs));
      guestLogin(authGuestTokenRefreshs);
    }
  }, [authGuestTokenRefreshs]);

  const value = useMemo(
    () => ({ isAuthenticated, user, login, guest, guestLogin }),
    [isAuthenticated, user, login, guest, guestLogin],
  );

  return <authContext.Provider value={value}>{children}</authContext.Provider>;
};
