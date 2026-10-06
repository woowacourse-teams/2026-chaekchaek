import { instance } from '@/services/core/http';

import type {
  PostAuthGuestTokenRefreshRequestDto,
  PostAuthGuestTokenRefreshResponseDto,
} from './dto';

export const postAuthGuestTokenRefresh = async ({
  headers: { 'X-Guest-Token': guestToken },
}: PostAuthGuestTokenRefreshRequestDto): Promise<PostAuthGuestTokenRefreshResponseDto> => {
  const response = await instance('/api/v1/auth/guest-token/refresh', {
    method: 'post',
    headers: {
      'X-Guest-Token': guestToken,
    },
  });

  return response.data;
};
