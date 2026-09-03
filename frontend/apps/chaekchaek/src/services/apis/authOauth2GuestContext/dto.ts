import type { ResponseDto } from '@/services/apis/api.types';

export interface PostAuthOauth2GuestContextRequestDto {
  headers: { 'X-Guest-Token': string };
}

export type PostAuthOauth2GuestContextResponseDto = ResponseDto<undefined>;
