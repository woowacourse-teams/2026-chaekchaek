import { useState, useMemo, useCallback, useEffect } from 'react';

import { getMembersMe } from '@/services/apis/membersMe/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { postAuthGuestToken, getAuthGuestToken } from '@/services/apis/authGuestToken/repository';
import { postAuthGuestTokenRefresh } from '@/services/apis/authGuestTokenRefresh/repository';
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

  const updateAccount = useCallback((userData: UserData) => {
    setIsAuthenticated(true);
    setUser(userData);
  }, []);

  const getMembersMeLoadData = useCallback(async () => {
    return await getMembersMe({});
  }, []);
  const { status: membersMeStatus } = useLoadData({
    queryFn: getMembersMeLoadData,
  });

  const updateGuestAccount = useCallback((guestData: GuestData) => {
    setIsAuthenticated(false);
    setGuest(guestData);
  }, []);

  const { mutate: postAuthGuestTokenMutate } = useExecute({
    executeFn: postAuthGuestToken,
    onSuccess: (authGuestToken: GuestData) => {
      localStorage.setItem('guest', JSON.stringify(authGuestToken));
      updateGuestAccount(authGuestToken);
    },
  });

  const { mutate: postAuthGuestTokenRefreshMutate } = useExecute({
    executeFn: postAuthGuestTokenRefresh,
    onSuccess: (authGuestTokenRefresh: GuestData) => {
      localStorage.setItem('guest', JSON.stringify(authGuestTokenRefresh));
      updateGuestAccount(authGuestTokenRefresh);
    },
  });

  const logoutGuest = () => {
    setGuest(null);
    localStorage.removeItem('guest');
  };

  useEffect(() => {
    const initializeAuth = async () => {
      if (membersMeStatus.data) {
        updateAccount(membersMeStatus.data);
        logoutGuest();
        return;
      }

      if (
        membersMeStatus.status === 'error' &&
        membersMeStatus.error &&
        membersMeStatus.error?.status === 401
      ) {
        if (guest === null) {
          postAuthGuestTokenMutate({});
        }

        if (guest) {
          try {
            const latestGuest = await getAuthGuestToken({}, { guestToken: guest.guestToken });

            const newGuestData = { ...latestGuest, guestToken: guest.guestToken };

            localStorage.setItem('guest', JSON.stringify(newGuestData));
            updateGuestAccount(newGuestData);

            if (canRenew(latestGuest.expiresAt)) {
              postAuthGuestTokenRefreshMutate({}, { guestToken: guest.guestToken });
            }
          } catch {
            postAuthGuestTokenMutate({});
          }
        }
      }
    };

    initializeAuth();
  }, [membersMeStatus]);

  const value = useMemo(
    () => ({ isAuthenticated, user, updateAccount, guest, updateGuestAccount }),
    [isAuthenticated, user, updateAccount, guest, updateGuestAccount],
  );

  return <authContext.Provider value={value}>{children}</authContext.Provider>;
};
