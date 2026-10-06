import { instance } from '@/services/core/http';

import type { GetAuthGuestTokenRequestDto, GetAuthGuestTokenResponseDto } from './dto';

export const getAuthGuestToken =
  async ({}: GetAuthGuestTokenRequestDto): Promise<GetAuthGuestTokenResponseDto> => {
    const response = await instance('/api/v1/auth/guest-token', {
      method: 'get',
    });

    return response.data;
  };

import type { PostAuthGuestTokenRequestDto, PostAuthGuestTokenResponseDto } from './dto';

export const postAuthGuestToken =
  async ({}: PostAuthGuestTokenRequestDto): Promise<PostAuthGuestTokenResponseDto> => {
    const response = await instance('/api/v1/auth/guest-token', {
      method: 'post',
    });

    return response.data;
  };
