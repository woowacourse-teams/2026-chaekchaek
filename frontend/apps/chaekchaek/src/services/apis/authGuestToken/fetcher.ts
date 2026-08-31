import { instance } from '@/services/core/http';

import type { PostAuthGuestTokenRequestDto, PostAuthGuestTokenResponseDto } from './dto';

export const postAuthGuestToken =
  async ({}: PostAuthGuestTokenRequestDto): Promise<PostAuthGuestTokenResponseDto> => {
    const response = await instance('/api/v1/auth/guest-token', {
      method: 'post',
    });

    return response.data;
  };
