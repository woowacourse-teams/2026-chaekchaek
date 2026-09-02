import type { ResponseDto } from '@/services/apis/api.types';

export interface PostAuthGuestTokenRefreshsRequestDto {
  headers: {
    'X-Guest-Token': string;
  };
}

export type PostAuthGuestTokenRefreshsResponseDto = ResponseDto<{
  nickname: string;
  guestToken: string;
  expiresAt: string;
}>;
