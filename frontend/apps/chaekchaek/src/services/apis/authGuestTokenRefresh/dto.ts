import type { ResponseDto } from '@/services/apis/api.types';

export interface PostAuthGuestTokenRefreshRequestDto {
  headers: {
    'X-Guest-Token': string;
  };
}

export type PostAuthGuestTokenRefreshResponseDto = ResponseDto<{
  nickname: string;
  guestToken: string;
  expiresAt: string;
  actorId: number;
  actorType: 'MEMBER' | 'GUEST';
}>;
