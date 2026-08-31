import type { ResponseDto } from '@/services/apis/api.types';

export interface PostAuthGuestTokenRequestDto {}

export type PostAuthGuestTokenResponseDto = ResponseDto<{
  guestToken: string;
  nickname: string;
  expiresAt: string;
}>;
