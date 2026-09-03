import { instance } from '@/services/core/http';

import type {
  PostAuthOauth2GuestContextRequestDto,
  PostAuthOauth2GuestContextResponseDto,
} from './dto';

export const postAuthOauth2GuestContext = async ({
  headers: { 'X-Guest-Token': guestToken },
}: PostAuthOauth2GuestContextRequestDto): Promise<PostAuthOauth2GuestContextResponseDto> => {
  const response = await instance('/api/v1/auth/oauth2/guest-context', {
    method: 'post',
    headers: { 'X-Guest-Token': guestToken },
  });

  return response.data;
};
