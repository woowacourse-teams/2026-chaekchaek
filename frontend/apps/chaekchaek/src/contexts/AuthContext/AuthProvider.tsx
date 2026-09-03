import { useState, useMemo, useCallback, useEffect, useRef } from 'react';

import { getMembersMe } from '@/services/apis/membersMe/repository';
import { useLoadData } from '@/services/core/useLoadData';
import { postAuthGuestToken } from '@/services/apis/authGuestToken/repository';
import { postAuthGuestTokenRefreshs } from '@/services/apis/authGuestTokenRefreshs/repository';
import { postAuthOauth2GuestContext } from '@/services/apis/authOauth2GuestContext/repository';
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

  const refLink = useRef<null | string>(null);

  const { mutate: postAuthOauth2GuestContextMutate } = useExecute({
    executeFn: postAuthOauth2GuestContext,
    onSuccess: () => {
      if (refLink.current) window.location.href = refLink.current;
    },
  });

  const login = useCallback(
    async (link: string) => {
      refLink.current = link;

      if (!guest) {
        window.location.href = link;
        return;
      }

      await postAuthOauth2GuestContextMutate(
        {},
        {
          guestToken: guest.guestToken,
        },
      );
    },
    [guest, postAuthOauth2GuestContextMutate],
  );

  useEffect(() => {
    if (membersMeStatus.data) return updateAccount(membersMeStatus.data);

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
      updateGuestAccount(authGuestToken);
    }
  }, [authGuestToken]);

  useEffect(() => {
    if (authGuestTokenRefreshs) {
      localStorage.setItem('guest', JSON.stringify(authGuestTokenRefreshs));
      updateGuestAccount(authGuestTokenRefreshs);
    }
  }, [authGuestTokenRefreshs]);

  const value = useMemo(
    () => ({ isAuthenticated, login, user, updateAccount, guest, updateGuestAccount }),
    [isAuthenticated, login, user, updateAccount, guest, updateGuestAccount],
  );

  return <authContext.Provider value={value}>{children}</authContext.Provider>;
};
