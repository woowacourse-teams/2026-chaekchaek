import { renderHook, waitFor } from '@testing-library/react';

import { afterEach, beforeEach, describe, it, expect } from 'vitest';

import { server } from '@/mocks/msw/server';
import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';

import { AuthProvider } from './AuthProvider';
import type { GuestData, UserData } from './AuthContext.types';
import { useAuthContext } from './useAuthContext';

describe('AuthProvider', () => {
  beforeEach(() => {
    localStorage.removeItem('guest');
  });

  afterEach(() => {
    localStorage.removeItem('guest');
  });

  it('사용자가 이미 로그인을 한 경우 인증된 상태로 사용자 정보를 제공한다', async () => {
    const user: UserData = {
      accountStatus: 'ACTIVE',
      nickname: '책책이',
      profileImageUrl: 'https://example.com/profile.png',
      displayAnonymous: false,
      anonymousNickname: '익명의 독서가',
      memberId: 1,
      actorId: 1,
      actorType: 'MEMBER',
    };

    server.use(
      http.get(`${ENV.APP_API_URL}/api/v1/members/me`, () => {
        return HttpResponse.json(user);
      }),
    );

    const { result } = renderHook(() => useAuthContext(), { wrapper: AuthProvider });

    await waitFor(() => {
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.user).toEqual(user);
      expect(result.current.guest).toBeNull();
    });
  });

  describe('회원 로그인이 되어 있지 않은 경우', () => {
    beforeEach(() => {
      server.use(
        http.get(`${ENV.APP_API_URL}/api/v1/members/me`, () => {
          return HttpResponse.json({ status: 401 }, { status: 401 });
        }),
        http.post(`${ENV.APP_API_URL}/api/v1/auth/reissue`, () => {
          return HttpResponse.json({ status: 401 }, { status: 401 });
        }),
      );
    });

    it('사용자가 guest 로그인을 한 경우 게스트로 인증된 상태로 게스트 정보를 제공한다', async () => {
      const guest: GuestData = {
        guestToken: 'existing-guest-token',
        nickname: '기존 게스트',
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
        actorId: 1,
        actorType: 'GUEST',
      };
      localStorage.setItem('guest', JSON.stringify(guest));

      const requestedGuestTokens: (string | null)[] = [];
      let issueRequestCount = 0;

      server.use(
        http.get(`${ENV.APP_API_URL}/api/v1/auth/guest-token`, ({ request }) => {
          requestedGuestTokens.push(request.headers.get('X-Guest-Token'));
          const { guestToken, ...guestInfo } = guest;
          return HttpResponse.json(guestInfo);
        }),
        http.post(`${ENV.APP_API_URL}/api/v1/auth/guest-token`, () => {
          issueRequestCount += 1;
          return HttpResponse.json(guest);
        }),
      );

      const { result } = renderHook(() => useAuthContext(), { wrapper: AuthProvider });
      const initialGuest = result.current.guest;

      expect(initialGuest).toEqual(guest);

      await waitFor(() => {
        expect(requestedGuestTokens).toEqual([guest.guestToken]);
        expect(result.current.guest).toEqual(guest);
        expect(result.current.guest).not.toBe(initialGuest);
      });

      expect(issueRequestCount).toBe(0);
      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });

    it('사용자가 guest 로그인을 안한 경우 게스트 로그인을 요청하고 게스트 정보를 제공한다', async () => {
      const guest: GuestData = {
        guestToken: 'new-guest-token',
        nickname: '새 게스트',
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
        actorId: 2,
        actorType: 'GUEST',
      };
      let issueRequestCount = 0;

      server.use(
        http.post(`${ENV.APP_API_URL}/api/v1/auth/guest-token`, () => {
          issueRequestCount += 1;
          return HttpResponse.json(guest);
        }),
      );

      expect(localStorage.getItem('guest')).toBeNull();

      const { result } = renderHook(() => useAuthContext(), { wrapper: AuthProvider });

      await waitFor(() => {
        expect(result.current.guest).toEqual(guest);
      });

      expect(issueRequestCount).toBe(1);
      expect(JSON.parse(localStorage.getItem('guest')!)).toEqual(guest);
      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });
  });
});
