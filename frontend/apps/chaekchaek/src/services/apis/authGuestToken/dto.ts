import type { ResponseDto } from '@/services/apis/api.types';

export interface GetAuthGuestTokenRequestDto {}

export type GetAuthGuestTokenResponseDto = ResponseDto<{
  actorType: 'MEMBER' | 'GUEST';
  actorId: number;
  nickname: string;
  expiresAt: string;
}>;

export interface PostAuthGuestTokenRequestDto {}

export type PostAuthGuestTokenResponseDto = ResponseDto<{
  guestToken: string;
  nickname: string;
  expiresAt: string;
  actorId: number;
  actorType: 'MEMBER' | 'GUEST';
}>;
