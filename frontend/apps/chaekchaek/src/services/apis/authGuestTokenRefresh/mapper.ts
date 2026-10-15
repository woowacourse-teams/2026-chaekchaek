import type { PostAuthGuestTokenRefreshResponseDto } from './dto';
import type { PostAuthGuestTokenRefreshCommand } from './repository.types';

// PostAuthGuestTokenRefresh
export const mapPostAuthGuestTokenRefreshModelToRequestDTO = (
  model: PostAuthGuestTokenRefreshCommand,
): PostAuthGuestTokenRefreshCommand => {
  return model;
};

export const mapPostAuthGuestTokenRefreshResponseDTOToModel = (
  response: PostAuthGuestTokenRefreshResponseDto,
) => {
  return response;
};
