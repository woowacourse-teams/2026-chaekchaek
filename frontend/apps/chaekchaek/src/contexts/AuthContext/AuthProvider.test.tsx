import { renderHook, waitFor } from '@testing-library/react';

import { describe, it, expect } from 'vitest';

import { server } from '@/mocks/msw/server';
import { http, HttpResponse } from 'msw';

import { ENV } from '@/configs/env';

import { AuthProvider } from './AuthProvider';
import type { UserData } from './AuthContext.types';
import { useAuthContext } from './useAuthContext';

describe('AuthProvider', () => {
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
});
