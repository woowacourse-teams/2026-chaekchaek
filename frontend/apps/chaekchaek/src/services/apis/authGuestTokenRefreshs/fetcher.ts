import { instance } from '@/services/core/http';

import type {
  PostAuthGuestTokenRefreshsRequestDto,
  PostAuthGuestTokenRefreshsResponseDto,
} from './dto';

export const postAuthGuestTokenRefreshs = async ({
  headers: { 'X-Guest-Token': guestToken },
}: PostAuthGuestTokenRefreshsRequestDto): Promise<PostAuthGuestTokenRefreshsResponseDto> => {
  const response = await instance('/api/v1/authGuestTokenRefreshs', {
    method: 'post',
    headers: {
      'X-Guest-Token': guestToken,
    },
  });

  return response.data;
};
